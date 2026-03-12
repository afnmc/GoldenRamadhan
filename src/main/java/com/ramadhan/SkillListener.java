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
    
    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        this.armorManager = plugin.getArmorManager();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isLunarBlade(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;
        
        // Anti-spam hit - FIX: final variable for inner class        final String hitKey = "lunarHit_" + p.getUniqueId().toString().substring(0, 8);
        if (target.hasMetadata(hitKey)) return;
        target.setMetadata(hitKey, new FixedMetadataValue(plugin, true));
        new BukkitRunnable() {
            @Override
            public void run() {
                target.removeMetadata(hitKey, plugin);
            }
        }.runTaskLater(plugin, 10);
        
        LunarPlayerData data = getData(p);
        data.addGauge(GAUGE_PER_HIT);
        sendGaugeUpdate(p, data);
        
        long now = System.currentTimeMillis();
        
        // SKILL 2: Sneak + Hit = Dash Slash
        if (p.isSneaking()) {
            executeSkill2_DashSlash(p, target);
            data.lastHitTime = now;
            return;
        }
        
        // SKILL 1: Basic Combo
        if (now - data.lastHitTime < COMBO_WINDOW_MS) {
            data.combo++;
            if (data.combo > MAX_COMBO) data.combo = MAX_COMBO;
            executeSkill1_Combo(p, target, data.combo);
        } else {
            data.combo = 1;
            executeBasicStrike(p, target);
        }
        data.lastHitTime = now;
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (hasLunarShield(p)) {
            e.setDamage(e.getDamage() * 0.9);
            if (new Random().nextInt(100) < 30) {
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 3, 
                    new Particle.DustOptions(CRESCENT_SILVER, 1.2f));
            }
        }
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();        if (!isLunarBlade(p)) return;
        
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            LunarPlayerData data = getData(p);
            
            if (data.lunarGauge >= MAX_LUNAR_GAUGE && !data.isCharging) {
                data.isCharging = true;
                data.chargeTicks = 0;
                startChargeEffect(p);
            } else if (data.isCharging) {
                data.isCharging = false;
                if (data.chargeTicks >= 20) {
                    executeSkill3_Ultimate(p);
                    data.chargeTicks = 0;
                    data.lunarGauge = 0;
                    sendGaugeUpdate(p, data);
                }
            } else {
                sendGaugeUpdate(p, data);
            }
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
            }
        }
        
        if (armorManager.tryMoonStep(p)) { 
            data.moonStepReady = false; 
            new BukkitRunnable() { 
                @Override 
                public void run() { data.moonStepReady = true; }
            }.runTaskLater(plugin, 60); 
        }
    }

    // ===== SKILL 1: BASIC COMBO =====
    private void executeBasicStrike(Player p, LivingEntity target) {        target.damage(3.0, p);
        target.setVelocity(p.getLocation().getDirection().multiply(0.15).setY(0.05));
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.1, 0.2, 0.1, 0);
        target.getWorld().playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.3f, 1.0f);
    }
    
    private void executeSkill1_Combo(Player p, LivingEntity target, int combo) {
        final World world = p.getWorld();
        if (combo == 2) {
            world.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.4f, 1.4f);
            new BukkitRunnable() { 
                @Override 
                public void run() {
                    target.damage(4.0, p);
                    target.setVelocity(new Vector(0, 0.6, 0));
                    try { target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, false)); } catch(Exception ignored) {}
                    Location loc = target.getLocation().add(0, 0.5, 0);
                    world.spawnParticle(Particle.DUST, loc, 10, new Particle.DustOptions(CRESCENT_SILVER, 1.2f));
                    world.spawnParticle(Particle.FLAME, loc, 5, 0.2f, 0.2f, 0.2f, 0.05f);
                }
            }.runTaskLater(plugin, 4L);
        } else if (combo == 3) {
            world.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.5f);
            final Vector dir = p.getLocation().getDirection().setY(0).normalize();
            new BukkitRunnable() {
                int ticks = 0;
                @Override 
                public void run() {
                    if (ticks >= 10) { 
                        executeCombo3Slash(p, dir); 
                        this.cancel(); 
                        return; 
                    }
                    p.setVelocity(dir.clone().multiply(1.8).setY(0.05));
                    Location trail = p.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.DUST, trail, 2, new Particle.DustOptions(MOON_WHITE, 1.5f));
                    ticks++;
                }
            }.runTaskTimer(plugin, 0, 1);
        }
        getData(p).combo = 0;
    }
    
    private void executeCombo3Slash(Player p, Vector direction) {
        World world = p.getWorld();
        Location slashLoc = p.getLocation().add(0, 1, 0);
        for(double angle = -60; angle <= 60; angle += 12) {
            double rad = Math.toRadians(angle);
            Vector offset = new Vector(Math.cos(rad) * 2.5, 0, Math.sin(rad) * 2.5);            Location particleLoc = slashLoc.clone().add(offset.rotateAroundY(Math.toRadians(90)));
            world.spawnParticle(Particle.DUST, particleLoc, 2, new Particle.DustOptions(MOON_WHITE, 1.5f));
        }
        world.playSound(slashLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 1.4f);
        world.getNearbyEntities(slashLoc, 3.5, 2.0, 3.5).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                Vector toEnemy = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize();
                if (toEnemy.dot(direction) > 0.2) {
                    le.damage(2.5, p);
                    le.setVelocity(direction.clone().multiply(0.6).setY(0.3));
                }
            }
        });
    }

    // ===== SKILL 2: DASH SLASH (Sneak+Hit) =====
    private void executeSkill2_DashSlash(Player p, LivingEntity target) {
        final World world = p.getWorld();
        final Vector dir = p.getLocation().getDirection().setY(0).normalize();
        p.setVelocity(dir.clone().multiply(2.5).setY(0.3));
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 12) {
                    Location impactLoc = p.getLocation().add(0, 1, 0);
                    world.spawnParticle(Particle.EXPLOSION, impactLoc, 1);
                    world.spawnParticle(Particle.DUST, impactLoc, 20, new Particle.DustOptions(GOLD, 2.0f));
                    world.spawnParticle(Particle.FLAME, impactLoc, 15, 0.3f, 0.3f, 0.3f, 0.1f);
                    world.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.6f);
                    
                    target.damage(6.0, p);
                    target.setVelocity(dir.clone().multiply(1.0).setY(0.5));
                    
                    world.getNearbyEntities(impactLoc, 3.0, 2.5, 3.0).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(p) && !en.equals(target)) {
                            le.damage(4.0, p);
                            le.setVelocity(dir.clone().multiply(0.7).setY(0.4));
                        }
                    });
                    this.cancel();
                    return;
                }
                Location trail = p.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.DUST, trail, 4, new Particle.DustOptions(CRESCENT_SILVER, 1.8f));
                world.spawnParticle(Particle.FLAME, trail, 2, 0.15f, 0.15f, 0.15f, 0);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);    }

    // ===== SKILL 3: ULTIMATE (Right-Click Hold) =====
    private void executeSkill3_Ultimate(Player p) {
        final World world = p.getWorld();
        final Location center = p.getLocation().clone();
        final boolean isElite = armorManager.hasFullEliteSet(p);
        
        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.0f);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 0.9f);
        p.sendTitle(isElite ? "§f§l🌑" : "§f§l🌕", isElite ? "§7§lLUNAR ECLIPSE" : "§6§lGOLDEN MOON", 5, 20, 10);
        p.setVelocity(new Vector(0, 0.3, 0));
        p.setInvulnerable(true);
        
        new BukkitRunnable() {
            int phase = 0;
            @Override 
            public void run() {
                if (phase == 0) { 
                    spawnUltimateArena(center, world, isElite ? Color.fromRGB(100,100,150) : GOLD); 
                } else if (phase == 1) { 
                    spawnRisingCrescent(center, world); 
                } else if (phase == 2) { 
                    executeUltimateImpact(p, center, world, isElite);
                } else { 
                    p.setInvulnerable(false); 
                    this.cancel(); 
                    return; 
                }
                phase++;
            }
        }.runTaskTimer(plugin, 0, 25);
    }
    
    private void spawnUltimateArena(Location center, World world, Color color) {
        for(int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            Location loc = center.clone().add(Math.cos(angle) * 8, 0.2, Math.sin(angle) * 8);
            world.spawnParticle(Particle.DUST, loc, 2, new Particle.DustOptions(color, 1.5f));
        }
    }
    
    private void spawnRisingCrescent(Location center, World world) {
        Location loc = center.clone().add(0, 8, 0);
        world.spawnParticle(Particle.DUST, loc, 6, 0.3f, 0.5f, 0.3f, 0, new Particle.DustOptions(MOON_WHITE, 2.0f));
    }
    
    private void executeUltimateImpact(Player p, Location center, World world, boolean isElite) {
        double radius = isElite ? 12.0 : 8.0;
        world.spawnParticle(Particle.DUST, center, 50, (int)(radius/2), 1, (int)(radius/2), 0.1f, new Particle.DustOptions(MOON_WHITE, 1.8f));        world.spawnParticle(Particle.FLAME, center, 30, (int)(radius/3), 0.6f, (int)(radius/3), 0.1f);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 0.9f);
        
        world.getNearbyEntities(center, radius, radius, radius).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                le.damage(isElite ? 6.0 : 4.0, p);
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

    // ===== HELPERS =====
    private void startChargeEffect(Player p) {
        sendActionBar(p, "§e§l✦ CHARGING...");
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.4f, 1.0f);
    }
    
    private void updateChargeVisual(Player p, int chargeTicks) {
        if(chargeTicks % 15 == 0) {
            float pitch = 0.9f + (chargeTicks * 0.01f);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.2f, Math.min(pitch, 1.8f));
        }
        if(chargeTicks >= 40) {
            p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 5, new Particle.DustOptions(GOLD, 2.0f));
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
    
    private LunarPlayerData getData(Player p) {         return playerData.computeIfAbsent(p.getUniqueId(), k -> new LunarPlayerData()); 
    }
    
    private boolean isLunarBlade(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
    
    private void sendActionBar(Player p, String msg) { 
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg)); 
    }
    
    // ===== DATA CLASS =====
    private static class LunarPlayerData {
        int combo = 0; 
        long lastHitTime = 0; 
        int lunarGauge = 0;
        boolean isCharging = false; 
        int chargeTicks = 0;
        boolean moonStepReady = true;
        
        void addGauge(int amount) { 
            lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount); 
        }
    }
                                                     }
