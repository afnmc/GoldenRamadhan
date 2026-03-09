package com.ramadhan;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {

    private final JavaPlugin plugin;
    private final FileConfiguration config;

    private final Map<UUID, Long> blinkCD = new HashMap<>();
    private final Map<UUID, Long> dashCD = new HashMap<>();
    private final Map<UUID, Long> domainCD = new HashMap<>();

    public SkillListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    // ===============================
    // BLINK LIGHTNING CHAIN
    // jump + hit (player di udara)
    // ===============================

    @EventHandler
    public void onBlinkAttack(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player)) return;

        Player p = (Player) e.getDamager();

        // aktif jika player sedang di udara
        if (p.isOnGround()) return;

        if (cooldown(blinkCD, p, "skills.blink.cooldown")) return;

        double range = config.getDouble("skills.blink.range");
        double damage = config.getDouble("skills.blink.damage");

        List<LivingEntity> targets = new ArrayList<>();

        for (Entity en : p.getNearbyEntities(range, range, range)) {

            if (en instanceof LivingEntity && en != p) {
                targets.add((LivingEntity) en);
            }

        }

        if (targets.isEmpty()) return;

        Collections.shuffle(targets);

        new BukkitRunnable() {

            int index = 0;

            public void run() {

                if (index >= targets.size()) {
                    cancel();
                    return;
                }

                LivingEntity target = targets.get(index);

                Location loc = target.getLocation().add(0, 1, 0);

                p.teleport(loc);

                target.damage(damage, p);

                // lightning chain
                loc.getWorld().strikeLightningEffect(loc);

                // particle trail
                loc.getWorld().spawnParticle(
                        Particle.valueOf(config.getString("skills.blink.particle")),
                        loc,
                        40,
                        0.5,0.5,0.5
                );

                index++;

            }

        }.runTaskTimer(plugin, 0, config.getInt("skills.blink.speed-tick"));
    }


    // ===============================
    // DASH MUNDUR MAJU
    // sneak + hit
    // ===============================

    @EventHandler
    public void onDashAttack(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player)) return;

        Player p = (Player) e.getDamager();

        if (!p.isSneaking()) return;

        if (cooldown(dashCD, p, "skills.dash.cooldown")) return;

        double back = config.getDouble("skills.dash.dash-back");
        double forward = config.getDouble("skills.dash.dash-forward");
        double damage = config.getDouble("skills.dash.damage");

        Vector backVector = p.getLocation().getDirection().multiply(-back);

        p.setVelocity(backVector);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            Vector forwardVector = p.getLocation().getDirection().multiply(forward);

            p.setVelocity(forwardVector);

            for (Entity en : p.getNearbyEntities(3,3,3)) {

                if (en instanceof LivingEntity && en != p) {

                    ((LivingEntity) en).damage(damage, p);

                }

            }

            p.getWorld().spawnParticle(
                    Particle.valueOf(config.getString("skills.dash.particle")),
                    p.getLocation(),
                    40,
                    1,1,1
            );

        }, 6);

    }


    // ===============================
    // DOMAIN SWORD
    // sneak + right click
    // ===============================

    @EventHandler
    public void onDomain(PlayerInteractEvent e) {

        Player p = e.getPlayer();

        if (!p.isSneaking()) return;

        if (!e.getAction().toString().contains("RIGHT")) return;

        if (cooldown(domainCD, p, "skills.domain.cooldown")) return;

        Location center = p.getLocation();
        World w = center.getWorld();

        p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.domain")));

        // SKY CRACK

        new BukkitRunnable() {

            int tick = 0;

            public void run() {

                if (tick > 40) {
                    cancel();
                    spawnSword(center, p);
                    return;
                }

                Location sky = center.clone().add(0,25,0);

                w.spawnParticle(
                        Particle.valueOf(config.getString("skills.domain.sky-crack-particle")),
                        sky,
                        40,
                        2,2,2
                );

                tick++;

            }

        }.runTaskTimer(plugin, 0, 2);


        // MAGIC RUNE CIRCLE

        for (double angle = 0; angle < 360; angle += 10) {

            double x = Math.cos(Math.toRadians(angle)) * 7;
            double z = Math.sin(Math.toRadians(angle)) * 7;

            Location rune = center.clone().add(x,0.1,z);

            w.spawnParticle(
                    Particle.valueOf(config.getString("skills.domain.rune-particle")),
                    rune,
                    5,
                    0,0,0
            );

        }

    }


    // ===============================
    // SWORD FALL
    // ===============================

    private void spawnSword(Location loc, Player p) {

        World w = loc.getWorld();

        int size = config.getInt("skills.domain.sword.size");
        int height = config.getInt("skills.domain.sword.height");

        Material mat = Material.valueOf(config.getString("skills.domain.sword.block"));

        new BukkitRunnable() {

            int y = height;

            public void run() {

                if (y <= 0) {

                    impact(loc, p);

                    cancel();
                    return;

                }

                for (int x = -size/2; x <= size/2; x++) {
                    for (int z = -size/2; z <= size/2; z++) {

                        Location blockLoc = loc.clone().add(x,y,z);

                        if (blockLoc.getBlock().getType() == Material.AIR) {

                            blockLoc.getBlock().setType(mat);

                            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                                blockLoc.getBlock().setType(Material.AIR);

                            }, 40);

                        }

                    }
                }

                y--;

            }

        }.runTaskTimer(plugin, 0, 1);

    }


    // ===============================
    // IMPACT
    // ===============================

    private void impact(Location loc, Player p) {

        World w = loc.getWorld();

        w.spawnParticle(
                Particle.valueOf(config.getString("skills.domain.impact.shockwave-particle")),
                loc,
                10
        );

        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.6f);

        double radius = config.getDouble("skills.domain.impact.radius");

        for (Entity en : w.getNearbyEntities(loc, radius, radius, radius)) {

            if (en instanceof LivingEntity && en != p) {

                ((LivingEntity) en).damage(
                        config.getDouble("skills.domain.impact.damage"),
                        p
                );

            }

        }

    }


    // ===============================
    // COOLDOWN SYSTEM
    // ===============================

    private boolean cooldown(Map<UUID, Long> map, Player p, String path) {

        int cd = config.getInt(path);

        long now = System.currentTimeMillis();

        if (map.containsKey(p.getUniqueId())) {

            long diff = (now - map.get(p.getUniqueId())) / 1000;

            if (diff < cd) {

                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.skill-cooldown")
                                .replace("%time%", String.valueOf(cd - diff))));

                return true;

            }

        }

        map.put(p.getUniqueId(), now);

        return false;

    }

}
