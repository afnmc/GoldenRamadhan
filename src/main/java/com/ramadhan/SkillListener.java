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

    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color MOON_WHITE = Color.fromRGB(255, 250, 240);
    private static final Color CRESCENT_SILVER = Color.fromRGB(200, 200, 220);
    private static final Color STAR_SPARKLE = Color.fromRGB(255, 240, 180);
    private static final Color WIND_CYAN = Color.fromRGB(150, 220, 255);

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
        if (!(e.getDamager() instanceof Player)) return;        Player p = (Player) e.getDamager();
        if (!isLunarBlade(p)) return;
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
            animateMoonlightBeam(p, target);
            return;
        }
        if (now - data.lastHitStart > SKILL1_HOLD_MS + 300) {
            data.lastHitStart = 0;
            data.skill1Used = false;
        }

        target.damage(2.0, p);
        animateWindSlash(target.getLocation().add(0, 1, 0), p.getWorld(), p.getLocation().getDirection());
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (hasLunarShield(p)) {
            e.setDamage(e.getDamage() * 0.85f);
            if (random.nextInt(100) < 30) {
                epicSparkle(p.getLocation().add(0, 1.3f, 0), CRESCENT_SILVER, 5);
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
                data.chargeStart = System.currentTimeMillis();                sendActionBar(p, "§6§l✦ §fMenahan... §7(Lepas untuk Panggilan Bulan)");
                animateChargeSequence(p);
            } else if (data.isCharging) {
                long ct = System.currentTimeMillis() - data.chargeStart;
                if (ct >= 1000) {
                    data.isCharging = false;
                    animateMoonSummonUltimate(p);
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
                animateBlessingRain(p);
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
            epicAura(p.getLocation().add(0, 1.6f, 0), GOLD, 3);
        }
        if (armorManager.tryMoonStep(p)) {
            data.moonStepReady = false;
            new BukkitRunnable() { public void run() { getData(p).moonStepReady = true; }}.runTaskLater(plugin, 60);
        }
    }

    // ==========================================
    // 🌀 WIND SLASH EFFECT
    // ==========================================
    private void animateWindSlash(org.bukkit.Location loc, org.bukkit.World world, Vector direction) {
        double slashAngle = (random.nextDouble() - 0.5) * 90;
        double rad = Math.toRadians(slashAngle);
                for (int i = 0; i < 12; i++) {
            final int frame = i;
            new BukkitRunnable() {
                public void run() {
                    double progress = (double) frame / 11.0;
                    float randX = (float)((random.nextDouble() - 0.5) * 0.4);
                    float randY = (float)(random.nextDouble() * 0.5);
                    float randZ = (float)((random.nextDouble() - 0.5) * 0.4);
                    
                    Vector windOffset = new Vector(
                            Math.cos(rad) * (0.3f + (float)(progress * 0.8f)) + randX,
                            (float)(progress * 0.6f) + randY,
                            Math.sin(rad) * (0.3f + (float)(progress * 0.8f)) + randZ
                    );
                    
                    org.bukkit.Location particleLoc = loc.clone().add(windOffset);
                    Color windColor = frame % 3 == 0 ? WIND_CYAN : CRESCENT_SILVER;
                    float size = 1.2f + (float)(random.nextDouble() * 0.5f);
                    
                    world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(windColor, size));
                    if (random.nextInt(4) == 0) {
                        world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1f, 0.1f, 0.1f, 0);
                    }
                }
            }.runTaskLater(plugin, frame * 2);
        }
        
        new BukkitRunnable() {
            public void run() {
                for (int i = 0; i < 20; i++) {
                    final int spark = i;
                    new BukkitRunnable() {
                        public void run() {
                            Vector spread = new Vector(
                                    (float)((random.nextDouble() - 0.5) * 2.5),
                                    (float)(random.nextDouble() * 2.0),
                                    (float)((random.nextDouble() - 0.5) * 2.5)
                            );
                            Color burstColor = random.nextInt(3) == 0 ? GOLD : STAR_SPARKLE;
                            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, 
                                    new Particle.DustOptions(burstColor, 1.3f + (float)(random.nextDouble() * 0.4f)));
                        }
                    }.runTaskLater(plugin, spark);
                }
            }
        }.runTaskLater(plugin, 24);
    }

    // ==========================================
    // ⚔️ SKILL 1: SERANGAN CAHAYA BULAN    // ==========================================
    private void animateMoonlightBeam(final Player p, final LivingEntity target) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location startLoc = p.getLocation().clone();
        final org.bukkit.Location targetLoc = target.getLocation().clone();
        final Vector direction = targetLoc.toVector().subtract(startLoc.toVector()).setY(0).normalize();
        final double distance = startLoc.distance(targetLoc);

        final Vector dashDirection = direction.clone();
        new BukkitRunnable() {
            int dashFrame = 0;
            public void run() {
                if (dashFrame >= 10) { this.cancel(); return; }
                float dashVariation = (float)((random.nextDouble() - 0.5) * 0.15);
                p.setVelocity(dashDirection.clone().multiply(2.5f + dashVariation).setY(0.3f));
                
                for (int i = 0; i < 3; i++) {
                    float randX = (float)((random.nextDouble() - 0.5) * 0.3);
                    float randY = (float)(random.nextDouble() * 0.4);
                    float randZ = (float)((random.nextDouble() - 0.5) * 0.3);
                    org.bukkit.Location afterimageLoc = p.getLocation().clone().add(randX, randY, randZ);
                    world.spawnParticle(Particle.DUST, afterimageLoc, 2, 
                            new Particle.DustOptions(CRESCENT_SILVER, 1.6f + (float)(random.nextDouble() * 0.3f)));
                }
                dashFrame++;
            }
        }.runTaskTimer(plugin, 0, 1);

        new BukkitRunnable() {
            int beamFrame = 0;
            public void run() {
                if (beamFrame >= 18) {
                    executeBeamImpact(p, target, targetLoc, world, direction);
                    this.cancel(); return;
                }
                double progress = (double) beamFrame / 17.0;
                org.bukkit.Location beamLoc = startLoc.clone().add(direction.clone().multiply(progress * distance));
                
                float coreSize = 2.0f + (float)(random.nextDouble() * 0.5f);
                world.spawnParticle(Particle.DUST, beamLoc, 5, new Particle.DustOptions(MOON_WHITE, coreSize));
                
                for (int a = 0; a < 8; a++) {
                    double angle = (random.nextDouble() * 360) + (beamFrame * 15);
                    double rad = Math.toRadians(angle);
                    float radiusVariation = 0.4f + (float)(random.nextDouble() * 0.2f);
                    Vector auraOffset = new Vector(
                            Math.cos(rad) * radiusVariation,
                            (float)(random.nextDouble() * 0.15),
                            Math.sin(rad) * radiusVariation
                    );                    world.spawnParticle(Particle.DUST, beamLoc.clone().add(auraOffset), 1, 
                            new Particle.DustOptions(GOLD, 1.5f + (float)(random.nextDouble() * 0.3f)));
                }
                
                if (beamFrame % 2 == 0) {
                    int sparkCount = 3 + random.nextInt(3);
                    for (int s = 0; s < sparkCount; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            Vector spread = new Vector(
                                    (float)((random.nextDouble() - 0.5) * 0.6),
                                    (float)(random.nextDouble() * 0.7),
                                    (float)((random.nextDouble() - 0.5) * 0.6)
                            );
                            world.spawnParticle(Particle.DUST, beamLoc.clone().add(spread), 1, 
                                    new Particle.DustOptions(STAR_SPARKLE, 1.3f + (float)(random.nextDouble() * 0.3f)));
                        }}.runTaskLater(plugin, spark);
                    }
                }
                beamFrame++;
            }
        }.runTaskTimer(plugin, 10, 1);
    }

    private void executeBeamImpact(final Player p, final LivingEntity target, 
            final org.bukkit.Location impactLoc, final org.bukkit.World world, final Vector direction) {
        
        final float impactVariation = (float)((random.nextDouble() - 0.5) * 0.2);
        
        p.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f + impactVariation, 2.0f);
        p.playSound(impactLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.5f);
        world.spawnParticle(Particle.EXPLOSION, impactLoc, 2);
        
        for (int ring = 0; ring < 3; ring++) {
            final int r = ring;
            new BukkitRunnable() {
                public void run() {
                    for (int a = 0; a < 36; a++) {
                        double angle = (random.nextDouble() * 360) + (r * 10);
                        double rad = Math.toRadians(angle);
                        float radius = 1.6f + r * 0.9f + (float)(random.nextDouble() * 0.3f);
                        float heightVar = (float)(random.nextDouble() * 0.2f);
                        Vector ringOffset = new Vector(
                                Math.cos(rad) * radius,
                                0.2f + heightVar,
                                Math.sin(rad) * radius
                        );
                        world.spawnParticle(Particle.DUST, impactLoc.clone().add(ringOffset), 1, 
                                new Particle.DustOptions(GOLD, 1.6f + (float)(random.nextDouble() * 0.3f)));
                    }                }
            }.runTaskLater(plugin, r * 3 + random.nextInt(3));
        }
        
        for (int s = 0; s < 30; s++) {
            final int spark = s;
            new BukkitRunnable() {
                public void run() {
                    Vector spread = new Vector(
                            (float)((random.nextDouble() - 0.5) * 3.0),
                            (float)(random.nextDouble() * 2.5),
                            (float)((random.nextDouble() - 0.5) * 3.0)
                    );
                    Color burstColor = random.nextInt(4) == 0 ? GOLD : STAR_SPARKLE;
                    world.spawnParticle(Particle.DUST, impactLoc.clone().add(spread), 1, 
                            new Particle.DustOptions(burstColor, 1.3f + (float)(random.nextDouble() * 0.5f)));
                }
            }.runTaskLater(plugin, spark + random.nextInt(5));
        }
        
        for (int f = 0; f < 15; f++) {
            final int flame = f;
            new BukkitRunnable() {
                public void run() {
                    Vector spread = new Vector(
                            (float)((random.nextDouble() - 0.5) * 1.8),
                            (float)(random.nextDouble() * 1.5),
                            (float)((random.nextDouble() - 0.5) * 1.8)
                    );
                    world.spawnParticle(Particle.FLAME, impactLoc.clone().add(spread), 1, 
                            0.15f + (float)(random.nextDouble() * 0.1f),
                            0.15f + (float)(random.nextDouble() * 0.1f),
                            0.15f + (float)(random.nextDouble() * 0.1f),
                            0.05f);
                }
            }.runTaskLater(plugin, flame + random.nextInt(4));
        }
        
        target.damage(4.0, p);
        target.setVelocity(direction.clone().multiply(0.9f).setY(0.6f));
        
        new BukkitRunnable() {
            int glowFrame = 0;
            public void run() {
                if (glowFrame >= 15) { this.cancel(); return; }
                double pulse = Math.sin(glowFrame * 0.5 + random.nextDouble()) * 0.4 + 0.8;
                world.spawnParticle(Particle.DUST, impactLoc, 4, 
                        new Particle.DustOptions(MOON_WHITE, 1.8f + (float)(pulse * 0.5f)));
                if (glowFrame % 2 == 0) epicSparkle(impactLoc, GOLD, 2 + random.nextInt(2));
                glowFrame++;            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // ==========================================
    // ✨ SKILL 2: HUJAN BERKAH
    // ==========================================
    private void animateBlessingRain(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        
        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.7f);
        p.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 2.1f);
        sendActionBar(p, "§e§l✦ §f☁️ HUJAN BERKAH DITURUNKAN! ☁️");
        
        final float moonXVar = (float)((random.nextDouble() - 0.5) * 0.8);
        final float moonZVar = (float)((random.nextDouble() - 0.5) * 0.8);
        
        new BukkitRunnable() {
            int moonFrame = 0;
            public void run() {
                if (moonFrame >= 30) { this.cancel(); return; }
                org.bukkit.Location moonLoc = center.clone().add(
                        moonXVar + (float)(Math.sin(moonFrame * 0.3) * 0.4),
                        14 + (float)(Math.sin(moonFrame * 0.35) * 0.7),
                        moonZVar + (float)(Math.cos(moonFrame * 0.3) * 0.4)
                );
                
                for (int a = 0; a < 15; a++) {
                    double angle = -75 + random.nextDouble() * 150;
                    double rad = Math.toRadians(angle);
                    double radius = 1.6 + Math.sin(moonFrame * 0.4 + random.nextDouble()) * 0.5;
                    float heightVar = (float)(random.nextDouble() * 0.3);
                    Vector crescentOffset = new Vector(
                            Math.cos(rad) * radius,
                            heightVar,
                            Math.sin(rad) * radius * 0.5
                    );
                    world.spawnParticle(Particle.DUST, moonLoc.clone().add(crescentOffset), 1, 
                            new Particle.DustOptions(MOON_WHITE, 2.2f + (float)(random.nextDouble() * 0.4f)));
                }
                if (moonFrame % 4 == 0) epicAura(moonLoc, GOLD, 2);
                moonFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);

        for (int orb = 0; orb < 12; orb++) {
            final int o = orb;
            new BukkitRunnable() {
                public void run() {                    double angle = random.nextDouble() * Math.PI * 2;
                    double distance = 1.5 + random.nextDouble() * 4.0;
                    final float tiltX = (float)((random.nextDouble() - 0.5) * 1.5);
                    final float tiltZ = (float)((random.nextDouble() - 0.5) * 1.5);
                    
                    final org.bukkit.Location dropStart = center.clone().add(
                            Math.cos(angle) * distance + tiltX,
                            20 + random.nextDouble() * 3,
                            Math.sin(angle) * distance + tiltZ
                    );
                    
                    final float driftX = (float)((random.nextDouble() - 0.5) * 0.08);
                    final float driftZ = (float)((random.nextDouble() - 0.5) * 0.08);
                    
                    new BukkitRunnable() {
                        int fallFrame = 0;
                        public void run() {
                            if (fallFrame >= 40) {
                                org.bukkit.Location impactLoc = dropStart.clone();
                                impactLoc.setY(center.getY() + (random.nextDouble() - 0.5) * 0.5);
                                
                                world.spawnParticle(Particle.EXPLOSION, impactLoc, 2 + random.nextInt(2));
                                world.spawnParticle(Particle.DUST, impactLoc, 35 + random.nextInt(10), 
                                        new Particle.DustOptions(GOLD, 2.0f + (float)(random.nextDouble() * 0.4f)));
                                world.spawnParticle(Particle.FLAME, impactLoc, 18 + random.nextInt(8), 
                                        0.3f + (float)(random.nextDouble() * 0.15f),
                                        0.3f + (float)(random.nextDouble() * 0.15f),
                                        0.3f + (float)(random.nextDouble() * 0.15f),
                                        0.1f + (float)(random.nextDouble() * 0.05f));
                                world.spawnParticle(Particle.CRIT, impactLoc, 22 + random.nextInt(10), 
                                        0.4f + (float)(random.nextDouble() * 0.15f),
                                        0.4f + (float)(random.nextDouble() * 0.15f),
                                        0.4f + (float)(random.nextDouble() * 0.15f),
                                        0.1f + (float)(random.nextDouble() * 0.05f));
                                world.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 
                                        0.65f + (float)(random.nextDouble() * 0.15f), 1.5f + (float)(random.nextDouble() * 0.3f));
                                
                                float aoeRadius = 3.5f + (float)(random.nextDouble() * 0.8f);
                                for (org.bukkit.entity.Entity en : world.getNearbyEntities(impactLoc, aoeRadius, aoeRadius, aoeRadius)) {
                                    if (en instanceof LivingEntity && !en.equals(p)) {
                                        LivingEntity le = (LivingEntity) en;
                                        le.damage(6.0, p);
                                        Vector kb = new Vector(
                                                (float)((random.nextDouble() - 0.5) * 0.8),
                                                0.5f + (float)(random.nextDouble() * 0.3),
                                                (float)((random.nextDouble() - 0.5) * 0.8)
                                        );
                                        le.setVelocity(kb);
                                        for (int s = 0; s < 10; s++) {
                                            final int spark = s;                                            new BukkitRunnable() { public void run() { 
                                                epicSparkle(le.getLocation().add(0, 1, 0), STAR_SPARKLE, 2 + random.nextInt(2)); 
                                            }}.runTaskLater(plugin, spark + random.nextInt(3));
                                        }
                                    }
                                }
                                
                                new BukkitRunnable() {
                                    int auraFrame = 0;
                                    public void run() {
                                        if (auraFrame >= 22) { this.cancel(); return; }
                                        double radius = 0.7 + auraFrame * 0.2 + random.nextDouble() * 0.15;
                                        for (int a = 0; a < 24; a++) {
                                            double angle = random.nextDouble() * 360 + auraFrame * 5;
                                            double rad = Math.toRadians(angle);
                                            float heightVar = (float)(random.nextDouble() * 0.15);
                                            Vector auraOffset = new Vector(
                                                    Math.cos(rad) * radius,
                                                    0.1f + heightVar,
                                                    Math.sin(rad) * radius
                                            );
                                            world.spawnParticle(Particle.DUST, impactLoc.clone().add(auraOffset), 1, 
                                                    new Particle.DustOptions(CRESCENT_SILVER, 1.4f + (float)(random.nextDouble() * 0.2f)));
                                        }
                                        if (auraFrame % 3 == 0) epicSparkle(impactLoc, GOLD, 3 + random.nextInt(2));
                                        auraFrame++;
                                    }
                                }.runTaskTimer(plugin, 0, 2);
                                this.cancel(); return;
                            }
                            
                            org.bukkit.Location currentLoc = dropStart.clone();
                            currentLoc.setY(dropStart.getY() - fallFrame * 0.5 + (random.nextDouble() - 0.5) * 0.1);
                            currentLoc.add(driftX * fallFrame, 0, driftZ * fallFrame);
                            
                            world.spawnParticle(Particle.DUST, currentLoc, 4, 
                                    new Particle.DustOptions(MOON_WHITE, 1.8f + (float)(random.nextDouble() * 0.4f)));
                            
                            int trailCount = 4 + random.nextInt(3);
                            for (int t = 0; t < trailCount; t++) {
                                float trailY = (float)(t * 0.5 + 0.3 + random.nextDouble() * 0.2);
                                float trailX = (float)((random.nextDouble() - 0.5) * 0.25);
                                float trailZ = (float)((random.nextDouble() - 0.5) * 0.25);
                                org.bukkit.Location trailLoc = currentLoc.clone().add(trailX, trailY, trailZ);
                                world.spawnParticle(Particle.DUST, trailLoc, 1, 
                                        new Particle.DustOptions(GOLD, 1.5f + (float)(random.nextDouble() * 0.3f)));
                            }
                            
                            if (fallFrame % 3 == 0) {
                                int sparkCount = 3 + random.nextInt(3);                                for (int s = 0; s < sparkCount; s++) {
                                    final int spark = s;
                                    new BukkitRunnable() { public void run() { 
                                        epicSparkle(currentLoc, STAR_SPARKLE, 1 + random.nextInt(2)); 
                                    }}.runTaskLater(plugin, spark + random.nextInt(2));
                                }
                            }
                            if (random.nextInt(5) == 0) {
                                world.spawnParticle(Particle.FLAME, currentLoc, 2 + random.nextInt(2), 
                                        0.1f + (float)(random.nextDouble() * 0.1f),
                                        0.1f + (float)(random.nextDouble() * 0.1f),
                                        0.1f + (float)(random.nextDouble() * 0.1f), 0);
                            }
                            fallFrame++;
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
            }.runTaskLater(plugin, o * 2 + random.nextInt(4));
        }
    }

    // ==========================================
    // 🌕 SKILL 3: PANGGILAN BULAN
    // ==========================================
    private void animateMoonSummonUltimate(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        final double baseRadius = 6.0;
        
        p.setVelocity(new Vector(0, 0.5f, 0));
        p.setInvulnerable(true);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 0.98f);
        p.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.92f);
        p.sendTitle("§f§l🌕", "§6§l✦ PANGGILAN BULAN ✦", 6, 28, 9);
        sendActionBar(p, "§6§l🌙 §fBerkah Bulan Suci Diturunkan...");

        for (int corner = 0; corner < 6; corner++) {
            final int c = corner;
            new BukkitRunnable() {
                int frame = 0;
                public void run() {
                    if (frame >= 35) { this.cancel(); return; }
                    double baseAngle = Math.toRadians(c * 60);
                    double progress = Math.min(1.0, (double) frame / 32.0);
                    double radiusVar = 0.8 + random.nextDouble() * 0.4;
                    double currentRadius = progress * baseRadius * radiusVar;
                    double angleWobble = (random.nextDouble() - 0.5) * 8;
                    double angle = baseAngle + Math.toRadians(frame * 5 + angleWobble);
                    float heightVar = 0.3f + (float)(progress * 0.8) + (float)(random.nextDouble() * 0.2);
                                        org.bukkit.Location cornerLoc = center.clone().add(
                            Math.cos(angle) * currentRadius,
                            heightVar,
                            Math.sin(angle) * currentRadius
                    );
                    
                    int particleCount = 2 + random.nextInt(3);
                    float particleSize = 1.8f + (float)(random.nextDouble() * 0.5f);
                    double pulse = Math.sin(frame * 0.4 + random.nextDouble()) * 0.4 + 0.7;
                    
                    for (int i = 0; i < particleCount; i++) {
                        float offset = (float)((random.nextDouble() - 0.5) * 0.3);
                        world.spawnParticle(Particle.DUST, cornerLoc.clone().add(offset, 0, offset), 1, 
                                new Particle.DustOptions(GOLD, (float)(particleSize * pulse)));
                    }
                    
                    if (frame % 4 == 0) {
                        world.spawnParticle(Particle.FLAME, cornerLoc, 2 + random.nextInt(2), 
                                0.15f + (float)(random.nextDouble() * 0.1f),
                                0.15f + (float)(random.nextDouble() * 0.1f),
                                0.15f + (float)(random.nextDouble() * 0.1f), 0);
                    }
                    
                    if (frame % 6 == 0 && frame > 12 && random.nextInt(3) == 0) {
                        int nextC = (c + 1) % 6;
                        double nextAngle = Math.toRadians(nextC * 60) + Math.toRadians(frame * 5);
                        org.bukkit.Location nextLoc = center.clone().add(
                                Math.cos(nextAngle) * currentRadius,
                                heightVar,
                                Math.sin(nextAngle) * currentRadius
                        );
                        elegantLine(cornerLoc, nextLoc, GOLD, 2 + random.nextInt(2));
                    }
                    frame++;
                }
            }.runTaskTimer(plugin, c * 7 + random.nextInt(5), 2);
        }

        new BukkitRunnable() {
            int bladeFrame = 0;
            public void run() {
                if (bladeFrame >= 40) {
                    executeMoonCrashImpact(p, center, world, baseRadius);
                    this.cancel(); return;
                }
                double yVariation = Math.sin(bladeFrame * 0.25 + random.nextDouble()) * 0.3;
                double y = bladeFrame * 0.45 + yVariation;
                final float wobbleX = (float)(Math.sin(bladeFrame * 0.3) * 0.4);
                final float wobbleZ = (float)(Math.cos(bladeFrame * 0.3) * 0.4);
                                org.bukkit.Location bladeLoc = center.clone().add(wobbleX, y + 10, wobbleZ);
                
                for (int a = 0; a < 14; a++) {
                    double angle = -55 + random.nextDouble() * 110;
                    double rad = Math.toRadians(angle);
                    double bladeWidth = 1.6 + Math.sin(bladeFrame * 0.3 + random.nextDouble()) * 0.7;
                    float heightVar = (float)(random.nextDouble() * 0.2);
                    Vector offset = new Vector(
                            Math.cos(rad) * bladeWidth,
                            heightVar,
                            Math.sin(rad) * bladeWidth * 0.45
                    );
                    world.spawnParticle(Particle.DUST, bladeLoc.clone().add(offset), 2, 
                            new Particle.DustOptions(MOON_WHITE, 2.2f + (float)(random.nextDouble() * 0.5f)));
                }
                
                if (bladeFrame % 5 == 0 && random.nextInt(2) == 0) {
                    epicAura(bladeLoc, GOLD, 3 + random.nextInt(2));
                }
                
                if (bladeFrame % 3 == 0) {
                    int sparkCount = 5 + random.nextInt(4);
                    for (int s = 0; s < sparkCount; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            Vector spread = new Vector(
                                    (float)((random.nextDouble() - 0.5) * 3.5),
                                    (float)(random.nextDouble() * 1.5),
                                    (float)((random.nextDouble() - 0.5) * 3.5)
                            );
                            world.spawnParticle(Particle.DUST, bladeLoc.clone().add(spread), 1, 
                                    new Particle.DustOptions(STAR_SPARKLE, 1.4f + (float)(random.nextDouble() * 0.4f)));
                        }}.runTaskLater(plugin, spark + random.nextInt(3));
                    }
                }
                bladeFrame++;
            }
        }.runTaskTimer(plugin, 35 + random.nextInt(5), 2);
    }

    private void executeMoonCrashImpact(final Player p, final org.bukkit.Location center, 
            final org.bukkit.World world, final double baseRadius) {
        
        final float impactVariation = (float)((random.nextDouble() - 0.5) * 0.3);
        
        for (final Player viewer : center.getWorld().getPlayers()) {
            if (viewer.getLocation().distance(center) < baseRadius + 12) {
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 
                        0.45f + impactVariation, 2.0f + (float)(random.nextDouble() * 0.3f));
                viewer.playSound(viewer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,                         0.35f, 2.2f + (float)(random.nextDouble() * 0.4f));
            }
        }
        
        int pillarCount = 8 + random.nextInt(3);
        for (int i = 0; i < pillarCount; i++) {
            final int idx = i;
            new BukkitRunnable() {
                public void run() {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double radiusVar = 0.75 + random.nextDouble() * 0.2;
                    float heightVar = (float)(random.nextDouble() * 0.3);
                    org.bukkit.Location pillarLoc = center.clone().add(
                            Math.cos(angle) * (baseRadius * radiusVar),
                            heightVar,
                            Math.sin(angle) * (baseRadius * radiusVar)
                    );
                    animateElegantPillar(pillarLoc, world, CRESCENT_SILVER);
                }
            }.runTaskLater(plugin, i * 4 + random.nextInt(5));
        }
        
        new BukkitRunnable() {
            int waveFrame = 0;
            public void run() {
                if (waveFrame >= 40) { this.cancel(); return; }
                double ringRadius = baseRadius * 0.4 + waveFrame * 0.22 + random.nextDouble() * 0.15;
                if (ringRadius <= baseRadius * 1.2) {
                    for (int a = 0; a < 36; a++) {
                        double angle = random.nextDouble() * 360 + waveFrame * 4 + random.nextDouble() * 10;
                        double rad = Math.toRadians(angle);
                        float radiusVar = (float)(1.0 + random.nextDouble() * 0.15);
                        float heightVar = 0.08f + (float)(random.nextDouble() * 0.08);
                        org.bukkit.Location ringLoc = center.clone().add(
                                Math.cos(rad) * ringRadius * radiusVar,
                                heightVar,
                                Math.sin(rad) * ringRadius * radiusVar
                        );
                        world.spawnParticle(Particle.DUST, ringLoc, 2, 
                                new Particle.DustOptions(MOON_WHITE, 1.6f + (float)(random.nextDouble() * 0.3f)));
                    }
                }
                if (waveFrame % 4 == 0 && random.nextInt(2) == 0) {
                    for (int a = 0; a < 18; a++) {
                        double angle = random.nextDouble() * 360 - waveFrame * 3;
                        double rad = Math.toRadians(angle);
                        org.bukkit.Location sparkLoc = center.clone().add(
                                Math.cos(rad) * (ringRadius * 0.7),
                                0.15f + (float)(random.nextDouble() * 0.1f),
                                Math.sin(rad) * (ringRadius * 0.7)                        );
                        epicSparkle(sparkLoc, STAR_SPARKLE, 1);
                    }
                }
                if (waveFrame % 5 == 0) {
                    int sparkCount = 6 + random.nextInt(4);
                    for (int s = 0; s < sparkCount; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            org.bukkit.Location sparkLoc = center.clone().add(
                                    (random.nextDouble() - 0.5) * baseRadius * 1.3,
                                    2.0 + random.nextDouble() * 6.0,
                                    (random.nextDouble() - 0.5) * baseRadius * 1.3
                            );
                            epicSparkle(sparkLoc, GOLD, 1 + random.nextInt(2));
                        }}.runTaskLater(plugin, spark + random.nextInt(4));
                    }
                }
                waveFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        float aoeRadius = (float)(baseRadius + random.nextDouble() * 1.5);
        for (org.bukkit.entity.Entity en : world.getNearbyEntities(center, aoeRadius, aoeRadius, aoeRadius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                final LivingEntity le = (LivingEntity) en;
                final double dist = le.getLocation().distance(center);
                final double damage = 8.0 * (1.0 - dist / (aoeRadius * 1.5));
                le.damage(Math.max(damage, 3.5), p);
                Vector kb = new Vector(
                        (float)((random.nextDouble() - 0.5) * 1.2),
                        0.7f + (float)(random.nextDouble() * 0.3),
                        (float)((random.nextDouble() - 0.5) * 1.2)
                );
                le.setVelocity(kb);
                new BukkitRunnable() {
                    int hitFrame = 0;
                    public void run() {
                        if (hitFrame >= 12) { this.cancel(); return; }
                        epicSparkle(le.getLocation().add(0, 1.4, 0), MOON_WHITE, 3 + random.nextInt(3));
                        hitFrame++;
                    }
                }.runTaskTimer(plugin, 0, 3);
                try {
                    le.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.ABSORPTION, 90, 0, false, false));
                    le.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.REGENERATION, 70, 0, false, false));
                } catch (Exception ignored) {}
            }        }
        
        try {
            if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), 
                        p.getHealth() + 6.5 + random.nextDouble() * 1.5));
            }
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.ABSORPTION, 220, 1, false, false));
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.REGENERATION, 140, 0, false, false));
        } catch (Exception ignored) {}
        
        new BukkitRunnable() {
            int finaleFrame = 0;
            public void run() {
                if (finaleFrame >= 30) {
                    if (p.isOnline()) {
                        p.setInvulnerable(false);
                        epicAura(p.getLocation().add(0, 1.8, 0), GOLD, 8);
                        world.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.85f, 1.5f);
                        world.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.65f, 2.2f);
                        sendActionBar(p, "§6§l✦ §fBerkah Bulan Menyertaimu!");
                    }
                    this.cancel(); return;
                }
                for (int i = 0; i < 10; i++) {
                    double angle = random.nextDouble() * 360 + finaleFrame * 8;
                    float radiusVar = 1.5f + finaleFrame * 0.1f + (float)(random.nextDouble() * 0.3);
                    float heightVar = finaleFrame * 0.2f + (float)(random.nextDouble() * 0.15);
                    Vector offset = new Vector(
                            Math.cos(Math.toRadians(angle)) * radiusVar,
                            heightVar,
                            Math.sin(Math.toRadians(angle)) * radiusVar
                    );
                    world.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), 2, 
                            new Particle.DustOptions(GOLD, 1.7f + (float)(random.nextDouble() * 0.4f)));
                }
                if (finaleFrame % 5 == 0) {
                    int sparkCount = 4 + random.nextInt(3);
                    for (int s = 0; s < sparkCount; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            Vector spread = new Vector(
                                    (float)((random.nextDouble() - 0.5) * 2.5),
                                    0.9f + (float)(random.nextDouble() * 1.8),
                                    (float)((random.nextDouble() - 0.5) * 2.5)
                            );
                            world.spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, 
                                    new Particle.DustOptions(STAR_SPARKLE, 1.5f + (float)(random.nextDouble() * 0.4f)));                        }}.runTaskLater(plugin, spark + random.nextInt(3));
                    }
                }
                finaleFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateElegantPillar(final org.bukkit.Location loc, final org.bukkit.World world, final Color color) {
        new BukkitRunnable() {
            int height = 0;
            public void run() {
                if (height >= 22) { this.cancel(); return; }
                final float wobbleX = (float)(Math.sin(height * 0.4 + random.nextDouble()) * 0.25);
                final float wobbleZ = (float)(Math.cos(height * 0.4 + random.nextDouble()) * 0.25);
                org.bukkit.Location pillarLoc = loc.clone().add(wobbleX, height, wobbleZ);
                
                int particleCount = 2 + random.nextInt(3);
                float particleSize = 1.6f + (float)(random.nextDouble() * 0.3f);
                world.spawnParticle(Particle.DUST, pillarLoc, particleCount, 
                        new Particle.DustOptions(color, particleSize));
                
                if (height % 3 == 0 && random.nextInt(2) == 0) {
                    world.spawnParticle(Particle.FLAME, pillarLoc, 2 + random.nextInt(2), 
                            0.1f + (float)(random.nextDouble() * 0.08f),
                            0.07f + (float)(random.nextDouble() * 0.05f),
                            0.1f + (float)(random.nextDouble() * 0.08f), 0);
                }
                if (height % 4 == 0) {
                    for (int a = 0; a < 4; a++) {
                        double angle = random.nextDouble() * 360;
                        double rad = Math.toRadians(angle);
                        float radiusVar = 0.5f + (float)(random.nextDouble() * 0.2f);
                        Vector sparkleOffset = new Vector(Math.cos(rad) * radiusVar, 0, Math.sin(rad) * radiusVar);
                        epicSparkle(pillarLoc.clone().add(sparkleOffset), STAR_SPARKLE, 1);
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
        final Vector step = dir.clone().normalize().multiply(0.3f);
        for (double i = 0; i < dist; i += 0.3) {            float offsetX = (float)((random.nextDouble() - 0.5) * 0.15);
            float offsetY = (float)(random.nextDouble() * 0.1);
            float offsetZ = (float)((random.nextDouble() - 0.5) * 0.15);
            org.bukkit.Location lineLoc = from.clone().add(step.clone().multiply(i / 0.3));
            lineLoc.add(offsetX, offsetY, offsetZ);
            world.spawnParticle(Particle.DUST, lineLoc, 1, 
                    new Particle.DustOptions(color, 1.4f + (float)(random.nextDouble() * 0.2f)));
        }
    }

    // ==========================================
    // 🎨 HELPERS
    // ==========================================
    private void elegantHitEffect(final org.bukkit.Location loc, final org.bukkit.World world) {
        int critCount = 4 + random.nextInt(3);
        for (int i = 0; i < critCount; i++) {
            float spreadX = (float)((random.nextDouble() - 0.5) * 0.15);
            float spreadY = (float)(random.nextDouble() * 0.25);
            float spreadZ = (float)((random.nextDouble() - 0.5) * 0.15);
            world.spawnParticle(Particle.CRIT, loc.clone().add(spreadX, spreadY, spreadZ), 1, 
                    0.1f, 0.2f, 0.1f, 0);
        }
        int dustCount = 3 + random.nextInt(3);
        for (int i = 0; i < dustCount; i++) {
            world.spawnParticle(Particle.DUST, loc, 1, 
                    new Particle.DustOptions(GOLD, 1.2f + (float)(random.nextDouble() * 0.4f)));
        }
    }

    private void epicSparkle(final org.bukkit.Location loc, final Color color, final int count) {
        final org.bukkit.World world = loc.getWorld();
        for (int i = 0; i < count; i++) {
            Vector spread = new Vector(
                    (float)((random.nextDouble() - 0.5) * 0.35),
                    (float)(random.nextDouble() * 0.45),
                    (float)((random.nextDouble() - 0.5) * 0.35)
            );
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, 
                    new Particle.DustOptions(color, 1.25f + (float)(random.nextDouble() * 0.25f)));
        }
    }

    private void epicAura(final org.bukkit.Location loc, final Color color, final int rings) {
        final org.bukkit.World world = loc.getWorld();
        for (int r = 0; r < rings; r++) {
            final int ring = r;
            new BukkitRunnable() {
                public void run() {
                    for (int a = 0; a < 24; a++) {
                        double angle = random.nextDouble() * 360;                        double rad = Math.toRadians(angle);
                        float radiusVar = 1.2f + ring * 0.42f + (float)(random.nextDouble() * 0.15f);
                        float heightVar = 0.25f + (float)(random.nextDouble() * 0.1f);
                        Vector offset = new Vector(
                                Math.cos(rad) * radiusVar,
                                heightVar,
                                Math.sin(rad) * radiusVar
                        );
                        org.bukkit.Location auraLoc = loc.clone().add(offset);
                        world.spawnParticle(Particle.DUST, auraLoc, 1, 
                                new Particle.DustOptions(color, 1.45f + (float)(random.nextDouble() * 0.25f)));
                    }
                }
            }.runTaskLater(plugin, r * 4 + random.nextInt(3));
        }
    }

    private void animateChargeSequence(final Player p) {
        new BukkitRunnable() {
            int pulse = 0;
            public void run() {
                PlayerSkillData data = getData(p);
                if (!data.isCharging || !p.isOnline()) { this.cancel(); return; }
                
                double pulseVar = Math.sin(pulse * 0.35 + random.nextDouble()) * 0.6;
                double radius = 1.2 + pulseVar;
                
                for (int a = 0; a < 18; a++) {
                    double angle = random.nextDouble() * 360;
                    double rad = Math.toRadians(angle);
                    float heightVar = 0.7f + (float)(Math.sin(pulse * 0.2 + random.nextDouble()) * 0.4);
                    org.bukkit.Location auraLoc = p.getLocation().add(
                            Math.cos(rad) * radius,
                            heightVar,
                            Math.sin(rad) * radius
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 2, 
                            new Particle.DustOptions(GOLD, 1.7f + (float)(random.nextDouble() * 0.3f)));
                }
                
                if (pulse % 5 == 0) {
                    int sparkCount = 4 + random.nextInt(3);
                    for (int s = 0; s < sparkCount; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            Vector spread = new Vector(
                                    (float)((random.nextDouble() - 0.5) * 2.0),
                                    0.5f + (float)(random.nextDouble() * 2.0),
                                    (float)((random.nextDouble() - 0.5) * 2.0)
                            );                            p.getWorld().spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, 
                                    new Particle.DustOptions(STAR_SPARKLE, 1.4f + (float)(random.nextDouble() * 0.3f)));
                        }}.runTaskLater(plugin, spark + random.nextInt(3));
                    }
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
        boolean skill2Cooldown = false;        boolean skill3Cooldown = false;
        boolean isCharging = false;
        boolean moonStepReady = true;
        int lunarGauge = 0;
        long chargeStart = 0;
        void addGauge(final int amount) { lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount); }
    }
                                                    }
