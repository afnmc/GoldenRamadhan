package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {

    private final GoldenMoon plugin; // SEKARANG SAMA DENGAN MAIN CLASS LO
    private final Map<UUID, Integer> hitStack = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, List<BukkitTask>> activeTasks = new HashMap<>();

    private static final int MAX_STACK = 5;
    private static final long HIT_TIMEOUT = 3000; 

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startComboWatcher();
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isHolding(p)) return; 
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastHitTime.containsKey(id) && (now - lastHitTime.get(id) > HIT_TIMEOUT)) {
            reset(p);
        }

        int stack = hitStack.getOrDefault(id, 0);
        if (stack >= MAX_STACK) return;

        stack++;
        hitStack.put(id, stack);
        lastHitTime.put(id, now);

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.5f + (stack * 0.2f));
        spawnOrbitCrescent(p, stack);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;

        Player p = e.getPlayer();
        if (!isHolding(p)) return;
        
        UUID id = p.getUniqueId();
        if (hitStack.getOrDefault(id, 0) < MAX_STACK) return;

        executeFinisher(p);
        reset(p);
    }

    private void spawnOrbitCrescent(Player p, int stack) {
        UUID id = p.getUniqueId();
        
        BukkitTask task = new BukkitRunnable() {
            double angle = stack * (360.0 / MAX_STACK);

            @Override
            public void run() {
                if (!p.isOnline() || !hitStack.containsKey(id)) {
                    this.cancel();
                    return;
                }

                angle += 10; 
                double rad = Math.toRadians(angle);
                double radius = 0.7; 

                Location loc = p.getLocation().clone().add(0, 1.2, 0);
                double x = Math.cos(rad) * radius;
                double z = Math.sin(rad) * radius;
                loc.add(x, 0, z);

                p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0, 2);

        activeTasks.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
    }

    private void executeFinisher(Player p) {
        World w = p.getWorld();
        Location center = p.getLocation().add(0, 1, 0);
        double radius = 5.0;

        List<Entity> targets = p.getNearbyEntities(radius, radius, radius);

        drawSpecialCrescent(p.getLocation().add(0, 2.5, 0), p);

        new BukkitRunnable() {
            double t = 0;
            @Override
            public void run() {
                t += 0.3;
                for (double a = 0; a < Math.PI * 2; a += Math.PI / 10) {
                    double x = Math.cos(a) * t;
                    double z = Math.sin(a) * t;
                    w.spawnParticle(Particle.END_ROD, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }

                if (t >= 3.0) {
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                    w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);

                    for (Entity e : targets) {
                        if (e instanceof LivingEntity le && le != p) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10));
                            le.damage(40.0, p);
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void drawSpecialCrescent(Location center, Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Vector rightDir = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        for (double t = -1.3; t <= 1.3; t += 0.05) {
            double curveWidth = Math.cos(t) * 1.5; 
            double height = t * 1.3; 
            
            double tiltedX = (curveWidth * Math.cos(0.35)) - (height * Math.sin(0.35));
            double tiltedY = (curveWidth * Math.sin(0.35)) + (height * Math.cos(0.35));

            Location dot = center.clone().add(rightDir.clone().multiply(tiltedX)).add(0, tiltedY, 0);
            p.getWorld().spawnParticle(Particle.DUST, dot, 1, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
        }
    }

    private void reset(Player p) {
        UUID id = p.getUniqueId();
        hitStack.remove(id);
        lastHitTime.remove(id);
        if (activeTasks.containsKey(id)) {
            activeTasks.get(id).forEach(BukkitTask::cancel);
            activeTasks.remove(id);
        }
    }

    private void startComboWatcher() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (UUID id : new HashSet<>(lastHitTime.keySet())) {
                    if (now - lastHitTime.get(id) > HIT_TIMEOUT) {
                        Player p = Bukkit.getPlayer(id);
                        if (p != null) reset(p);
                        else {
                            hitStack.remove(id);
                            lastHitTime.remove(id);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    private boolean isHolding(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        if (!item.getItemMeta().hasDisplayName()) return false;
        
        return item.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
