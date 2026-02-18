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
        startVisualTask();
    }

    // --- VISUAL SABIT (RUNCING & SOLID) ---
    private void startVisualTask() {
        new BukkitRunnable() {
            double rot = 0;
            @Override
            public void run() {
                rot += 0.15;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHolding(p)) drawSolidMoon(p, rot);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // 1 Tick biar super smooth
    }

    private void drawSolidMoon(Player p, double rot) {
        Location center = p.getLocation().add(0, 1.2, 0).add(p.getLocation().getDirection().setY(0).normalize().multiply(-0.55));
        Vector right = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize();
        
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        Color col = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);

        // Step 0.03 membuat partikel rapat (Solid). t=1.7 membuat ujung overlap (Nempel).
        for (double t = -1.7; t <= 1.7; t += 0.03) {
            double taper = Math.cos(t / 2.1); // Rumus menyatukan ujung ke tengah
            double x = Math.cos(t) * 1.05 * taper;
            double y = Math.sin(t) * 1.05;

            double rx = (x * Math.cos(rot)) - (y * Math.sin(rot));
            double ry = (x * Math.sin(rot)) + (y * Math.cos(rot));

            Location pLoc = center.clone().add(right.clone().multiply(rx)).add(0, ry, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(col, 0.45f));
        }
    }

    // --- SKILL PASIF (COMBO & DASH) ---
    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        int stack = Math.min(comboStack.getOrDefault(id, 0) + 1, 5);
        comboStack.put(id, stack);

        if (stack == 5) p.sendTitle("", "§f§lMOONLIGHT ACTIVE", 0, 10, 5);

        // Skill Dash: Teleport ke posisi musuh jika mati
        if (target.getHealth() <= e.getFinalDamage()) {
            p.teleport(target.getLocation());
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 2f);
            p.getWorld().spawnParticle(Particle.WITCH, p.getLocation(), 10);
        }
    }

    // --- SKILL AKTIF (SHIFT / SNEAK) ---
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        if (stack >= 5) {
            // SKILL BURST
            comboStack.put(p.getUniqueId(), 0);
            p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 1);
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.2f);
            for (Entity en : p.getNearbyEntities(5, 5, 5)) {
                if (en instanceof LivingEntity le && en != p) {
                    le.damage(12, p);
                    le.setVelocity(le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.5));
                }
            }
        } else {
            // SKILL RECALL (HEAL)
            double maxH = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            p.setHealth(Math.min(p.getHealth() + 1.0, maxH));
            p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1.5, 0), 1);
        }
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
