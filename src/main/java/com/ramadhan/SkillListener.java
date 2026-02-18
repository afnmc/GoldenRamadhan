package com.ramadhan;

import org.bukkit.*;
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
    private final Random random = new Random();
    private final Map<UUID, Integer> comboStack = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startMoonAura();
    }

    private void startMoonAura() {
        new BukkitRunnable() {
            double rot = 0;
            double speed = 0.1;
            @Override
            public void run() {
                // Rotasi dinamis: Cepat-Pelan-Cepat
                if (random.nextDouble() > 0.8) speed = 0.06 + (random.nextDouble() * 0.22);
                rot += speed;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHolding(p)) drawSharpMoon(p, rot);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawSharpMoon(Player p, double rot) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        Color col = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);
        Location center = loc.clone().add(0, 1.2, 0).add(dir.multiply(-0.45));

        for (double layer = 0.95; layer <= 1.15; layer += 0.2) {
            for (double t = -1.6; t <= 1.6; t += 0.08) {
                // RUMUS RUNCING (Taper): Ujung atas & bawah ditarik ke tengah (nempel)
                double taper = Math.cos(t / 2.05); 
                double x = Math.cos(t) * layer * taper;
                double y = Math.sin(t) * layer;

                double rx = (x * Math.cos(rot)) - (y * Math.sin(rot));
                double ry = (x * Math.sin(rot)) + (y * Math.cos(rot));

                Location pLoc = center.clone().add(right.clone().multiply(rx)).add(0, ry, 0);
                p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(col, 0.55f));
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        UUID id = p.getUniqueId();
        int stack = comboStack.getOrDefault(id, 0);
        if (System.currentTimeMillis() - lastHit.getOrDefault(id, 0L) > 4000) stack = 0;
        stack = Math.min(stack + 1, 5);
        comboStack.put(id, stack);
        lastHit.put(id, System.currentTimeMillis());
        if (stack == 5) p.sendTitle("", "§f§lMOONLIGHT READY", 0, 15, 5);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        if (comboStack.getOrDefault(p.getUniqueId(), 0) >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.5f);
            // Tambahin Burst Effect di sini kalau mau
        }
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
