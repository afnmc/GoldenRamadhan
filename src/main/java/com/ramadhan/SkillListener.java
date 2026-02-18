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
    private final Random rand = new Random();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startTicking();
    }

    private void startTicking() {
        new BukkitRunnable() {
            double rot = 0;
            @Override
            public void run() {
                rot += 0.15;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isHolding(p)) continue;
                    
                    int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
                    drawBackMoon(p, rot, stack);
                    
                    if (stack >= 5) {
                        drawAtomEffect(p, rot); // Partikel Atom
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // 1. Sabit & Atom Logic
    private void drawBackMoon(Player p, double rot, int stack) {
        Location loc = p.getLocation().add(0, 1.2, 0).add(p.getLocation().getDirection().setY(0).normalize().multiply(-0.55));
        Vector right = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize();
        Color col = (stack >= 5) ? Color.fromRGB(255, 255, 240) : Color.fromRGB(255, 215, 0);

        for (double t = -1.7; t <= 1.7; t += 0.05) {
            double taper = Math.cos(t / 2.12);
            double rx = (Math.cos(t) * 1.05 * taper * Math.cos(rot)) - (Math.sin(t) * 1.05 * Math.sin(rot));
            double ry = (Math.cos(t) * 1.05 * taper * Math.sin(rot)) + (Math.sin(t) * 1.05 * Math.cos(rot));
            p.getWorld().spawnParticle(Particle.DUST, loc.clone().add(right.clone().multiply(rx)).add(0, ry, 0), 1, new Particle.DustOptions(col, 0.6f));
        }
    }

    private void drawAtomEffect(Player p, double rot) {
        // Partikel melingkar silang kaya orbit atom
        Location loc = p.getLocation().add(0, 1, 0);
        for (int i = 0; i < 2; i++) {
            double angle = rot + (i * Math.PI);
            double x = Math.cos(angle) * 0.8;
            double z = Math.sin(angle) * 0.8;
            double y = Math.sin(angle * 2) * 0.5;
            p.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        // 2. Attack Biasa: Tebasan Miring Random (Kuning Keputihan)
        drawMiringSlash(target.getLocation());
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§lREADY", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
        }

        // 3. TP Kill: Efek Bener-bener dari Atas ke Bawah
        if (target.getHealth() <= e.getFinalDamage()) {
            p.teleport(target.getLocation());
            drawVerticalThunder(target.getLocation());
        }
    }

    private void drawMiringSlash(Location loc) {
        boolean direction = rand.nextBoolean();
        for (double i = -0.8; i <= 0.8; i += 0.1) {
            double offset = direction ? i : -i;
            Location pLoc = loc.clone().add(i, (offset * 0.5) + 1, 0);
            loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 200), 1f));
        }
    }

    private void drawVerticalThunder(Location loc) {
        // Dari langit (y+5) ke bawah
        for (double y = 0; y <= 6; y += 0.2) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 3, new Particle.DustOptions(Color.YELLOW, 1.5f));
        }
        // Ledakan Putih Kuning di bawah
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 2);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        if (stack >= 5) {
            // 4. Burst: Atom menyatu, nusuk musuh, mental
            comboStack.put(p.getUniqueId(), 0);
            p.getWorld().spawnParticle(Particle.SONIC_BOOM, p.getLocation().add(p.getLocation().getDirection().multiply(1.5)), 1);
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.5f);
            
            for (Entity en : p.getNearbyEntities(6, 6, 6)) {
                if (en instanceof LivingEntity le && en != p) {
                    le.damage(20, p);
                    le.setVelocity(p.getLocation().getDirection().multiply(2).setY(0.5));
                }
            }
        } else {
            // 5. Recall: Kuning Memutar Badan naik ke atas
            drawSpiralHeal(p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 2));
        }
    }

    private void drawSpiralHeal(Player p) {
        new BukkitRunnable() {
            double y = 0;
            @Override
            public void run() {
                for (int i = 0; i < 2; i++) {
                    double angle = y * 5;
                    double x = Math.cos(angle) * 0.7;
                    double z = Math.sin(angle) * 0.7;
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(x, y, z), 1, new Particle.DustOptions(Color.YELLOW, 0.8f));
                }
                y += 0.2;
                if (y > 2.5) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
