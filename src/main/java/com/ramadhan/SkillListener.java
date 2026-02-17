package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.HashMap;
import java.util.UUID;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final HashMap<UUID, Long> cd = new HashMap<>();

    public SkillListener(GoldenMoon plugin) { this.plugin = plugin; }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        var item = p.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().getDisplayName().contains("Golden Crescent Blade")) return;

        long now = System.currentTimeMillis();
        int cooldown = plugin.getConfig().getInt("lunar-sweep-cooldown") * 1000;

        if (cd.containsKey(p.getUniqueId()) && now < cd.get(p.getUniqueId()) + cooldown) return;

        if (Math.random() < plugin.getConfig().getDouble("lunar-sweep-chance")) {
            cd.put(p.getUniqueId(), now);
            drawCrescent(e.getEntity().getLocation(), Color.YELLOW);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1);
            for (Entity n : e.getEntity().getNearbyEntities(3, 3, 3)) {
                if (n instanceof LivingEntity le && n != p) le.damage(4.0);
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        var item = p.getInventory().getItemInMainHand();
        if (item != null && item.hasItemMeta() && item.getItemMeta().getDisplayName().contains("Golden Crescent Blade") && e.isSneaking()) {
            drawCrescent(p.getLocation().add(0, 1, 0), Color.ORANGE);
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 4));
            p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1, 1);
        }
    }

    private void drawCrescent(Location loc, Color col) {
        for (double t = 0; t < Math.PI; t += 0.1) {
            double x = Math.sin(t) * 1.5;
            double z = Math.cos(t) * 1.5;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0, z), 1, new Particle.DustOptions(col, 1.5f));
        }
    }
}
