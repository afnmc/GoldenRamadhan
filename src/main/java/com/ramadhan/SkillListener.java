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
    private final Map<UUID, Long> recallCooldown = new HashMap<>();

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
                    
                    // Efek Sabit di Punggung (Optimized for Java/Bedrock)
                    drawBackMoon(p, rot, stack);
                    
                    if (stack > 0 && stack < 5) drawSideStack(p, stack);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Efek Sabit Punggung: Pakai END_ROD & WAX_ON agar tidak jatuh di Bedrock
     */
    private void drawBackMoon(Player p, double rot, int stack) {
        Location loc = p.getLocation().add(0, 1.2, 0);
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        loc.add(dir.multiply(-0.4)); // Biar nempel di punggung

        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        // Step 0.3 agar tidak terlalu rapat (mengurangi trail/jejak saat jalan)
        for (double t = -1.5; t <= 1.5; t += 0.3) {
            double taper = Math.cos(t / 2.0);
            double rx = (Math.cos(t) * taper * Math.cos(rot)) - (Math.sin(t) * Math.sin(rot));
            double ry = (Math.cos(t) * taper * Math.sin(rot)) + (Math.sin(t) * Math.cos(rot));
            
            Location pLoc = loc.clone().add(right.clone().multiply(rx)).add(0, ry, 0);

            if (stack >= 5) {
                p.getWorld().spawnParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0, 0);
            } else {
                p.getWorld().spawnParticle(Particle.WAX_ON, pLoc, 1, 0, 0, 0, 0);
                p.getWorld().spawnParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawSideStack(Player p, int stack) {
        Location loc = p.getLocation().add(0, 0.8, 0);
        for (int i = 0; i < stack; i++) {
            double side = (i % 2 == 0) ? 0.6 : -0.6;
            Vector v = p.getLocation().getDirection().getCrossProduct(new Vector(0,1,0)).normalize().multiply(side);
            p.getWorld().spawnParticle(Particle.GLOW, loc.clone().add(v), 1, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        // Dash kecil saat mukul
        p.setVelocity(p.getLocation().getDirection().multiply(0.2).setY(0.1));
        
        // Slash effect (Putih instan)
        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§l● LUNAR READY ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        }

        if (target.getHealth() <= e.getFinalDamage()) {
            p.teleport(target.getLocation());
            executeTPKillEffect(target.getLocation());
        }
    }

    private void executeTPKillEffect(Location loc) {
        new BukkitRunnable() {
            double y = 4.0;
            @Override
            public void run() {
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 2, 0, 0, 0, 0);
                y -= 0.5;
                if (y <= 0) {
                    loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            executeLunarUlti(p);
        } else {
            // Skill Heal Biasa (Recall)
            long now = System.currentTimeMillis();
            if (recallCooldown.getOrDefault(p.getUniqueId(), 0L) > now) return;
            recallCooldown.put(p.getUniqueId(), now + 10000); 
            drawSpiralRecall(p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0));
        }
    }

    /**
     * ULTIMATE: Buka Arena Domain + Buff Speed
     */
    private void executeLunarUlti(Player p) {
        Location center = p.getLocation();
        
        // Buff Speed II buat yang buka Combo
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 1)); 
        p.sendMessage("§f§l● §e§lLUNAR DOMAIN ACTIVATED §f§l●");
        
        p.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.2f);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);

        // Draw Arena di Tanah
        drawLunarArena(center);

        // Damage & Knockup musuh sekitar
        for (Entity en : p.getNearbyEntities(7, 7, 7)) {
            if (en instanceof LivingEntity le && en != p) {
                le.getWorld().spawnParticle(Particle.SONIC_BOOM, le.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                le.damage(18, p);
                le.setVelocity(new Vector(0, 0.8, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
            }
        }
    }

    private void drawLunarArena(Location loc) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 40) { this.cancel(); return; }

                // Outer Circle
                double radius = 6.0;
                for (double i = 0; i < Math.PI * 2; i += 0.4) {
                    double x = Math.cos(i) * radius;
                    double z = Math.sin(i) * radius;
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
                }

                // Logo Bulan Sabit (Crescent) di Tengah
                for (double t = -1.2; t <= 1.2; t += 0.15) {
                    // Sisi Luar Kuning
                    double x1 = Math.cos(t) * 2.8;
                    double z1 = Math.sin(t) * 2.8;
                    loc.getWorld().spawnParticle(Particle.WAX_ON, loc.clone().add(x1 - 1.2, 0.1, z1), 1, 0, 0, 0, 0);

                    // Sisi Dalam Putih
                    double x2 = Math.cos(t) * 2.0;
                    double z2 = Math.sin(t) * 2.0;
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x2 - 0.5, 0.1, z2), 1, 0, 0, 0, 0);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawSpiralRecall(Player p) {
        new BukkitRunnable() {
            double y = 0;
            @Override
            public void run() {
                for (int i = 0; i < 4; i++) {
                    double angle = (y * 5) + (i * Math.PI / 2);
                    p.getWorld().spawnParticle(Particle.GLOW, p.getLocation().add(Math.cos(angle)*1.0, y, Math.sin(angle)*1.0), 1, 0, 0, 0, 0);
                }
                y += 0.4;
                if (y > 2.5) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
