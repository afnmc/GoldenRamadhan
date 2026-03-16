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
            if (now - d.lastDash < 4000) return;
            castRpgDash(p);
            d.lastDash = now;
        } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (now - d.lastSlash < 2000) return;
            castRpgCrescent(p);
            d.lastSlash = now;
        } else if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (now - d.lastUlt < 25000) return;
            castRpgUltimate(p);
            d.lastUlt = now;
        }
    }

    // =====================================================
    // ⚡ SKILL 1: THUNDER STRIKE
    // =====================================================
    private void castRpgDash(Player p) {
        int tier = getArmorTier(p);
        World w = p.getWorld();
        Vector dir = p.getLocation().getDirection().setY(0).normalize();

        if (tier == 2) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10, 5, false, false));
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 0.5f);
            
            new BukkitRunnable() {
                int slashes = 0;
                public void run() {
                    if (slashes >= 4) { cancel(); return; }
                    
                    Collection<Entity> targets = w.getNearbyEntities(p.getLocation(), 8, 4, 8);
                    LivingEntity target = null;
                    for (Entity en : targets) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            target = (LivingEntity) en;
                            break; 
                        }
                    }
                    
                    if (target != null) {
                        p.teleport(target.getLocation().add(dir.clone().multiply(-1)));
                        target.damage(6.0, p);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20, 10));
                        w.spawnParticle(Particle.ENCHANTED_HIT, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                        w.spawnParticle(Particle.FLASH, target.getLocation(), 1);
                        w.playSound(target.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.5f);
                    } else {
                        p.setVelocity(dir.multiply(2));
                    }
                    slashes++;
                }
            }.runTaskTimer(plugin, 0, 3);
        } else {
            p.setVelocity(dir.multiply(tier == 1 ? 2.0 : 1.2));
            w.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
        }
    }

    // =====================================================
    // 🌙 SKILL 2: VERDANT STORM
    // =====================================================
    private void castRpgCrescent(Player p) {
        int tier = getArmorTier(p);
        World w = p.getWorld();

        if (tier == 2) {
            p.sendTitle("", "§a§oVerdant Storm!", 5, 20, 5);
            
            new BukkitRunnable() {
                int ticks = 0;
                public void run() {
                    if (ticks > 60) { cancel(); return; }
                    
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.toRadians((ticks * 20) + (i * 120));
                        Location orbit = p.getLocation().add(Math.cos(angle) * 3, 1, Math.sin(angle) * 3);
                        w.spawnParticle(Particle.SWEEP_ATTACK, orbit, 1);
                        w.spawnParticle(Particle.DUST, orbit, 3, new Particle.DustOptions(Color.LIME, 1.5f));
                        
                        for (Entity en : w.getNearbyEntities(orbit, 1.5, 1.5, 1.5)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                LivingEntity le = (LivingEntity) en;
                                le.damage(3.0, p);
                                Vector kb = le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.5).setY(0.3);
                                le.setVelocity(kb);
                                w.playSound(le.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1f);
                            }
                        }
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0, 1);
        } else {
            launchBasicCrescent(p, tier);
        }
    }

    private void launchBasicCrescent(Player p, int tier) {
        Vector dir = p.getLocation().getDirection().multiply(1.5);
        Location loc = p.getEyeLocation();
        new BukkitRunnable() {
            int i = 0;
            public void run() {
                if (i > 15) { cancel(); return; }
                loc.add(dir);
                p.getWorld().spawnParticle(Particle.DUST, loc, 5, new Particle.DustOptions(tier == 1 ? Color.GREEN : Color.WHITE, 1.5f));
                for (Entity en : p.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                    if (en instanceof LivingEntity && !en.equals(p)) ((LivingEntity) en).damage(5.0, p);
                }
                i++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // =====================================================
    // 🌕 SKILL 3: LUNAR CATACLYSM
    // =====================================================
    private void castRpgUltimate(Player p) {
        int tier = getArmorTier(p);
        World w = p.getWorld();
        Location center = p.getLocation();

        if (tier < 2) {
            p.sendMessage("§eUltimate casted!");
            return;
        }

        p.sendTitle("§6§lLUNAR CATACLYSM", "§7Menghancurkan area...", 10, 40, 10);
        w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        
        List<LivingEntity> caughtEnemies = new ArrayList<>();
        for (Entity en : w.getNearbyEntities(center, 8, 5, 8)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                LivingEntity le = (LivingEntity) en;
                le.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 2));
                le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 60, 1));
                caughtEnemies.add(le);
            }
        }

        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t > 20) {
                    executeUltimateSlam(p, center, caughtEnemies);
                    cancel();
                    return;
                }
                for (int i = 0; i < 360; i += 30) {
                    double angle = Math.toRadians(i + t * 10);
                    Location part = center.clone().add(Math.cos(angle) * 5, t * 0.2, Math.sin(angle) * 5);
                    w.spawnParticle(Particle.DUST, part, 2, new Particle.DustOptions(Color.YELLOW, 2f));
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void executeUltimateSlam(Player p, Location center, List<LivingEntity> enemies) {
        World w = p.getWorld();
        w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.1f);
        w.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 0.8f);
        
        for (Entity en : w.getNearbyEntities(center, 15, 15, 15)) {
            if (en instanceof Player targetPlayer) {
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_MINECART_RIDING, 1f, 0.5f);
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 1));
            }
        }

        w.spawnParticle(Particle.EXPLOSION_EMITTER, center, 3);
        w.spawnParticle(Particle.LAVA, center, 50, 2, 0.5, 2, 0.1);

        for (LivingEntity le : enemies) {
            le.removePotionEffect(PotionEffectType.LEVITATION);
            le.setVelocity(new Vector(0, -2.5, 0));
            le.damage(15.0, p);
            w.spawnParticle(Particle.BLOCK, le.getLocation(), 30, 0.5, 0.5, 0.5, Bukkit.createBlockData(Material.DIRT));
        }
    }

    // =====================================================
    // 🛠️ UTILS
    // =====================================================
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
    private static class PlayerData { long lastSlash = 0, lastDash = 0, lastUlt = 0; }
}
