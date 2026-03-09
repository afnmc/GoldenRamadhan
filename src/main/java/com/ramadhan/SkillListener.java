package me.plugin.skills;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {

    private JavaPlugin plugin;
    private FileConfiguration config;

    private Map<UUID, Long> blinkCD = new HashMap<>();
    private Map<UUID, Long> dashCD = new HashMap<>();
    private Map<UUID, Long> domainCD = new HashMap<>();

    public SkillListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    // ===============================
    // BLINK LIGHTNING CHAIN
    // ===============================

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player)) return;

        Player p = (Player) e.getDamager();

        if (!p.isJumping()) return;

        if (cooldown(blinkCD, p, "skills.blink.cooldown")) return;

        double range = config.getDouble("skills.blink.range");

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

                Location loc = target.getLocation().add(0,1,0);

                p.teleport(loc);

                target.damage(config.getDouble("skills.blink.damage"), p);

                loc.getWorld().spawnParticle(
                        Particle.valueOf(config.getString("skills.blink.particle")),
                        loc, 30, 0.5,0.5,0.5
                );

                loc.getWorld().strikeLightningEffect(loc);

                index++;

            }

        }.runTaskTimer(plugin,0, config.getInt("skills.blink.speed-tick"));
    }


    // ===============================
    // DASH MUNDUR MAJU
    // ===============================

    @EventHandler
    public void onSneakHit(EntityDamageByEntityEvent e){

        if(!(e.getDamager() instanceof Player)) return;

        Player p = (Player) e.getDamager();

        if(!p.isSneaking()) return;

        if(cooldown(dashCD,p,"skills.dash.cooldown")) return;

        Vector back = p.getLocation().getDirection().multiply(-config.getDouble("skills.dash.dash-back"));

        p.setVelocity(back);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            Vector forward = p.getLocation().getDirection().multiply(config.getDouble("skills.dash.dash-forward"));

            p.setVelocity(forward);

            for(Entity en : p.getNearbyEntities(3,3,3)){

                if(en instanceof LivingEntity){

                    ((LivingEntity) en).damage(config.getDouble("skills.dash.damage"),p);

                }

            }

            p.getWorld().spawnParticle(
                    Particle.valueOf(config.getString("skills.dash.particle")),
                    p.getLocation(),
                    40,
                    1,1,1
            );

        },6);

    }


    // ===============================
    // DOMAIN SWORD 15x15
    // ===============================

    @EventHandler
    public void onRightClick(PlayerInteractEvent e){

        Player p = e.getPlayer();

        if(!p.isSneaking()) return;

        if(cooldown(domainCD,p,"skills.domain.cooldown")) return;

        Location center = p.getLocation();

        World w = center.getWorld();

        p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.domain")));

        // SKY CRACK EFFECT

        new BukkitRunnable(){

            int t = 0;

            public void run(){

                if(t > 40){
                    cancel();
                    spawnSword(center,p);
                    return;
                }

                Location sky = center.clone().add(0,25,0);

                w.spawnParticle(
                        Particle.valueOf(config.getString("skills.domain.sky-crack-particle")),
                        sky,
                        40,
                        2,2,2
                );

                t++;

            }

        }.runTaskTimer(plugin,0,2);

        // RUNE CIRCLE

        for(double angle = 0; angle < 360; angle += 10){

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

    private void spawnSword(Location loc, Player p){

        World w = loc.getWorld();

        int size = config.getInt("skills.domain.sword.size");

        int height = config.getInt("skills.domain.sword.height");

        Material mat = Material.valueOf(config.getString("skills.domain.sword.block"));

        new BukkitRunnable(){

            int y = height;

            public void run(){

                if(y <= 0){

                    impact(loc,p);

                    cancel();
                    return;
                }

                for(int x=-size/2; x<=size/2; x++){
                    for(int z=-size/2; z<=size/2; z++){

                        Location b = loc.clone().add(x,y,z);

                        if(b.getBlock().getType() == Material.AIR){

                            b.getBlock().setType(mat);

                            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                                b.getBlock().setType(Material.AIR);

                            },40);

                        }

                    }
                }

                y--;

            }

        }.runTaskTimer(plugin,0,1);

    }


    // ===============================
    // IMPACT
    // ===============================

    private void impact(Location loc, Player p){

        World w = loc.getWorld();

        w.spawnParticle(
                Particle.valueOf(config.getString("skills.domain.impact.shockwave-particle")),
                loc,
                10
        );

        w.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE,2,0.6f);

        double radius = config.getDouble("skills.domain.impact.radius");

        for(Entity en : w.getNearbyEntities(loc,radius,radius,radius)){

            if(en instanceof LivingEntity){

                ((LivingEntity) en).damage(
                        config.getDouble("skills.domain.impact.damage"),
                        p
                );

            }

        }

        // GROUND CRACK VISUAL

        for(int i=0;i<40;i++){

            double x = (Math.random()-0.5)*radius*2;
            double z = (Math.random()-0.5)*radius*2;

            Location crack = loc.clone().add(x,0,z);

            w.spawnParticle(
                    Particle.valueOf(config.getString("skills.domain.impact.ground-crack-particle")),
                    crack,
                    20,
                    0.3,0.1,0.3,
                    Material.STONE.createBlockData()
            );

        }

    }


    // ===============================
    // COOLDOWN
    // ===============================

    private boolean cooldown(Map<UUID,Long> map, Player p, String path){

        int cd = config.getInt(path);

        long now = System.currentTimeMillis();

        if(map.containsKey(p.getUniqueId())){

            long diff = (now - map.get(p.getUniqueId())) / 1000;

            if(diff < cd){

                p.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        config.getString("messages.skill-cooldown")
                                .replace("%time%",String.valueOf(cd-diff))));

                return true;

            }

        }

        map.put(p.getUniqueId(), now);

        return false;

    }

}
