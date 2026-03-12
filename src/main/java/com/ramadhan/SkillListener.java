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

    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color MOON_WHITE = Color.fromRGB(255, 250, 240);
    private static final Color CRESCENT_SILVER = Color.fromRGB(200, 200, 220);
    private static final Color STAR_SPARKLE = Color.fromRGB(255, 240, 180);

    private final GoldenMoon plugin;
    private final ArmorManager armorManager;
    private final Map<java.util.UUID, LunarPlayerData> playerData = new HashMap<>();

    private static final long COMBO_WINDOW_MS = 1200;
    private static final int MAX_LUNAR_GAUGE = 100;
    private static final int GAUGE_PER_HIT = 15;

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        this.armorManager = plugin.getArmorManager();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (!isLunarBlade(p)) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) e.getEntity();

        LunarPlayerData data = getData(p);
        long now = System.currentTimeMillis();
        if (now - data.lastHitTime < 100) return;
        data.lastHitTime = now;

        data.addGauge(GAUGE_PER_HIT);

        if (p.isSneaking() && data.combo == 0) {
            animateCrescentDash(p, target);
            data.combo = 0;
            return;
        }

        data.combo++;
        if (data.combo == 2) {
            animateCombo2(p, target);
        } else if (data.combo >= 3) {
            animateCombo3(p, target);
            data.combo = 0;
        }

        if (data.combo > 0 && now - data.lastHitTime > COMBO_WINDOW_MS) {
            data.combo = 0;
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (hasLunarShield(p)) {
            e.setDamage(e.getDamage() * 0.9);
            if (new Random().nextInt(100) < 25) {
                gentleSparkle(p.getLocation().add(0, 1.2, 0), CRESCENT_SILVER, 2);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {            e.setCancelled(true);
            LunarPlayerData data = getData(p);

            if (data.lunarGauge >= MAX_LUNAR_GAUGE && !data.isCharging) {
                data.isCharging = true;
                sendActionBar(p, "§e§l✦ §fMenahan... §7(Lepas untuk Berkah Bulan)");
                startChargeAnimation(p);
            } else if (data.isCharging) {
                data.isCharging = false;
                animateGoldenBlessing(p);
                data.lunarGauge = 0;
                data.combo = 0;
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        LunarPlayerData data = getData(p);

        if (data.isCharging && System.currentTimeMillis() % 400 < 50) {
            gentleAura(p.getLocation().add(0, 1.5, 0), GOLD, 1);
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

    private void animateCombo2(Player p, LivingEntity target) {
        target.setVelocity(new Vector(0, 0.5, 0));
        target.damage(4.0, p);
        final org.bukkit.Location start = target.getLocation().add(0, 1, 0);
        final org.bukkit.World world = target.getWorld();

        for (int frame = 0; frame < 5; frame++) {
            final int f = frame;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = Math.toRadians(f * 30 - 60);
                    Vector offset = new Vector(Math.cos(angle) * 1.2, 0.3 + f * 0.2, Math.sin(angle) * 1.2);                    org.bukkit.Location particleLoc = start.clone().add(offset);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(CRESCENT_SILVER, 1.3f));
                    if (f == 2) {
                        world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1f, 0.1f, 0.1f, 0);
                    }
                }
            }.runTaskLater(plugin, f * 2);
        }
        world.playSound(start, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.4f, 1.4f);
    }

    private void animateCombo3(Player p, LivingEntity target) {
        target.damage(3.0, p);
        final org.bukkit.Location center = target.getLocation().add(0, 1.2, 0);
        final org.bukkit.World world = target.getWorld();

        for (int i = 0; i < 8; i++) {
            final int idx = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = Math.toRadians(idx * 45);
                    Vector dir = new Vector(Math.cos(angle) * 1.5, 0.2, Math.sin(angle) * 1.5);
                    org.bukkit.Location particleLoc = center.clone().add(dir);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(STAR_SPARKLE, 1.2f));
                    world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.05f, 0.05f, 0.05f, 0);
                }
            }.runTaskLater(plugin, idx);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                world.spawnParticle(Particle.DUST, center, 3, new Particle.DustOptions(GOLD, 1.8f));
                world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.6f);
            }
        }.runTaskLater(plugin, 8);
    }

    private void animateCrescentDash(Player p, LivingEntity target) {
        final org.bukkit.World world = p.getWorld();
        final Vector dir = p.getLocation().getDirection().setY(0).normalize();
        p.setVelocity(dir.clone().multiply(2.2).setY(0.25));

        for (int frame = 0; frame < 6; frame++) {
            final int f = frame;
            new BukkitRunnable() {
                @Override
                public void run() {
                    org.bukkit.Location trailLoc = p.getLocation().add(0, 1, 0);                    for (double angle = -40; angle <= 40; angle += 20) {
                        double rad = Math.toRadians(angle);
                        Vector arcOffset = new Vector(Math.cos(rad) * 0.6, 0, Math.sin(rad) * 0.6);
                        org.bukkit.Location particleLoc = trailLoc.clone().add(arcOffset.rotateAroundY(Math.toRadians(90)));
                        world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(CRESCENT_SILVER, 1.4f));
                    }
                    if (f % 2 == 0) {
                        gentleSparkle(trailLoc, GOLD, 1);
                    }
                }
            }.runTaskLater(plugin, f * 2);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                org.bukkit.Location impactLoc = p.getLocation().add(0, 1, 0);
                for (double angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    Vector burstOffset = new Vector(Math.cos(rad) * 1.8, 0.3, Math.sin(rad) * 1.8);
                    org.bukkit.Location particleLoc = impactLoc.clone().add(burstOffset);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(GOLD, 1.6f));
                }
                world.playSound(impactLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 1.5f);
                target.damage(5.5, p);
                target.setVelocity(dir.clone().multiply(0.9).setY(0.5));

                int hitCount = 0;
                for (org.bukkit.entity.Entity en : world.getNearbyEntities(impactLoc, 2.8, 2.2, 2.8)) {
                    if (hitCount >= 8) break;
                    if (en instanceof LivingEntity && !en.equals(p) && !en.equals(target)) {
                        LivingEntity le = (LivingEntity) en;
                        le.damage(3.5, p);
                        le.setVelocity(dir.clone().multiply(0.6).setY(0.35));
                        gentleSparkle(le.getLocation().add(0, 1, 0), STAR_SPARKLE, 2);
                        hitCount++;
                    }
                }
            }
        }.runTaskLater(plugin, 12);
    }

    private void animateGoldenBlessing(Player p) {
        final org.bukkit.World world = p.getWorld();
        final org.bukkit.Location center = p.getLocation().clone();
        final boolean isElite = armorManager.hasFullEliteSet(p);
        final double radius = isElite ? 11.0 : 8.0;

        p.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 1.0f);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 0.85f);        p.sendTitle("§f§l🌕", "§6§lBerkah Bulan Suci", 4, 18, 6);

        for (int corner = 0; corner < 6; corner++) {
            final int c = corner;
            new BukkitRunnable() {
                int frame = 0;

                @Override
                public void run() {
                    if (frame >= 12) {
                        this.cancel();
                        return;
                    }
                    double baseAngle = Math.toRadians(c * 60);
                    double progress = frame / 12.0;
                    double currentRadius = progress * (isElite ? 10 : 7);
                    double angle = baseAngle + Math.toRadians(frame * 8);
                    org.bukkit.Location cornerLoc = center.clone().add(
                            Math.cos(angle) * currentRadius,
                            0.25 + progress * 0.3,
                            Math.sin(angle) * currentRadius
                    );
                    world.spawnParticle(Particle.DUST, cornerLoc, 1, new Particle.DustOptions(GOLD, 1.5f));
                    if (frame % 3 == 0) {
                        world.spawnParticle(Particle.FLAME, cornerLoc, 1, 0.1f, 0.1f, 0.1f, 0);
                    }
                    frame++;
                }
            }.runTaskTimer(plugin, c * 3, 2);
        }

        new BukkitRunnable() {
            int frame = 0;

            @Override
            public void run() {
                if (frame >= 16) {
                    executeBlessingImpact(p, center, world, isElite, radius);
                    this.cancel();
                    return;
                }
                double y = frame * 0.55;
                org.bukkit.Location bladeLoc = center.clone().add(0, y + 4, 0);
                for (double angle = -35; angle <= 35; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double bladeRadius = 1.2 + Math.sin(frame * 0.3) * 0.3;
                    Vector offset = new Vector(Math.cos(rad) * bladeRadius, 0, Math.sin(rad) * bladeRadius * 0.4);
                    org.bukkit.Location particleLoc = bladeLoc.clone().add(offset);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(MOON_WHITE, 1.8f));
                }                if (frame % 4 == 0) {
                    gentleAura(bladeLoc, GOLD, 2);
                }
                frame++;
            }
        }.runTaskTimer(plugin, 18, 2);
    }

    private void executeBlessingImpact(Player p, org.bukkit.Location center, org.bukkit.World world, boolean isElite, double radius) {
        for (Player viewer : center.getWorld().getPlayers()) {
            if (viewer.getLocation().distance(center) < radius + 6) {
                viewer.playSound(viewer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.3f, 1.8f);
            }
        }

        for (int i = 0; i < (isElite ? 6 : 4); i++) {
            final int idx = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    double angle = Math.toRadians(idx * (360.0 / (isElite ? 6 : 4)));
                    org.bukkit.Location pillarLoc = center.clone().add(
                            Math.cos(angle) * (radius * 0.7), 0, Math.sin(angle) * (radius * 0.7));
                    animateLightPillar(pillarLoc, world, isElite ? CRESCENT_SILVER : GOLD);
                }
            }.runTaskLater(plugin, i * 4);
        }

        new BukkitRunnable() {
            int pulse = 0;

            @Override
            public void run() {
                if (pulse >= 20) {
                    this.cancel();
                    return;
                }
                for (double angle = 0; angle < 360; angle += 18) {
                    double rad = Math.toRadians(angle + pulse * 5);
                    double ringRadius = radius * 0.3 + Math.sin(pulse * 0.4) * 1.5;
                    org.bukkit.Location ringLoc = center.clone().add(
                            Math.cos(rad) * ringRadius, 0.05, Math.sin(rad) * ringRadius);
                    world.spawnParticle(Particle.DUST, ringLoc, 1, new Particle.DustOptions(MOON_WHITE, 1.3f));
                }
                if (pulse % 3 == 0) {
                    for (int s = 0; s < 3; s++) {
                        org.bukkit.Location sparkLoc = center.clone().add(
                                (Math.random() - 0.5) * radius,
                                1 + Math.random() * 3,
                                (Math.random() - 0.5) * radius                        );
                        gentleSparkle(sparkLoc, STAR_SPARKLE, 1);
                    }
                }
                pulse++;
            }
        }.runTaskTimer(plugin, 0, 3);

        for (org.bukkit.entity.Entity en : world.getNearbyEntities(center, radius, radius, radius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                LivingEntity le = (LivingEntity) en;
                le.damage(isElite ? 5.5 : 3.5, p);
                le.setVelocity(new Vector(0, 0.7, 0));
                try {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 70, 0, false, false));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 50, 0, false, false));
                } catch (Exception ignored) {
                }
                gentleSparkle(le.getLocation().add(0, 1.3, 0), MOON_WHITE, 2);
            }
        }

        try {
            if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 5.0));
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, isElite ? 1 : 0, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false));
        } catch (Exception ignored) {
        }

        p.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline()) {
                    p.setInvulnerable(false);
                    gentleAura(p.getLocation().add(0, 1.5, 0), GOLD, 4);
                    world.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 1.3f);
                }
            }
        }.runTaskLater(plugin, 50);
    }

    private void animateLightPillar(org.bukkit.Location loc, org.bukkit.World world, Color color) {
        new BukkitRunnable() {
            int height = 0;

            @Override
            public void run() {                if (height >= 14) {
                    this.cancel();
                    return;
                }
                org.bukkit.Location pillarLoc = loc.clone().add(0, height, 0);
                world.spawnParticle(Particle.DUST, pillarLoc, 1, new Particle.DustOptions(color, 1.4f));
                if (height % 2 == 0) {
                    world.spawnParticle(Particle.FLAME, pillarLoc, 1, 0.08f, 0.05f, 0.08f, 0);
                }
                height++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void gentleSparkle(org.bukkit.Location loc, Color color, int count) {
        org.bukkit.World world = loc.getWorld();
        for (int i = 0; i < count; i++) {
            Vector spread = new Vector(
                    (Math.random() - 0.5) * 0.25,
                    Math.random() * 0.35,
                    (Math.random() - 0.5) * 0.25
            );
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1,
                    new Particle.DustOptions(color, 1.3f));
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
                        Vector offset = new Vector(Math.cos(rad) * (1.0 + ring * 0.4), 0.2, Math.sin(rad) * (1.0 + ring * 0.4));
                        org.bukkit.Location auraLoc = loc.clone().add(offset);
                        world.spawnParticle(Particle.DUST, auraLoc, 1, new Particle.DustOptions(color, 1.4f));
                    }
                }
            }.runTaskLater(plugin, r * 3);
        }
    }

    private void startChargeAnimation(final Player p) {
        new BukkitRunnable() {
            int pulse = 0;

            @Override            public void run() {
                LunarPlayerData data = getData(p);
                if (!data.isCharging || !p.isOnline()) {
                    this.cancel();
                    return;
                }
                double radius = 1.1 + Math.sin(pulse * 0.35) * 0.5;
                for (double angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    org.bukkit.Location auraLoc = p.getLocation().add(
                            Math.cos(rad) * radius,
                            0.6 + Math.sin(pulse * 0.2) * 0.3,
                            Math.sin(rad) * radius
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 1,
                            new Particle.DustOptions(GOLD, 1.6f));
                }
                int bars = Math.min(5, (pulse / 5) % 6);
                StringBuilder bar = new StringBuilder("§7[§f");
                for (int i = 0; i < bars; i++) bar.append("▮");
                for (int i = 0; i < 5 - bars; i++) bar.append("▯");
                bar.append("]");
                sendActionBar(p, "§e§l✦ §fBerkah Terkumpul §7" + bar.toString());
                pulse++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private boolean hasLunarShield(Player p) {
        ItemStack offhand = p.getInventory().getItemInOffHand();
        return offhand != null && offhand.hasItemMeta() &&
                offhand.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE);
    }

    private LunarPlayerData getData(Player p) {
        return playerData.computeIfAbsent(p.getUniqueId(), new java.util.function.Function<java.util.UUID, LunarPlayerData>() {
            @Override
            public LunarPlayerData apply(java.util.UUID uuid) {
                return new LunarPlayerData();
            }
        });
    }

    private boolean isLunarBlade(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() &&
                item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private void sendActionBar(Player p, String msg) {        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    private static class LunarPlayerData {
        int combo = 0;
        long lastHitTime = 0;
        int lunarGauge = 0;
        boolean isCharging = false;
        boolean moonStepReady = true;

        void addGauge(int amount) {
            lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount);
        }
    }
                }
