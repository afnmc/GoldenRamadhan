package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> comboStack = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startTicking();
    }

    private void startTicking() {
        new BukkitRunnable() {
            double rot = 0;
            @Override
            public void run() {
                rot += 0.20;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isHolding(p)) continue;
                    int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
                    drawBackMoon(p, rot, stack);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // --- Efek Sabit di Punggung (Kuning/Putih) ---
    private void drawBackMoon(Player p, double rot, int stack) {
        Location loc = p.getLocation().add(0, 1.2, 0);
        Vector dir = p.getLocation().getDirection().setY(0).normalize().multiply(-0.4);
        loc.add(dir);
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        Color col = stack >= 5 ? Color.WHITE : Color.fromRGB(255, 230, 100);
        Particle.DustOptions dust = new Particle.DustOptions(col, 1.0f);

        for (double t = -1.5; t <= 1.5; t += 0.4) {
            double taper = Math.cos(t / 2.0);
            double rx = (Math.cos(t) * taper * Math.cos(rot)) - (Math.sin(t) * Math.sin(rot));
            double ry = (Math.cos(t) * taper * Math.sin(rot)) + (Math.sin(t) * Math.cos(rot));
            Location pLoc = loc.clone().add(right.clone().multiply(rx * 0.6)).add(0, ry * 0.6, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, dust);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        // Visual Hit (Slash Putih)
        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§e§l● READY ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        // SYARAT: Lompat + Jongkok + 5 Stack
        if (stack >= 5 && !((Entity)p).isOnGround()) {
            comboStack.put(p.getUniqueId(), 0);
            openLunamArena(p);
        }
    }

    private void openLunamArena(Player p) {
        Location center = p.getLocation().clone();
        p.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.5f);
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 120) { this.cancel(); return; }

                // 1. Arena Ring (Kuning-Putih)
                drawCircle(center, 9.0, Color.fromRGB(255, 215, 0)); 
                drawCircle(center, 5.0, Color.WHITE);

                // 2. Lunam Swords Storm (Menit 01:25)
                if (ticks % 10 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = Math.random() * 8.5;
                    Location swordLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    spawnFallingSword(swordLoc, p);
                }

                // 3. Lunam Souls (Menit 01:51)
                if (ticks % 30 == 0) {
                    spawnOrbitSoul(center);
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawCircle(Location loc, double radius, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.8f);
        for (double i = 0; i < Math.PI * 2; i += 0.3) {
            double x = Math.cos(i) * radius;
            double z = Math.sin(i) * radius;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0, dust);
        }
    }

    private void spawnFallingSword(Location loc, Player p) {
        new BukkitRunnable() {
            double y = 10.0;
            @Override
            public void run() {
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 2, 0, 0, 0, 0.01);
                y -= 1.5;
                if (y <= 0) {
                    loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
                    loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 1.2f);
                    
                    // Damage musuh di titik jatuh
                    for (Entity en : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                        if (en instanceof LivingEntity le && en != p) {
                            le.damage(6.0, p);
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnOrbitSoul(Location center) {
        new BukkitRunnable() {
            double angle = 0;
            int time = 0;
            @Override
            public void run() {
                angle += 0.2;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                center.getWorld().spawnParticle(Particle.WAX_OFF, center.clone().add(x, 1, z), 1, 0, 0, 0, 0);
                if (time++ > 50) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
