package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {
    
    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color MOON_WHITE = Color.fromRGB(255, 250, 240);
    private static final Color CRESCENT_SILVER = Color.fromRGB(200, 200, 220);
    
    private final GoldenMoon plugin;
    private final ArmorManager armorManager;
    private final Map<UUID, LunarPlayerData> playerData = new HashMap<>();
    
    private static final int MAX_COMBO = 3;
    private static final long COMBO_WINDOW_MS = 1200;
    private static final int MAX_LUNAR_GAUGE = 100;
    private static final int GAUGE_PER_HIT = 15;
    private static final int GAUGE_PER_PARRY = 20;
    private static final long PARRY_WINDOW_MS = 300;
    
    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        this.armorManager = plugin.getArmorManager();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isLunarBlade(p)) return;        if (!(e.getEntity() instanceof LivingEntity target)) return;
        
        // ✅ FIX: Cancel jika blocking
        if (isBlockingWithShield(p)) { 
            e.setCancelled(true); 
            return; 
        }
        
        // ✅ FIX: Jangan process damage berkali-kali
        if (target.hasMetadata("lunarHit_" + p.getUniqueId().toString().substring(0, 8))) {
            return;
        }
        target.setMetadata("lunarHit_" + p.getUniqueId().toString().substring(0, 8), new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override
            public void run() {
                target.removeMetadata("lunarHit_" + p.getUniqueId().toString().substring(0, 8), plugin);
            }
        }.runTaskLater(plugin, 10);
        
        LunarPlayerData data = getData(p);
        data.addGauge(GAUGE_PER_HIT);
        sendGaugeUpdate(p, data);
        
        long now = System.currentTimeMillis();
        if (now - data.lastHitTime < COMBO_WINDOW_MS) {
            data.combo++;
            if (data.combo > MAX_COMBO) data.combo = MAX_COMBO;
            executeComboSkill(p, target, data.combo);
        } else {
            data.combo = 1;
            executeBasicStrike(p, target);  // ✅ FIX: Basic strike ringan
        }
        data.lastHitTime = now;
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!hasLunarShield(p)) return;
        LunarPlayerData data = getData(p);
        if (data.lastBlockStart > 0 && System.currentTimeMillis() - data.lastBlockStart <= PARRY_WINDOW_MS) {
            e.setCancelled(true);
            data.addGauge(GAUGE_PER_PARRY);
            sendGaugeUpdate(p, data);
            executePerfectParry(p);
            data.lastBlockStart = 0;
            data.isBlocking = false;
            return;
        }        if (data.isBlocking) {
            e.setDamage(e.getDamage() * 0.2);
        }
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        
        boolean hasShield = hasLunarShield(p);
        
        // ✅ PRIORITAS: Shield blocking
        if (hasShield && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            e.setCancelled(true);
            LunarPlayerData data = getData(p);
            data.isBlocking = true;
            data.lastBlockStart = System.currentTimeMillis();
            startBlockEffect(p);
            return;
        }
        
        // ✅ Ultimate charge (hanya sword, tanpa shield)
        if (!hasShield && isLunarBlade(p) && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            e.setCancelled(true);
            LunarPlayerData data = getData(p);
            
            if (data.lunarGauge >= MAX_LUNAR_GAUGE && !data.isCharging) {
                data.isCharging = true;
                data.chargeTicks = 0;
                startChargeEffect(p);
            } else if (data.isCharging) {
                data.isCharging = false;
                if (data.chargeTicks >= 20) {
                    executeLunarEclipse(p);
                    data.chargeTicks = 0;
                    data.lunarGauge = 0;
                    sendGaugeUpdate(p, data);
                }
            } else {
                sendActionBar(p, "§7✦ §eCharge Lunar Gauge: §f" + data.lunarGauge + "§7/§f" + MAX_LUNAR_GAUGE);
            }
        }
        
        // ✅ Shield bash
        if (hasShield && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && getData(p).isBlocking) {
            e.setCancelled(true);
            executeShieldBash(p);
        }
    }    
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        LunarPlayerData data = getData(p);
        if (data.isCharging) {
            data.chargeTicks++;
            updateChargeVisual(p, data.chargeTicks);
            if (data.chargeTicks > 100) { 
                data.isCharging = false; 
                data.chargeTicks = 0; 
                sendActionBar(p, "§c✦ Charge cancelled"); 
            }
        }
        if (armorManager.tryMoonStep(p)) { 
            data.moonStepReady = false; 
            new BukkitRunnable() { 
                @Override 
                public void run() { 
                    data.moonStepReady = true; 
                }
            }.runTaskLater(plugin, 60); 
        }
    }
    
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        if (!e.isSneaking()) return;
        LunarPlayerData data = getData(p);
        if (data.isBlocking) { 
            data.isBlocking = false; 
            data.lastBlockStart = 0; 
        }
    }
    
    // ✅ FIX: BASIC STRIKE - RINGAN, NO LAG
    private void executeBasicStrike(Player p, LivingEntity target) {
        // Damage kecil (1.5 hearts = 3 HP)
        double damage = 3.0;
        if (getData(p).parryBonus) {
            damage = 5.0;  // Bonus damage setelah parry
        }
        target.damage(damage, p);
        
        // Knockback minimal
        Vector kb = p.getLocation().getDirection().multiply(0.15).setY(0.05);
        target.setVelocity(kb);        
        // ✅ FIX: Particle minimal (cuma 2 particles)
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.1, 0.2, 0.1, 0);
        
        // Sound ringan
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.3f, 1.0f);
    }
    
    private void executeComboSkill(Player p, LivingEntity target, int combo) {
        final Player finalP = p;
        final LivingEntity finalTarget = target;
        final World world = p.getWorld();
        
        if (combo == 2) {
            world.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.4f, 1.4f);
            new BukkitRunnable() { 
                @Override 
                public void run() {
                    finalTarget.damage(4.0, finalP);
                    finalTarget.setVelocity(new Vector(0, 0.6, 0));
                    try { 
                        finalTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, false)); 
                    } catch(Exception ignored) {}
                    
                    // ✅ FIX: Particle lebih sedikit
                    Location impactLoc = finalTarget.getLocation().add(0, 0.5, 0);
                    world.spawnParticle(Particle.DUST, impactLoc, 8, new Particle.DustOptions(CRESCENT_SILVER, 1.2f));
                    world.spawnParticle(Particle.FLAME, impactLoc, 4, 0.2f, 0.2f, 0.2f, 0.05f);
                }
            }.runTaskLater(plugin, 4L);
            
        } else if (combo == 3) {
            world.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.5f);
            Vector dir = p.getLocation().getDirection().setY(0).normalize();
            
            new BukkitRunnable() {
                int dashTicks = 0;
                final Vector dashVel = dir.clone().multiply(1.8);
                @Override 
                public void run() {
                    if (dashTicks >= 10) { 
                        executeStarfallSlash(finalP, dir); 
                        this.cancel(); 
                        return; 
                    }
                    finalP.setVelocity(dashVel.clone().setY(0.05));
                    dashTicks++;
                }
            }.runTaskTimer(plugin, 0, 1);        }
        getData(p).combo = 0;
    }
    
    private void executeStarfallSlash(Player p, Vector direction) {
        World world = p.getWorld();
        Location slashLoc = p.getLocation().add(0, 1, 0);
        
        // ✅ FIX: Particle lebih efisien
        for(double angle = -60; angle <= 60; angle += 15) {
            double rad = Math.toRadians(angle);
            Vector offset = new Vector(Math.cos(rad) * 2.5, 0, Math.sin(rad) * 2.5);
            Location particleLoc = slashLoc.clone().add(offset.rotateAroundY(Math.toRadians(90)));
            world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(MOON_WHITE, 1.5f));
        }
        
        world.playSound(slashLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 1.4f);
        
        // ✅ FIX: Damage kecil, no lag
        world.getNearbyEntities(slashLoc, 3.5, 2.0, 3.5).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                Vector toEnemy = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize();
                double dot = toEnemy.dot(direction);
                if (dot > 0.2) {
                    le.damage(2.0, p);
                    le.setVelocity(direction.clone().multiply(0.6).setY(0.3));
                    world.spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0);
                }
            }
        });
    }

    private void startBlockEffect(Player p) {
        sendActionBar(p, "§b✦ §fBlocking... §7(Release on hit to Parry!)");
        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_PLACE, 0.2f, 1.0f);
    }
    
    private void executePerfectParry(Player p) {
        World world = p.getWorld();
        Location parryLoc = p.getLocation().add(0, 1, 0);
        
        world.spawnParticle(Particle.FLASH, parryLoc, 1);
        world.spawnParticle(Particle.DUST, parryLoc, 15, new Particle.DustOptions(GOLD, 2.0f));
        
        world.playSound(parryLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.8f);
        p.playSound(parryLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 2.0f);
        
        p.setInvulnerable(true);
        sendActionBar(p, "§a✦ §fPERFECT PARRY!");
                new BukkitRunnable() { 
            @Override 
            public void run() {
                p.setInvulnerable(false);
                getData(p).parryBonus = true;
                new BukkitRunnable() { 
                    @Override 
                    public void run() { 
                        getData(p).parryBonus = false; 
                    }
                }.runTaskLater(plugin, 40L);
            }
        }.runTaskLater(plugin, 8L);
    }
    
    private void executeShieldBash(Player p) {
        World world = p.getWorld();
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        
        world.playSound(p.getLocation(), Sound.BLOCK_ANVIL_HIT, 0.4f, 0.8f);
        
        world.getNearbyEntities(p.getLocation(), 2.0, 1.5, 2.0).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                le.damage(1.0, p);
                le.setVelocity(dir.clone().multiply(0.8).setY(0.2));
                try {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 2, false, false));
                } catch(Exception ignored) {}
            }
        });
        
        getData(p).addGauge(8);
        sendGaugeUpdate(p, getData(p));
    }

    private void executeLunarEclipse(Player p) {
        final Player finalP = p;
        final World world = p.getWorld();
        final Location center = p.getLocation().clone();
        
        boolean isElite = armorManager.hasFullEliteSet(p);
        String title = isElite ? "§f§l🌑" : "§f§l🌕";
        String subtitle = isElite ? "§7§lLUNAR ECLIPSE" : "§6§lGOLDEN MOON BLESSING";
        
        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.0f);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.9f);
        p.sendTitle(title, subtitle, 5, 20, 10);
        
        p.setVelocity(new Vector(0, 0.3, 0));
        p.setInvulnerable(true);        
        new BukkitRunnable() {
            int phase = 0;
            @Override 
            public void run() {
                if (phase == 0) { 
                    spawnBlessingArena(center, world, isElite ? Color.fromRGB(100, 100, 150) : GOLD); 
                    world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 0.7f);
                } else if (phase == 1) { 
                    spawnRisingCrescent(center, world); 
                    world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.2f);
                } else if (phase == 2) { 
                    executeBlessingImpact(finalP, center, world, isElite);
                } else { 
                    p.setInvulnerable(false); 
                    this.cancel(); 
                    return; 
                }
                phase++;
            }
        }.runTaskTimer(plugin, 0, 25);
    }
    
    private void executeBlessingImpact(Player p, Location center, World world, boolean isElite) {
        double radius = isElite ? 12.0 : 8.0;
        
        for(Player viewer : center.getWorld().getPlayers()) {
            if(viewer.getLocation().distance(center) < radius + 5) {
                viewer.sendTitle("§f§l✦", "§b§lBlessing", 2, 10, 3);
            }
        }
        
        // ✅ FIX: Particle lebih efisien
        world.spawnParticle(Particle.DUST, center, 40, (int)(radius/2), 1, (int)(radius/2), 0.1f, new Particle.DustOptions(MOON_WHITE, 1.8f));
        world.spawnParticle(Particle.FLAME, center, 20, (int)(radius/3), 0.6f, (int)(radius/3), 0.1f);
        
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 0.9f);
        world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.8f);
        
        // ✅ FIX: Damage lebih kecil, no lag
        world.getNearbyEntities(center, radius, radius, radius).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                le.damage(isElite ? 5.0 : 3.0, p);
                le.setVelocity(new Vector(0, 0.8, 0));
                try {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 80, 0, false, false));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, false, false));
                } catch(Exception ignored) {}
            }
        });        
        try {
            p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 180, isElite ? 1 : 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, false, false));
        } catch(Exception ignored) {}
    }
    
    private void spawnBlessingArena(Location center, World world, Color color) {
        for(int corner = 0; corner < 6; corner++) {
            double baseAngle = corner * 60;
            double angle = Math.toRadians(baseAngle);
            Location cornerLoc = center.clone().add(Math.cos(angle) * 8, 0.2, Math.sin(angle) * 8);
            world.spawnParticle(Particle.DUST, cornerLoc, 1, new Particle.DustOptions(color, 1.5f));
        }
    }
    
    private void spawnRisingCrescent(Location center, World world) {
        Location crescentCenter = center.clone().add(0, 8, 0);
        world.spawnParticle(Particle.DUST, crescentCenter, 4, 0.3f, 0.5f, 0.3f, 0, new Particle.DustOptions(MOON_WHITE, 2.0f));
    }

    private void startChargeEffect(Player p) {
        sendActionBar(p, "§e§l✦ CHARGING... §7[▮▮▮▮▮]");
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.0f);
    }
    
    private void updateChargeVisual(Player p, int chargeTicks) {
        if(chargeTicks % 15 == 0) {
            float pitch = 0.9f + (chargeTicks * 0.01f);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.2f, Math.min(pitch, 1.8f));
        }
    }
    
    private void sendGaugeUpdate(Player p, LunarPlayerData data) {
        int bars = (int) Math.ceil(data.lunarGauge / 20.0);
        String bar = "§7[§f" + "▮".repeat(bars) + "§7" + "▯".repeat(5 - bars) + "]";
        sendActionBar(p, "§b✦ Gauge: §7" + bar + " §f" + data.lunarGauge + "%");
    }

    private boolean hasLunarShield(Player p) {
        ItemStack offhand = p.getInventory().getItemInOffHand();
        return offhand != null && offhand.hasItemMeta() && 
               offhand.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE);
    }
    
    private boolean isBlockingWithShield(Player p) { 
        return hasLunarShield(p) && getData(p).isBlocking; 
    }
        private LunarPlayerData getData(Player p) { 
        return playerData.computeIfAbsent(p.getUniqueId(), k -> new LunarPlayerData()); 
    }
    
    private boolean isLunarBlade(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
    
    private void sendActionBar(Player p, String msg) { 
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg)); 
    }
    
    private static class LunarPlayerData {
        int combo = 0; 
        long lastHitTime = 0; 
        int lunarGauge = 0;
        boolean isCharging = false; 
        int chargeTicks = 0;
        boolean isBlocking = false; 
        long lastBlockStart = 0;
        boolean parryBonus = false; 
        boolean moonStepReady = true;
        
        void addGauge(int amount) { 
            lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount); 
        }
    }
            }
