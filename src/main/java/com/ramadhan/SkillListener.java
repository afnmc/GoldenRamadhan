package com.ramadhan;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class SkillListener implements Listener {
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            var item = p.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Golden Crescent Blade")) {
                if (Math.random() < 0.20) {
                    e.getEntity().getWorld().spawnParticle(Particle.FLASH, e.getEntity().getLocation().add(0,1,0), 5);
                    e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
                    for (Entity n : e.getEntity().getNearbyEntities(3, 3, 3)) {
                        if (n instanceof LivingEntity le && n != p) le.damage(4.0);
                    }
                    p.sendMessage("§6§l» LUNAR SWEEP ACTIVATED! «");
                }
            }
        }
    }
}

