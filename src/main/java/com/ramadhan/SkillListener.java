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
    }

    // SKILL 1: SHIELD ORBIT (Jongkok)
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingBlade(p) || !e.isSneaking()) return;

        // Efek Kebal
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 3));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);

        // Partikel Orbit Otomatis
        new BukkitRunnable() {
            int ticks = 0;
            double angle = 0;

            @Override
            public void run() {
                if (!p.isOnline() || !p.isSneaking() || ticks > 100) { // 5 Detik Max
                    this.cancel();
                    return;
                }

                angle += 0.2; // Kecepatan putar
                // Rumus Matematika Lingkaran
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                
                Location loc = p.getLocation().add(x, 1, z);
                
                // Spawn Partikel Sabit Kuning Muter
                p.getWorld().spawnParticle(Particle.DUST, loc, 5, new Particle.DustOptions(Color.ORANGE, 1.5f));
                p.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0, 0, 0, 0);

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // SKILL 2: KILL TELEPORT (Dash)
    @EventHandler
    public void onKill(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isHoldingBlade(p)) return;

        if (e.getEntity() instanceof LivingEntity victim) {
            // Cek apakah musuh mati kena hit ini
            if (victim.getHealth() - e.getFinalDamage() <= 0) {
                
                // Teleport ke BELAKANG mayat musuh
                Location victimLoc = victim.getLocation();
                Vector dir = victimLoc.getDirection().normalize().multiply(-1.5); // Mundur 1.5 blok
                Location teleportLoc = victimLoc.add(dir);
                
                // Hindari teleport ke dalam blok
                if (teleportLoc.getBlock().getType() != Material.AIR) {
                    teleportLoc = victimLoc; // Kalau sempit, teleport ke tempat mayat aja
                }

                p.teleport(teleportLoc);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                p.sendMessage("§6§l⚡ DASH KILL!");
                
                // Efek Ledakan Kecil
                p.getWorld().spawnParticle(Particle.EXPLOSION, victim.getLocation(), 1);
            }
        }
    }

    private boolean isHoldingBlade(Player p) {
        var item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
