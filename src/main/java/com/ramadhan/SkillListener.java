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
        animateSwordSparkle(target.getLocation().add(0, 1, 0), p.getWorld());
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
                sendActionBar(p, "§6§l✦ §fMenahan...");
                animateChargeSequence(p);
            } else if (data.isCharging) {
                long ct = System.currentTimeMillis() - data.chargeStart;
                if (ct >= 1000) {                    data.isCharging = false;
                    animateMoonDomainUltimate(p);
                    data.lunarGauge = 0;
                    data.skill3Cooldown = true;
                    sendActionBar(p, "§6§l✦ §fPANGGILAN BULAN!");
                    new BukkitRunnable() {
                        public void run() {
                            getData(p).skill3Cooldown = false;
                        }
                    }.runTaskLater(plugin, 1200);
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
                animateBlessingStorm(p);
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
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        PlayerSkillData data = getData(p);
        if (data.isCharging && System.currentTimeMillis() % 100 < 25) {
            animateChargeAura(p.getLocation().add(0, 1.6f, 0), p.getWorld());
        }
        if (armorManager.tryMoonStep(p)) {
            data.moonStepReady = false;
            new BukkitRunnable() {
                public void run() {
                    getData(p).moonStepReady = true;
                }
            }.runTaskLater(plugin, 60);
        }
    }

    // ==========================================    // ⚔️ SKILL 1: MOONLIGHT SLASH (EPIC PARTICLES)
    // ==========================================
    private void animateMoonlightSlash(final Player p, final LivingEntity target) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location playerLoc = p.getLocation().clone();
        final org.bukkit.Location targetLoc = target.getLocation().clone();
        final Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).setY(0).normalize();
        animateSwordCharge(p, playerLoc, world);
        
        // SHORT DASH with SWORD-FOCUSED particles
        new BukkitRunnable() {
            int dashFrame = 0;
            public void run() {
                if (dashFrame >= 8) { this.cancel(); return; }
                float dashProgress = (float)dashFrame / 7.0f;
                Vector dashMove = direction.clone().multiply(1.5f * dashProgress);
                org.bukkit.Location dashLoc = playerLoc.clone().add(dashMove);
                Vector swordOffset = direction.clone().multiply(0.8f);
                org.bukkit.Location swordLoc = dashLoc.clone().add(swordOffset);
                
                // 🗡️ SWORD CORE - YELLOW/WHITE gradient (MORE particles)
                for (int i = 0; i < 8; i++) world.spawnParticle(Particle.DUST, swordLoc, 1, new Particle.DustOptions(MOON_YELLOW, 2.2f));
                for (int i = 0; i < 6; i++) world.spawnParticle(Particle.DUST, swordLoc, 1, new Particle.DustOptions(MOON_WHITE, 1.7f));
                
                // 💨 WIND TRAIL around blade (MORE particles, random angles)
                for (int i = 0; i < 6; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    Vector windOffset = new Vector(Math.cos(angle) * 0.5f, (float)(random.nextDouble() * 0.4), Math.sin(angle) * 0.5f);
                    world.spawnParticle(Particle.DUST, swordLoc.clone().add(windOffset), 1, new Particle.DustOptions(WIND_SILVER, 1.3f));
                }
                
                // ✨ SPARKLE bursts around sword
                if (dashFrame % 2 == 0) {
                    for (int s = 0; s < 4; s++) {
                        Vector sparkSpread = new Vector((float)((random.nextDouble()-0.5)*0.7), (float)(random.nextDouble()*0.6), (float)((random.nextDouble()-0.5)*0.7));
                        world.spawnParticle(Particle.DUST, swordLoc.clone().add(sparkSpread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.2f));
                    }
                }
                dashFrame++;
            }
        }.runTaskTimer(plugin, 6, 1);
        
        // GRID SLASH impact
        new BukkitRunnable() {
            public void run() {
                animateGridSlash(targetLoc, direction, world, target);
            }
        }.runTaskLater(plugin, 14);
        
        // LINGERING AFTERGLOW (MORE particles)        new BukkitRunnable() {
            int glowFrame = 0;
            public void run() {
                if (glowFrame >= 15) { this.cancel(); return; }
                org.bukkit.Location glowLoc = targetLoc.clone().add(0, 1, 0);
                double pulse = Math.sin(glowFrame * 0.5) * 0.3 + 0.85;
                
                // Grid fade with MORE particles
                for (int row = -3; row <= 3; row++) {
                    for (int col = -3; col <= 3; col++) {
                        if (random.nextInt(2) == 0) continue;
                        Vector gridOffset = new Vector(row * 0.5f, 0, col * 0.5f);
                        world.spawnParticle(Particle.DUST, glowLoc.clone().add(gridOffset), 1, new Particle.DustOptions(MOON_WHITE, (float)(1.2f * pulse)));
                    }
                }
                
                // Extra sparkle bursts
                if (glowFrame % 2 == 0) {
                    animateSwordSparkle(glowLoc, world);
                    animateSwordSparkle(glowLoc.clone().add(0.5, 0.3, 0), world);
                }
                glowFrame++;
            }
        }.runTaskTimer(plugin, 20, 2);
    }

    private void animateSwordCharge(final Player p, final org.bukkit.Location loc, final org.bukkit.World world) {
        new BukkitRunnable() {
            int chargeFrame = 0;
            public void run() {
                if (chargeFrame >= 6) { this.cancel(); return; }
                Vector swordOffset = p.getLocation().getDirection().multiply(0.9f);
                org.bukkit.Location swordLoc = loc.clone().add(swordOffset);
                
                // Rotating charge ring (MORE particles)
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30 + chargeFrame * 15);
                    Vector ringOffset = new Vector(Math.cos(angle) * 0.6f, 0.35f + (float)(Math.sin(chargeFrame * 0.5) * 0.25), Math.sin(angle) * 0.6f);
                    Color chargeColor = chargeFrame % 2 == 0 ? MOON_YELLOW : MOON_WHITE;
                    world.spawnParticle(Particle.DUST, swordLoc.clone().add(ringOffset), 1, new Particle.DustOptions(chargeColor, 1.5f));
                }
                
                // Inner sparkles
                if (chargeFrame % 2 == 0) {
                    for (int s = 0; s < 5; s++) {
                        Vector sparkSpread = new Vector((float)((random.nextDouble()-0.5)*0.7), (float)(random.nextDouble()*0.6), (float)((random.nextDouble()-0.5)*0.7));
                        world.spawnParticle(Particle.DUST, swordLoc.clone().add(sparkSpread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.2f));
                    }
                }
                                // Flame accents
                if (chargeFrame % 3 == 0) {
                    for (int f = 0; f < 3; f++) world.spawnParticle(Particle.FLAME, swordLoc, 1, 0.12f, 0.12f, 0.12f, 0);
                }
                chargeFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateGridSlash(final org.bukkit.Location center, final Vector direction, final org.bukkit.World world, final LivingEntity target) {
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 1.9f);
        world.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
        final float gridSize = 3.0f;
        final int gridSteps = 12;
        
        // Horizontal lines (MORE particles)
        for (int row = -2; row <= 2; row++) {
            final int r = row;
            new BukkitRunnable() {
                int step = 0;
                public void run() {
                    if (step >= gridSteps) { this.cancel(); return; }
                    float progress = (float)step / (gridSteps - 1);
                    float lineLength = gridSize * progress;
                    for (float t = 0; t <= lineLength; t += 0.25f) {
                        Vector lineOffset = new Vector(t - gridSize * 0.5f, r * 0.6f, 0);
                        lineOffset = rotateVector(lineOffset, direction);
                        Color lineColor = step < 6 ? MOON_YELLOW : MOON_WHITE;
                        world.spawnParticle(Particle.DUST, center.clone().add(lineOffset), 1, new Particle.DustOptions(lineColor, 1.4f));
                        // Extra sparkle on line
                        if (random.nextInt(3) == 0) world.spawnParticle(Particle.DUST, center.clone().add(lineOffset), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.1f));
                    }
                    step++;
                }
            }.runTaskTimer(plugin, r * 2, 1);
        }
        
        // Vertical lines (MORE particles)
        for (int col = -2; col <= 2; col++) {
            final int c = col;
            new BukkitRunnable() {
                int step = 0;
                public void run() {
                    if (step >= gridSteps) { this.cancel(); return; }
                    float progress = (float)step / (gridSteps - 1);
                    float lineLength = gridSize * progress;
                    for (float t = 0; t <= lineLength; t += 0.25f) {
                        Vector lineOffset = new Vector(0, c * 0.6f, t - gridSize * 0.5f);
                        lineOffset = rotateVector(lineOffset, direction);
                        Color lineColor = step < 6 ? MOON_WHITE : MOON_YELLOW;                        world.spawnParticle(Particle.DUST, center.clone().add(lineOffset), 1, new Particle.DustOptions(lineColor, 1.4f));
                        if (random.nextInt(3) == 0) world.spawnParticle(Particle.DUST, center.clone().add(lineOffset), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.1f));
                    }
                    step++;
                }
            }.runTaskTimer(plugin, 6 + c * 2, 1);
        }
        
        // IMPACT BURST (MORE particles)
        new BukkitRunnable() {
            public void run() {
                world.spawnParticle(Particle.EXPLOSION, center, 2);
                // Yellow burst
                for (int i = 0; i < 35; i++) {
                    final int spark = i;
                    new BukkitRunnable() {
                        public void run() {
                            Vector spread = new Vector((float)((random.nextDouble()-0.5)*2.5), (float)(random.nextDouble()*2.0), (float)((random.nextDouble()-0.5)*2.5));
                            world.spawnParticle(Particle.DUST, center.clone().add(spread), 1, new Particle.DustOptions(MOON_YELLOW, 1.3f + (float)(random.nextDouble()*0.5f)));
                        }
                    }.runTaskLater(plugin, spark);
                }
                // White burst
                for (int i = 0; i < 25; i++) {
                    final int spark = i;
                    new BukkitRunnable() {
                        public void run() {
                            Vector spread = new Vector((float)((random.nextDouble()-0.5)*2.0), (float)(random.nextDouble()*1.5), (float)((random.nextDouble()-0.5)*2.0));
                            world.spawnParticle(Particle.DUST, center.clone().add(spread), 1, new Particle.DustOptions(MOON_WHITE, 1.2f + (float)(random.nextDouble()*0.4f)));
                        }
                    }.runTaskLater(plugin, spark + 5);
                }
                // Flame accent
                for (int i = 0; i < 15; i++) {
                    final int flame = i;
                    new BukkitRunnable() {
                        public void run() {
                            Vector spread = new Vector((float)((random.nextDouble()-0.5)*1.8), (float)(random.nextDouble()*1.2), (float)((random.nextDouble()-0.5)*1.8));
                            world.spawnParticle(Particle.FLAME, center.clone().add(spread), 1, 0.15f, 0.15f, 0.15f, 0.05f);
                        }
                    }.runTaskLater(plugin, flame + 3);
                }
                target.damage(4.0, p);
                target.setVelocity(direction.clone().multiply(0.8f).setY(0.5f));
            }
        }.runTaskLater(plugin, 12);
    }

    private Vector rotateVector(Vector v, Vector direction) {
        double angle = Math.atan2(direction.getZ(), direction.getX());        double x = v.getX() * Math.cos(angle) - v.getZ() * Math.sin(angle);
        double z = v.getX() * Math.sin(angle) + v.getZ() * Math.cos(angle);
        return new Vector(x, v.getY(), z);
    }

    private void animateSwordSparkle(final org.bukkit.Location loc, final org.bukkit.World world) {
        for (int i = 0; i < 7; i++) {
            final int spark = i;
            new BukkitRunnable() {
                public void run() {
                    Vector spread = new Vector((float)((random.nextDouble()-0.5)*0.5), (float)(random.nextDouble()*0.6), (float)((random.nextDouble()-0.5)*0.5));
                    world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.2f));
                }
            }.runTaskLater(plugin, spark);
        }
    }

    private void animateShieldSparkle(final org.bukkit.Location loc, final org.bukkit.World world) {
        for (int i = 0; i < 6; i++) {
            Vector spread = new Vector((float)((random.nextDouble()-0.5)*0.4), (float)(random.nextDouble()*0.5), (float)((random.nextDouble()-0.5)*0.4));
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(WIND_SILVER, 1.2f));
        }
    }

    // ==========================================
    // ✨ SKILL 2: BLESSING STORM (MORE PARTICLES)
    // ==========================================
    private void animateBlessingStorm(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.8f);
        sendActionBar(p, "§e§l✦ §f☁️ HUJAN BERKAH DITURUNKAN! ☁️");
        
        // Sky preparation (MORE particles)
        new BukkitRunnable() {
            int prepFrame = 0;
            public void run() {
                if (prepFrame >= 25) { this.cancel(); return; }
                float cloudRadius = 4.5f + (float)(Math.sin(prepFrame * 0.3) * 0.6);
                for (int i = 0; i < 16; i++) {
                    double angle = Math.toRadians(i * 22.5 + prepFrame * 3);
                    Vector cloudOffset = new Vector(Math.cos(angle) * cloudRadius, 8.5f + (float)(Math.sin(prepFrame * 0.4) * 0.5), Math.sin(angle) * cloudRadius);
                    world.spawnParticle(Particle.DUST, center.clone().add(cloudOffset), 2, new Particle.DustOptions(WIND_SILVER, 1.4f));
                }
                if (prepFrame % 4 == 0 && random.nextInt(2) == 0) world.spawnParticle(Particle.FLASH, center.clone().add(0, 9.5, 0), 1);
                prepFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // Falling blessings (MORE orbs, MORE particles per orb)        for (int orb = 0; orb < 12; orb++) {
            final int o = orb;
            new BukkitRunnable() {
                public void run() {
                    animateFallingBlessing(center, world, p, o);
                }
            }.runTaskLater(plugin, o * 2 + random.nextInt(5));
        }
        
        // Ground wave (MORE particles)
        new BukkitRunnable() {
            int waveFrame = 0;
            public void run() {
                if (waveFrame >= 30) { this.cancel(); return; }
                float radius = 2.2f + waveFrame * 0.16f;
                if (radius <= 6.0f) {
                    for (int a = 0; a < 32; a++) {
                        double angle = Math.toRadians(a * 11.25 + waveFrame * 4);
                        Vector ringOffset = new Vector(Math.cos(angle) * radius, 0.12f, Math.sin(angle) * radius);
                        world.spawnParticle(Particle.DUST, center.clone().add(ringOffset), 1, new Particle.DustOptions(MOON_WHITE, 1.5f));
                        if (a % 4 == 0) world.spawnParticle(Particle.DUST, center.clone().add(ringOffset), 1, new Particle.DustOptions(MOON_YELLOW, 1.3f));
                    }
                }
                if (waveFrame % 3 == 0) {
                    for (int s = 0; s < 6; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            org.bukkit.Location sparkLoc = center.clone().add((random.nextDouble()-0.5)*6.0, 1.8+random.nextDouble()*5.0, (random.nextDouble()-0.5)*6.0);
                            animateSwordSparkle(sparkLoc, world);
                        }}.runTaskLater(plugin, spark);
                    }
                }
                waveFrame++;
            }
        }.runTaskTimer(plugin, 35, 2);
    }

    private void animateFallingBlessing(final org.bukkit.Location center, final org.bukkit.World world, final Player p, final int orbIndex) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 1.2 + random.nextDouble() * 4.5;
        final float tiltX = (float)((random.nextDouble() - 0.5) * 1.4);
        final float tiltZ = (float)((random.nextDouble() - 0.5) * 1.4);
        final org.bukkit.Location dropStart = center.clone().add(Math.cos(angle) * distance + tiltX, 17.0, Math.sin(angle) * distance + tiltZ);
        final float driftX = (float)((random.nextDouble() - 0.5) * 0.07);
        final float driftZ = (float)((random.nextDouble() - 0.5) * 0.07);
        
        new BukkitRunnable() {
            int fallFrame = 0;
            public void run() {
                if (fallFrame >= 35) {                    org.bukkit.Location impactLoc = dropStart.clone();
                    impactLoc.setY(center.getY() + (random.nextDouble() - 0.5) * 0.5);
                    
                    // Impact burst (MORE particles)
                    world.spawnParticle(Particle.EXPLOSION, impactLoc, 2);
                    for (int i = 0; i < 25; i++) world.spawnParticle(Particle.DUST, impactLoc, 1, new Particle.DustOptions(MOON_YELLOW, 1.9f));
                    for (int i = 0; i < 20; i++) world.spawnParticle(Particle.DUST, impactLoc, 1, new Particle.DustOptions(MOON_WHITE, 1.5f));
                    for (int i = 0; i < 12; i++) world.spawnParticle(Particle.FLAME, impactLoc, 1, 0.35f, 0.35f, 0.35f, 0.12f);
                    world.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 1.6f);
                    
                    // Damage
                    for (org.bukkit.entity.Entity en : world.getNearbyEntities(impactLoc, 3.2, 3.2, 3.2)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            LivingEntity le = (LivingEntity) en;
                            le.damage(6.0, p);
                            le.setVelocity(new Vector(0, 0.6f, 0));
                            for (int s = 0; s < 5; s++) animateSwordSparkle(le.getLocation().add(0, 1, 0), world);
                        }
                    }
                    
                    // Lingering glow (MORE particles)
                    new BukkitRunnable() {
                        int glowFrame = 0;
                        public void run() {
                            if (glowFrame >= 18) { this.cancel(); return; }
                            float pulse = 1.0f + (float)(Math.sin(glowFrame * 0.4) * 0.25);
                            world.spawnParticle(Particle.DUST, impactLoc, 4, new Particle.DustOptions(MOON_WHITE, pulse));
                            if (glowFrame % 2 == 0) animateSwordSparkle(impactLoc, world);
                            glowFrame++;
                        }
                    }.runTaskTimer(plugin, 0, 2);
                    this.cancel();
                    return;
                }
                
                // Falling orb trail (MORE particles)
                org.bukkit.Location currentLoc = dropStart.clone();
                currentLoc.setY(dropStart.getY() - fallFrame * 0.52);
                currentLoc.add(driftX * fallFrame, 0, driftZ * fallFrame);
                
                // Core orb
                for (int i = 0; i < 4; i++) world.spawnParticle(Particle.DUST, currentLoc, 1, new Particle.DustOptions(MOON_YELLOW, 1.7f));
                
                // Wind trail (MORE particles)
                for (int t = 0; t < 5; t++) {
                    org.bukkit.Location trailLoc = currentLoc.clone().add(0, t * 0.45f + 0.35f, 0);
                    world.spawnParticle(Particle.DUST, trailLoc, 1, new Particle.DustOptions(WIND_SILVER, 1.2f));
                }
                
                // Sparkle around orb                if (fallFrame % 3 == 0) {
                    for (int s = 0; s < 3; s++) animateSwordSparkle(currentLoc, world);
                }
                // Flame accent
                if (random.nextInt(4) == 0) world.spawnParticle(Particle.FLAME, currentLoc, 2, 0.12f, 0.12f, 0.12f, 0);
                fallFrame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🌕 SKILL 3: MOON DOMAIN ULTIMATE (MAX PARTICLES)
    // ==========================================
    private void animateMoonDomainUltimate(final Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        final float domainRadius = 6.5f;
        p.setVelocity(new Vector(0, 0.45f, 0));
        p.setInvulnerable(true);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.1f, 0.97f);
        p.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.94f);
        p.sendTitle("§f§l🌕", "§6§l✦ PANGGILAN BULAN ✦", 6, 28, 9);
        sendActionBar(p, "§6§l🌙 §fDomain Bulan Aktif...");
        
        animateDomainBoundary(center, domainRadius, world);
        animateRisingBlade(center, world);
        new BukkitRunnable() { public void run() { animateDomainImpact(p, center, domainRadius, world); }}.runTaskLater(plugin, 55);
        new BukkitRunnable() {
            int waveFrame = 0;
            public void run() {
                if (waveFrame >= 35) { this.cancel(); return; }
                float radius = 1.7f + waveFrame * 0.19f;
                if (radius <= domainRadius) {
                    for (int a = 0; a < 40; a++) {
                        double angle = Math.toRadians(a * 9 + waveFrame * 3.5);
                        Vector ringOffset = new Vector(Math.cos(angle) * radius, 0.14f, Math.sin(angle) * radius);
                        world.spawnParticle(Particle.DUST, center.clone().add(ringOffset), 1, new Particle.DustOptions(MOON_WHITE, 1.6f));
                        if (a % 5 == 0) world.spawnParticle(Particle.DUST, center.clone().add(ringOffset), 1, new Particle.DustOptions(MOON_YELLOW, 1.4f));
                    }
                }
                if (waveFrame % 3 == 0) {
                    for (int s = 0; s < 8; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            org.bukkit.Location sparkLoc = center.clone().add((random.nextDouble()-0.5)*domainRadius, 2.0+random.nextDouble()*6.0, (random.nextDouble()-0.5)*domainRadius);
                            animateSwordSparkle(sparkLoc, world);
                        }}.runTaskLater(plugin, spark);
                    }
                }
                waveFrame++;            }
        }.runTaskTimer(plugin, 60, 2);
        new BukkitRunnable() { public void run() { animateDomainFinale(p, center, world); }}.runTaskLater(plugin, 95);
    }

    private void animateDomainBoundary(final org.bukkit.Location center, final float radius, final org.bukkit.World world) {
        new BukkitRunnable() {
            int frame = 0;
            public void run() {
                if (frame >= 30) { this.cancel(); return; }
                float progress = (float)frame / 29.0f;
                float currentRadius = radius * progress;
                for (int corner = 0; corner < 6; corner++) {
                    double angle = Math.toRadians(corner * 60 + frame * 4);
                    Vector cornerOffset = new Vector(Math.cos(angle) * currentRadius, 0.25f + progress * 0.6f, Math.sin(angle) * currentRadius);
                    org.bukkit.Location cornerLoc = center.clone().add(cornerOffset);
                    for (int i = 0; i < 4; i++) world.spawnParticle(Particle.DUST, cornerLoc, 1, new Particle.DustOptions(MOON_YELLOW, 1.8f));
                    if (frame > 8 && frame % 3 == 0) {
                        int nextCorner = (corner + 1) % 6;
                        double nextAngle = Math.toRadians(nextCorner * 60 + frame * 4);
                        Vector nextOffset = new Vector(Math.cos(nextAngle) * currentRadius, 0.25f + progress * 0.6f, Math.sin(nextAngle) * currentRadius);
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
                if (bladeFrame >= 35) { this.cancel(); return; }
                float y = 5.5f + bladeFrame * 0.42f;
                org.bukkit.Location bladeLoc = center.clone().add(0, y, 0);
                for (int row = -3; row <= 3; row++) {
                    for (int col = -2; col <= 2; col++) {
                        if (random.nextInt(3) == 0) continue;
                        Vector bladeOffset = new Vector(col * 0.55f, row * 0.42f, 0);
                        Color bladeColor = (row + col) % 2 == 0 ? MOON_YELLOW : MOON_WHITE;
                        world.spawnParticle(Particle.DUST, bladeLoc.clone().add(bladeOffset), 1, new Particle.DustOptions(bladeColor, 1.5f));
                    }
                }
                if (bladeFrame % 4 == 0) animateChargeAura(bladeLoc, world);
                if (bladeFrame % 3 == 0) {
                    for (int s = 0; s < 4; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            Vector spread = new Vector((float)((random.nextDouble()-0.5)*3.0), (float)(random.nextDouble()*1.3), (float)((random.nextDouble()-0.5)*3.0));                            world.spawnParticle(Particle.DUST, bladeLoc.clone().add(spread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.3f));
                        }}.runTaskLater(plugin, spark);
                    }
                }
                bladeFrame++;
            }
        }.runTaskTimer(plugin, 25, 2);
    }

    private void animateDomainImpact(final Player p, final org.bukkit.Location center, final float radius, final org.bukkit.World world) {
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.0f, 1.4f);
        world.spawnParticle(Particle.DUST, center, 50, new Particle.DustOptions(MOON_YELLOW, 2.1f));
        world.spawnParticle(Particle.DUST, center, 40, new Particle.DustOptions(MOON_WHITE, 1.7f));
        world.spawnParticle(Particle.EXPLOSION, center, 3);
        
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            new BukkitRunnable() {
                public void run() {
                    double angle = Math.toRadians(idx * 36);
                    org.bukkit.Location pillarLoc = center.clone().add(Math.cos(angle) * radius * 0.87f, 0, Math.sin(angle) * radius * 0.87f);
                    animateLightPillar(pillarLoc, world);
                }
            }.runTaskLater(plugin, i * 3);
        }
        
        for (org.bukkit.entity.Entity en : world.getNearbyEntities(center, radius, radius, radius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                LivingEntity le = (LivingEntity) en;
                le.damage(8.0, p);
                le.setVelocity(new Vector(0, 0.75f, 0));
                for (int s = 0; s < 4; s++) animateSwordSparkle(le.getLocation().add(0, 1.4, 0), world);
            }
        }
        
        try {
            if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 6.5));
            }
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.ABSORPTION, 200, 1, false, false));
        } catch (Exception ignored) {}
    }

    private void animateLightPillar(final org.bukkit.Location loc, final org.bukkit.World world) {
        new BukkitRunnable() {
            int height = 0;
            public void run() {
                if (height >= 20) { this.cancel(); return; }
                org.bukkit.Location pillarLoc = loc.clone().add(0, height, 0);
                for (int i = 0; i < 4; i++) world.spawnParticle(Particle.DUST, pillarLoc, 1, new Particle.DustOptions(MOON_WHITE, 1.6f));                if (height % 3 == 0) world.spawnParticle(Particle.FLAME, pillarLoc, 2, 0.12f, 0.09f, 0.12f, 0);
                if (height % 4 == 0) {
                    for (double angle = 0; angle < 360; angle += 90) {
                        double rad = Math.toRadians(angle);
                        Vector sparkleOffset = new Vector(Math.cos(rad) * 0.65f, 0, Math.sin(rad) * 0.65f);
                        animateSwordSparkle(pillarLoc.clone().add(sparkleOffset), world);
                    }
                }
                height++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateDomainFinale(final Player p, final org.bukkit.Location center, final org.bukkit.World world) {
        p.setInvulnerable(false);
        animateChargeAura(center.clone().add(0, 1.6f, 0), world);
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 1.5f);
        world.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 2.3f);
        sendActionBar(p, "§6§l✦ §fBerkah Bulan Menyertaimu!");
        new BukkitRunnable() {
            int finaleFrame = 0;
            public void run() {
                if (finaleFrame >= 25) { this.cancel(); return; }
                for (int i = 0; i < 10; i++) {
                    double angle = Math.toRadians(i * 36 + finaleFrame * 8);
                    Vector offset = new Vector(Math.cos(angle) * (1.7f + finaleFrame * 0.12f), finaleFrame * 0.17f, Math.sin(angle) * (1.7f + finaleFrame * 0.12f));
                    world.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), 2, new Particle.DustOptions(MOON_YELLOW, 1.7f));
                }
                if (finaleFrame % 4 == 0) {
                    for (int s = 0; s < 6; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            Vector spread = new Vector((float)((random.nextDouble()-0.5)*2.8), 1.0f+(float)(random.nextDouble()*2.0), (float)((random.nextDouble()-0.5)*2.8));
                            world.spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.4f));
                        }}.runTaskLater(plugin, spark);
                    }
                }
                finaleFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void animateLine(final org.bukkit.Location from, final org.bukkit.Location to, final Color color, final org.bukkit.World world) {
        Vector dir = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        if (dist < 0.1) return;
        Vector step = dir.clone().normalize().multiply(0.28f);
        for (double i = 0; i < dist; i += 0.28) {
            org.bukkit.Location lineLoc = from.clone().add(step.clone().multiply((float)(i / 0.28)));
            world.spawnParticle(Particle.DUST, lineLoc, 1, new Particle.DustOptions(color, 1.4f));            if (random.nextInt(4) == 0) world.spawnParticle(Particle.DUST, lineLoc, 1, new Particle.DustOptions(SPARKLE_GOLD, 1.2f));
        }
    }

    private void animateChargeAura(final org.bukkit.Location loc, final org.bukkit.World world) {
        for (int r = 0; r < 4; r++) {
            final int ring = r;
            new BukkitRunnable() {
                public void run() {
                    for (int a = 0; a < 24; a++) {
                        double angle = Math.toRadians(a * 15);
                        Vector offset = new Vector(Math.cos(angle) * (1.1f + ring * 0.45f), 0.22f, Math.sin(angle) * (1.1f + ring * 0.45f));
                        world.spawnParticle(Particle.DUST, loc.clone().add(offset), 1, new Particle.DustOptions(MOON_YELLOW, 1.5f));
                    }
                }
            }.runTaskLater(plugin, r * 3);
        }
    }

    private void animateChargeSequence(final Player p) {
        new BukkitRunnable() {
            int pulse = 0;
            public void run() {
                PlayerSkillData data = getData(p);
                if (!data.isCharging || !p.isOnline()) { this.cancel(); return; }
                double radius = 1.1 + Math.sin(pulse * 0.32) * 0.55;
                for (int a = 0; a < 18; a++) {
                    double angle = Math.toRadians(a * 20);
                    org.bukkit.Location auraLoc = p.getLocation().add(Math.cos(angle) * radius, 0.65f + (float)(Math.sin(pulse * 0.22) * 0.35), Math.sin(angle) * radius);
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 2, new Particle.DustOptions(MOON_YELLOW, 1.7f));
                }
                if (pulse % 4 == 0) {
                    for (int s = 0; s < 5; s++) {
                        final int spark = s;
                        new BukkitRunnable() { public void run() {
                            Vector spread = new Vector((float)((random.nextDouble()-0.5)*2.2), 0.55f+(float)(random.nextDouble()*2.2), (float)((random.nextDouble()-0.5)*2.2));
                            p.getWorld().spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, new Particle.DustOptions(SPARKLE_GOLD, 1.3f));
                        }}.runTaskLater(plugin, spark);
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
        final ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
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
