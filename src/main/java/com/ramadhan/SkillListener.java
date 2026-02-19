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
        startLunamSoulsTask();
    }

    // 1. Lunam Souls - Efek Orbiting (Visual & Pasif) [01:51]
    private void startLunamSoulsTask() {
        new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                angle += 0.2;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isHolding(p)) continue;
                    
                    int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
                    Location loc = p.getLocation().add(0, 1, 0);
                    
                    // Orbiting Particles (Kuning Emas & Putih Lunar)
                    double x = Math.cos(angle) * 0.8;
                    double z = Math.sin(angle) * 0.8;
                    Color color = stack >= 5 ? Color.WHITE : Color.fromRGB(255, 215, 0);
                    p.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0, z), 1, 0, 0, 0, 0, new Particle.DustOptions(color, 1.0f));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        int stack = comboStack.getOrDefault(p.getUniqueId(), 0) + 1;
        
        // 2. Lunam Blade - Charged Attack tiap 3 Hit [00:11]
        if (stack % 3 == 0) {
            target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
            
            // Efek Slash melingkar (Kuning)
            drawSlashEffect(target.getLocation());
        }

        comboStack.put(p.getUniqueId(), Math.min(stack, 5));
        
        if (stack == 5) {
            p.sendTitle("", "§e§l● LUNAR POWER READY ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        }
    }

    private void drawSlashEffect(Location loc) {
        for (double i = 0; i < Math.PI * 2; i += 0.5) {
            double x = Math.cos(i) * 1.5;
            double z = Math.sin(i) * 1.5;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.5, z), 1, 0, 0, 0, 0, new Particle.DustOptions(Color.fromRGB(255, 255, 150), 1.5f));
        }
    }

    @EventHandler
    public void onUltiActivate(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        
        // Mekanik: Harus 5 stack DAN sedang melompat
        if (comboStack.getOrDefault(p.getUniqueId(), 0) >= 5 && !((Entity)p).isOnGround()) {
            comboStack.put(p.getUniqueId(), 0);
            executeSwordsStorm(p);
        }
    }

    // 3. Lunam Swords Storm - Ultimate Arena [01:25]
    private void executeSwordsStorm(Player p) {
        Location center = p.getLocation().clone();
        p.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.2f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 100) { this.cancel(); return; }

                // Arena Ring (Kuning-Putih)
                drawArenaRing(center, 8.0, Color.fromRGB(255, 215, 0));
                drawArenaRing(center, 4.0, Color.WHITE);

                // Spawn Pedang Jatuh (Mirip Video)
                if (ticks % 6 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = Math.random() * 8.0;
                    Location dropLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    spawnLunarSword(dropLoc, p);
                }
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawArenaRing(Location loc, double radius, Color col) {
        Particle.DustOptions dust = new Particle.DustOptions(col, 1.8f);
        for (double i = 0; i < Math.PI * 2; i += 0.3) {
            double x = Math.cos(i) * radius;
            double z = Math.sin(i) * radius;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0, dust);
        }
    }

    private void spawnLunarSword(Location loc, Player p) {
        new BukkitRunnable() {
            double y = 12.0;
            @Override
            public void run() {
                // Beam pedang (Putih)
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 2, 0.05, 0.5, 0.05, 0.01);
                y -= 1.5;
                
                if (y <= 0) {
                    loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
                    loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 0.5f, 1.8f);
                    
                    // Damage Area
                    for (Entity en : loc.getWorld().getNearbyEntities(loc, 2.5, 2.5, 2.5)) {
                        if (en instanceof LivingEntity le && en != p) {
                            le.damage(8.0, p);
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 2));
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
