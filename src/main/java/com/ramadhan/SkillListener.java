package com.ramadhan;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> comboStack = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startAdvancedAura();
    }

    private void startAdvancedAura() {
        new BukkitRunnable() {
            double rot = 0;
            double speed = 0.1;
            @Override
            public void run() {
                // EFEK KECEPATAN RANDOM: Kadang cepat, kadang pelan
                if (random.nextDouble() > 0.85) {
                    speed = 0.04 + (random.nextDouble() * 0.22);
                }
                rot += speed;

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHolding(p)) drawDoubleMoon(p, rot);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawDoubleMoon(Player p, double rot) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        Color col = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);
        Location center = loc.clone().add(0, 1.2, 0).add(dir.multiply(-0.45));

        // DOUBLE LAYER MOON: Bikin bentuk bulan lebih berisi
        for (double layer = 1.0; layer <= 1.25; layer += 0.25) {
            for (double t = -1.4; t <= 1.4; t += 0.2) {
                double x = Math.cos(t) * layer; 
                double y = Math.sin(t) * layer;

                double rx = (x * Math.cos(rot)) - (y * Math.sin(rot));
                double ry = (x * Math.sin(rot)) + (y * Math.cos(rot));

                Location pLoc = center.clone().add(right.clone().multiply(rx)).add(0, ry, 0);
                p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(col, 0.65f));
            }
        }
    }

    @EventHandler
    public void onSwing(PlayerInteractEvent e) {
        if (isHolding(e.getPlayer()) && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            playSlash(e.getPlayer());
        }
    }

    private void playSlash(Player p) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        boolean flip = random.nextBoolean();

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
        for (double i = -1.0; i <= 1.0; i += 0.15) {
            Location loc = eye.clone().add(dir.clone().multiply(1.3 + (1 - i*i)*0.4))
                .add(right.clone().multiply(i * 1.4))
                .add(0, (flip ? i : -i) * 0.8 - 0.4, 0);
            p.getWorld().spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(Color.fromRGB(255,255,210), 0.7f));
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        int stack = comboStack.getOrDefault(id, 0);
        if (System.currentTimeMillis() - lastHit.getOrDefault(id, 0L) > 4000) stack = 0;
        
        stack = Math.min(stack + 1, 5);
        comboStack.put(id, stack);
        lastHit.put(id, System.currentTimeMillis());

        if (stack == 5) p.sendTitle("", "§f§lMOONLIGHT READY", 0, 15, 5);
        
        if (target.getHealth() - e.getFinalDamage() <= 0) {
            p.teleport(target.getLocation().subtract(p.getLocation().getDirection().setY(0).normalize().multiply(1.1)));
            p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.8f);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            executeBurst(p);
        } else {
            // RECALL: Muncul saat jongkok tapi stack belum penuh
            executeRecall(p);
        }
    }

    private void executeRecall(Player p) {
        new BukkitRunnable() {
            int t = 0; double h = 0;
            @Override
            public void run() {
                if (!p.isSneaking() || t > 25) {
                    if (t > 25) {
                        double maxH = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                        p.setHealth(Math.min(p.getHealth() + 3.0, maxH));
                        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
                    }
                    this.cancel(); return;
                }
                double x = Math.cos(t * 0.7) * 0.7;
                double z = Math.sin(t * 0.7) * 0.7;
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(x, h, z), 1, new Particle.DustOptions(Color.YELLOW, 0.8f));
                h += 0.08; t++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void executeBurst(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.5f);
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            new BukkitRunnable() {
                double d = 0;
                @Override
                public void run() {
                    d += 0.75;
                    Location loc = p.getLocation().add(Math.cos(angle)*d, 1, Math.sin(angle)*d);
                    p.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.02);
                    for (Entity en : p.getNearbyEntities(6, 6, 6)) {
                        if (en instanceof LivingEntity le && en != p && en.getLocation().distance(loc) < 1.6) {
                            le.damage(16, p);
                            le.setVelocity(en.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.2).setY(0.4));
                        }
                    }
                    if (d > 7) this.cancel();
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
