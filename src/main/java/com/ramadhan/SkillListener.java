package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {
    
    // GOLD COLOR HELPER (karena Color.GOLD tidak ada)
    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color MOON_YELLOW = Color.fromRGB(255, 223, 100);
    
    private final GoldenMoon plugin;
    private final Map<UUID, LunarPlayerData> playerData = new HashMap<>();
    
    // Config constants
    private static final int MAX_COMBO = 3;
    private static final long COMBO_WINDOW_MS = 1200; // 1.2 detik dalam ms
    private static final int MAX_LUNAR_GAUGE = 100;
    private static final int GAUGE_PER_HIT = 25;
    
    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // 🎮 EVENT HANDLERS
    // ==========================================
    
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isLunarBlade(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;
        
        LunarPlayerData data = getData(p);
        
        // Add Lunar Gauge on-hit
        data.addGauge(GAUGE_PER_HIT);
        sendGaugeUpdate(p, data);
        
        // Combo System
        long now = System.currentTimeMillis();
        if (now - data.lastHitTime < COMBO_WINDOW_MS) {
            data.combo++;
            if (data.combo > MAX_COMBO) data.combo = MAX_COMBO;
            executeComboSkill(p, target, data.combo);
        } else {
            data.combo = 1;
            executeBasicStrike(p, target);
        }
        data.lastHitTime = now;
        
        // Screen shake on combo 2+
        if (data.combo >= 2) {
            triggerScreenShake(p, 0.08, 3);
        }
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            LunarPlayerData data = getData(p);
            
            if (data.isCharging) {
                // Release charge
                data.isCharging = false;
                if (data.chargeTicks >= 20) {
                    executeGoldenMoonRequiem(p);
                    data.chargeTicks = 0;
                    data.lunarGauge = 0;
                    sendGaugeUpdate(p, data);
                }
            } else if (data.lunarGauge >= MAX_LUNAR_GAUGE) {
                // Start charging
                data.isCharging = true;
                data.chargeTicks = 0;
                startChargeEffect(p);
            } else {
                sendActionBar(p, "§7✦ §eCharge Lunar Gauge: §f" + data.lunarGauge + "§7/§f" + MAX_LUNAR_GAUGE);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
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
                sendActionBar(p, "§c✦ Charge cancelled");
            }
        }
    }

    // ==========================================
    // ⚔️ COMBAT SKILLS
    // ==========================================
    
    private void executeBasicStrike(Player p, LivingEntity target) {
        target.setVelocity(p.getLocation().getDirection().multiply(0.4).setY(0.2));
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.9f);
        spawnLunarSpark(target.getLocation().add(0, 1, 0), MOON_YELLOW, 5);
    }
    
    private void executeComboSkill(Player p, LivingEntity target, int combo) {
        final Player finalP = p;
        final LivingEntity finalTarget = target;
        final World world = p.getWorld();
        
        if (combo == 2) {
            // 🌙 MOONFALL STRIKE
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnCrescentTrail(p.getLocation(), p.getLocation().getDirection(), MOON_YELLOW, 12);
                    
                    finalTarget.setVelocity(new Vector(0, 1.8, 0));
                    finalTarget.damage(6.0, finalP);
                    
                    Location impactLoc = finalTarget.getLocation().add(0, 0.5, 0);
                    world.spawnParticle(Particle.EXPLOSION, impactLoc, 1);
                    world.spawnParticle(Particle.DUST, impactLoc, 20, new Particle.DustOptions(MOON_YELLOW, 2.0f));
                    world.spawnParticle(Particle.FLAME, impactLoc, 10, 0.3, 0.3, 0.3, 0.1);
                    world.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                    
                    spawnLunarCrater(impactLoc, world, 30);
                    triggerScreenShake(finalP, 0.12, 5);
                }
            }.runTaskLater(plugin, 8L);
            
        } else if (combo == 3) {
            // 🌙 CRESCENT WALTZ (Dash + Slash)
            p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.9f, 1.3f);
            
            Vector dir = p.getLocation().getDirection().setY(0).normalize();
            
            new BukkitRunnable() {
                int dashTicks = 0;
                final Vector dashVel = dir.clone().multiply(2.8);
                
                @Override
                public void run() {
                    if (dashTicks >= 10) {
                        executeCrescentSlash(finalP, dir);
                        this.cancel();
                        return;
                    }
                    
                    finalP.setVelocity(dashVel.clone().setY(0.1));
                    spawnCrescentTrail(finalP.getLocation(), dir, Color.ORANGE, 4);
                    world.spawnParticle(Particle.DUST, finalP.getLocation().add(0, 1, 0), 3, 
                        new Particle.DustOptions(MOON_YELLOW, 1.5f));
                    
                    // Hit entities during dash
                    world.getNearbyEntities(finalP.getLocation(), 1.5, 1.2, 1.5).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(finalP) && !le.hasMetadata("dashHit")) {
                            le.setMetadata("dashHit", new FixedMetadataValue(plugin, true));
                            le.damage(3.0, finalP);
                            le.setVelocity(dir.clone().multiply(0.8).setY(0.3));
                            spawnLunarSpark(le.getLocation().add(0, 1, 0), MOON_YELLOW, 6);
                        }
                    });
                    
                    dashTicks++;
                }
            }.runTaskTimer(plugin, 0, 1);
            
            // Clear metadata
            new BukkitRunnable() {
                @Override
                public void run() {
                    world.getEntities().forEach(en -> en.removeMetadata("dashHit", plugin));
                }
            }.runTaskLater(plugin, 40L);
        }
        
        getData(p).combo = 0;
    }
    
    private void executeCrescentSlash(Player p, Vector direction) {
        World world = p.getWorld();
        Location slashLoc = p.getLocation().add(0, 1, 0);
        
        // Crescent arc particles
        for(double angle = -60; angle <= 60; angle += 8) {
            double rad = Math.toRadians(angle);
            Vector offset = new Vector(Math.cos(rad) * 2.5, 0, Math.sin(rad) * 2.5);
            Location particleLoc = slashLoc.clone().add(offset.rotateAroundY(Math.toRadians(90)));
            
            world.spawnParticle(Particle.DUST, particleLoc, 2, new Particle.DustOptions(GOLD, 2.2f));
            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0);
        }
        
        world.playSound(slashLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.4f);
        world.playSound(slashLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.2f);
        
        // Damage in arc
        world.getNearbyEntities(slashLoc, 3.5, 2.0, 3.5).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                Vector toEnemy = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize();
                double dot = toEnemy.dot(direction);
                if (dot > 0.3) {
                    le.damage(9.0, p);
                    le.setVelocity(direction.clone().multiply(1.2).setY(0.6));
                    spawnLunarSpark(le.getLocation().add(0, 1, 0), MOON_YELLOW, 10);
                    world.spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
                }
            }
        });
        
        triggerScreenShake(p, 0.15, 6);
    }

    // ==========================================
    // 🌕 ULTIMATE: GOLDEN MOON REQUIEM
    // ==========================================
    private void executeGoldenMoonRequiem(Player p) {
        final Player finalP = p;
        final World world = p.getWorld();
        final Location center = p.getLocation().clone();
        
        p.playSound(center, Sound.BLOCK_BELL_RESONATE, 1.5f, 1.0f);
        p.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.9f);
        p.sendTitle("§f§l🌕", "§6§lGOLDEN MOON REQUIEM", 5, 20, 10);
        
        p.setVelocity(new Vector(0, 0.6, 0));
        p.setInvulnerable(true);
        
        new BukkitRunnable() {
            int phase = 0;
            @Override
            public void run() {
                if (phase == 0) {
                    spawnLunarArena(center, world, GOLD);
                    world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
                } else if (phase == 1) {
                    spawnRisingBladeEffect(center, world);
                    world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.1f);
                } else if (phase == 2) {
                    world.spawnParticle(Particle.DUST, center.clone().add(0, 2, 0), 50, 
                        4, 2, 4, 0.1, new Particle.DustOptions(MOON_YELLOW, 3.0f));
                    world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.7f);
                } else if (phase == 3) {
                    executeMoonImpact(finalP, center, world);
                } else {
                    p.setInvulnerable(false);
                    this.cancel();
                    return;
                }
                phase++;
            }
        }.runTaskTimer(plugin, 0, 25);
    }
    
    private void executeMoonImpact(Player p, Location center, World world) {
        // Screen flash
        for(Player viewer : center.getWorld().getPlayers()) {
            viewer.sendTitle("§f§l✦", "§e§lMOONFALL", 3, 10, 5);
        }
        
        // Lightning strikes
        for(int i = 0; i < 5; i++) {
            final int delay = i * 4;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location strikeLoc = center.clone().add(
                        (Math.random() - 0.5) * 8, 0, (Math.random() - 0.5) * 8
                    );
                    world.strikeLightningEffect(strikeLoc);
                    world.playSound(strikeLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.7f);
                }
            }.runTaskLater(plugin, delay);
        }
        
        // Ground eruption
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 8);
        world.spawnParticle(Particle.DUST, center, 80, 5, 2, 5, 0.2, 
            new Particle.DustOptions(Color.ORANGE, 3.5f));
        world.spawnParticle(Particle.FLAME, center, 60, 4, 1.5, 4, 0.15);
        world.spawnParticle(Particle.CLOUD, center, 40, 6, 2, 6, 0.2);
        
        // Block crack with fallback
        try {
            world.spawnParticle(Particle.valueOf("BLOCK_CRACK"), center, 150, 7, 1, 7, 0.15,
                Bukkit.createBlockData(Material.GOLD_BLOCK));
        } catch(Exception ignored) {
            world.spawnParticle(Particle.CLOUD, center, 100, 6, 1.5, 6, 0.2);
        }
        
        // Epic sounds
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.7f);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 2f, 0.5f);
        world.playSound(center, Sound.ENTITY_WITHER_DEATH, 1.5f, 0.8f);
        world.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.6f);
        
        // AOE Damage
        world.getNearbyEntities(center, 12.0, 12.0, 12.0).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                double dist = le.getLocation().distance(center);
                double damage = 45.0 * (1.0 - dist / 12.0);
                
                le.damage(Math.max(damage, 10.0), p);
                le.setVelocity(new Vector(0, 2.2, 0));
                le.setFireTicks(140);
                
                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
                le.getWorld().spawnParticle(Particle.DUST, le.getLocation().add(0, 1, 0), 15, 
                    new Particle.DustOptions(MOON_YELLOW, 2.0f));
                world.playSound(le.getLocation(), Sound.ENTITY_GENERIC_HURT, 1f, 1.2f);
            }
        });
        
        spawnLunarZone(center, world, p, 120);
    }

    // ==========================================
    // 🎨 VISUAL HELPERS
    // ==========================================
    
    private void spawnLunarSpark(Location loc, Color color, int count) {
        World world = loc.getWorld();
        for(int i = 0; i < count; i++) {
            Vector spread = new Vector(
                (Math.random() - 0.5) * 0.4,
                Math.random() * 0.5,
                (Math.random() - 0.5) * 0.4
            );
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, 
                new Particle.DustOptions(color, 1.8f));
        }
        world.spawnParticle(Particle.FLAME, loc, count / 2, 0.2, 0.3, 0.2, 0.05);
    }
    
    private void spawnCrescentTrail(Location from, Vector direction, Color color, int density) {
        World world = from.getWorld();
        for(int i = 0; i < density; i++) {
            double progress = (double) i / density;
            Vector offset = direction.clone().multiply(progress * 2.5);
            double arc = Math.sin(progress * Math.PI) * 0.8;
            offset.rotateAroundY(Math.toRadians(90 + arc * 30));
            
            Location particleLoc = from.clone().add(0, 1, 0).add(offset);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 
                new Particle.DustOptions(color, 2.0f));
        }
    }
    
    private void spawnLunarCrater(Location center, World world, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks) { this.cancel(); return; }
                
                double radius = 3.0 + Math.sin(ticks * 0.3) * 0.5;
                for(double angle = 0; angle < 360; angle += 12) {
                    double rad = Math.toRadians(angle);
                    Location edge = center.clone().add(Math.cos(rad) * radius, 0.02, Math.sin(rad) * radius);
                    world.spawnParticle(Particle.DUST, edge, 1, new Particle.DustOptions(GOLD, 1.5f));
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnLunarArena(Location center, World world, Color color) {
        for(int corner = 0; corner < 6; corner++) {
            double baseAngle = corner * 60;
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if(ticks >= 40) { this.cancel(); return; }
                    
                    double rotation = ticks * 3;
                    double angle = Math.toRadians(baseAngle + rotation);
                    Location cornerLoc = center.clone().add(
                        Math.cos(angle) * 9, 0.3, Math.sin(angle) * 9
                    );
                    
                    world.spawnParticle(Particle.DUST, cornerLoc, 2, 
                        new Particle.DustOptions(color, 2.0f));
                    world.spawnParticle(Particle.FLAME, cornerLoc, 1, 0.1, 0.1, 0.1, 0);
                    
                    if(ticks % 4 == 0) {
                        double nextAngle = Math.toRadians(baseAngle + 60 + rotation);
                        Location nextLoc = center.clone().add(
                            Math.cos(nextAngle) * 9, 0.3, Math.sin(nextAngle) * 9
                        );
                        drawParticleLine(cornerLoc, nextLoc, color, 1);
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, corner * 2, 1);
        }
    }
    
    private void spawnRisingBladeEffect(Location center, World world) {
        new BukkitRunnable() {
            int frame = 0;
            @Override
            public void run() {
                if(frame >= 25) { this.cancel(); return; }
                
                double y = frame * 0.8;
                Location bladeCenter = center.clone().add(0, y, 0);
                
                for(double h = 0; h < 6; h += 0.6) {
                    world.spawnParticle(Particle.DUST, bladeCenter.clone().add(0, h, 0), 2,
                        new Particle.DustOptions(GOLD, 2.5f));
                    world.spawnParticle(Particle.END_ROD, bladeCenter.clone().add(0, h, 0), 1, 0, 0, 0, 0);
                }
                
                world.spawnParticle(Particle.DUST, bladeCenter, 8, 0.5, 1, 0.5, 0,
                    new Particle.DustOptions(MOON_YELLOW, 3.0f));
                
                frame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnLunarZone(Location center, World world, Player caster, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if(ticks >= durationTicks) { 
                    world.spawnParticle(Particle.DUST, center, 30, 3, 1, 3, 0,
                        new Particle.DustOptions(GOLD, 2.0f));
                    this.cancel(); 
                    return; 
                }
                
                for(int i = 0; i < 5; i++) {
                    Location particleLoc = center.clone().add(
                        (Math.random() - 0.5) * 10,
                        0.5 + Math.random() * 2,
                        (Math.random() - 0.5) * 10
                    );
                    world.spawnParticle(Particle.DUST, particleLoc, 1,
                        new Particle.DustOptions(MOON_YELLOW, 1.2f));
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 4);
    }
    
    private void drawParticleLine(Location from, Location to, Color color, int density) {
        Vector dir = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        if(dist < 0.1) return;
        dir.normalize().multiply(0.6);
        
        World world = from.getWorld();
        for(double i = 0; i < dist; i += 0.6) {
            world.spawnParticle(Particle.DUST, from.clone().add(dir.clone().multiply(i)), 1,
                new Particle.DustOptions(color, 1.5f));
        }
    }
    
    private void triggerScreenShake(Player p, double intensity, int ticks) {
        if(intensity <= 0) return;
        
        Location original = p.getLocation().clone();
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if(count >= ticks) {
                    p.teleport(original);
                    this.cancel();
                    return;
                }
                
                double shakeX = (Math.random() - 0.5) * intensity;
                double shakeZ = (Math.random() - 0.5) * intensity;
                Location shaken = original.clone().add(shakeX, 0, shakeZ);
                p.teleport(shaken);
                count++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🔋 CHARGE & GAUGE SYSTEM
    // ==========================================
    
    private void startChargeEffect(Player p) {
        sendActionBar(p, "§e§l✦ CHARGING... §7[§f▮▮▮▮▮§7]");
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.0f);
        
        new BukkitRunnable() {
            int chargeFrame = 0;
            @Override
            public void run() {
                LunarPlayerData data = getData(p);
                if(!data.isCharging) { this.cancel(); return; }
                
                double radius = 1.5 + Math.sin(chargeFrame * 0.4) * 0.8;
                for(double angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    Location auraLoc = p.getLocation().add(
                        Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 1,
                        new Particle.DustOptions(GOLD, 2.0f));
                }
                
                int bars = Math.min(5, data.chargeTicks / 20);
                String bar = "§7[§f" + "▮".repeat(bars) + "§7" + "▯".repeat(5 - bars) + "]";
                sendActionBar(p, "§e§l✦ CHARGING... §7" + bar + " §f" + (data.chargeTicks / 20) + "s");
                
                chargeFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    
    private void updateChargeVisual(Player p, int chargeTicks) {
        if(chargeTicks % 10 == 0) {
            float pitch = 0.8f + (chargeTicks * 0.02f);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, Math.min(pitch, 2.0f));
        }
        if(chargeTicks >= 40) {
            p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 5,
                new Particle.DustOptions(Color.ORANGE, 2.5f));
        }
    }
    
    private void sendGaugeUpdate(Player p, LunarPlayerData data) {
        int bars = (int) Math.ceil(data.lunarGauge / 20.0);
        String bar = "§7[§f" + "▮".repeat(bars) + "§7" + "▯".repeat(5 - bars) + "]";
        sendActionBar(p, "§b✦ Lunar Gauge: §7" + bar + " §f" + data.lunarGauge + "%");
    }

    // ==========================================
    // 📦 DATA & UTILS
    // ==========================================
    
    private LunarPlayerData getData(Player p) {
        return playerData.computeIfAbsent(p.getUniqueId(), k -> new LunarPlayerData());
    }
    
    private boolean isLunarBlade(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
    
    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }
    
    // Clean up player data on quit
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        playerData.remove(e.getPlayer().getUniqueId());
    }
    
    private static class LunarPlayerData {
        int combo = 0;
        long lastHitTime = 0;
        int lunarGauge = 0;
        boolean isCharging = false;
        int chargeTicks = 0;
        
        void addGauge(int amount) {
            lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount);
        }
    }
                    }
