package com.ramadhan;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> comboStack = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            double rot = 0;
            @Override
            public void run() {
                rot += 0.15;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHolding(p)) drawMoon(p, rot);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawMoon(Player p, double rot) {
        Location loc = p.getLocation().add(0, 1.2, 0).add(p.getLocation().getDirection().setY(0).normalize().multiply(-0.55));
        Vector right = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize();
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        Color col = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);

        for (double t = -1.7; t <= 1.7; t += 0.03) {
            double taper = Math.cos(t / 2.12);
            double x = Math.cos(t) * 1.05 * taper;
            double y = Math.sin(t) * 1.05;
            double rx = (x * Math.cos(rot)) - (y * Math.sin(rot));
            double ry = (x * Math.sin(rot)) + (y * Math.cos(rot));
            Location pLoc = loc.clone().add(right.clone().multiply(rx)).add(0, ry, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(col, 0.45f));
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        UUID id = p.getUniqueId();
        comboStack.put(id, Math.min(comboStack.getOrDefault(id, 0) + 1, 5));
        if (e.getEntity() instanceof LivingEntity target && target.getHealth() <= e.getFinalDamage()) {
            p.teleport(target.getLocation());
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 2f);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            for (Entity en : p.getNearbyEntities(5, 5, 5)) if (en instanceof LivingEntity le && en != p) le.damage(10, p);
        } else {
            double max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            p.setHealth(Math.min(p.getHealth() + 1.0, max));
        }
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
