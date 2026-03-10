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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {
    
    private final GoldenMoon plugin;
    
    // --- LUNAR FLOW SYSTEM ---
    private final Map<UUID, LunarPlayerData> playerData = new HashMap<>();
    
    // --- CONFIG (Bisa dipindah ke config.yml nanti) ---
    private static final int MAX_COMBO = 3;
    private static final double COMBO_WINDOW = 1.2; // detik
    private static final int MAX_LUNAR_GAUGE = 100;
    private static final double GAUGE_PER_HIT = 25;
    
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
        
        // Tambah Lunar Gauge on-hit
        data.addGauge(GAUGE_PER_HIT);
        sendGaugeUpdate(p, data);
        
        // Combo System: Hit dalam window = lanjut combo
        long now = System.currentTimeMillis();
        if (now - data.lastHitTime < COMBO_WINDOW * 1000) {
            data.combo++;
            executeComboSkill(p, target, data.combo);
        } else {
            data.combo = 1; // Reset ke hit pertama
            executeBasicStrike(p, target);
        }
        data.lastHitTime = now;
        
        // Screen shake effect (immersion)
        if (data.combo >= 2) {
            triggerScreenShake(p, 0.08, 3);
        }
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        
        // Hold Right-Click = Charge Ultimate
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            LunarPlayerData data = getData(p);
            
            if (data.isCharging) {
                // Release charge = Execute Ultimate
                data.isCharging = false;
                if (data.chargeTicks >= 20) { // Minimal charge 1 detik
                    executeGoldenMoonRequiem(p);
                    data.chargeTicks = 0;
                    data.lunarGauge = 0; // Spend all gauge
                    sendGaugeUpdate(p, data);
                }
            } else if (data.lunarGauge >= MAX_LUNAR_GAUGE) {
                // Start charging
                data.isCharging = true;
                data.chargeTicks = 0;
                startChargeEffect(p);
            } else {
                // Not enough gauge
                sendActionBar(p, "§7✦ §eCharge Lunar Gauge: §f" + data.lunarGauge + "§7/§f" + MAX_LUNAR_GAUGE);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
            }
        }
    }
    
    // Charge tick handler
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        
        LunarPlayerData data = getData(p);
        if (data.isCharging) {
            data.chargeTicks++;
            updateChargeVisual(p, data.chargeTicks);
            
            // Auto-cancel if too long
            if (data.chargeTicks > 100) {
                data.isCharging = false;
                data.chargeTicks = 0;
                sendActionBar(p, "§c✦ Charge cancelled (too long)");
            }
        }
    }

    // ==========================================
    // ⚔️ COMBAT SKILLS (Brutal Legend Style)
    // ==========================================
    
    // --- BASIC: First Hit ---
    private void executeBasicStrike(Player p, LivingEntity target) {
        // Heavy impact feel
        target.setVelocity(p.getLocation().getDirection().multiply(0.4).setY(0.2));
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1f, 0.9f);
        
        // Golden spark on hit
        spawnLunarSpark(target.getLocation().add(0, 1, 0), Color.YELLOW, 5);
    }
    
    // --- COMBO 2: "Moonfall Strike" ---
    private void executeComboSkill(Player p, LivingEntity target, int combo) {
        final Player finalP = p;
        final LivingEntity finalTarget = target;
        final World world = p.getWorld();
        
        if (combo == 2) {
            // 🌙 MOONFALL STRIKE: Downward slam that launches enemy
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);
            
            // Wind-up animation (brief delay for impact feel)
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Visual: Blade glows, crescent trail
                    spawnCrescentTrail(p.getLocation(), p.getLocation().getDirection(), Color.GOLD, 12);
                    
                    // Strike: Launch enemy upward with golden shockwave
                    finalTarget.setVelocity(new Vector(0, 1.8, 0));
                    finalTarget.damage(6.0, finalP);
                    
                    // Impact VFX at target location
                    Location impactLoc = finalTarget.getLocation().add(0, 0.5, 0);
                    world.spawnParticle(Particle.EXPLOSION, impactLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.DUST, impactLoc, 20, new Particle.DustOptions(Color.YELLOW, 2.0f));
                    world.spawnParticle(Particle.FLAME, impactLoc, 10, 0.3, 0.3, 0.3, 0.1);
                    world.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                    
                    // Lunar crater effect (lingering)
                    spawnLunarCrater(impactLoc, world, 30);
                    
                    // Screen shake for attacker (immersion)
                    triggerScreenShake(finalP, 0.12, 5);
                }
            }.runTaskLater(plugin, 8L);
            
        } else if (combo == 3) {
            // 🌙 CRESCENT WALTZ: Dash + multi-hit slash
            p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.9f, 1.3f);
            
            Vector dir = p.getLocation().getDirection().setY(0).normalize();
            
            // Dash forward with trail
            new BukkitRunnable() {
                int dashTicks = 0;
                final Vector dashVel = dir.multiply(2.8);
                
                @Override
                public void run() {
                    if (dashTicks >= 10) {
                        // End dash: Wide crescent slash
                        executeCrescentSlash(finalP, dir);
                        this.cancel();
                        return;
                    }
                    
                    // Dash movement
                    finalP.setVelocity(dashVel.clone().setY(0.1));
                    
                    // Trail: Golden crescent particles
                    spawnCrescentTrail(finalP.getLocation(), dir, Color.ORANGE, 4);
                    world.spawnParticle(Particle.DUST, finalP.getLocation().add(0, 1, 0), 3, 
                        new Particle.DustOptions(Color.YELLOW, 1.5f));
                    
                    // Hit entities during dash
                    world.getNearbyEntities(finalP.getLocation(), 1.5, 1.2, 1.5).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(finalP) && !le.hasMetadata("dashHit")) {
                            le.setMetadata("dashHit", new FixedMetadataValue(plugin, true));
                            le.damage(3.0, finalP);
                            le.setVelocity(dir.multiply(0.8).setY(0.3));
                            spawnLunarSpark(le.getLocation().add(0, 1, 0), Color.GOLD, 6);
                        }
                    });
                    
                    dashTicks++;
                }
            }.runTaskTimer(plugin, 0, 1);
            
            // Clear dash hit metadata after delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    world.getEntities().forEach(en -> en.removeMetadata("dashHit", plugin));
                }
            }.runTaskLater(plugin, 40L);
        }
        
        // Reset combo after execution
        getData(p).combo = 0;
    }
    
    // Helper: Crescent Slash (end of dash)
    private void executeCrescentSlash(Player p, Vector direction) {
        World world = p.getWorld();
        Location slashLoc = p.getLocation().add(0, 1, 0);
        
        // Visual: Wide golden crescent arc
        for(double angle = -60; angle <= 60; angle += 8) {
            double rad = Math.toRadians(angle);
            Vector offset = new Vector(Math.cos(rad) * 2.5, 0, Math.sin(rad) * 2.5);
            Location particleLoc = slashLoc.clone().add(offset.rotateAroundY(Math.toRadians(90)));
            
            world.spawnParticle(Particle.DUST, particleLoc, 2, new Particle.DustOptions(Color.GOLD, 2.2f));
            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0);
        }
        
        // Sound: Epic slash
        world.playSound(slashLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.4f);
        world.playSound(slashLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.2f);
        
        // Damage in arc
        world.getNearbyEntities(slashLoc, 3.5, 2.0, 3.5).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                // Check if in front arc
                Vector toEnemy = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize();
                double dot = toEnemy.dot(direction);
                if (dot > 0.3) { // ~70 degree arc
                    le.damage(9.0, p);
                    le.setVelocity(direction.multiply(1.2).setY(0.6));
                    spawnLunarSpark(le.getLocation().add(0, 1, 0), Color.YELLOW, 10);
                    world.spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
                }
            }
        });
        
        // Screen shake
        triggerScreenShake(p, 0.15, 6);
    }

    // ==========================================
    // 🌕 ULTIMATE: "GOLDEN MOON REQUIEM"
    // Hold Right-Click to Charge → Release for Cataclysm
    // ==========================================
    private void executeGoldenMoonRequiem(Player p) {
        final Player finalP = p;
        final World world = p.getWorld();
        final Location center = p.getLocation().clone();
        
        // === PHASE 1: CHARGE RELEASE ===
        p.playSound(center, Sound.BLOCK_BELL_RESONATE, 1.5f, 1.0f);
        p.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.9f);
        p.sendTitle("§f§l🌕", "§6§lGOLDEN MOON REQUIEM", 5, 20, 10);
        
        // Player floats up slightly (cinematic)
        p.setVelocity(new Vector(0, 0.6, 0));
        p.setInvulnerable(true);
        
        // === PHASE 2: ARENA SUMMON ===
        new BukkitRunnable() {
            int phase = 0;
            @Override
            public void run() {
                if (phase == 0) {
                    // Hexagon moon arena appears
                    spawnLunarArena(center, world, Color.GOLD);
                    world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.8f);
                    
                } else if (phase == 1) {
                    // Blade rises to sky
                    spawnRisingBladeEffect(center, world);
                    world.playSound(center, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.1f);
                    
                } else if (phase == 2) {
                    // Time slow effect (simulated with particle density)
                    world.spawnParticle(Particle.DUST, center.clone().add(0, 2, 0), 50, 
                        4, 2, 4, 0.1, new Particle.DustOptions(Color.YELLOW, 3.0f));
                    world.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.7f);
                    
                } else if (phase == 3) {
                    // === IMPACT: MOON CRASH ===
                    executeMoonImpact(finalP, center, world);
                    
                } else {
                    // Cleanup
                    p.setInvulnerable(false);
                    this.cancel();
                    return;
                }
                phase++;
            }
        }.runTaskTimer(plugin, 0, 25); // 1.25s per phase
    }
    
    // --- Impact: Moon Crash ---
    private void executeMoonImpact(Player p, Location center, World world) {
        // Screen flash
        for(Player viewer : center.getWorld().getPlayers()) {
            viewer.sendTitle("§f§l✦", "§e§lMOONFALL", 3, 10, 5);
        }
        
        // Lightning strikes (multiple)
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
        
        // Block crack (gold)
        try {
            world.spawnParticle(Particle.valueOf("BLOCK_CRACK"), center, 150, 7, 1, 7, 0.15,
                Bukkit.createBlockData(Material.GOLD_BLOCK));
        } catch(Exception ignored) {
            world.spawnParticle(Particle.CLOUD, center, 100, 6, 1.5, 6, 0.2);
        }
        
        // Epic sound layering
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.7f);
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 2f, 0.5f);
        world.playSound(center, Sound.ENTITY_WITHER_DEATH, 1.5f, 0.8f);
        world.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.6f);
        
        // AOE Damage with style
        world.getNearbyEntities(center, 12.0, 12.0, 12.0).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                double dist = le.getLocation().distance(center);
                double damage = 45.0 * (1.0 - dist / 12.0); // Falloff
                
                le.damage(Math.max(damage, 10.0), p);
                le.setVelocity(new Vector(0, 2.2, 0)); // Launch upward
                le.setFireTicks(140); // Golden burn
                
                // Hit VFX
                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
                le.getWorld().spawnParticle(Particle.DUST, le.getLocation().add(0, 1, 0), 15, 
                    new Particle.DustOptions(Color.YELLOW, 2.0f));
                world.playSound(le.getLocation(), Sound.ENTITY_GENERIC_HURT, 1f, 1.2f);
            }
        });
        
        // Lingering moon zone (healing for allies, DoT for enemies)
        spawnLunarZone(center, world, p, 120); // 6 seconds
    }

    // ==========================================
    // 🎨 VISUAL HELPERS (Cinematic, Not "Bocil")
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
            
            // Crescent arc offset
            double arc = Math.sin(progress * Math.PI) * 0.8;
            offset.rotateAroundY(Math.toRadians(90 + arc * 30));
            
            Location particleLoc = from.clone().add(0, 1, 0).add(offset);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 
                new Particle.DustOptions(color, 2.0f));
        }
    }
    
    private void spawnLunarCrater(Location center, World world, int durationTicks) {
        // Glowing golden circle on ground
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks) { this.cancel(); return; }
                
                double radius = 3.0 + Math.sin(ticks * 0.3) * 0.5;
                for(double angle = 0; angle < 360; angle += 12) {
                    double rad = Math.toRadians(angle);
                    Location edge = center.clone().add(Math.cos(rad) * radius, 0.02, Math.sin(rad) * radius);
                    world.spawnParticle(Particle.DUST, edge, 1, new Particle.DustOptions(Color.GOLD, 1.5f));
                }
                
                // Fade out
                if(ticks > durationTicks - 20) {
                    double alpha = 1.0 - ((double)(ticks - (durationTicks - 20)) / 20);
                    // Particle density decreases naturally by skipping spawns
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnLunarArena(Location center, World world, Color color) {
        // Hexagon ring with rotating particles
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
                    
                    // Connect corners with faint lines
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
        // Golden blade silhouette rising
        new BukkitRunnable() {
            int frame = 0;
            @Override
            public void run() {
                if(frame >= 25) { this.cancel(); return; }
                
                double y = frame * 0.8;
                Location bladeCenter = center.clone().add(0, y, 0);
                
                // Blade core (vertical line of particles)
                for(double h = 0; h < 6; h += 0.6) {
                    world.spawnParticle(Particle.DUST, bladeCenter.clone().add(0, h, 0), 2,
                        new Particle.DustOptions(Color.GOLD, 2.5f));
                    world.spawnParticle(Particle.END_ROD, bladeCenter.clone().add(0, h, 0), 1, 0, 0, 0, 0);
                }
                
                // Glow aura
                world.spawnParticle(Particle.DUST, bladeCenter, 8, 0.5, 1, 0.5, 0,
                    new Particle.DustOptions(Color.YELLOW, 3.0f));
                
                frame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnLunarZone(Location center, World world, Player caster, int durationTicks) {
        // Lingering zone: golden mist + subtle effects
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if(ticks >= durationTicks) { 
                    // Final fade burst
                    world.spawnParticle(Particle.DUST, center, 30, 3, 1, 3, 0,
                        new Particle.DustOptions(Color.GOLD, 2.0f));
                    this.cancel(); 
                    return; 
                }
                
                // Ambient particles
                for(int i = 0; i < 5; i++) {
                    Location particleLoc = center.clone().add(
                        (Math.random() - 0.5) * 10,
                        0.5 + Math.random() * 2,
                        (Math.random() - 0.5) * 10
                    );
                    world.spawnParticle(Particle.DUST, particleLoc, 1,
                        new Particle.DustOptions(Color.YELLOW, 1.2f));
                }
                
                // Subtle heal/dot logic could go here (if you want gameplay effect)
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 4); // Every 0.2s
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
    
    // Screen shake simulation (via camera bob)
    private void triggerScreenShake(Player p, double intensity, int ticks) {
        if(intensity <= 0) return;
        
        Location original = p.getLocation().clone();
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if(count >= ticks) {
                    // Return to original (smooth)
                    p.teleport(original);
                    this.cancel();
                    return;
                }
                
                // Random micro-offset
                double shakeX = (Math.random() - 0.5) * intensity;
                double shakeZ = (Math.random() - 0.5) * intensity;
                Location shaken = original.clone().add(shakeX, 0, shakeZ);
                p.teleport(shaken);
                
                count++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🔋 LUNAR GAUGE & CHARGE SYSTEM
    // ==========================================
    
    private void startChargeEffect(Player p) {
        sendActionBar(p, "§e§l✦ CHARGING... §7[§f▮▮▮▮▮§7]");
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.6f, 1.0f);
        
        // Visual charge aura
        new BukkitRunnable() {
            int chargeFrame = 0;
            @Override
            public void run() {
                LunarPlayerData data = getData(p);
                if(!data.isCharging) { this.cancel(); return; }
                
                // Pulsing golden aura
                double radius = 1.5 + Math.sin(chargeFrame * 0.4) * 0.8;
                for(double angle = 0; angle < 360; angle += 20) {
                    double rad = Math.toRadians(angle);
                    Location auraLoc = p.getLocation().add(
                        Math.cos(rad) * radius, 0.5, Math.sin(rad) * radius
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 1,
                        new Particle.DustOptions(Color.GOLD, 2.0f));
                }
                
                // Update charge bar in actionbar
                int bars = Math.min(5, data.chargeTicks / 20);
                String bar = "§7[§f" + "▮".repeat(bars) + "§7" + "▯".repeat(5 - bars) + "]";
                sendActionBar(p, "§e§l✦ CHARGING... §7" + bar + " §f" + (data.chargeTicks / 20) + "s");
                
                chargeFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    
    private void updateChargeVisual(Player p, int chargeTicks) {
        // Intensify particles as charge builds
        if(chargeTicks % 10 == 0) {
            float pitch = 0.8f + (chargeTicks * 0.02f);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, Math.min(pitch, 2.0f));
        }
        
        if(chargeTicks >= 40) {
            // Critical charge: red-gold pulse
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
    
    // Data class for per-player state
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
