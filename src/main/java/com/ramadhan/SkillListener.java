package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startPermanentAura();
    }

    // --- 1. AURA & BULAN SABIT PUNGGUNG (🌙) ---
    private void startPermanentAura() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHolding(p)) {
                        Location loc = p.getLocation();
                        // Aura kelap-kelip badan
                        p.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0, 0.8, 0), 2, 0.3, 0.5, 0.3, 0);
                        drawBackCrescent(p);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 4L); 
    }

    private void drawBackCrescent(Player p) {
        Location loc = p.getLocation();
        Vector backDir = loc.getDirection().setY(0).normalize().multiply(-1);
        Location backCenter = loc.clone().add(backDir.multiply(0.45)).add(0, 1.3, 0);
        Vector sideDir = new Vector(-backDir.getZ(), 0, backDir.getX()).normalize();

        for (double i = -0.7; i <= 0.7; i += 0.05) {
            double curveAmount = (0.49 - Math.pow(i, 2)) * 1.3; 
            Location dot = backCenter.clone().add(0, i, 0).add(sideDir.clone().multiply(curveAmount));
            p.getWorld().spawnParticle(Particle.DUST, dot, 1, new Particle.DustOptions(Color.YELLOW, 0.8f));
        }
    }

    // --- 2. ATTACK & EXECUTION (DASH KILL) ---
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (e.getEntity() instanceof LivingEntity target) {
            drawHorizontalSlash(target.getLocation().add(0, 1, 0), p.getLocation().getDirection());
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
            target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);

            if (target.getHealth() - e.getFinalDamage() <= 0) {
                drawVerticalStab(target.getLocation());
                Vector dash = target.getLocation().getDirection().multiply(-1.5);
                p.teleport(target.getLocation().add(dash));
                p.sendMessage("§6§l⚡ LUNAR EXECUTION!");
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
            }
        }
    }

    private void drawHorizontalSlash(Location loc, Vector dir) {
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        for (double i = -1; i <= 1; i += 0.1) {
            double offset = (1 - Math.pow(i, 2)) * 0.5;
            Location pLoc = loc.clone().add(side.clone().multiply(i)).add(dir.clone().multiply(offset));
            loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(Color.YELLOW, 1.2f));
        }
    }

    private void drawVerticalStab(Location loc) {
        for (double y = 0; y <= 3; y += 0.15) {
            double curve = (Math.pow(y - 1.5, 2) - 2.25) * 0.2; 
            Location dot = loc.clone().add(curve, 3 - y, 0);
            loc.getWorld().spawnParticle(Particle.DUST, dot, 3, new Particle.DustOptions(Color.ORANGE, 2.0f));
        }
    }

    // --- 3. RECALL SKILL (MLBB STYLE) ---
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        new BukkitRunnable() {
            double angle = 0;
            double radius = 1.8; // Lebar bawah
            double height = 0;   // Mulai dari kaki
            int t = 0;

            @Override
            public void run() {
                // Berhenti jika lepas shift atau sudah 3 detik (60 ticks)
                if (!p.isOnline() || !p.isSneaking() || t > 60) {
                    if (t > 60) { // Efek Finish: Simbol Bulan di Atas
                        drawCrescentSymbol(p.getLocation().add(0, 2.5, 0));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                        p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0, 1, 0), 3);
                    }
                    this.cancel();
                    return;
                }

                // Efek Spiral Mengerucut ke Atas (Recalling Visual)
                for (int i = 0; i < 2; i++) {
                    angle += 0.4;
                    radius -= 0.028; // Mengecilkan lingkaran
                    height += 0.045; // Menaikkan posisi

                    if (radius < 0.2) radius = 0.2;

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location partLoc = p.getLocation().add(x, height, z);
                    p.getWorld().spawnParticle(Particle.DUST, partLoc, 1, new Particle.DustOptions(Color.ORANGE, 1.3f));
                    p.getWorld().spawnParticle(Particle.WAX_ON, partLoc, 1, 0, 0, 0, 0);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L); 
    }

    private void drawCrescentSymbol(Location loc) {
        // Gambar sabit kecil horizontal sebagai mahkota di akhir recall
        for (double i = -0.6; i <= 0.6; i += 0.1) {
            double curve = (0.36 - Math.pow(i, 2)) * 0.7;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(i, 0, curve), 5, new Particle.DustOptions(Color.YELLOW, 1.5f));
        }
    }

    private boolean isHolding(Player p) {
        var i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
