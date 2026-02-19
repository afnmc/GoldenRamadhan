package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> comboStack = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLunamInteraction(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        // FITUR 1: ATTACK + JONGKOK = DASH BLINK [Lincah & Bling-bling]
        if (p.isSneaking() && stack < 5) {
            executeBlinkDash(p, target);
        }

        // Tambah stack tiap hit normal
        stack = Math.min(stack + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        // FITUR 2: 5 STACK + JONGKOK (SAAT HIT) = ARENA LURUS MAJU MUNDUR
        if (stack >= 5 && p.isSneaking()) {
            comboStack.put(p.getUniqueId(), 0); // Reset stack
            executeLunamStraightArena(p);
        }
        
        // Efek hit bling-bling
        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
    }

    private void executeBlinkDash(Player p, LivingEntity target) {
        Location loc = target.getLocation().subtract(p.getLocation().getDirection().multiply(1));
        p.teleport(loc);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        
        // Efek bling-bling Kuning Putih
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1, new Particle.DustOptions(Color.YELLOW, 1.5f));
        p.getWorld().spawnParticle(Particle.WHITE_ASH, p.getLocation(), 10, 0.5, 0.5, 0.5, 0.05);
    }

    private void executeLunamStraightArena(Player p) {
        Location start = p.getLocation();
        Vector direction = start.getDirection().setY(0).normalize();
        Vector backward = direction.clone().multiply(-1);

        // 1. Mundur Sambil Bikin Jalur Damage
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step > 10) { 
                    this.cancel();
                    // 2. Setelah mundur, otomatis Maju lagi (Rush Back)
                    rushBack(p, start);
                    return; 
                }
                
                p.setVelocity(backward.clone().multiply(0.8));
                Location trail = p.getLocation();
                
                // Visual Jalur (Kuning Putih)
                p.getWorld().spawnParticle(Particle.DUST, trail, 10, 0.2, 0.1, 0.2, 0.05, new Particle.DustOptions(Color.WHITE, 2.0f));
                p.getWorld().spawnParticle(Particle.DUST, trail, 10, 0.5, 0.2, 0.5, 0.05, new Particle.DustOptions(Color.YELLOW, 1.0f));
                
                // Damage Area di sepanjang jalur mundur
                for (Entity en : p.getNearbyEntities(3, 3, 3)) {
                    if (en instanceof LivingEntity le && en != p) {
                        le.damage(10.0, p);
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation(), 5);
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void rushBack(Player p, Location originalPos) {
        p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 2f);
        Vector toOriginal = originalPos.toVector().subtract(p.getLocation().toVector()).normalize();
        
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i > 5 || p.getLocation().distance(originalPos) < 1.5) {
                    p.teleport(originalPos);
                    this.cancel();
                    return;
                }
                p.setVelocity(toOriginal.multiply(1.5));
                p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 1, 0, 0, 0, 0);
                i++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
