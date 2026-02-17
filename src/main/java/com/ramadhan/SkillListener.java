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
    public SkillListener(GoldenMoon plugin) { this.plugin = plugin; }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 3));
        new BukkitRunnable() {
            double angle = 0;
            int t = 0;
            @Override
            public void run() {
                if (!p.isOnline() || !p.isSneaking() || t > 100) { this.cancel(); return; }
                angle += 0.3;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(x, 1, z), 5, new Particle.DustOptions(Color.ORANGE, 1.5f));
                t += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (e.getEntity() instanceof LivingEntity target) {
            if (target.getHealth() - e.getFinalDamage() <= 0) {
                Vector dash = target.getLocation().getDirection().multiply(-1.2);
                p.teleport(target.getLocation().add(dash));
                p.sendMessage("§6§l⚡ LUNAR DASH!");
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
        }
    }

    private boolean isHolding(Player p) {
        var i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
