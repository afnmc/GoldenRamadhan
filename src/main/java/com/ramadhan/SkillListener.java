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

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startPermanentAura(); // Menjalankan aura terus-menerus
    }

    // --- 1. AURA BADAN & SABIT PUNGGUNG (Otomatis) ---
    private void startPermanentAura() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHolding(p)) {
                        Location loc = p.getLocation();
                        
                        // Aura Partikel Kuning Kelap-kelip di sekitar kaki & badan
                        p.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0, 0.2, 0), 2, 0.3, 0.2, 0.3, 0);
                        p.getWorld().spawnParticle(Particle.WAX_OFF, loc.clone().add(0, 1.2, 0), 1, 0.2, 0.4, 0.2, 0);
                        
                        // Gambar Bulan Sabit di Punggung
                        drawBackCrescent(p);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 4L); // Dipercepat jadi tiap 4 ticks agar aura lebih padat
    }

    private void drawBackCrescent(Player p) {
        Location loc = p.getLocation();
        // Vektor ke arah belakang punggung player
        Vector dir = loc.getDirection().setY(0).normalize().multiply(-0.35); 
        Location backCenter = loc.clone().add(dir).add(0, 1.1, 0);
        
        // Vektor samping untuk lebar sabit
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        for (double i = -0.4; i <= 0.4; i += 0.1) {
            double curve = Math.pow(i, 2) * 0.6; // Kelengkungan sabit
            Location dot = backCenter.clone().add(side.clone().multiply(i)).add(dir.clone().multiply(curve));
            p.getWorld().spawnParticle(Particle.DUST, dot, 1, new Particle.DustOptions(Color.YELLOW, 0.7f));
        }
    }

    // --- 2. ATTACK SLASH & VERTICAL STAB (Kill) ---
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        
        if (e.getEntity() instanceof LivingEntity target) {
            // Efek Tebasan Horizontal (Setiap Hit)
            drawHorizontalSlash(target.getLocation().add(0, 1, 0), p.getLocation().getDirection());
            
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
            target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);

            // Efek Kill: Dash + Sabit Menusuk Tanah
            if (target.getHealth() - e.getFinalDamage() <= 0) {
                drawVerticalStab(target.getLocation());
                
                Vector dash = target.getLocation().getDirection().multiply(-1.5);
                p.teleport(target.getLocation().add(dash));
                
                p.sendMessage("§6§l⚡ LUNAR EXECUTION!");
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 2f);
            }
        }
    }

    private void drawHorizontalSlash(Location loc, Vector dir) {
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        for (double i = -1; i <= 1; i += 0.2) {
            double offset = Math.pow(i, 2) * 0.5;
            Location pLoc = loc.clone().add(side.clone().multiply(i)).subtract(dir.clone().multiply(offset));
            loc.getWorld().spawnParticle(Particle.DUST, pLoc, 2, new Particle.DustOptions(Color.YELLOW, 1.2f));
        }
    }

    private void drawVerticalStab(Location loc) {
        for (double y = 0; y <= 3; y += 0.2) {
            double curve = Math.pow(y - 1.5, 2) * 0.3;
            Location dot = loc.clone().add(curve, 3 - y, 0);
            loc.getWorld().spawnParticle(Particle.DUST, dot, 5, new Particle.DustOptions(Color.ORANGE, 2.0f));
            loc.getWorld().spawnParticle(Particle.WAX_ON, dot, 1, 0, 0, 0, 0);
        }
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
    }

    // --- 3. SHIELD ORBIT (Jongkok) ---
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 3));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);

        new BukkitRunnable() {
            double angle = 0;
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline() || !p.isSneaking() || t > 100) { this.cancel(); return; }
                
                angle += 0.4;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                
                Location orbitLoc = p.getLocation().add(x, 1, z);
                p.getWorld().spawnParticle(Particle.DUST, orbitLoc, 5, new Particle.DustOptions(Color.ORANGE, 1.5f));
                p.getWorld().spawnParticle(Particle.FIREWORK, orbitLoc, 1, 0, 0, 0, 0.02);
                
                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private boolean isHolding(Player p) {
        var i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
