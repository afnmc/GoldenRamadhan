package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {

    private final GoldenMoon plugin;
    private final Map<UUID, PlayerData> data = new HashMap<>();
    private final Random r = new Random();

    public SkillListener(GoldenMoon plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingSword(p)) return;
        PlayerData d = get(p);
        long now = System.currentTimeMillis();

        if (p.isSneaking() && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            if (now - d.lastSkill1 < 3000) return;
            executeSkill1(p);
            d.lastSkill1 = now;
        } 
        else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (now - d.lastSkill2 < 2000) return;
            executeSkill2(p);
            d.lastSkill2 = now;
        }
        else if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (now - d.lastSkill3 < 25000) return;
            executeSkill3(p);
            d.lastSkill3 = now;
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (getArmorTier(p) == 2 && p.isSneaking()) {
            e.setCancelled(true);
            p.setFlying(false);
            if (!p.getAllowFlight()) activateEliteFlight(p);
        }
    }

    private void executeSkill1(Player p) {
        int tier = getArmorTier(p);
        World w = p.getWorld();

        if (tier == 2) {
            Location oldLoc = p.getLocation();
            Location newLoc = p.getLocation().add(p.getLocation().getDirection().multiply(7));
            p.teleport(newLoc);
            w.spawnParticle(Particle.WHITE_SMOKE, oldLoc, 20, 0.2, 1, 0.2, 0.05);
            w.playSound(newLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
            w.spawnParticle(Particle.FLASH, newLoc, 1);
        } else if (tier == 1) {
            p.setVelocity(p.getLocation().getDirection().multiply(2.0).setY(0.2));
            // FIXED LINE BELOW:
            w.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);
        } else {
            p.setVelocity(new Vector(0, 0.5, 0));
        }
    }

    private void executeSkill2(Player p) {
        int tier = getArmorTier(p);
        World w = p.getWorld();

        if (tier == 2) {
            Location targetLoc = p.getLocation().add(p.getLocation().getDirection().multiply(5));
            new BukkitRunnable() {
                int i = 0;
                public void run() {
                    if (i > 10) { cancel(); return; }
                    w.spawnParticle(Particle.DUST, targetLoc.clone().add(0, i * 0.5, 0), 10, new Particle.DustOptions(Color.YELLOW, 2f));
                    if (i == 5) {
                        w.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 2f);
                        for (Entity en : w.getNearbyEntities(targetLoc, 3, 3, 3)) {
                            if (en instanceof LivingEntity le && !en.equals(p)) le.damage(8, p);
                        }
                    }
                    i++;
                }
            }.runTaskTimer(plugin, 0, 1);
        } else if (tier == 1) {
            spawnSlice(p, Color.WHITE);
        } else {
            w.spawnParticle(Particle.CRIT, p.getLocation().add(p.getLocation().getDirection().multiply(2)), 10);
        }
    }

    private void executeSkill3(Player p) {
        int tier = getArmorTier(p);
        World w = p.getWorld();

        if (tier == 2) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 5));
            p.sendTitle("§6§lGOLDEN MOON", "§fGathering Cosmic Energy...", 10, 40, 10);
            
            new BukkitRunnable() {
                int t = 0;
                public void run() {
                    if (t > 60) {
                        p.removePotionEffect(PotionEffectType.LEVITATION);
                        p.setVelocity(new Vector(0, -6, 0));
                        checkSlam(p);
                        cancel();
                        return;
                    }
                    w.spawnParticle(Particle.END_ROD, p.getLocation(), 5, 0.5, 0.5, 0.5, 0.05);
                    t++;
                }
            }.runTaskTimer(plugin, 0, 1);
        } else if (tier == 1) {
            w.spawnParticle(Particle.EXPLOSION_EMITTER, p.getLocation(), 1);
            w.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
            for (Entity en : w.getNearbyEntities(p.getLocation(), 5, 5, 5)) {
                if (en instanceof LivingEntity le && !en.equals(p)) le.damage(10, p);
            }
        } else {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
        }
    }

    private void activateEliteFlight(Player p) {
        p.setAllowFlight(true);
        p.setFlying(true);
        p.sendTitle("§e§lSKY MODE", "§fTerbang aktif selama 30 detik", 10, 20, 10);

        new BukkitRunnable() {
            int ticks = 0;
            public void run() {
                if (ticks >= 600 || !p.isOnline() || getArmorTier(p) < 2) {
                    p.setFlying(false);
                    p.setAllowFlight(false);
                    p.sendMessage("§cMode Fly berakhir.");
                    cancel();
                    return;
                }
                Location l = p.getLocation();
                p.getWorld().spawnParticle(Particle.CLOUD, l, 2, 0.2, 0.1, 0.2, 0.02);
                p.getWorld().spawnParticle(Particle.DUST, l, 3, new Particle.DustOptions(Color.YELLOW, 1f));
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private void checkSlam(Player p) {
        new BukkitRunnable() {
            public void run() {
                if (p.isOnGround()) {
                    World w = p.getWorld();
                    Location l = p.getLocation();
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, l, 3);
                    w.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
                    for (Entity en : w.getNearbyEntities(l, 8, 4, 8)) {
                        if (en instanceof LivingEntity le && !en.equals(p)) {
                            le.damage(18, p);
                            le.setVelocity(le.getLocation().toVector().subtract(l.toVector()).normalize().multiply(2).setY(1));
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private void spawnSlice(Player p, Color col) {
        Vector dir = p.getLocation().getDirection().multiply(1.5);
        Location loc = p.getEyeLocation();
        new BukkitRunnable() {
            int i = 0;
            public void run() {
                if (i > 15) { cancel(); return; }
                loc.add(dir);
                p.getWorld().spawnParticle(Particle.DUST, loc, 8, new Particle.DustOptions(col, 1.5f));
                for (Entity en : p.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (en instanceof LivingEntity le && !en.equals(p)) {
                        le.damage(6, p);
                        i = 20;
                    }
                }
                i++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private int getArmorTier(Player p) {
        if (plugin.getArmorManager().hasFullEliteSet(p)) return 2;
        if (plugin.getArmorManager().hasCrescentSet(p)) return 1;
        return 0;
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private PlayerData get(Player p) { return data.computeIfAbsent(p.getUniqueId(), k -> new PlayerData()); }
    private static class PlayerData { long lastSkill1 = 0, lastSkill2 = 0, lastSkill3 = 0; }
}
