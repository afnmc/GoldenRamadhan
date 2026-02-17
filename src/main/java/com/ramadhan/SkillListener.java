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

import java.util.Random;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final Random random = new Random();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startPermanentAura();
    }

    // --- 1. AURA BULAN SABIT (Tetap seperti settingan terakhir yang sudah bagus) ---
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

    private void drawBackCrescent(Player p, boolean isRecallExplosion) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        Vector backDir = dir.clone().multiply(-1);
        Vector rightDir = new Vector(-dir.getZ(), 0, dir.getX()).normalize();

        double heightOffset = isRecallExplosion ? 2.5 : 1.2; 
        double backOffset = isRecallExplosion ? 0 : 0.4;
        
        Location center = loc.clone().add(0, heightOffset, 0).add(backDir.multiply(backOffset));

        for (double t = -1.3; t <= 1.3; t += 0.05) {
            double curveWidth = Math.cos(t) * 1.5; 
            double height = t * 1.3; 
            
            // Rotasi Miring (Tilt)
            double tiltedX = (curveWidth * Math.cos(0.35)) - (height * Math.sin(0.35));
            double tiltedY = (curveWidth * Math.sin(0.35)) + (height * Math.cos(0.35));

            Location particleLoc = center.clone()
                    .add(rightDir.clone().multiply(tiltedX))
                    .add(0, tiltedY, 0);

            // Depth Effect
            double depth = Math.abs(t) * 0.3; 
            particleLoc.add(dir.clone().multiply(depth));

            Particle.DustOptions color = isRecallExplosion ? 
                new Particle.DustOptions(Color.fromRGB(255, 223, 0), 1.5f) : 
                new Particle.DustOptions(Color.YELLOW, 0.8f);

            p.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, color);
            
            if (isRecallExplosion && Math.random() > 0.8) {
                 p.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    // --- 2. SISTEM SWING & SLASH (Baru: Random & Diagonal) ---
    
    // Event saat memukul angin/block (Animasi Angin)
    @EventHandler
    public void onSwing(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p)) return;
        
        // Deteksi klik kiri (pukul)
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            playSlashAnimation(p);
        }
    }

    // Event saat memukul musuh (Impact)
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        
        if (e.getEntity() instanceof LivingEntity target) {
            // Efek Impact (Ledakan kecil saat kena)
            target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
            
            // Efek Debuff
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));

            // Kill Effect (Dash & Thunder)
            if (target.getHealth() - e.getFinalDamage() <= 0) {
                drawVerticalStab(target.getLocation());
                p.teleport(target.getLocation().add(target.getLocation().getDirection().multiply(-1.5)));
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                p.sendMessage("§6§l⚡ LUNAR EXECUTION!");
            }
        }
    }

    // Method Animasi Slash Diagonal Random
    private void playSlashAnimation(Player p) {
        Location eyeLoc = p.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize(); // Vektor kanan
        
        // Randomize arah: true = KananAtas ke KiriBawah, false = KiriAtas ke KananBawah
        boolean slashFromRight = random.nextBoolean();
        
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.4f);

        // Loop menggambar garis tebasan
        for (double i = -1.2; i <= 1.2; i += 0.1) {
            
            // 1. Curve (Lengkungan ke depan) -> Biar gak gepeng
            double forwardCurve = (1.44 - (i * i)) * 0.4; 
            
            // 2. Tentukan posisi Horizontal (Kanan/Kiri)
            // Kalau i negatif dia di kiri, positif di kanan
            double horizontalOffset = i * 1.5; 

            // 3. Tentukan posisi Vertikal (Atas/Bawah) -> Biar Diagonal
            // Jika slashFromRight: Saat i positif (kanan), posisi harus tinggi.
            double verticalSlope = slashFromRight ? (i * 0.8) : (-i * 0.8);
            
            // Hitung lokasi partikel
            Location particleLoc = eyeLoc.clone()
                    .add(dir.clone().multiply(1.5 + forwardCurve)) // Jarak dari mata + lengkungan
                    .add(right.clone().multiply(horizontalOffset)) // Geser Kanan/Kiri
                    .add(0, verticalSlope, 0) // Geser Atas/Bawah (Miring)
                    .subtract(0, 0.5, 0); // Turunkan dikit biar pas di tengah layar

            p.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.YELLOW, 0.6f));
            
            // Tambah partikel putih di ujung biar tajam
            if (Math.abs(i) > 0.8) {
                p.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawVerticalStab(Location loc) {
        for (double y = 0; y <= 3; y += 0.15) {
            double curve = (Math.pow(y - 1.5, 2) - 2.25) * 0.2;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(curve, 3 - y, 0), 3, new Particle.DustOptions(Color.ORANGE, 2.0f));
        }
    }

    // --- 3. RECALL (Tetap sama) ---
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

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
                        p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0, 2.3, 0), 3);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    }
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 2; i++) {
                    angle += 0.4;
                    radius -= 0.025;
                    height += 0.04; 
                    if (radius < 0.2) radius = 0.2;
                    if (height > 2.2) height = 2.2; 
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(x, height, z), 1, new Particle.DustOptions(Color.ORANGE, 1.2f));
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
