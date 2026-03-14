package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SkillListener implements Listener {

    private static final Color MOON_YELLOW = Color.fromRGB(255, 230, 100);
    private static final Color MOON_WHITE = Color.fromRGB(255, 250, 240);
    private static final Color WIND_SILVER = Color.fromRGB(220, 220, 230);
    private static final Color SPARKLE_GOLD = Color.fromRGB(255, 240, 150);

    private final GoldenMoon plugin;
    private final ArmorManager armorManager;
    private final Map<java.util.UUID, PlayerSkillData> playerData = new HashMap<java.util.UUID, PlayerSkillData>();
    private final Random random = new Random();

    private static final int MAX_LUNAR_GAUGE = 100;
    private static final int GAUGE_PER_HIT = 15;
    private static final long SKILL1_HOLD_MS = 250;

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        this.armorManager = plugin.getArmorManager();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();        if (!isLunarBlade(p)) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) e.getEntity();

        PlayerSkillData data = getData(p);
        long now = System.currentTimeMillis();
        if (now - data.lastHitTime < 120) return;
        data.lastHitTime = now;
        data.addGauge(GAUGE_PER_HIT);

        if (data.lastHitStart == 0) {
            data.lastHitStart = now;
        } else if (now - data.lastHitStart >= SKILL1_HOLD_MS && !data.skill1Used) {
            data.skill1Used = true;
            animateMoonlightSlash(p, target);
            return;
        }
        if (now - data.lastHitStart > SKILL1_HOLD_MS + 300) {
            data.lastHitStart = 0;
            data.skill1Used = false;
        }

        target.damage(2.0, p);
        animateSwordSparkle(p.getLocation().add(0, 1, 0), p.getWorld());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (hasLunarShield(p)) {
            e.setDamage(e.getDamage() * 0.85f);
            if (random.nextInt(100) < 30) {
                animateShieldSparkle(p.getLocation().add(0, 1.3f, 0), p.getWorld());
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            PlayerSkillData data = getData(p);
            if (data.lunarGauge >= MAX_LUNAR_GAUGE && !data.isCharging && !data.skill3Cooldown) {
                data.isCharging = true;
                data.chargeStart = System.currentTimeMillis();
                sendActionBar(p, "§6§l✦ §fMenahan... §7(Lepas untuk Panggilan Bulan)");                animateChargeSequence(p);
            } else if (data.isCharging) {
                long ct = System.currentTimeMillis() - data.chargeStart;
                if (ct >= 1000) {
                    data.isCharging = false;
                    animateMoonDomainUltimate(p);
                    data.lunarGauge = 0;
                    data.skill3Cooldown = true;
                    sendActionBar(p, "§6§l✦ §f🌕 PANGGILAN BULAN AKTIF! 🌕");
                    new BukkitRunnable() { public void run() { getData(p).skill3Cooldown = false; }}.runTaskLater(plugin, 1200);
                } else {
                    data.isCharging = false;
                    sendActionBar(p, "§c✦ §fTahan minimal 1 detik!");
                }
            }
        }

        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && p.isSneaking()) {
            PlayerSkillData data = getData(p);
            if (!data.skill2Cooldown) {
                e.setCancelled(true);
                animateBlessingStorm(p);
                data.skill2Cooldown = true;
                new BukkitRunnable() { public void run() { getData(p).skill2Cooldown = false; }}.runTaskLater(plugin, 60);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        PlayerSkillData data = getData(p);
        if (data.isCharging && System.currentTimeMillis() % 100 < 25) {
            animateChargeAura(p.getLocation().add(0, 1.6f, 0), p.getWorld());
        }
        if (armorManager.tryMoonStep(p)) {
            data.moonStepReady = false;
            new BukkitRunnable() { public void run() { getData(p).moonStepReady = true; }}.runTaskLater(plugin, 60);
        }
    }

    // ==========================================
    // ⚔️ SKILL 1: MOONLIGHT SLASH (PROGRESSIVE - 4 STAGES)
    // ==========================================
    private void animateMoonlightSlash(final Player p, final LivingEntity target) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location playerLoc = p.getLocation().clone();
        final org.bukkit.Location targetLoc = target.getLocation().clone();
        final Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).setY(0).normalize();        final double distance = Math.min(5.0, playerLoc.distance(targetLoc));

        // 🎬 STAGE 1: Sword charge buildup (0-0.3s)
        animateSwordCharge(p, playerLoc, world);

        // 🎬 STAGE 2: Short dash with sword trail (0.3-0.6s)
        new BukkitRunnable() {
            int dashFrame = 0;
            public void run() {
                if (dashFrame >= 8) { this.cancel(); return; }
                
                // SHORT dash (only 1.5 blocks) with sword-focused particles
                float dashProgress = (float)dashFrame / 7.0f;
                Vector dashMove = direction.clone().multiply(1.5f * dashProgress);
                org.bukkit.Location dashLoc = playerLoc.clone().add(dashMove);
                
                // 🗡️ SWORD-FOCUSED particles (not player body)
                Vector swordOffset = direction.clone().multiply(0.8f);
                org.bukkit.Location swordLoc = dashLoc.clone().add(swordOffset);
                
                // Blade core (yellow/white gradient)
                world.spawnParticle(Particle.DUST, swordLoc, 6, 
                        new Particle.DustOptions(MOON_YELLOW, 2.0f));
                world.spawnParticle(Particle.DUST, swordLoc, 4, 
                        new Particle.DustOptions(MOON_WHITE, 1.5f));
                
                // Wind trail around blade
                for (int i = 0; i < 4; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    Vector windOffset = new Vector(
                            Math.cos(angle) * 0.4f,
                            (float)(random.nextDouble() * 0.3),
                            Math.sin(angle) * 0.4f
                    );
                    world.spawnParticle(Particle.DUST, swordLoc.clone().add(windOffset), 1, 
                            new Particle.DustOptions(WIND_SILVER, 1.2f));
                }
                
                dashFrame++;
            }
        }.runTaskTimer(plugin, 6, 1);

        // 🎬 STAGE 3: Grid slash impact (0.6-1.0s)
        new BukkitRunnable() {
            public void run() {
                animateGridSlash(targetLoc, direction, world, target);
            }
        }.runTaskLater(plugin, 14);

        // 🎬 STAGE 4: Lingering afterglow (1.0-1.5s)        new BukkitRunnable() {
            int glowFrame = 0;
            public void run() {
                if (glowFrame >= 12) { this.cancel(); return; }
                
                org.bukkit.Location glowLoc = targetLoc.clone().add(0, 1, 0);
                double pulse = Math.sin(glowFrame * 0.5) * 0.3 + 0.85;
                
                // Grid fade effect
                for (int row = -2; row <= 2; row++) {
                    for (int col = -2; col <= 2; col++) {
                        if (random.nextInt(3) == 0) continue;
                        Vector gridOffset = new Vector(row * 0.5f, 0, col * 0.5f);
                        world.spawnParticle(Particle.DUST, glowLoc.clone().add(gridOffset), 1, 
                                new Particle.DustOptions(MOON_WHITE, (float)(1.0f * pulse)));
                    }
                }
                
                if (glowFrame % 2 == 0) {
                    animateSwordSparkle(glowLoc, world);
                }
                glowFrame++;
            }
        }.runTaskTimer(plugin, 20, 2);
    }

    private void animateSwordCharge(final Player p, final org.bukkit.Location loc, final org.bukkit.World world) {
        // Charge particles around sword position
        new BukkitRunnable() {
            int chargeFrame = 0;
            public void run() {
                if (chargeFrame >= 6) { this.cancel(); return; }
                
                Vector swordOffset = p.getLocation().getDirection().multiply(0.9f);
                org.bukkit.Location swordLoc = loc.clone().add(swordOffset);
                
                // Rotating charge ring
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45 + chargeFrame * 15);
                    Vector ringOffset = new Vector(
                            Math.cos(angle) * 0.5f,
                            0.3f + (float)(Math.sin(chargeFrame * 0.5) * 0.2),
                            Math.sin(angle) * 0.5f
                    );
                    Color chargeColor = chargeFrame % 2 == 0 ? MOON_YELLOW : MOON_WHITE;
                    world.spawnParticle(Particle.DUST, swordLoc.clone().add(ringOffset), 1, 
                            new Particle.DustOptions(chargeColor, 1.4f));
                }
                
                // Sparkle burst every 2 frames                if (chargeFrame % 2 == 0) {
                    for (int s = 0; s < 3; s++) {
                        Vector sparkSpread = new Vector(
                                (float)((random.nextDouble() - 0.5) * 0.6),
                                (float)(random.nextDouble() * 0.5),
                                (float)((random.nextDouble() - 0.5) * 0.6)
                        );
                        world.spawnParticle(Particle.DUST, swordLoc.clone().add(sparkSpread), 1, 
                                new Particle.DustOptions(SPARKLE_GOLD, 1.1f));
                    }
                }
                chargeFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateGridSlash(final org.bukkit.Location center, final Vector direction, 
            final org.bukkit.World world, final LivingEntity target) {
        
        // Play impact sound
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.8f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.4f);
        
        // 🎯 GRID SLASH PATTERN (3x3 grid with animated lines)
        final float gridSize = 2.5f;
        final int gridSteps = 10;
        
        // Animate horizontal lines
        for (int row = -1; row <= 1; row++) {
            final int r = row;
            new BukkitRunnable() {
                int step = 0;
                public void run() {
                    if (step >= gridSteps) { this.cancel(); return; }
                    
                    float progress = (float)step / (gridSteps - 1);
                    float lineLength = gridSize * progress;
                    
                    // Draw line segment
                    for (float t = 0; t <= lineLength; t += 0.3f) {
                        Vector lineOffset = new Vector(
                                t - gridSize * 0.5f,
                                r * 0.6f,
                                0
                        );
                        // Rotate line to face direction
                        lineOffset = rotateVector(lineOffset, direction);
                        
                        Color lineColor = step < 5 ? MOON_YELLOW : MOON_WHITE;
                        world.spawnParticle(Particle.DUST, center.clone().add(lineOffset), 1,                                 new Particle.DustOptions(lineColor, 1.3f));
                    }
                    step++;
                }
            }.runTaskTimer(plugin, r * 2, 1);
        }
        
        // Animate vertical lines (delayed for layered effect)
        for (int col = -1; col <= 1; col++) {
            final int c = col;
            new BukkitRunnable() {
                int step = 0;
                public void run() {
                    if (step >= gridSteps) { this.cancel(); return; }
                    
                    float progress = (float)step / (gridSteps - 1);
                    float lineLength = gridSize * progress;
                    
                    for (float t = 0; t <= lineLength; t += 0.3f) {
                        Vector lineOffset = new Vector(
                                0,
                                c * 0.6f,
                                t - gridSize * 0.5f
                        );
                        lineOffset = rotateVector(lineOffset, direction);
                        
                        Color lineColor = step < 5 ? MOON_WHITE : MOON_YELLOW;
                        world.spawnParticle(Particle.DUST, center.clone().add(lineOffset), 1, 
                                new Particle.DustOptions(lineColor, 1.3f));
                    }
                    step++;
                }
            }.runTaskTimer(plugin, 5 + c * 2, 1);
        }
        
        // Impact burst at center
        new BukkitRunnable() {
            public void run() {
                // Explosion core
                world.spawnParticle(Particle.EXPLOSION, center, 1);
                
                // Yellow/white burst
                for (int i = 0; i < 25; i++) {
                    final int spark = i;
                    new BukkitRunnable() {
                        public void run() {
                            Vector spread = new Vector(
                                    (float)((random.nextDouble() - 0.5) * 2.0),
                                    (float)(random.nextDouble() * 1.5),
                                    (float)((random.nextDouble() - 0.5) * 2.0)                            );
                            Color burstColor = random.nextInt(2) == 0 ? MOON_YELLOW : MOON_WHITE;
                            world.spawnParticle(Particle.DUST, center.clone().add(spread), 1, 
                                    new Particle.DustOptions(burstColor, 1.2f + (float)(random.nextDouble() * 0.4f)));
                        }
                    }.runTaskLater(plugin, spark);
                }
                
                // Wind accent particles
                for (int i = 0; i < 12; i++) {
                    final int wind = i;
                    new BukkitRunnable() {
                        public void run() {
                            Vector windSpread = new Vector(
                                    (float)((random.nextDouble() - 0.5) * 1.5),
                                    (float)(random.nextDouble() * 1.0),
                                    (float)((random.nextDouble() - 0.5) * 1.5)
                            );
                            world.spawnParticle(Particle.DUST, center.clone().add(windSpread), 1, 
                                    new Particle.DustOptions(WIND_SILVER, 1.0f));
                        }
                    }.runTaskLater(plugin, wind + 2);
                }
                
                // Damage target
                target.damage(4.0, p);
                target.setVelocity(direction.clone().multiply(0.8f).setY(0.5f));
            }
        }.runTaskLater(plugin, 10);
    }

    private Vector rotateVector(Vector v, Vector direction) {
        // Simple 2D rotation to face direction
        double angle = Math.atan2(direction.getZ(), direction.getX());
        double x = v.getX() * Math.cos(angle) - v.getZ() * Math.sin(angle);
        double z = v.getX() * Math.sin(angle) + v.getZ() * Math.cos(angle);
        return new Vector(x, v.getY(), z);
    }

    private void animateSwordSparkle(final org.bukkit.Location loc, final org.bukkit.World world) {
        for (int i = 0; i < 5; i++) {
            final int spark = i;
            new BukkitRunnable() {
                public void run() {
                    Vector spread = new Vector(
                            (float)((random.nextDouble() - 0.5) * 0.4),
                            (float)(random.nextDouble() * 0.5),
                            (float)((random.nextDouble() - 0.5) * 0.4)
                    );
                    world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1,                             new Particle.DustOptions(SPARKLE_GOLD, 1.1f));
                }
            }.runTaskLater(plugin, spark);
        }
    }

    private void animateShieldSparkle(final org.bukkit.Location loc, final org.bukkit.World world) {
        for (int i = 0; i < 4; i++) {
            Vector spread = new Vector(
                    (float)((random.nextDouble() - 0.5) * 0.35),
                    (float)(random.nextDouble() * 0.4),
                    (float)((random.nextDouble() - 0.5) * 0.35)
            );
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, 
                    new Particle.DustOptions(WIND_SILVER, 1.15f));
        }
    }

    // ==========================================
    // ✨ SKILL 2: BLESSING STORM (PROGRESSIVE - 3 STAGES)
    // ==========================================
    private void animateBlessingStorm(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        
        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.7f);
        sendActionBar(p, "§e§l✦ §f☁️ HUJAN BERKAH DITURUNKAN! ☁️");

        // 🎬 STAGE 1: Sky preparation (moon clouds)
        animateStormPreparation(center, world);

        // 🎬 STAGE 2: Tilted blessing orbs falling
        for (int orb = 0; orb < 10; orb++) {
            final int o = orb;
            new BukkitRunnable() {
                public void run() {
                    animateFallingBlessing(center, world, p, o);
                }
            }.runTaskLater(plugin, o * 3 + random.nextInt(4));
        }

        // 🎬 STAGE 3: Ground blessing wave
        new BukkitRunnable() {
            int waveFrame = 0;
            public void run() {
                if (waveFrame >= 25) { this.cancel(); return; }
                
                float radius = 2.0f + waveFrame * 0.15f;
                if (radius <= 5.5f) {
                    for (int a = 0; a < 24; a++) {                        double angle = Math.toRadians(a * 15 + waveFrame * 4);
                        Vector ringOffset = new Vector(
                                Math.cos(angle) * radius,
                                0.1f,
                                Math.sin(angle) * radius
                        );
                        world.spawnParticle(Particle.DUST, center.clone().add(ringOffset), 1, 
                                new Particle.DustOptions(MOON_WHITE, 1.4f));
                    }
                }
                
                // Random sparkles in domain
                if (waveFrame % 4 == 0) {
                    for (int s = 0; s < 4; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            org.bukkit.Location sparkLoc = center.clone().add(
                                    (random.nextDouble() - 0.5) * 5.0,
                                    1.5 + random.nextDouble() * 4.0,
                                    (random.nextDouble() - 0.5) * 5.0
                            );
                            animateSwordSparkle(sparkLoc, world);
                        }}.runTaskLater(plugin, spark);
                    }
                }
                waveFrame++;
            }
        }.runTaskTimer(plugin, 30, 2);
    }

    private void animateStormPreparation(final org.bukkit.Location center, final org.bukkit.World world) {
        new BukkitRunnable() {
            int prepFrame = 0;
            public void run() {
                if (prepFrame >= 20) { this.cancel(); return; }
                
                // Cloud ring above player
                float cloudRadius = 4.0f + (float)(Math.sin(prepFrame * 0.3) * 0.5);
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30 + prepFrame * 3);
                    Vector cloudOffset = new Vector(
                            Math.cos(angle) * cloudRadius,
                            8.0f + (float)(Math.sin(prepFrame * 0.4) * 0.4),
                            Math.sin(angle) * cloudRadius
                    );
                    world.spawnParticle(Particle.DUST, center.clone().add(cloudOffset), 2, 
                            new Particle.DustOptions(WIND_SILVER, 1.3f));
                }
                
                // Occasional lightning hint                if (prepFrame % 5 == 0 && random.nextInt(3) == 0) {
                    world.spawnParticle(Particle.FLASH, center.clone().add(0, 9, 0), 1);
                }
                prepFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateFallingBlessing(final org.bukkit.Location center, final org.bukkit.World world, 
            final Player p, final int orbIndex) {
        
        // Random start position with tilt
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 1.0 + random.nextDouble() * 4.0;
        final float tiltX = (float)((random.nextDouble() - 0.5) * 1.2);
        final float tiltZ = (float)((random.nextDouble() - 0.5) * 1.2);
        
        final org.bukkit.Location dropStart = center.clone().add(
                Math.cos(angle) * distance + tiltX,
                16.0,
                Math.sin(angle) * distance + tiltZ
        );
        
        // Horizontal drift during fall
        final float driftX = (float)((random.nextDouble() - 0.5) * 0.06);
        final float driftZ = (float)((random.nextDouble() - 0.5) * 0.06);
        
        new BukkitRunnable() {
            int fallFrame = 0;
            public void run() {
                if (fallFrame >= 32) {
                    // Impact
                    org.bukkit.Location impactLoc = dropStart.clone();
                    impactLoc.setY(center.getY() + (random.nextDouble() - 0.5) * 0.4);
                    
                    // Impact burst
                    world.spawnParticle(Particle.DUST, impactLoc, 20, 
                            new Particle.DustOptions(MOON_YELLOW, 1.8f));
                    world.spawnParticle(Particle.DUST, impactLoc, 15, 
                            new Particle.DustOptions(MOON_WHITE, 1.4f));
                    world.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 1.5f);
                    
                    // Damage in 3-block radius
                    for (org.bukkit.entity.Entity en : world.getNearbyEntities(impactLoc, 3.0, 3.0, 3.0)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            LivingEntity le = (LivingEntity) en;
                            le.damage(6.0, p);
                            le.setVelocity(new Vector(0, 0.55f, 0));
                            animateSwordSparkle(le.getLocation().add(0, 1, 0), world);
                        }                    }
                    
                    // Lingering glow
                    new BukkitRunnable() {
                        int glowFrame = 0;
                        public void run() {
                            if (glowFrame >= 15) { this.cancel(); return; }
                            float pulse = 1.0f + (float)(Math.sin(glowFrame * 0.4) * 0.2);
                            world.spawnParticle(Particle.DUST, impactLoc, 3, 
                                    new Particle.DustOptions(MOON_WHITE, pulse));
                            glowFrame++;
                        }
                    }.runTaskTimer(plugin, 0, 2);
                    
                    this.cancel();
                    return;
                }
                
                // Falling orb with trail
                org.bukkit.Location currentLoc = dropStart.clone();
                currentLoc.setY(dropStart.getY() - fallFrame * 0.5);
                currentLoc.add(driftX * fallFrame, 0, driftZ * fallFrame);
                
                // Core orb
                world.spawnParticle(Particle.DUST, currentLoc, 3, 
                        new Particle.DustOptions(MOON_YELLOW, 1.6f));
                
                // Wind trail
                for (int t = 0; t < 3; t++) {
                    org.bukkit.Location trailLoc = currentLoc.clone().add(0, t * 0.4f + 0.3f, 0);
                    world.spawnParticle(Particle.DUST, trailLoc, 1, 
                            new Particle.DustOptions(WIND_SILVER, 1.1f));
                }
                
                // Occasional sparkle
                if (fallFrame % 4 == 0) {
                    animateSwordSparkle(currentLoc, world);
                }
                fallFrame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🌕 SKILL 3: MOON DOMAIN ULTIMATE (PROGRESSIVE - 5 STAGES)
    // ==========================================
    private void animateMoonDomainUltimate(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        final float domainRadius = 6.0f;        
        p.setVelocity(new Vector(0, 0.4f, 0));
        p.setInvulnerable(true);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.95f);
        p.sendTitle("§f§l🌕", "§6§l✦ PANGGILAN BULAN ✦", 5, 25, 8);
        sendActionBar(p, "§6§l🌙 §fDomain Bulan Aktif...");

        // 🎬 STAGE 1: Domain boundary summon
        animateDomainBoundary(center, domainRadius, world);

        // 🎬 STAGE 2: Rising moon blade
        animateRisingBlade(center, world);

        // 🎬 STAGE 3: Moon crash impact
        new BukkitRunnable() {
            public void run() {
                animateDomainImpact(p, center, domainRadius, world);
            }
        }.runTaskLater(plugin, 50);

        // 🎬 STAGE 4: Blessing wave expansion
        new BukkitRunnable() {
            int waveFrame = 0;
            public void run() {
                if (waveFrame >= 30) { this.cancel(); return; }
                
                float radius = 1.5f + waveFrame * 0.18f;
                if (radius <= domainRadius) {
                    for (int a = 0; a < 30; a++) {
                        double angle = Math.toRadians(a * 12 + waveFrame * 3);
                        Vector ringOffset = new Vector(
                                Math.cos(angle) * radius,
                                0.12f,
                                Math.sin(angle) * radius
                        );
                        world.spawnParticle(Particle.DUST, center.clone().add(ringOffset), 1, 
                                new Particle.DustOptions(MOON_WHITE, 1.5f));
                    }
                }
                waveFrame++;
            }
        }.runTaskTimer(plugin, 55, 2);

        // 🎬 STAGE 5: Finale blessing
        new BukkitRunnable() {
            public void run() {
                animateDomainFinale(p, center, world);
            }
        }.runTaskLater(plugin, 85);
    }
    private void animateDomainBoundary(final org.bukkit.Location center, final float radius, final org.bukkit.World world) {
        new BukkitRunnable() {
            int frame = 0;
            public void run() {
                if (frame >= 25) { this.cancel(); return; }
                
                float progress = (float)frame / 24.0f;
                float currentRadius = radius * progress;
                
                // Hexagon corners
                for (int corner = 0; corner < 6; corner++) {
                    double angle = Math.toRadians(corner * 60 + frame * 4);
                    Vector cornerOffset = new Vector(
                            Math.cos(angle) * currentRadius,
                            0.2f + progress * 0.5f,
                            Math.sin(angle) * currentRadius
                    );
                    org.bukkit.Location cornerLoc = center.clone().add(cornerOffset);
                    
                    world.spawnParticle(Particle.DUST, cornerLoc, 3, 
                            new Particle.DustOptions(MOON_YELLOW, 1.7f));
                    
                    // Connect corners with lines
                    if (frame > 5 && frame % 3 == 0) {
                        int nextCorner = (corner + 1) % 6;
                        double nextAngle = Math.toRadians(nextCorner * 60 + frame * 4);
                        Vector nextOffset = new Vector(
                                Math.cos(nextAngle) * currentRadius,
                                0.2f + progress * 0.5f,
                                Math.sin(nextAngle) * currentRadius
                        );
                        animateLine(center.clone().add(cornerOffset), center.clone().add(nextOffset), MOON_YELLOW, world);
                    }
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateRisingBlade(final org.bukkit.Location center, final org.bukkit.World world) {
        new BukkitRunnable() {
            int bladeFrame = 0;
            public void run() {
                if (bladeFrame >= 30) { this.cancel(); return; }
                
                float y = 5.0f + bladeFrame * 0.4f;
                org.bukkit.Location bladeLoc = center.clone().add(0, y, 0);
                
                // Blade silhouette with grid pattern                for (int row = -2; row <= 2; row++) {
                    for (int col = -1; col <= 1; col++) {
                        if (random.nextInt(4) == 0) continue;
                        Vector bladeOffset = new Vector(
                                col * 0.5f,
                                row * 0.4f,
                                0
                        );
                        Color bladeColor = (row + col) % 2 == 0 ? MOON_YELLOW : MOON_WHITE;
                        world.spawnParticle(Particle.DUST, bladeLoc.clone().add(bladeOffset), 1, 
                                new Particle.DustOptions(bladeColor, 1.4f));
                    }
                }
                
                // Golden glow pulse
                if (bladeFrame % 4 == 0) {
                    animateChargeAura(bladeLoc, world);
                }
                bladeFrame++;
            }
        }.runTaskTimer(plugin, 20, 2);
    }

    private void animateDomainImpact(final Player p, final org.bukkit.Location center, 
            final float radius, final org.bukkit.World world) {
        
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 1.3f);
        
        // Impact burst
        world.spawnParticle(Particle.DUST, center, 40, 
                new Particle.DustOptions(MOON_YELLOW, 2.0f));
        world.spawnParticle(Particle.DUST, center, 30, 
                new Particle.DustOptions(MOON_WHITE, 1.6f));
        
        // Light pillars at domain edges
        for (int i = 0; i < 8; i++) {
            final int idx = i;
            new BukkitRunnable() {
                public void run() {
                    double angle = Math.toRadians(idx * 45);
                    org.bukkit.Location pillarLoc = center.clone().add(
                            Math.cos(angle) * radius * 0.85f,
                            0,
                            Math.sin(angle) * radius * 0.85f
                    );
                    animateLightPillar(pillarLoc, world);
                }
            }.runTaskLater(plugin, i * 3);
        }
                // Damage in domain
        for (org.bukkit.entity.Entity en : world.getNearbyEntities(center, radius, radius, radius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                LivingEntity le = (LivingEntity) en;
                le.damage(8.0, p);
                le.setVelocity(new Vector(0, 0.7f, 0));
            }
        }
        
        // Self heal
        try {
            if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 6.0));
            }
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.ABSORPTION, 180, 1, false, false));
        } catch (Exception ignored) {}
    }

    private void animateLightPillar(final org.bukkit.Location loc, final org.bukkit.World world) {
        new BukkitRunnable() {
            int height = 0;
            public void run() {
                if (height >= 18) { this.cancel(); return; }
                
                org.bukkit.Location pillarLoc = loc.clone().add(0, height, 0);
                world.spawnParticle(Particle.DUST, pillarLoc, 3, 
                        new Particle.DustOptions(MOON_WHITE, 1.5f));
                
                if (height % 3 == 0) {
                    world.spawnParticle(Particle.FLAME, pillarLoc, 1, 0.1f, 0.08f, 0.1f, 0);
                }
                height++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateDomainFinale(final Player p, final org.bukkit.Location center, final org.bukkit.World world) {
        p.setInvulnerable(false);
        
        // Final blessing burst
        animateChargeAura(center.clone().add(0, 1.5f, 0), world);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.4f);
        sendActionBar(p, "§6§l✦ §fBerkah Bulan Menyertaimu!");
        
        // Rising golden particles
        new BukkitRunnable() {
            int finaleFrame = 0;
            public void run() {
                if (finaleFrame >= 20) { this.cancel(); return; }                
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45 + finaleFrame * 7);
                    Vector offset = new Vector(
                            Math.cos(angle) * (1.5f + finaleFrame * 0.1f),
                            finaleFrame * 0.15f,
                            Math.sin(angle) * (1.5f + finaleFrame * 0.1f)
                    );
                    world.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), 2, 
                            new Particle.DustOptions(MOON_YELLOW, 1.6f));
                }
                finaleFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateLine(final org.bukkit.Location from, final org.bukkit.Location to, 
            final Color color, final org.bukkit.World world) {
        Vector dir = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        if (dist < 0.1) return;
        Vector step = dir.clone().normalize().multiply(0.25f);
        
        for (double i = 0; i < dist; i += 0.25) {
            org.bukkit.Location lineLoc = from.clone().add(step.clone().multiply((float)(i / 0.25)));
            world.spawnParticle(Particle.DUST, lineLoc, 1, new Particle.DustOptions(color, 1.3f));
        }
    }

    private void animateChargeAura(final org.bukkit.Location loc, final org.bukkit.World world) {
        for (int r = 0; r < 3; r++) {
            final int ring = r;
            new BukkitRunnable() {
                public void run() {
                    for (int a = 0; a < 20; a++) {
                        double angle = Math.toRadians(a * 18);
                        Vector offset = new Vector(
                                Math.cos(angle) * (1.0f + ring * 0.4f),
                                0.2f,
                                Math.sin(angle) * (1.0f + ring * 0.4f)
                        );
                        world.spawnParticle(Particle.DUST, loc.clone().add(offset), 1, 
                                new Particle.DustOptions(MOON_YELLOW, 1.4f));
                    }
                }
            }.runTaskLater(plugin, r * 3);
        }
    }

    private void animateChargeSequence(final Player p) {        new BukkitRunnable() {
            int pulse = 0;
            public void run() {
                PlayerSkillData data = getData(p);
                if (!data.isCharging || !p.isOnline()) { this.cancel(); return; }
                
                double radius = 1.0 + Math.sin(pulse * 0.3) * 0.5;
                for (int a = 0; a < 16; a++) {
                    double angle = Math.toRadians(a * 22.5);
                    org.bukkit.Location auraLoc = p.getLocation().add(
                            Math.cos(angle) * radius,
                            0.6f + (float)(Math.sin(pulse * 0.2) * 0.3),
                            Math.sin(angle) * radius
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 2, 
                            new Particle.DustOptions(MOON_YELLOW, 1.6f));
                }
                
                int bars = Math.min(5, pulse / 6);
                StringBuilder bar = new StringBuilder("§7[§f");
                for (int i = 0; i < bars; i++) bar.append("▮");
                for (int i = 0; i < 5 - bars; i++) bar.append("▯");
                bar.append("]");
                sendActionBar(p, "§6§l✦ §fBerkah Terkumpul §7" + bar.toString());
                pulse++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // ==========================================
    // 📦 UTILS
    // ==========================================
    private boolean hasLunarShield(final Player p) {
        final ItemStack offhand = p.getInventory().getItemInOffHand();
        return offhand != null && offhand.hasItemMeta() && 
                offhand.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE);
    }

    private PlayerSkillData getData(final Player p) {
        final java.util.UUID uuid = p.getUniqueId();
        if (!playerData.containsKey(uuid)) playerData.put(uuid, new PlayerSkillData());
        return playerData.get(uuid);
    }

    private boolean isLunarBlade(final Player p) {
        final ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
                item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
    private void sendActionBar(final Player p, final String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    // ==========================================
    // 📊 PLAYER DATA CLASS
    // ==========================================
    private static class PlayerSkillData {
        long lastHitTime = 0;
        long lastHitStart = 0;
        boolean skill1Used = false;
        boolean skill2Cooldown = false;
        boolean skill3Cooldown = false;
        boolean isCharging = false;
        boolean moonStepReady = true;
        int lunarGauge = 0;
        long chargeStart = 0;
        void addGauge(final int amount) { lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount); }
    }
                                        }
