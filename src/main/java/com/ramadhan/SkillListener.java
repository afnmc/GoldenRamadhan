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

    // 🎨 EPIC GOLDEN MOON PALETTE
    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color MOON_WHITE = Color.fromRGB(255, 250, 240);
    private static final Color CRESCENT_SILVER = Color.fromRGB(200, 200, 220);
    private static final Color STAR_SPARKLE = Color.fromRGB(255, 240, 180);
    private static final Color LUNAR_PURPLE = Color.fromRGB(180, 160, 220);

    private final GoldenMoon plugin;
    private final ArmorManager armorManager;
    private final Map<java.util.UUID, PlayerSkillData> playerData = new HashMap<java.util.UUID, PlayerSkillData>();

    private static final int MAX_LUNAR_GAUGE = 100;
    private static final int GAUGE_PER_HIT = 15;
    private static final long SKILL1_HOLD_MS = 250;

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        this.armorManager = plugin.getArmorManager();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {        if (!(e.getDamager() instanceof Player)) return;
        final Player p = (Player) e.getDamager();
        if (!isLunarBlade(p)) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        final LivingEntity target = (LivingEntity) e.getEntity();

        final PlayerSkillData data = getData(p);
        final long now = System.currentTimeMillis();
        if (now - data.lastHitTime < 120) return;
        data.lastHitTime = now;
        data.addGauge(GAUGE_PER_HIT);

        // 🌙 SKILL 1: Serangan Cahaya Bulan (Hold 0.25s)
        if (data.lastHitStart == 0) {
            data.lastHitStart = now;
        } else if (now - data.lastHitStart >= SKILL1_HOLD_MS && !data.skill1Used) {
            data.skill1Used = true;
            animateMoonlightBeam(p, target);
            return;
        }
        if (now - data.lastHitStart > SKILL1_HOLD_MS + 300) {
            data.lastHitStart = 0;
            data.skill1Used = false;
        }

        // Basic hit with elegant effect
        target.damage(2.0, p);
        elegantHitEffect(target.getLocation().add(0, 1, 0), p.getWorld());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        final Player p = (Player) e.getEntity();
        if (hasLunarShield(p)) {
            e.setDamage(e.getDamage() * 0.85);
            if (new Random().nextInt(100) < 30) {
                elegantSparkle(p.getLocation().add(0, 1.3, 0), CRESCENT_SILVER, 3);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            final PlayerSkillData data = getData(p);            
            // 🌕 SKILL 3: Panggilan Bulan (Ultimate)
            if (data.lunarGauge >= MAX_LUNAR_GAUGE && !data.isCharging && !data.skill3Cooldown) {
                data.isCharging = true;
                data.chargeStart = System.currentTimeMillis();
                sendActionBar(p, "§6§l✦ §fMenahan... §7(Lepas untuk Panggilan Bulan)");
                animateChargeSequence(p);
            } else if (data.isCharging) {
                final long chargeTime = System.currentTimeMillis() - data.chargeStart;
                if (chargeTime >= 1000) {
                    data.isCharging = false;
                    animateMoonSummonUltimate(p);
                    data.lunarGauge = 0;
                    data.skill3Cooldown = true;
                    sendActionBar(p, "§6§l✦ §f🌕 PANGGILAN BULAN AKTIF! 🌕");
                    new BukkitRunnable() {
                        public void run() {
                            getData(p).skill3Cooldown = false;
                        }
                    }.runTaskLater(plugin, 1200);
                } else {
                    data.isCharging = false;
                    sendActionBar(p, "§c✦ §fTahan minimal 1 detik!");
                }
            }
        }

        // ✨ SKILL 2: Hujan Berkah (Sneak + Left Click)
        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && p.isSneaking()) {
            final PlayerSkillData data = getData(p);
            if (!data.skill2Cooldown) {
                e.setCancelled(true);
                animateBlessingRain(p);
                data.skill2Cooldown = true;
                new BukkitRunnable() {
                    public void run() {
                        getData(p).skill2Cooldown = false;
                    }
                }.runTaskLater(plugin, 60);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        final Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        final PlayerSkillData data = getData(p);

        // Charge animation pulse        if (data.isCharging && System.currentTimeMillis() % 150 < 30) {
            epicAura(p.getLocation().add(0, 1.6, 0), GOLD, 2);
        }

        // Moon Step animation
        if (armorManager.tryMoonStep(p)) {
            data.moonStepReady = false;
            new BukkitRunnable() {
                public void run() {
                    getData(p).moonStepReady = true;
                }
            }.runTaskLater(plugin, 60);
        }
    }

    // ==========================================
    // ⚔️ SKILL 1: SERANGAN CAHAYA BULAN (EPIC)
    // ==========================================
    private void animateMoonlightBeam(final Player p, final LivingEntity target) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location startLoc = p.getLocation().clone();
        final org.bukkit.Location targetLoc = target.getLocation().clone();
        final Vector direction = targetLoc.toVector().subtract(startLoc.toVector()).setY(0).normalize();
        final double distance = startLoc.distance(targetLoc);

        // 🎬 Wind-up: Golden charge particles
        for (int i = 0; i < 5; i++) {
            final int frame = i;
            new BukkitRunnable() {
                public void run() {
                    double progress = (double) frame / 4.0;
                    org.bukkit.Location chargeLoc = startLoc.clone().add(0, 1.2 + progress * 0.3, 0);
                    
                    // Charging spiral
                    for (double angle = 0; angle < 360; angle += 45) {
                        double rad = Math.toRadians(angle + frame * 20);
                        Vector spiralOffset = new Vector(
                                Math.cos(rad) * (0.3 + progress * 0.5),
                                progress * 0.2,
                                Math.sin(rad) * (0.3 + progress * 0.5)
                        );
                        world.spawnParticle(Particle.DUST, chargeLoc.clone().add(spiralOffset), 1,
                                new Particle.DustOptions(GOLD, 1.4f));
                    }
                    if (frame % 2 == 0) {
                        world.spawnParticle(Particle.FLAME, chargeLoc, 2, 0.1f, 0.1f, 0.1f, 0);
                    }
                }
            }.runTaskLater(plugin, i * 2);
        }
        // 🎬 Beam launch: Light projectile
        new BukkitRunnable() {
            int beamFrame = 0;
            public void run() {
                if (beamFrame >= 12) {
                    // Impact sequence
                    executeBeamImpact(p, target, targetLoc, world, direction);
                    this.cancel();
                    return;
                }
                
                // Beam position with trail
                double progress = (double) beamFrame / 11.0;
                org.bukkit.Location beamLoc = startLoc.clone().add(direction.clone().multiply(progress * distance));
                
                // Core beam
                world.spawnParticle(Particle.DUST, beamLoc, 3, new Particle.DustOptions(MOON_WHITE, 2.0f));
                
                // Golden aura around beam
                for (double angle = 0; angle < 360; angle += 60) {
                    double rad = Math.toRadians(angle);
                    Vector auraOffset = new Vector(Math.cos(rad) * 0.4, 0, Math.sin(rad) * 0.4);
                    world.spawnParticle(Particle.DUST, beamLoc.clone().add(auraOffset), 1,
                            new Particle.DustOptions(GOLD, 1.5f));
                }
                
                // Sparkle trail
                if (beamFrame % 2 == 0) {
                    elegantSparkle(beamLoc, STAR_SPARKLE, 2);
                }
                
                beamFrame++;
            }
        }.runTaskTimer(plugin, 10, 1);
    }

    private void executeBeamImpact(final Player p, final LivingEntity target, 
            final org.bukkit.Location impactLoc, final org.bukkit.World world, final Vector direction) {
        
        // 🎬 Impact flash
        p.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 2.0f);
        p.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.4f);
        
        // Explosion burst
        world.spawnParticle(Particle.EXPLOSION, impactLoc, 1);
        
        // Golden shockwave ring
        for (int ring = 0; ring < 3; ring++) {
            final int r = ring;            new BukkitRunnable() {
                public void run() {
                    for (double angle = 0; angle < 360; angle += 12) {
                        double rad = Math.toRadians(angle);
                        Vector ringOffset = new Vector(
                                Math.cos(rad) * (1.5 + r * 0.8),
                                0.2,
                                Math.sin(rad) * (1.5 + r * 0.8)
                        );
                        world.spawnParticle(Particle.DUST, impactLoc.clone().add(ringOffset), 1,
                                new Particle.DustOptions(GOLD, 1.6f));
                    }
                }
            }.runTaskLater(plugin, r * 2);
        }
        
        // Star sparkle burst
        for (int s = 0; s < 15; s++) {
            final int spark = s;
            new BukkitRunnable() {
                public void run() {
                    Vector spread = new Vector(
                            (Math.random() - 0.5) * 2.0,
                            Math.random() * 1.5,
                            (Math.random() - 0.5) * 2.0
                    );
                    world.spawnParticle(Particle.DUST, impactLoc.clone().add(spread), 1,
                            new Particle.DustOptions(STAR_SPARKLE, 1.3f));
                }
            }.runTaskLater(plugin, spark);
        }
        
        // Damage + knockback
        target.damage(4.0, p);
        target.setVelocity(direction.clone().multiply(0.9).setY(0.6));
        
        // Lingering glow
        new BukkitRunnable() {
            int glowFrame = 0;
            public void run() {
                if (glowFrame >= 10) { this.cancel(); return; }
                double pulse = Math.sin(glowFrame * 0.5) * 0.3 + 0.7;
                world.spawnParticle(Particle.DUST, impactLoc, 2,
                        new Particle.DustOptions(MOON_WHITE, 1.8f * pulse));
                glowFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // ==========================================    // ✨ SKILL 2: HUJAN BERKAH (EPIC AOE)
    // ==========================================
    private void animateBlessingRain(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        
        // 🎬 Intro: Sky darkens + moon appears
        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
        p.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 2.0f);
        sendActionBar(p, "§e§l✦ §f☁️ Hujan Berkah Diturunkan! ☁️");
        
        // Moon icon above player
        new BukkitRunnable() {
            int moonFrame = 0;
            public void run() {
                if (moonFrame >= 20) { this.cancel(); return; }
                org.bukkit.Location moonLoc = center.clone().add(0, 12 + Math.sin(moonFrame * 0.3) * 0.5, 0);
                
                // Crescent moon shape
                for (double angle = -60; angle <= 60; angle += 8) {
                    double rad = Math.toRadians(angle);
                    double radius = 1.5 + Math.sin(moonFrame * 0.4) * 0.3;
                    Vector crescentOffset = new Vector(
                            Math.cos(rad) * radius,
                            0,
                            Math.sin(rad) * radius * 0.4
                    );
                    world.spawnParticle(Particle.DUST, moonLoc.clone().add(crescentOffset), 1,
                            new Particle.DustOptions(MOON_WHITE, 2.2f));
                }
                moonFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);

        // 🎬 Blessing orbs falling (8 orbs with elegant trails)
        for (int orb = 0; orb < 8; orb++) {
            final int o = orb;
            new BukkitRunnable() {
                public void run() {
                    // Random position in 4-block radius
                    double angle = Math.random() * Math.PI * 2;
                    double distance = 1.0 + Math.random() * 3.0;
                    final org.bukkit.Location dropStart = center.clone().add(
                            Math.cos(angle) * distance, 15, Math.sin(angle) * distance
                    );
                    
                    // Falling animation with trail
                    new BukkitRunnable() {
                        int fallFrame = 0;
                        public void run() {                            if (fallFrame >= 30) {
                                // 💥 Impact explosion
                                final org.bukkit.Location impactLoc = dropStart.clone().setY(center.getY());
                                
                                // Explosion particles
                                world.spawnParticle(Particle.EXPLOSION, impactLoc, 1);
                                world.spawnParticle(Particle.DUST, impactLoc, 20,
                                        new Particle.DustOptions(GOLD, 2.0f));
                                world.spawnParticle(Particle.FLAME, impactLoc, 12, 0.3f, 0.3f, 0.3f, 0.1f);
                                world.spawnParticle(Particle.CRIT, impactLoc, 15, 0.4f, 0.4f, 0.4f, 0.1f);
                                
                                // Sound
                                world.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 1.5f);
                                
                                // Damage: 6 HP in 3-block radius
                                for (org.bukkit.entity.Entity en : world.getNearbyEntities(impactLoc, 3.0, 3.0, 3.0)) {
                                    if (en instanceof LivingEntity && !en.equals(p)) {
                                        LivingEntity le = (LivingEntity) en;
                                        le.damage(6.0, p);
                                        le.setVelocity(new Vector(0, 0.6, 0));
                                        // Hit sparkle
                                        for (int s = 0; s < 5; s++) {
                                            final int spark = s;
                                            new BukkitRunnable() {
                                                public void run() {
                                                    elegantSparkle(le.getLocation().add(0, 1, 0), STAR_SPARKLE, 1);
                                                }
                                            }.runTaskLater(plugin, spark);
                                        }
                                    }
                                }
                                
                                // Lingering blessing aura
                                new BukkitRunnable() {
                                    int auraFrame = 0;
                                    public void run() {
                                        if (auraFrame >= 15) { this.cancel(); return; }
                                        double radius = 0.5 + auraFrame * 0.15;
                                        for (double angle = 0; angle < 360; angle += 20) {
                                            double rad = Math.toRadians(angle + auraFrame * 5);
                                            Vector auraOffset = new Vector(
                                                    Math.cos(rad) * radius,
                                                    0.1,
                                                    Math.sin(rad) * radius
                                            );
                                            world.spawnParticle(Particle.DUST, impactLoc.clone().add(auraOffset), 1,
                                                    new Particle.DustOptions(CRESCENT_SILVER, 1.4f));
                                        }
                                        auraFrame++;
                                    }                                }.runTaskTimer(plugin, 0, 2);
                                
                                this.cancel();
                                return;
                            }
                            
                            // Falling trail particles
                            org.bukkit.Location currentLoc = dropStart.clone().setY(dropStart.getY() - fallFrame * 0.5);
                            
                            // Core orb
                            world.spawnParticle(Particle.DUST, currentLoc, 2,
                                    new Particle.DustOptions(MOON_WHITE, 1.8f));
                            
                            // Golden trail
                            for (int t = 0; t < 3; t++) {
                                org.bukkit.Location trailLoc = currentLoc.clone().add(0, t * 0.4 + 0.2, 0);
                                world.spawnParticle(Particle.DUST, trailLoc, 1,
                                        new Particle.DustOptions(GOLD, 1.4f));
                            }
                            
                            // Sparkle around orb
                            if (fallFrame % 3 == 0) {
                                elegantSparkle(currentLoc, STAR_SPARKLE, 2);
                            }
                            
                            fallFrame++;
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
            }.runTaskLater(plugin, o * 3);
        }
    }

    // ==========================================
    // 🌕 SKILL 3: PANGGILAN BULAN (ULTIMATE - CINEMATIC)
    // ==========================================
    private void animateMoonSummonUltimate(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        final double radius = 5.0;
        
        // 🎬 CINEMATIC INTRO
        p.setVelocity(new Vector(0, 0.4, 0));
        p.setInvulnerable(true);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 0.95f);
        p.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.5f, 0.9f);
        p.sendTitle("§f§l🌕", "§6§l✦ PANGGILAN BULAN ✦", 5, 25, 8);
        sendActionBar(p, "§6§l🌙 §fBerkah Bulan Suci Diturunkan...");

        // Phase 1: Arena summon - Hexagon with elegant animation        for (int corner = 0; corner < 6; corner++) {
            final int c = corner;
            new BukkitRunnable() {
                int frame = 0;
                public void run() {
                    if (frame >= 25) { this.cancel(); return; }
                    
                    double baseAngle = Math.toRadians(c * 60);
                    double progress = Math.min(1.0, (double) frame / 22.0);
                    double currentRadius = progress * radius;
                    double angle = baseAngle + Math.toRadians(frame * 5);
                    
                    org.bukkit.Location cornerLoc = center.clone().add(
                            Math.cos(angle) * currentRadius,
                            0.3 + progress * 0.6,
                            Math.sin(angle) * currentRadius
                    );
                    
                    // Corner particle with pulse
                    double pulse = Math.sin(frame * 0.4) * 0.3 + 0.7;
                    world.spawnParticle(Particle.DUST, cornerLoc, 3,
                            new Particle.DustOptions(GOLD, 1.8f * pulse));
                    
                    // Flame accent
                    if (frame % 4 == 0) {
                        world.spawnParticle(Particle.FLAME, cornerLoc, 2, 0.15f, 0.15f, 0.15f, 0);
                    }
                    
                    // Connect corners with elegant lines
                    if (frame % 5 == 0 && frame > 8) {
                        int nextC = (c + 1) % 6;
                        double nextAngle = Math.toRadians(nextC * 60) + Math.toRadians(frame * 5);
                        org.bukkit.Location nextLoc = center.clone().add(
                                Math.cos(nextAngle) * currentRadius,
                                0.3 + progress * 0.6,
                                Math.sin(nextAngle) * currentRadius
                        );
                        elegantLine(cornerLoc, nextLoc, GOLD, 2);
                    }
                    frame++;
                }
            }.runTaskTimer(plugin, c * 5, 2);
        }

        // Phase 2: Rising Moon Blade (cinematic summon)
        new BukkitRunnable() {
            int bladeFrame = 0;
            public void run() {
                if (bladeFrame >= 30) {
                    // Phase 3: Moon Crash Impact                    executeMoonCrashImpact(p, center, world, radius);
                    this.cancel();
                    return;
                }
                
                double y = bladeFrame * 0.45;
                org.bukkit.Location bladeLoc = center.clone().add(0, y + 8, 0);
                
                // Blade silhouette with elegant arc
                for (double angle = -45; angle <= 45; angle += 7) {
                    double rad = Math.toRadians(angle);
                    double bladeWidth = 1.6 + Math.sin(bladeFrame * 0.3) * 0.5;
                    Vector offset = new Vector(
                            Math.cos(rad) * bladeWidth,
                            0,
                            Math.sin(rad) * bladeWidth * 0.4
                    );
                    org.bukkit.Location particleLoc = bladeLoc.clone().add(offset);
                    world.spawnParticle(Particle.DUST, particleLoc, 2,
                            new Particle.DustOptions(MOON_WHITE, 2.2f));
                }
                
                // Golden glow pulse around blade
                if (bladeFrame % 5 == 0) {
                    epicAura(bladeLoc, GOLD, 3);
                }
                
                // Sparkle rain around blade
                if (bladeFrame % 3 == 0) {
                    for (int s = 0; s < 4; s++) {
                        final int spark = s;
                        new BukkitRunnable() {
                            public void run() {
                                Vector spread = new Vector(
                                        (Math.random() - 0.5) * 2.5,
                                        Math.random() * 1.0,
                                        (Math.random() - 0.5) * 2.5
                                );
                                world.spawnParticle(Particle.DUST, bladeLoc.clone().add(spread), 1,
                                        new Particle.DustOptions(STAR_SPARKLE, 1.4f));
                            }
                        }.runTaskLater(plugin, spark);
                    }
                }
                
                bladeFrame++;
            }
        }.runTaskTimer(plugin, 28, 2);
    }
    private void executeMoonCrashImpact(final Player p, final org.bukkit.Location center,
            final org.bukkit.World world, final double radius) {
        
        // 🎬 Impact flash (elegant, not blinding)
        for (final Player viewer : center.getWorld().getPlayers()) {
            if (viewer.getLocation().distance(center) < radius + 8) {
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.4f, 1.9f);
                viewer.playSound(viewer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 2.2f);
            }
        }
        
        // Light pillars (Ramadan lantern aesthetic) - 8 pillars
        for (int i = 0; i < 8; i++) {
            final int idx = i;
            new BukkitRunnable() {
                public void run() {
                    double angle = Math.toRadians(idx * 45);
                    org.bukkit.Location pillarLoc = center.clone().add(
                            Math.cos(angle) * (radius * 0.8),
                            0,
                            Math.sin(angle) * (radius * 0.8)
                    );
                    animateElegantPillar(pillarLoc, world, CRESCENT_SILVER);
                }
            }.runTaskLater(plugin, i * 3);
        }
        
        // Ground blessing wave (flowing, expanding rings)
        new BukkitRunnable() {
            int waveFrame = 0;
            public void run() {
                if (waveFrame >= 30) { this.cancel(); return; }
                
                // Primary expanding ring
                double ringRadius = radius * 0.3 + waveFrame * 0.18;
                if (ringRadius <= radius) {
                    for (double angle = 0; angle < 360; angle += 12) {
                        double rad = Math.toRadians(angle + waveFrame * 4);
                        org.bukkit.Location ringLoc = center.clone().add(
                                Math.cos(rad) * ringRadius,
                                0.08,
                                Math.sin(rad) * ringRadius
                        );
                        world.spawnParticle(Particle.DUST, ringLoc, 2,
                                new Particle.DustOptions(MOON_WHITE, 1.6f));
                    }
                }
                
                // Secondary sparkle ring
                if (waveFrame % 4 == 0) {                    for (double angle = 0; angle < 360; angle += 24) {
                        double rad = Math.toRadians(angle - waveFrame * 3);
                        org.bukkit.Location sparkLoc = center.clone().add(
                                Math.cos(rad) * (ringRadius * 0.7),
                                0.15,
                                Math.sin(rad) * (ringRadius * 0.7)
                        );
                        elegantSparkle(sparkLoc, STAR_SPARKLE, 1);
                    }
                }
                
                // Random blessing sparkles in area
                if (waveFrame % 5 == 0) {
                    for (int s = 0; s < 5; s++) {
                        final int spark = s;
                        new BukkitRunnable() {
                            public void run() {
                                org.bukkit.Location sparkLoc = center.clone().add(
                                        (Math.random() - 0.5) * radius,
                                        1.5 + Math.random() * 4.5,
                                        (Math.random() - 0.5) * radius
                                );
                                elegantSparkle(sparkLoc, GOLD, 1);
                            }
                        }.runTaskLater(plugin, spark);
                    }
                }
                waveFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // AOE damage + blessing effect (8 HP in 5-block radius)
        for (org.bukkit.entity.Entity en : world.getNearbyEntities(center, radius, radius, radius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                final LivingEntity le = (LivingEntity) en;
                final double dist = le.getLocation().distance(center);
                final double damage = 8.0 * (1.0 - dist / (radius * 1.3));
                
                le.damage(Math.max(damage, 3.0), p);
                le.setVelocity(new Vector(0, 0.75, 0));
                
                // Hit effect with elegant sparkle
                new BukkitRunnable() {
                    int hitFrame = 0;
                    public void run() {
                        if (hitFrame >= 8) { this.cancel(); return; }
                        elegantSparkle(le.getLocation().add(0, 1.3, 0), MOON_WHITE, 2);
                        hitFrame++;
                    }
                }.runTaskTimer(plugin, 0, 3);                
                // Blessing particles around enemy
                try {
                    le.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.ABSORPTION, 80, 0, false, false));
                    le.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.REGENERATION, 60, 0, false, false));
                } catch (Exception ignored) {}
            }
        }
        
        // 🎁 Self blessing (player)
        try {
            if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 6.0));
            }
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.ABSORPTION, 180, 1, false, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.REGENERATION, 120, 0, false, false));
        } catch (Exception ignored) {}
        
        // 🎬 Final cinematic flourish
        new BukkitRunnable() {
            int finaleFrame = 0;
            public void run() {
                if (finaleFrame >= 20) {
                    if (p.isOnline()) {
                        p.setInvulnerable(false);
                        // Final blessing burst
                        epicAura(p.getLocation().add(0, 1.7, 0), GOLD, 6);
                        world.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.4f);
                        world.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 2.1f);
                        sendActionBar(p, "§6§l✦ §fBerkah Bulan Menyertaimu!");
                    }
                    this.cancel();
                    return;
                }
                
                // Rising golden particles around player
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + finaleFrame * 8);
                    Vector offset = new Vector(
                            Math.cos(angle) * (1.5 + finaleFrame * 0.1),
                            finaleFrame * 0.15,
                            Math.sin(angle) * (1.5 + finaleFrame * 0.1)
                    );
                    world.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), 2,
                            new Particle.DustOptions(GOLD, 1.7f));
                }                finaleFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateElegantPillar(final org.bukkit.Location loc, final org.bukkit.World world, final Color color) {
        new BukkitRunnable() {
            int height = 0;
            public void run() {
                if (height >= 18) { this.cancel(); return; }
                
                org.bukkit.Location pillarLoc = loc.clone().add(0, height, 0);
                
                // Core pillar
                world.spawnParticle(Particle.DUST, pillarLoc, 3,
                        new Particle.DustOptions(color, 1.6f));
                
                // Gentle flame core
                if (height % 3 == 0) {
                    world.spawnParticle(Particle.FLAME, pillarLoc, 2, 0.1f, 0.07f, 0.1f, 0);
                }
                
                // Sparkle around pillar
                if (height % 4 == 0) {
                    for (double angle = 0; angle < 360; angle += 90) {
                        double rad = Math.toRadians(angle);
                        Vector sparkleOffset = new Vector(Math.cos(rad) * 0.5, 0, Math.sin(rad) * 0.5);
                        elegantSparkle(pillarLoc.clone().add(sparkleOffset), STAR_SPARKLE, 1);
                    }
                }
                
                height++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void elegantLine(final org.bukkit.Location from, final org.bukkit.Location to,
            final Color color, final int density) {
        final org.bukkit.World world = from.getWorld();
        final Vector dir = to.toVector().subtract(from.toVector());
        final double dist = from.distance(to);
        if (dist < 0.1) return;
        final Vector step = dir.clone().normalize().multiply(0.4);
        
        for (double i = 0; i < dist; i += 0.4) {
            org.bukkit.Location lineLoc = from.clone().add(step.clone().multiply(i / 0.4));
            world.spawnParticle(Particle.DUST, lineLoc, 1,
                    new Particle.DustOptions(color, 1.4f));
        }
    }
    // ==========================================
    // 🎨 EPIC PARTICLE HELPERS
    // ==========================================

    private void elegantHitEffect(final org.bukkit.Location loc, final org.bukkit.World world) {
        // Crit sparks
        world.spawnParticle(Particle.CRIT, loc, 4, 0.1, 0.2, 0.1, 0);
        // Golden dust
        world.spawnParticle(Particle.DUST, loc, 3, new Particle.DustOptions(GOLD, 1.4f));
        // Small flame accent
        world.spawnParticle(Particle.FLAME, loc, 2, 0.1f, 0.1f, 0.1f, 0);
    }

    private void elegantSparkle(final org.bukkit.Location loc, final Color color, final int count) {
        final org.bukkit.World world = loc.getWorld();
        for (int i = 0; i < count; i++) {
            final Vector spread = new Vector(
                    (Math.random() - 0.5) * 0.25,
                    Math.random() * 0.35,
                    (Math.random() - 0.5) * 0.25
            );
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1,
                    new Particle.DustOptions(color, 1.3f));
        }
    }

    private void epicAura(final org.bukkit.Location loc, final Color color, final int rings) {
        final org.bukkit.World world = loc.getWorld();
        for (int r = 0; r < rings; r++) {
            final int ring = r;
            new BukkitRunnable() {
                public void run() {
                    for (double angle = 0; angle < 360; angle += 18) {
                        double rad = Math.toRadians(angle);
                        Vector offset = new Vector(
                                Math.cos(rad) * (1.2 + ring * 0.42),
                                0.25,
                                Math.sin(rad) * (1.2 + ring * 0.42)
                        );
                        org.bukkit.Location auraLoc = loc.clone().add(offset);
                        world.spawnParticle(Particle.DUST, auraLoc, 2,
                                new Particle.DustOptions(color, 1.5f));
                    }
                }
            }.runTaskLater(plugin, r * 4);
        }
    }

    private void animateChargeSequence(final Player p) {        new BukkitRunnable() {
            int pulse = 0;
            public void run() {
                final PlayerSkillData data = getData(p);
                if (!data.isCharging || !p.isOnline()) { this.cancel(); return; }
                
                // Pulsing crescent aura
                double radius = 1.2 + Math.sin(pulse * 0.35) * 0.55;
                for (double angle = 0; angle < 360; angle += 24) {
                    double rad = Math.toRadians(angle);
                    org.bukkit.Location auraLoc = p.getLocation().add(
                            Math.cos(rad) * radius,
                            0.7 + Math.sin(pulse * 0.2) * 0.35,
                            Math.sin(rad) * radius
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 2,
                            new Particle.DustOptions(GOLD, 1.7f));
                }
                
                // Golden sparkle rain around player
                if (pulse % 5 == 0) {
                    for (int s = 0; s < 4; s++) {
                        final int spark = s;
                        new BukkitRunnable() {
                            public void run() {
                                Vector spread = new Vector(
                                        (Math.random() - 0.5) * 1.5,
                                        0.5 + Math.random() * 1.5,
                                        (Math.random() - 0.5) * 1.5
                                );
                                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1,
                                        new Particle.DustOptions(STAR_SPARKLE, 1.4f));
                            }
                        }.runTaskLater(plugin, spark);
                    }
                }
                
                // Actionbar progress
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
        if (!playerData.containsKey(uuid)) {
            playerData.put(uuid, new PlayerSkillData());
        }
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
        int lunarGauge = 0;
        boolean isCharging = false;
        long chargeStart = 0;
        boolean skill3Cooldown = false;
        boolean moonStepReady = true;
        
        void addGauge(final int amount) {
            lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount);
        }
    }
                             }
