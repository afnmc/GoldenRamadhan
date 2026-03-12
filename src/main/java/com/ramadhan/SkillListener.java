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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SkillListener implements Listener {

    // 🎨 GOLDEN MOON RAMADHAN COLORS
    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color MOON_WHITE = Color.fromRGB(255, 250, 240);
    private static final Color CRESCENT_SILVER = Color.fromRGB(200, 200, 220);

    private final GoldenMoon plugin;
    private final ArmorManager armorManager;
    private final Map<java.util.UUID, PlayerSkillData> playerData = new HashMap<>();

    private static final int MAX_LUNAR_GAUGE = 100;
    private static final int GAUGE_PER_HIT = 20;
    private static final long SKILL1_HOLD_MS = 250;
    private static final long SKILL2_COOLDOWN_MS = 3000;
    private static final long SKILL3_COOLDOWN_MS = 60000;

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        this.armorManager = plugin.getArmorManager();
    }
    // ==========================================
    // 🎮 EVENT: BASIC ATTACK → SKILL 1
    // ==========================================
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (!isLunarBlade(p)) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) e.getEntity();

        PlayerSkillData data = getData(p);
        long now = System.currentTimeMillis();

        // Anti-spam: 150ms between hits
        if (now - data.lastHitTime < 150) return;
        data.lastHitTime = now;

        data.addGauge(GAUGE_PER_HIT);

        // 🌙 SKILL 1: Serangan Cahaya Bulan (Hold hit 0.25s)
        if (data.lastHitStart == 0) {
            data.lastHitStart = now;
        } else if (now - data.lastHitStart >= SKILL1_HOLD_MS && !data.skill1Used) {
            data.skill1Used = true;
            executeMoonlightBeam(p, target);
            return;
        }

        // Reset skill1 flag
        if (now - data.lastHitStart > SKILL1_HOLD_MS + 300) {
            data.lastHitStart = 0;
            data.skill1Used = false;
        }

        // Basic attack (light damage)
        target.damage(2.0, p);
        gentleHitEffect(target.getLocation().add(0, 1, 0), p.getWorld());
    }

    // ==========================================
    // 🛡️ SHIELD PASSIVE: Damage Reduction
    // ==========================================
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        
        if (hasLunarShield(p)) {
            e.setDamage(e.getDamage() * 0.85); // 15% reduction            if (new Random().nextInt(100) < 25) {
                gentleSparkle(p.getLocation().add(0, 1.3, 0), CRESCENT_SILVER, 2);
            }
        }
    }

    // ==========================================
    // 🎮 EVENT: SKILL 2 (Sneak+Hit) & SKILL 3 (Ultimate)
    // ==========================================
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            PlayerSkillData data = getData(p);

            // 🌕 SKILL 3: Panggilan Bulan (Ultimate)
            if (data.lunarGauge >= MAX_LUNAR_GAUGE && !data.isCharging && !data.skill3Cooldown) {
                data.isCharging = true;
                data.chargeStart = System.currentTimeMillis();
                sendActionBar(p, "§6§l✦ §fMenahan... §7(Lepas untuk Panggilan Bulan)");
                startChargeAnimation(p);
            } else if (data.isCharging) {
                long chargeTime = System.currentTimeMillis() - data.chargeStart;
                if (chargeTime >= 1000) {
                    data.isCharging = false;
                    executeMoonSummon(p);
                    data.lunarGauge = 0;
                    data.skill3Cooldown = true;
                    data.lastSkill3Time = System.currentTimeMillis();
                    sendActionBar(p, "§6§l✦ §fPANGGILAN BULAN AKTIF!");
                    
                    // 60 second cooldown
                    new BukkitRunnable() {
                        @Override
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

        // ✨ SKILL 2: Hujan Berkah (Sneak + Hit)
        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && p.isSneaking()) {            PlayerSkillData data = getData(p);
            if (!data.skill2Cooldown) {
                e.setCancelled(true);
                executeBlessingRain(p);
                data.skill2Cooldown = true;
                data.lastSkill2Time = System.currentTimeMillis();
                
                // 3 second cooldown
                new BukkitRunnable() {
                    @Override
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

        // Charge animation for ultimate
        if (data.isCharging) {
            if (System.currentTimeMillis() % 200 < 40) {
                gentleAura(p.getLocation().add(0, 1.6, 0), GOLD, 1);
            }
        }

        // Moon Step (Crescent boots)
        if (armorManager.tryMoonStep(p)) {
            data.moonStepReady = false;
            new BukkitRunnable() {
                @Override
                public void run() {
                    getData(p).moonStepReady = true;
                }
            }.runTaskLater(plugin, 60);
        }
    }

    // ==========================================
    // ⚔️ SKILL 1: SERANGAN CAHAYA BULAN
    // ==========================================
    private void executeMoonlightBeam(Player p, LivingEntity target) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location startLoc = p.getLocation().clone();
        final org.bukkit.Location targetLoc = target.getLocation().clone();        final Vector direction = targetLoc.toVector().subtract(startLoc.toVector()).setY(0).normalize();

        // 🎬 Light beam animation (3 frames)
        for (int frame = 0; frame < 3; frame++) {
            final int f = frame;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double progress = f / 2.0;
                    org.bukkit.Location beamLoc = startLoc.clone().add(
                            direction.clone().multiply(progress * startLoc.distance(targetLoc))
                    );
                    
                    // Beam particles
                    world.spawnParticle(Particle.DUST, beamLoc, 2, 
                            new Particle.DustOptions(GOLD, 1.6f));
                    if (f == 1) {
                        world.spawnParticle(Particle.FLAME, beamLoc, 1, 0.1f, 0.1f, 0.1f, 0);
                    }
                }
            }.runTaskLater(plugin, f * 3);
        }

        // Impact
        new BukkitRunnable() {
            @Override
            public void run() {
                org.bukkit.Location impactLoc = target.getLocation().add(0, 1, 0);
                
                // Sound + visual
                p.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.8f);
                world.spawnParticle(Particle.DUST, impactLoc, 8, 
                        new Particle.DustOptions(MOON_WHITE, 1.8f));
                world.spawnParticle(Particle.FLAME, impactLoc, 5, 0.2f, 0.2f, 0.2f, 0.08f);
                
                // Damage: 4 HP
                target.damage(4.0, p);
                target.setVelocity(direction.clone().multiply(0.7).setY(0.4));
            }
        }.runTaskLater(plugin, 9);
    }

    // ==========================================
    // ✨ SKILL 2: HUJAN BERKAH
    // ==========================================
    private void executeBlessingRain(Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        
        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.5f);        sendActionBar(p, "§e§l✦ §fHujan Berkah Diturunkan!");

        // 🎬 Rain animation: 5 orbs falling
        for (int orb = 0; orb < 5; orb++) {
            final int o = orb;
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Random position within 3-block radius
                    double angle = Math.random() * Math.PI * 2;
                    double distance = Math.random() * 2.5;
                    org.bukkit.Location dropLoc = center.clone().add(
                            Math.cos(angle) * distance,
                            8,
                            Math.sin(angle) * distance
                    );
                    
                    // Falling orb trail
                    new BukkitRunnable() {
                        int fallFrame = 0;
                        @Override
                        public void run() {
                            if (fallFrame >= 16) {
                                // Impact explosion
                                org.bukkit.Location impactLoc = dropLoc.clone().setY(center.getY());
                                world.spawnParticle(Particle.DUST, impactLoc, 10, 
                                        new Particle.DustOptions(GOLD, 1.7f));
                                world.spawnParticle(Particle.FLAME, impactLoc, 6, 0.25f, 0.25f, 0.25f, 0.1f);
                                world.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.4f);
                                
                                // Damage: 6 HP to enemies in 3-block radius
                                for (org.bukkit.entity.Entity en : world.getNearbyEntities(impactLoc, 3.0, 3.0, 3.0)) {
                                    if (en instanceof LivingEntity && !en.equals(p)) {
                                        LivingEntity le = (LivingEntity) en;
                                        le.damage(6.0, p);
                                        le.setVelocity(new Vector(0, 0.5, 0));
                                        gentleSparkle(le.getLocation().add(0, 1, 0), MOON_WHITE, 3);
                                    }
                                }
                                this.cancel();
                                return;
                            }
                            
                            // Trail particles while falling
                            world.spawnParticle(Particle.DUST, dropLoc.clone().setY(dropLoc.getY() - fallFrame * 0.5), 2, 
                                    new Particle.DustOptions(CRESCENT_SILVER, 1.4f));
                            fallFrame++;
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }            }.runTaskLater(plugin, o * 4);
        }
    }

    // ==========================================
    // 🌕 SKILL 3: PANGGILAN BULAN (ULTIMATE)
    // ==========================================
    private void executeMoonSummon(Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        final double radius = 5.0;
        
        // 🎬 Cinematic intro
        p.setVelocity(new Vector(0, 0.3, 0));
        p.setInvulnerable(true);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 0.9f);
        p.sendTitle("§f§l🌕", "§6§lPANGGILAN BULAN", 4, 20, 6);

        // Phase 1: Moon appears above
        new BukkitRunnable() {
            int frame = 0;
            @Override
            public void run() {
                if (frame >= 15) {
                    // Phase 2: Energy wave
                    executeMoonWave(p, center, world, radius);
                    this.cancel();
                    return;
                }
                
                // Rising moon particles
                double y = frame * 0.6;
                org.bukkit.Location moonLoc = center.clone().add(0, y + 10, 0);
                
                // Moon circle
                for (double angle = 0; angle < 360; angle += 12) {
                    double rad = Math.toRadians(angle);
                    Vector offset = new Vector(Math.cos(rad) * 2.5, 0, Math.sin(rad) * 2.5);
                    world.spawnParticle(Particle.DUST, moonLoc.clone().add(offset), 1, 
                            new Particle.DustOptions(MOON_WHITE, 2.0f));
                }
                
                // Golden glow
                if (frame % 3 == 0) {
                    gentleAura(moonLoc, GOLD, 2);
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    private void executeMoonWave(Player p, org.bukkit.Location center, 
            org.bukkit.World world, double radius) {
        
        // 🎬 Energy wave expansion
        new BukkitRunnable() {
            int waveFrame = 0;
            @Override
            public void run() {
                if (waveFrame >= 20) {
                    // Phase 3: Blessing effect + cleanup
                    applyBlessingEffect(p, center, world, radius);
                    this.cancel();
                    return;
                }
                
                // Expanding ring
                double currentRadius = waveFrame * 0.25;
                for (double angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle + waveFrame * 3);
                    org.bukkit.Location ringLoc = center.clone().add(
                            Math.cos(rad) * currentRadius,
                            0.1,
                            Math.sin(rad) * currentRadius
                    );
                    world.spawnParticle(Particle.DUST, ringLoc, 1, 
                            new Particle.DustOptions(GOLD, 1.5f));
                }
                
                // Sparkle rain
                if (waveFrame % 4 == 0) {
                    for (int s = 0; s < 3; s++) {
                        org.bukkit.Location sparkLoc = center.clone().add(
                                (Math.random() - 0.5) * radius,
                                1 + Math.random() * 3,
                                (Math.random() - 0.5) * radius
                        );
                        gentleSparkle(sparkLoc, CRESCENT_SILVER, 1);
                    }
                }
                waveFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void applyBlessingEffect(Player p, org.bukkit.Location center, 
            org.bukkit.World world, double radius) {
        
        // Sound + final flash
        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.8f, 1.2f);        world.spawnParticle(Particle.DUST, center, 30, 2, 1, 2, 0.1f, 
                new Particle.DustOptions(MOON_WHITE, 2.0f));
        
        // Damage: 8 HP to enemies in 5-block radius
        for (org.bukkit.entity.Entity en : world.getNearbyEntities(center, radius, radius, radius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                LivingEntity le = (LivingEntity) en;
                le.damage(8.0, p);
                le.setVelocity(new Vector(0, 0.6, 0));
                gentleSparkle(le.getLocation().add(0, 1.2, 0), GOLD, 3);
            }
        }
        
        // Self heal + buff
        try {
            if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 5.0));
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 0, false, false));
        } catch (Exception ignored) {}
        
        // End invulnerability
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline()) {
                    p.setInvulnerable(false);
                    gentleAura(p.getLocation().add(0, 1.5, 0), GOLD, 4);
                }
            }
        }.runTaskLater(plugin, 40);
    }

    // ==========================================
    // 🎨 ELEGANT PARTICLE HELPERS
    // ==========================================

    private void gentleHitEffect(org.bukkit.Location loc, org.bukkit.World world) {
        world.spawnParticle(Particle.CRIT, loc, 2, 0.08, 0.15, 0.08, 0);
        world.spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(GOLD, 1.2f));
    }

    private void gentleSparkle(org.bukkit.Location loc, Color color, int count) {
        org.bukkit.World world = loc.getWorld();
        for (int i = 0; i < count; i++) {
            Vector spread = new Vector(
                    (Math.random() - 0.5) * 0.2,
                    Math.random() * 0.3,
                    (Math.random() - 0.5) * 0.2
            );            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, 
                    new Particle.DustOptions(color, 1.2f));
        }
    }

    private void gentleAura(org.bukkit.Location loc, Color color, int rings) {
        org.bukkit.World world = loc.getWorld();
        for (int r = 0; r < rings; r++) {
            final int ring = r;
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (double angle = 0; angle < 360; angle += 24) {
                        double rad = Math.toRadians(angle);
                        Vector offset = new Vector(
                                Math.cos(rad) * (1.0 + ring * 0.35), 
                                0.2, 
                                Math.sin(rad) * (1.0 + ring * 0.35)
                        );
                        org.bukkit.Location auraLoc = loc.clone().add(offset);
                        world.spawnParticle(Particle.DUST, auraLoc, 1, 
                                new Particle.DustOptions(color, 1.3f));
                    }
                }
            }.runTaskLater(plugin, r * 3);
        }
    }

    private void startChargeAnimation(final Player p) {
        new BukkitRunnable() {
            int pulse = 0;
            @Override
            public void run() {
                PlayerSkillData data = getData(p);
                if (!data.isCharging || !p.isOnline()) {
                    this.cancel();
                    return;
                }
                
                double radius = 1.1 + Math.sin(pulse * 0.3) * 0.45;
                for (double angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    org.bukkit.Location auraLoc = p.getLocation().add(
                            Math.cos(rad) * radius,
                            0.6 + Math.sin(pulse * 0.15) * 0.25,
                            Math.sin(rad) * radius
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 1, 
                            new Particle.DustOptions(GOLD, 1.5f));
                }                
                // Actionbar progress
                int bars = Math.min(5, pulse / 5);
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

    private boolean hasLunarShield(Player p) {
        ItemStack offhand = p.getInventory().getItemInOffHand();
        return offhand != null && offhand.hasItemMeta() &&
                offhand.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE);
    }

    private PlayerSkillData getData(Player p) {
        return playerData.computeIfAbsent(p.getUniqueId(), new java.util.function.Function<java.util.UUID, PlayerSkillData>() {
            @Override
            public PlayerSkillData apply(java.util.UUID uuid) {
                return new PlayerSkillData();
            }
        });
    }

    private boolean isLunarBlade(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() &&
                item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    // ==========================================
    // 📊 PLAYER DATA CLASS
    // ==========================================
    private static class PlayerSkillData {
        long lastHitTime = 0;
        long lastHitStart = 0;
        boolean skill1Used = false;        boolean skill2Cooldown = false;
        long lastSkill2Time = 0;
        int lunarGauge = 0;
        boolean isCharging = false;
        long chargeStart = 0;
        boolean skill3Cooldown = false;
        long lastSkill3Time = 0;
        boolean moonStepReady = true;

        void addGauge(int amount) {
            lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount);
        }
    }
                        }
