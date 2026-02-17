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

import java.util.HashMap;
import java.util.UUID;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final HashMap<UUID, Long> cd = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startAuraTask();
    }

    // 1. AURA KELAP-KELIP (Samping Player)
    private void startAuraTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHoldingBlade(p)) {
                        Location loc = p.getLocation();
                        // Partikel kuning kelap-kelip di samping kiri-kanan
                        p.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0.6, 1, 0.6), 1, 0.1, 0.5, 0.1, 0);
                        p.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(-0.6, 1, -0.6), 1, 0.1, 0.5, 0.1, 0);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    // 2. SKILL HIT (Lunar Sweep)
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isHoldingBlade(p)) return;

        long now = System.currentTimeMillis();
        if (cd.containsKey(p.getUniqueId()) && now < cd.get(p.getUniqueId()) + 5000) return;

        if (Math.random() < 0.3) { // Peluang 30% biar lebih sering
            cd.put(p.getUniqueId(), now);
            Location targetLoc = e.getEntity().getLocation().add(0, 1, 0);
            
            // Tebasan Sabit Tebal di depan musuh
            drawThickCrescent(targetLoc, p.getLocation().getDirection(), Color.YELLOW, 1.5);
            
            // Logo Bulan di belakang player yang nge-hit
            Location behind = p.getLocation().add(p.getLocation().getDirection().multiply(-1)).add(0, 1.5, 0);
            p.getWorld().spawnParticle(Particle.FIREWORK, behind, 10, 0.2, 0.2, 0.2, 0.05);
            
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.0f);
            for (Entity n : e.getEntity().getNearbyEntities(3, 3, 3)) {
                if (n instanceof LivingEntity le && n != p) le.damage(6.0);
            }
        }
    }

    // 3. SKILL JONGKOK (Lunar Shield)
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingBlade(p) || !e.isSneaking()) return;

        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 3));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.5f);

        // Task khusus 5 detik: Sabit di depan mata + Bulan Muter
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!p.isOnline() || !p.isSneaking() || ticks > 100) { // 5 detik = 100 ticks
                    this.cancel();
                    return;
                }

                // Sabit di DEPAN MATA (Selalu ikut arah pandang)
                Vector dir = p.getLocation().getDirection().normalize();
                Location eyeLoc = p.getEyeLocation().add(dir.multiply(1.2));
                drawThickCrescent(eyeLoc, dir, Color.ORANGE, 0.8);

                // Bulan Muter mengelilingi player
                double angle = ticks * 0.2;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(x, 1, z), 2, 0, 0, 0, 0.02);
                p.getWorld().spawnParticle(Particle.WAX_ON, p.getLocation().add(x, 1.2, z), 1, 0, 0, 0, 0);

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // FUNGSI MATEMATIKA: Menggambar Sabit Mengikuti Arah Pandang (Vektor)
    private void drawThickCrescent(Location center, Vector direction, Color col, double size) {
        Vector side = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
        for (double i = -1; i <= 1; i += 0.1) {
            double offset = Math.pow(i, 2) * size; // Membuat lengkungan
            Location pLoc = center.clone().add(side.clone().multiply(i * size)).subtract(direction.clone().multiply(offset));
            center.getWorld().spawnParticle(Particle.DUST, pLoc, 5, new Particle.DustOptions(col, 2.0f));
            center.getWorld().spawnParticle(Particle.DUST, pLoc.add(0, 0.1, 0), 2, new Particle.DustOptions(Color.WHITE, 0.8f));
        }
    }

    private boolean isHoldingBlade(Player p) {
        var item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
