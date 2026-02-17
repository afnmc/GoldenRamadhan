package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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

    // --- 1. AURA BULAN SABIT PUNGGUNG (Miring & Melengkung Tajam) ---
    private void startPermanentAura() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHolding(p)) {
                        drawBackCrescent(p, false); 
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 4L); 
    }

    private void drawBackCrescent(Player p, boolean isExplosion) {
        Location loc = p.getLocation();
        Vector backDir = loc.getDirection().setY(0).normalize().multiply(-1);
        Location center = isExplosion ? loc.clone().add(0, 1.2, 0) : loc.clone().add(backDir.multiply(0.35)).add(0, 1.2, 0);
        
        Vector sideDir = new Vector(-backDir.getZ(), 0, backDir.getX()).normalize();
        
        for (double i = -0.7; i <= 0.7; i += 0.05) {
            double curve = (0.49 - Math.pow(i, 2)) * 1.4; 
            double tiltOffset = i * 0.3; // Efek miring ke kiri

            Location dot = center.clone()
                    .add(0, i + tiltOffset, 0) 
                    .add(sideDir.clone().multiply(curve));
            
            Particle.DustOptions color = isExplosion ? 
                new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.8f) : 
                new Particle.DustOptions(Color.YELLOW, 0.7f);

            p.getWorld().spawnParticle(Particle.DUST, dot, 1, color);
        }
    }

    // --- 2. EFFECT SAAT SWING (Klik Kiri Tanpa Hit) ---
    @EventHandler
    public void onSwing(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p)) return;
        
        // Cek jika player melakukan klik kiri (ayunan pedang)
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Munculkan tebasan di depan player (2 blok di depan mata)
            Location slashLoc = p.getEyeLocation().add(p.getLocation().getDirection().multiply(2));
            drawHorizontalSlash(slashLoc, p.getLocation().getDirection());
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.5f);
        }
    }

    // --- 3. ATTACK & DASH KILL (Saat Kena Entity) ---
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (e.getEntity() instanceof LivingEntity target) {
            // Partikel Flash saat kena hit
            target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));

            if (target.getHealth() - e.getFinalDamage() <= 0) {
                drawVerticalStab(target.getLocation());
                Vector dash = target.getLocation().getDirection().multiply(-1.5);
                p.teleport(target.getLocation().add(dash));
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                p.sendMessage("§6§l⚡ LUNAR EXECUTION!");
            }
        }
    }

    private void drawHorizontalSlash(Location loc, Vector dir) {
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        for (double i = -1; i <= 1; i += 0.1) {
            double offset = (1 - Math.pow(i, 2)) * 0.6; // Tebasan melengkung
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

    // --- 4. RECALL MODE (Spiral & Instant Heal) ---
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 70, 4));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

        new BukkitRunnable() {
            double angle = 0;
            double radius = 1.5; 
            double height = 0;   
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline() || !p.isSneaking() || t > 50) { 
                    if (t > 50) { 
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1));
                        drawBackCrescent(p, true); 
                        p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0, 1, 0), 3);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    }
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 2; i++) {
                    angle += 0.4;
                    radius -= 0.025;
                    height += 0.035; 

                    if (radius < 0.3) radius = 0.3;
                    if (height > 2.0) height = 2.0; 

                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;

                    Location partLoc = p.getLocation().add(x, height, z);
                    p.getWorld().spawnParticle(Particle.DUST, partLoc, 1, new Particle.DustOptions(Color.ORANGE, 1.2f));
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L); 
    }

    private boolean isHolding(Player p) {
        var i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
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
