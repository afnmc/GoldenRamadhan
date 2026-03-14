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
    private static final Color AURA_GLOW = Color.fromRGB(255, 220, 120);

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
            animateMoonlightSlash(p, target);
            return;
        }
        if (now - data.lastHitStart > SKILL1_HOLD_MS + 300) {
            data.lastHitStart = 0;
            data.skill1Used = false;
        }
        target.damage(2.0, p);
        spawnParticles(target.getLocation().add(0, 1, 0), p.getWorld(), SPARKLE_GOLD, 25);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (hasLunarShield(p)) {
            e.setDamage(e.getDamage() * 0.85f);
            if (random.nextInt(100) < 30) spawnParticles(p.getLocation().add(0, 1.3f, 0), p.getWorld(), WIND_SILVER, 20);
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
                sendActionBar(p, "§6§l✦ §fMenahan...");
                animateCharge(p);
            } else if (data.isCharging) {
                long ct = System.currentTimeMillis() - data.chargeStart;
                if (ct >= 1000) {
                    data.isCharging = false;                    animateUltimate(p);
                    data.lunarGauge = 0;
                    data.skill3Cooldown = true;
                    sendActionBar(p, "§6§l✦ §fPANGGILAN BULAN!");
                    new BukkitRunnable() { public void run() { getData(p).skill3Cooldown = false; }}.runTaskLater(plugin, 1200);
                } else {
                    data.isCharging = false;
                    sendActionBar(p, "§c✦ §fTahan 1 detik!");
                }
            }
        }
        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && p.isSneaking()) {
            PlayerSkillData data = getData(p);
            if (!data.skill2Cooldown) {
                e.setCancelled(true);
                animateStorm(p);
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
        if (data.isCharging && System.currentTimeMillis() % 100 < 25) spawnParticles(p.getLocation().add(0, 1.6f, 0), p.getWorld(), MOON_YELLOW, 12);
        if (armorManager.tryMoonStep(p)) { data.moonStepReady = false; new BukkitRunnable() { public void run() { getData(p).moonStepReady = true; }}.runTaskLater(plugin, 60); }
    }

    private void animateMoonlightSlash(final Player p, final LivingEntity target) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location playerLoc = p.getLocation().clone();
        final org.bukkit.Location targetLoc = target.getLocation().clone();
        final Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).setY(0).normalize();
        
        // MAX Charge particles
        for (int f = 0; f < 6; f++) {
            final int frame = f;
            new BukkitRunnable() { public void run() {
                Vector swordOffset = p.getLocation().getDirection().multiply(0.9f);
                org.bukkit.Location swordLoc = playerLoc.clone().add(swordOffset);
                // Outer ring - 30 particles
                for (int i = 0; i < 30; i++) {
                    double angle = Math.toRadians(i * 12 + frame * 15);
                    Vector ringOffset = new Vector(Math.cos(angle) * 0.8f, 0.45f + (float)(Math.sin(frame * 0.5) * 0.35), Math.sin(angle) * 0.8f);
                    Color c = frame % 2 == 0 ? MOON_YELLOW : MOON_WHITE;
                    world.spawnParticle(Particle.DUST, swordLoc.clone().add(ringOffset), 1, new Particle.DustOptions(c, 1.7f));
                }                // Inner ring - 20 particles
                for (int i = 0; i < 20; i++) {
                    double angle = Math.toRadians(i * 18 + frame * 20);
                    Vector ringOffset = new Vector(Math.cos(angle) * 0.4f, 0.35f, Math.sin(angle) * 0.4f);
                    world.spawnParticle(Particle.DUST, swordLoc.clone().add(ringOffset), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.4f));
                }
                // Flame bursts
                for (int i = 0; i < 12; i++) world.spawnParticle(Particle.FLAME, swordLoc, 1, 0.18f, 0.18f, 0.18f, 0);
                // Sparkle bursts
                if (frame % 2 == 0) spawnParticles(swordLoc, world, SPARKLE_GOLD, 10);
            }}.runTaskLater(plugin, f * 2);
        }
        
        // MAX Short dash sword particles
        new BukkitRunnable() {
            int dashFrame = 0;
            public void run() {
                if (dashFrame >= 8) { this.cancel(); return; }
                float progress = (float)dashFrame / 7.0f;
                Vector dashMove = direction.clone().multiply(1.5f * progress);
                org.bukkit.Location dashLoc = playerLoc.clone().add(dashMove);
                Vector swordOffset = direction.clone().multiply(0.8f);
                org.bukkit.Location swordLoc = dashLoc.clone().add(swordOffset);
                
                // CORE BLADE - YELLOW (30 particles)
                for (int i = 0; i < 30; i++) world.spawnParticle(Particle.DUST, swordLoc, 1, new Particle.DustOptions(MOON_YELLOW, 2.6f));
                // INNER BLADE - WHITE (25 particles)
                for (int i = 0; i < 25; i++) world.spawnParticle(Particle.DUST, swordLoc, 1, new Particle.DustOptions(MOON_WHITE, 2.1f));
                // GLOW LAYER - AURA (20 particles)
                for (int i = 0; i < 20; i++) world.spawnParticle(Particle.DUST, swordLoc, 1, new Particle.DustOptions(AURA_GLOW, 1.8f));
                
                // WIND TRAIL - SILVER (20 particles with random angles)
                for (int i = 0; i < 20; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    Vector windOffset = new Vector(Math.cos(angle) * 0.7f, (float)(random.nextDouble() * 0.6), Math.sin(angle) * 0.7f);
                    world.spawnParticle(Particle.DUST, swordLoc.clone().add(windOffset), 1, new Particle.DustOptions(WIND_SILVER, 1.5f));
                }
                
                // SPARKLE BURSTS (12 particles)
                if (dashFrame % 2 == 0) {
                    for (int s = 0; s < 12; s++) {
                        Vector sparkSpread = new Vector((float)((random.nextDouble()-0.5)*0.9), (float)(random.nextDouble()*0.8), (float)((random.nextDouble()-0.5)*0.9));
                        world.spawnParticle(Particle.DUST, swordLoc.clone().add(sparkSpread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.4f));
                    }
                }
                
                // FLAME ACCENTS (8 particles)
                if (dashFrame % 3 == 0) {
                    for (int f = 0; f < 8; f++) world.spawnParticle(Particle.FLAME, swordLoc, 1, 0.2f, 0.2f, 0.2f, 0);
                }                
                dashFrame++;
            }
        }.runTaskTimer(plugin, 12, 1);
        
        // MAX Grid slash impact particles
        new BukkitRunnable() { public void run() {
            world.playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.1f, 2.1f);
            world.playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.7f);
            world.spawnParticle(Particle.EXPLOSION, targetLoc, 4);
            
            // GRID LINES - 5x5 with 5 particles each
            for (int row = -4; row <= 4; row++) {
                for (int col = -4; col <= 4; col++) {
                    Vector gridOffset = new Vector(col * 0.6f, row * 0.7f, 0);
                    gridOffset = rotateVector(gridOffset, direction);
                    Color c = (row + col) % 2 == 0 ? MOON_YELLOW : MOON_WHITE;
                    for (int i = 0; i < 5; i++) world.spawnParticle(Particle.DUST, targetLoc.clone().add(gridOffset), 1, new Particle.DustOptions(c, 1.6f));
                }
            }
            
            // MASSIVE YELLOW BURST (100 particles)
            for (int i = 0; i < 100; i++) {
                Vector spread = new Vector((float)((random.nextDouble()-0.5)*3.5), (float)(random.nextDouble()*3.0), (float)((random.nextDouble()-0.5)*3.5));
                world.spawnParticle(Particle.DUST, targetLoc.clone().add(spread), 1, new Particle.DustOptions(MOON_YELLOW, 1.5f + (float)(random.nextDouble()*0.7f)));
            }
            
            // MASSIVE WHITE BURST (75 particles)
            for (int i = 0; i < 75; i++) {
                Vector spread = new Vector((float)((random.nextDouble()-0.5)*3.0), (float)(random.nextDouble()*2.5), (float)((random.nextDouble()-0.5)*3.0));
                world.spawnParticle(Particle.DUST, targetLoc.clone().add(spread), 1, new Particle.DustOptions(MOON_WHITE, 1.4f + (float)(random.nextDouble()*0.6f)));
            }
            
            // FLAME EXPLOSION (40 particles)
            for (int i = 0; i < 40; i++) {
                Vector spread = new Vector((float)((random.nextDouble()-0.5)*2.5), (float)(random.nextDouble()*2.0), (float)((random.nextDouble()-0.5)*2.5));
                world.spawnParticle(Particle.FLAME, targetLoc.clone().add(spread), 1, 0.22f, 0.22f, 0.22f, 0.08f);
            }
            
            // SPARKLE SHOWER (30 particles)
            for (int i = 0; i < 30; i++) {
                final int spark = i;
                new BukkitRunnable() { public void run() {
                    Vector spread = new Vector((float)((random.nextDouble()-0.5)*2.0), (float)(random.nextDouble()*1.5), (float)((random.nextDouble()-0.5)*2.0));
                    world.spawnParticle(Particle.DUST, targetLoc.clone().add(spread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.3f));
                }}.runTaskLater(plugin, spark);
            }
            
            target.damage(4.0, p);
            target.setVelocity(direction.clone().multiply(0.8f).setY(0.5f));        }}.runTaskLater(plugin, 20);
        
        // MAX Afterglow particles
        new BukkitRunnable() {
            int glowFrame = 0;
            public void run() {
                if (glowFrame >= 18) { this.cancel(); return; }
                double pulse = Math.sin(glowFrame * 0.5) * 0.3 + 0.85;
                // 7x7 grid afterglow
                for (int row = -5; row <= 5; row++) {
                    for (int col = -5; col <= 5; col++) {
                        if (random.nextInt(2) == 0) continue;
                        Vector gridOffset = new Vector(row * 0.6f, 0, col * 0.6f);
                        world.spawnParticle(Particle.DUST, targetLoc.clone().add(gridOffset), 1, new Particle.DustOptions(MOON_WHITE, (float)(1.4f * pulse)));
                    }
                }
                // Extra sparkle bursts
                if (glowFrame % 2 == 0) {
                    spawnParticles(targetLoc, world, SPARKLE_GOLD, 15);
                    spawnParticles(targetLoc.clone().add(0.5, 0.4, 0), world, MOON_YELLOW, 10);
                }
                // Lingering flame
                if (glowFrame % 3 == 0) {
                    for (int f = 0; f < 6; f++) world.spawnParticle(Particle.FLAME, targetLoc, 1, 0.15f, 0.15f, 0.15f, 0);
                }
                glowFrame++;
            }
        }.runTaskTimer(plugin, 32, 2);
    }

    private Vector rotateVector(Vector v, Vector direction) {
        double angle = Math.atan2(direction.getZ(), direction.getX());
        double x = v.getX() * Math.cos(angle) - v.getZ() * Math.sin(angle);
        double z = v.getX() * Math.sin(angle) + v.getZ() * Math.cos(angle);
        return new Vector(x, v.getY(), z);
    }

    private void animateStorm(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 2.0f);
        sendActionBar(p, "§e§l✦ §f☁️ HUJAN BERKAH DITURUNKAN! ☁️");
        
        // MAX Sky prep particles
        new BukkitRunnable() {
            int prepFrame = 0;
            public void run() {
                if (prepFrame >= 28) { this.cancel(); return; }
                float cloudRadius = 5.5f + (float)(Math.sin(prepFrame * 0.3) * 0.8);
                for (int i = 0; i < 25; i++) {                    double angle = Math.toRadians(i * 14.4 + prepFrame * 3);
                    Vector cloudOffset = new Vector(Math.cos(angle) * cloudRadius, 9.5f + (float)(Math.sin(prepFrame * 0.4) * 0.7), Math.sin(angle) * cloudRadius);
                    world.spawnParticle(Particle.DUST, center.clone().add(cloudOffset), 4, new Particle.DustOptions(WIND_SILVER, 1.6f));
                }
                // Inner cloud ring
                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians(i * 22.5 + prepFrame * 4);
                    Vector cloudOffset = new Vector(Math.cos(angle) * (cloudRadius * 0.7), 8.8f, Math.sin(angle) * (cloudRadius * 0.7));
                    world.spawnParticle(Particle.DUST, center.clone().add(cloudOffset), 2, new Particle.DustOptions(MOON_WHITE, 1.4f));
                }
                if (prepFrame % 4 == 0 && random.nextInt(2) == 0) {
                    world.spawnParticle(Particle.FLASH, center.clone().add(0, 10.5, 0), 3);
                    spawnParticles(center.clone().add(0, 10, 0), world, SPARKLE_GOLD, 8);
                }
                prepFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // MAX Falling orbs - more orbs, more particles per orb
        for (int orb = 0; orb < 15; orb++) {
            final int o = orb;
            new BukkitRunnable() { public void run() {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 1.8 + random.nextDouble() * 5.5;
                final float tiltX = (float)((random.nextDouble() - 0.5) * 1.8);
                final float tiltZ = (float)((random.nextDouble() - 0.5) * 1.8);
                final org.bukkit.Location dropStart = center.clone().add(Math.cos(angle) * distance + tiltX, 19.0, Math.sin(angle) * distance + tiltZ);
                final float driftX = (float)((random.nextDouble() - 0.5) * 0.09);
                final float driftZ = (float)((random.nextDouble() - 0.5) * 0.09);
                new BukkitRunnable() {
                    int fallFrame = 0;
                    public void run() {
                        if (fallFrame >= 38) {
                            org.bukkit.Location impactLoc = dropStart.clone();
                            impactLoc.setY(center.getY());
                            world.spawnParticle(Particle.EXPLOSION, impactLoc, 4);
                            // YELLOW BURST (60 particles)
                            for (int i = 0; i < 60; i++) world.spawnParticle(Particle.DUST, impactLoc, 1, new Particle.DustOptions(MOON_YELLOW, 2.1f));
                            // WHITE BURST (45 particles)
                            for (int i = 0; i < 45; i++) world.spawnParticle(Particle.DUST, impactLoc, 1, new Particle.DustOptions(MOON_WHITE, 1.7f));
                            // FLAME BURST (30 particles)
                            for (int i = 0; i < 30; i++) world.spawnParticle(Particle.FLAME, impactLoc, 1, 0.45f, 0.45f, 0.45f, 0.18f);
                            // SPARKLE SHOWER (25 particles)
                            for (int i = 0; i < 25; i++) {
                                final int spark = i;
                                new BukkitRunnable() { public void run() {
                                    Vector spread = new Vector((float)((random.nextDouble()-0.5)*2.0), (float)(random.nextDouble()*1.5), (float)((random.nextDouble()-0.5)*2.0));
                                    world.spawnParticle(Particle.DUST, impactLoc.clone().add(spread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.4f));
                                }}.runTaskLater(plugin, spark);
                            }                            world.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 1.8f);
                            for (org.bukkit.entity.Entity en : world.getNearbyEntities(impactLoc, 3.8, 3.8, 3.8)) {
                                if (en instanceof LivingEntity && !en.equals(p)) {
                                    LivingEntity le = (LivingEntity) en;
                                    le.damage(6.0, p);
                                    le.setVelocity(new Vector(0, 0.7f, 0));
                                    spawnParticles(le.getLocation().add(0, 1, 0), world, SPARKLE_GOLD, 12);
                                }
                            }
                            // MAX Lingering glow
                            new BukkitRunnable() {
                                int glowFrame = 0;
                                public void run() {
                                    if (glowFrame >= 25) { this.cancel(); return; }
                                    float pulse = 1.0f + (float)(Math.sin(glowFrame * 0.4) * 0.35);
                                    for (int i = 0; i < 10; i++) world.spawnParticle(Particle.DUST, impactLoc, 1, new Particle.DustOptions(MOON_WHITE, pulse));
                                    if (glowFrame % 2 == 0) {
                                        spawnParticles(impactLoc, world, SPARKLE_GOLD, 8);
                                        spawnParticles(impactLoc.clone().add(0.3, 0.2, 0.3), world, MOON_YELLOW, 5);
                                    }
                                    glowFrame++;
                                }
                            }.runTaskTimer(plugin, 0, 2);
                            this.cancel(); return;
                        }
                        org.bukkit.Location currentLoc = dropStart.clone();
                        currentLoc.setY(dropStart.getY() - fallFrame * 0.52);
                        currentLoc.add(driftX * fallFrame, 0, driftZ * fallFrame);
                        // Core orb - 8 particles
                        for (int i = 0; i < 8; i++) world.spawnParticle(Particle.DUST, currentLoc, 1, new Particle.DustOptions(MOON_YELLOW, 1.9f));
                        // Inner glow - 6 particles
                        for (int i = 0; i < 6; i++) world.spawnParticle(Particle.DUST, currentLoc, 1, new Particle.DustOptions(AURA_GLOW, 1.5f));
                        // Wind trail - 8 particles
                        for (int t = 0; t < 8; t++) {
                            org.bukkit.Location trailLoc = currentLoc.clone().add(0, t * 0.55f + 0.45f, 0);
                            world.spawnParticle(Particle.DUST, trailLoc, 1, new Particle.DustOptions(WIND_SILVER, 1.4f));
                        }
                        // Sparkle around orb
                        if (fallFrame % 3 == 0) spawnParticles(currentLoc, world, SPARKLE_GOLD, 10);
                        // Flame accent
                        if (random.nextInt(3) == 0) {
                            for (int f = 0; f < 5; f++) world.spawnParticle(Particle.FLAME, currentLoc, 1, 0.18f, 0.18f, 0.18f, 0);
                        }
                        fallFrame++;
                    }
                }.runTaskTimer(plugin, 0, 1);
            }}.runTaskLater(plugin, o * 2 + random.nextInt(5));
        }
        
        // MAX Ground wave particles        new BukkitRunnable() {
            int waveFrame = 0;
            public void run() {
                if (waveFrame >= 35) { this.cancel(); return; }
                float radius = 2.6f + waveFrame * 0.19f;
                if (radius <= 7.0f) {
                    // Outer ring - 50 particles
                    for (int a = 0; a < 50; a++) {
                        double angle = Math.toRadians(a * 7.2 + waveFrame * 4);
                        Vector ringOffset = new Vector(Math.cos(angle) * radius, 0.16f, Math.sin(angle) * radius);
                        world.spawnParticle(Particle.DUST, center.clone().add(ringOffset), 1, new Particle.DustOptions(MOON_WHITE, 1.7f));
                    }
                    // Inner ring - 30 particles
                    for (int a = 0; a < 30; a++) {
                        double angle = Math.toRadians(a * 12 + waveFrame * 5);
                        Vector ringOffset = new Vector(Math.cos(angle) * (radius * 0.7), 0.12f, Math.sin(angle) * (radius * 0.7));
                        world.spawnParticle(Particle.DUST, center.clone().add(ringOffset), 1, new Particle.DustOptions(MOON_YELLOW, 1.5f));
                    }
                }
                if (waveFrame % 3 == 0) {
                    for (int s = 0; s < 15; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            org.bukkit.Location sparkLoc = center.clone().add((random.nextDouble()-0.5)*8.0, 2.2+random.nextDouble()*7.0, (random.nextDouble()-0.5)*8.0);
                            spawnParticles(sparkLoc, world, SPARKLE_GOLD, 6);
                        }}.runTaskLater(plugin, spark);
                    }
                }
                waveFrame++;
            }
        }.runTaskTimer(plugin, 45, 2);
    }

    private void animateUltimate(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        final float domainRadius = 7.5f;
        p.setVelocity(new Vector(0, 0.55f, 0));
        p.setInvulnerable(true);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.3f, 0.99f);
        p.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.9f, 0.96f);
        p.sendTitle("§f§l🌕", "§6§l✦ PANGGILAN BULAN ✦", 8, 32, 11);
        sendActionBar(p, "§6§l🌙 §fDomain Bulan Aktif...");
        
        // MAX Boundary particles
        new BukkitRunnable() {
            int frame = 0;
            public void run() {
                if (frame >= 35) { this.cancel(); return; }
                float progress = (float)frame / 34.0f;                float currentRadius = domainRadius * progress;
                for (int corner = 0; corner < 6; corner++) {
                    double angle = Math.toRadians(corner * 60 + frame * 4);
                    Vector cornerOffset = new Vector(Math.cos(angle) * currentRadius, 0.35f + progress * 0.8f, Math.sin(angle) * currentRadius);
                    org.bukkit.Location cornerLoc = center.clone().add(cornerOffset);
                    // Corner core - 8 particles
                    for (int i = 0; i < 8; i++) world.spawnParticle(Particle.DUST, cornerLoc, 1, new Particle.DustOptions(MOON_YELLOW, 2.0f));
                    // Corner glow - 6 particles
                    for (int i = 0; i < 6; i++) world.spawnParticle(Particle.DUST, cornerLoc, 1, new Particle.DustOptions(AURA_GLOW, 1.6f));
                    // Connecting lines
                    if (frame > 12 && frame % 3 == 0) {
                        int nextCorner = (corner + 1) % 6;
                        double nextAngle = Math.toRadians(nextCorner * 60 + frame * 4);
                        Vector nextOffset = new Vector(Math.cos(nextAngle) * currentRadius, 0.35f + progress * 0.8f, Math.sin(nextAngle) * currentRadius);
                        animateLine(center.clone().add(cornerOffset), center.clone().add(nextOffset), MOON_YELLOW, world);
                    }
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // MAX Rising blade particles
        new BukkitRunnable() {
            int bladeFrame = 0;
            public void run() {
                if (bladeFrame >= 38) { this.cancel(); return; }
                float y = 6.5f + bladeFrame * 0.48f;
                org.bukkit.Location bladeLoc = center.clone().add(0, y, 0);
                // Blade grid - 9x7 with 3 particles each
                for (int row = -5; row <= 5; row++) {
                    for (int col = -4; col <= 4; col++) {
                        if (random.nextInt(2) == 0) continue;
                        Vector bladeOffset = new Vector(col * 0.65f, row * 0.48f, 0);
                        Color c = (row + col) % 2 == 0 ? MOON_YELLOW : MOON_WHITE;
                        for (int i = 0; i < 3; i++) world.spawnParticle(Particle.DUST, bladeLoc.clone().add(bladeOffset), 1, new Particle.DustOptions(c, 1.7f));
                    }
                }
                // Blade glow aura
                if (bladeFrame % 4 == 0) spawnParticles(bladeLoc, world, MOON_YELLOW, 25);
                // Sparkle rain
                if (bladeFrame % 3 == 0) {
                    for (int s = 0; s < 10; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            Vector spread = new Vector((float)((random.nextDouble()-0.5)*4.0), (float)(random.nextDouble()*1.8), (float)((random.nextDouble()-0.5)*4.0));
                            world.spawnParticle(Particle.DUST, bladeLoc.clone().add(spread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.5f));
                        }}.runTaskLater(plugin, spark);
                    }
                }
                bladeFrame++;            }
        }.runTaskTimer(plugin, 28, 2);
        
        // MAX Impact particles
        new BukkitRunnable() { public void run() {
            world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 1.6f);
            world.spawnParticle(Particle.DUST, center, 120, new Particle.DustOptions(MOON_YELLOW, 2.3f));
            world.spawnParticle(Particle.DUST, center, 90, new Particle.DustOptions(MOON_WHITE, 1.9f));
            world.spawnParticle(Particle.EXPLOSION, center, 5);
            
            // Light pillars - 14 pillars with MAX particles
            for (int i = 0; i < 14; i++) {
                final int idx = i;
                new BukkitRunnable() { public void run() {
                    double angle = Math.toRadians(idx * 25.7);
                    org.bukkit.Location pillarLoc = center.clone().add(Math.cos(angle) * domainRadius * 0.92f, 0, Math.sin(angle) * domainRadius * 0.92f);
                    new BukkitRunnable() {
                        int height = 0;
                        public void run() {
                            if (height >= 25) { this.cancel(); return; }
                            org.bukkit.Location pillarLoc2 = pillarLoc.clone().add(0, height, 0);
                            // Pillar core - 7 particles
                            for (int j = 0; j < 7; j++) world.spawnParticle(Particle.DUST, pillarLoc2, 1, new Particle.DustOptions(MOON_WHITE, 1.8f));
                            // Pillar glow - 4 particles
                            for (int j = 0; j < 4; j++) world.spawnParticle(Particle.DUST, pillarLoc2, 1, new Particle.DustOptions(AURA_GLOW, 1.5f));
                            // Flame core
                            if (height % 3 == 0) world.spawnParticle(Particle.FLAME, pillarLoc2, 3, 0.15f, 0.12f, 0.15f, 0);
                            height++;
                        }
                    }.runTaskTimer(plugin, 0, 2);
                }}.runTaskLater(plugin, i * 2);
            }
            
            // Damage + effects
            for (org.bukkit.entity.Entity en : world.getNearbyEntities(center, domainRadius, domainRadius, domainRadius)) {
                if (en instanceof LivingEntity && !en.equals(p)) {
                    LivingEntity le = (LivingEntity) en;
                    le.damage(8.0, p);
                    le.setVelocity(new Vector(0, 0.85f, 0));
                    spawnParticles(le.getLocation().add(0, 1.4, 0), world, SPARKLE_GOLD, 10);
                }
            }
            
            // Self heal + buff
            try {
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 7.5));
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 240, 1, false, false));
            } catch (Exception ignored) {}
        }}.runTaskLater(plugin, 65);
                // MAX Finale particles
        new BukkitRunnable() { public void run() {
            p.setInvulnerable(false);
            spawnParticles(center.clone().add(0, 1.8f, 0), world, MOON_YELLOW, 30);
            world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.1f, 1.7f);
            world.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 2.5f);
            sendActionBar(p, "§6§l✦ §fBerkah Bulan Menyertaimu!");
            new BukkitRunnable() {
                int finaleFrame = 0;
                public void run() {
                    if (finaleFrame >= 32) { this.cancel(); return; }
                    // Rising golden particles - 16 per frame
                    for (int i = 0; i < 16; i++) {
                        double angle = Math.toRadians(i * 22.5 + finaleFrame * 10);
                        Vector offset = new Vector(Math.cos(angle) * (1.9f + finaleFrame * 0.16f), finaleFrame * 0.21f, Math.sin(angle) * (1.9f + finaleFrame * 0.16f));
                        world.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), 4, new Particle.DustOptions(MOON_YELLOW, 1.9f));
                    }
                    // Sparkle shower
                    if (finaleFrame % 4 == 0) {
                        for (int s = 0; s < 15; s++) {
                            final int spark = s;
                            new BukkitRunnable() { public void run() {
                                Vector spread = new Vector((float)((random.nextDouble()-0.5)*3.5), 1.4f+(float)(random.nextDouble()*2.4), (float)((random.nextDouble()-0.5)*3.5));
                                world.spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.6f));
                            }}.runTaskLater(plugin, spark);
                        }
                    }
                    // Flame accents
                    if (finaleFrame % 5 == 0) {
                        for (int f = 0; f < 8; f++) world.spawnParticle(Particle.FLAME, p.getLocation(), 1, 0.2f, 0.2f, 0.2f, 0);
                    }
                    finaleFrame++;
                }
            }.runTaskTimer(plugin, 0, 2);
        }}.runTaskLater(plugin, 110);
    }

    private void animateCharge(final Player p) {
        new BukkitRunnable() {
            int pulse = 0;
            public void run() {
                PlayerSkillData data = getData(p);
                if (!data.isCharging || !p.isOnline()) { this.cancel(); return; }
                double radius = 1.3 + Math.sin(pulse * 0.38) * 0.65;
                // Outer aura ring - 24 particles
                for (int a = 0; a < 24; a++) {
                    double angle = Math.toRadians(a * 15);
                    org.bukkit.Location auraLoc = p.getLocation().add(Math.cos(angle) * radius, 0.75f + (float)(Math.sin(pulse * 0.28) * 0.45), Math.sin(angle) * radius);
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 4, new Particle.DustOptions(MOON_YELLOW, 1.9f));
                }                // Inner sparkle ring - 16 particles
                for (int a = 0; a < 16; a++) {
                    double angle = Math.toRadians(a * 22.5 + pulse * 5);
                    org.bukkit.Location auraLoc = p.getLocation().add(Math.cos(angle) * (radius * 0.6), 0.65f, Math.sin(angle) * (radius * 0.6));
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 2, new Particle.DustOptions(SPARKLE_GOLD, 1.5f));
                }
                if (pulse % 4 == 0) spawnParticles(p.getLocation(), p.getWorld(), SPARKLE_GOLD, 12);
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

    private void spawnParticles(org.bukkit.Location loc, org.bukkit.World world, Color color, int count) {
        for (int i = 0; i < count; i++) {
            Vector spread = new Vector((float)((random.nextDouble()-0.5)*0.7), (float)(random.nextDouble()*0.8), (float)((random.nextDouble()-0.5)*0.7));
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(color, 1.4f));
        }
    }

    private void animateLine(final org.bukkit.Location from, final org.bukkit.Location to, final Color color, final org.bukkit.World world) {
        Vector dir = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        if (dist < 0.1) return;
        Vector step = dir.clone().normalize().multiply(0.22f);
        for (double i = 0; i < dist; i += 0.22) {
            org.bukkit.Location lineLoc = from.clone().add(step.clone().multiply((float)(i / 0.22)));
            world.spawnParticle(Particle.DUST, lineLoc, 1, new Particle.DustOptions(color, 1.6f));
            if (random.nextInt(3) == 0) world.spawnParticle(Particle.DUST, lineLoc, 1, new Particle.DustOptions(SPARKLE_GOLD, 1.4f));
        }
    }

    private boolean hasLunarShield(final Player p) {
        final ItemStack offhand = p.getInventory().getItemInOffHand();
        return offhand != null && offhand.hasItemMeta() && offhand.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE);
    }

    private PlayerSkillData getData(final Player p) {
        final java.util.UUID uuid = p.getUniqueId();
        if (!playerData.containsKey(uuid)) playerData.put(uuid, new PlayerSkillData());
        return playerData.get(uuid);
    }

    private boolean isLunarBlade(final Player p) {
        final ItemStack item = p.getInventory().getItemInMainHand();        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private void sendActionBar(final Player p, final String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

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
