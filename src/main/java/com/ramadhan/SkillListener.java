package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SkillListener implements Listener {
    
    private static final Color DASH_PRIMARY = Color.fromRGB(0, 255, 255);
    private static final Color DASH_SECONDARY = Color.fromRGB(100, 200, 255);
    private static final Color DASH_ACCENT = Color.fromRGB(255, 255, 255);
    
    private static final Color CRESCENT_PRIMARY = Color.fromRGB(50, 255, 150);
    private static final Color CRESCENT_SECONDARY = Color.fromRGB(100, 255, 200);
    private static final Color CRESCENT_ACCENT = Color.fromRGB(200, 255, 220);
    
    private static final Color ULT_PRIMARY = Color.fromRGB(255, 215, 0);
    private static final Color ULT_SECONDARY = Color.fromRGB(180, 140, 220);
    private static final Color ULT_ACCENT = Color.fromRGB(255, 240, 180);

    private final GoldenMoon plugin;
    private final Map<UUID, PlayerData> data = new HashMap<>();
    private final Map<UUID, Long> moonMarked = new HashMap<>();
    private final Random r = new Random();
    
    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        data.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingSword(p)) return;

        PlayerData d = get(p);
        long now = System.currentTimeMillis();

        if (p.isSneaking() && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            if (now - d.lastDash < getDashCooldown(p)) {
                sab(p, "§b⚡ Cooldown: " + (getDashCooldown(p)/1000.0 - (now - d.lastDash)/1000.0) + "s");
                return;
            }
            performThunderStepDash(p);
            d.lastDash = now;
            return;
        }

        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (now - d.lastSlash < getCrescentCooldown(p)) {
                sab(p, "§a🌙 Cooldown: " + (getCrescentCooldown(p)/1000.0 - (now - d.lastSlash)/1000.0) + "s");
                return;
            }
            spawnEmeraldCrescent(p);
            d.lastSlash = now;
            return;
        }

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (now - d.lastUlt < 12000) {
                sab(p, "§6🌕 Cooldown: " + (12 - (now - d.lastUlt)/1000) + "s");
                return;
            }
            performGoldenMoonPinch(p);
            d.lastUlt = now;
        }
    }

    // ==========================================
    // ⚡ SKILL 1: THUNDER STEP (SMOOTH TELEPORT)
    // ==========================================
    private void performThunderStepDash(Player p) {
        World w = p.getWorld();
        Location start = p.getLocation();
        int tier = getArmorTier(p);
        Vector dir = start.getDirection().setY(0).normalize();
        double dist = 1.8 + tier * 0.6;
        Location target = start.clone().add(dir.multiply(dist));

        w.playSound(start, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 2.0f);
        
        // Animasi "After-image" (jejak bayangan)
        for (double i = 0; i < dist; i += 0.3) {
            Location step = start.clone().add(dir.clone().normalize().multiply(i));
            w.spawnParticle(Particle.DUST, step.add(0, 1, 0), 3, new Particle.DustOptions(DASH_PRIMARY, 1.2f));
            if (tier == 2) w.spawnParticle(Particle.FLASH, step, 1);
        }

        p.teleport(target);
        w.playSound(target, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.0f);

        // Landing shockwave halus
        new BukkitRunnable() {
            int step = 0;
            public void run() {
                if (step > 5) { cancel(); return; }
                drawCircle(target.clone().add(0, 0.1, 0), 0.5 + (step * 0.4), DASH_SECONDARY, w);
                step++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        checkHit(p, target, 2.5, 4.0, true);
    }

    // ==========================================
    // 🌙 SKILL 2: EMERALD CRESCENT (SMOOTH PROJECTILE)
    // ==========================================
    private void spawnEmeraldCrescent(Player p) {
        World w = p.getWorld();
        Location origin = p.getEyeLocation();
        Vector direction = origin.getDirection().normalize();
        int tier = getArmorTier(p);
        
        new BukkitRunnable() {
            double distanceTravelled = 0;
            final double maxDist = 16 + (tier * 3);
            final double speed = 0.9 + (tier * 0.1);

            public void run() {
                if (distanceTravelled > maxDist) { cancel(); return; }

                // Smooth interpolation untuk posisi proyektil
                Location current = origin.clone().add(direction.clone().multiply(distanceTravelled));
                
                // Animasi rotasi halus proyektil
                double rotation = distanceTravelled * 25; 
                drawSmoothCrescent(current, direction, rotation, tier, w);
                
                // Trail partikel daun yang jatuh (nature feel)
                w.spawnParticle(Particle.DUST, current, 2, new Particle.DustOptions(CRESCENT_ACCENT, 0.8f));
                if (r.nextBoolean()) {
                    Vector leafMotion = new Vector((r.nextDouble()-0.5)*0.2, -0.1, (r.nextDouble()-0.5)*0.2);
                    w.spawnParticle(Particle.DUST, current, 0, leafMotion.getX(), leafMotion.getY(), leafMotion.getZ(), new Particle.DustOptions(CRESCENT_SECONDARY, 0.7f));
                }

                // Hit detection tiap tick agar tidak "tembus" musuh
                for (Entity en : w.getNearbyEntities(current, 1.5, 1.5, 1.5)) {
                    if (en instanceof LivingEntity && !en.equals(p)) {
                        ((LivingEntity) en).damage(5.0 + (tier * 1.5), p);
                        w.playSound(en.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
                        if (tier < 2) { cancel(); return; } // Pierce hanya untuk tier 2
                    }
                }
                distanceTravelled += speed;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🌕 SKILL 3: GOLDEN MOON (SMOOTH CINEMATIC)
    // ==========================================
    private void performGoldenMoonPinch(Player p) {
        World w = p.getWorld();
        Location loc = p.getLocation();
        int tier = getArmorTier(p);

        // Fase 1: Slow Rise (Pemain melayang halus)
        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 30, 1, false, false));
        w.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);

        new BukkitRunnable() {
            int frame = 0;
            public void run() {
                if (frame > 40) { // Durasi ancang-ancang
                    executeSlam(p, tier);
                    cancel();
                    return;
                }

                // Animasi Arena Ring yang berputar halus
                double radius = 5.0 + (tier * 1.0);
                for (int i = 0; i < 3; i++) {
                    double angle = Math.toRadians((frame * 10) + (i * 120));
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    w.spawnParticle(Particle.DUST, loc.clone().add(x, 0.2, z), 2, new Particle.DustOptions(ULT_PRIMARY, 1.5f));
                }

                // Moon Convergence (Bulan-bulan kecil mendekat ke tengah)
                if (frame > 20) {
                    double progress = (frame - 20) / 20.0;
                    double moonDist = radius * (1.0 - progress);
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.toRadians(i * 90);
                        Location moonPos = loc.clone().add(Math.cos(angle)*moonDist, 3, Math.sin(angle)*moonDist);
                        w.spawnParticle(Particle.DUST, moonPos, 5, new Particle.DustOptions(ULT_ACCENT, 2.0f));
                    }
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void executeSlam(Player p, int tier) {
        World w = p.getWorld();
        Location loc = p.getLocation();
        
        p.removePotionEffect(PotionEffectType.LEVITATION);
        p.setVelocity(new Vector(0, -2.5, 0)); // Slam down cepat

        new BukkitRunnable() {
            public void run() {
                if (p.isOnGround()) {
                    w.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                    w.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_BREAK, 1.5f, 0.5f);
                    
                    // Visual Ledakan Halus (Expanding Ring)
                    for (int r = 1; r <= 5 + tier; r++) {
                        final double finalR = r;
                        new BukkitRunnable() {
                            public void run() {
                                drawCircle(p.getLocation(), finalR, ULT_PRIMARY, w);
                                if (tier == 2) drawCircle(p.getLocation(), finalR + 0.5, ULT_SECONDARY, w);
                            }
                        }.runTaskLater(plugin, r);
                    }
                    
                    checkHit(p, p.getLocation(), 6.0, 15.0 + (tier * 5), false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    // ==========================================
    // 🎨 DRAWING UTILS (SMOOTHING)
    // ==========================================
    
    private void drawSmoothCrescent(Location loc, Vector dir, double rotation, int tier, World w) {
        Vector side = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        for (double angle = -1.5; angle <= 1.5; angle += 0.2) {
            double x = Math.cos(angle + Math.toRadians(rotation)) * 1.2;
            double y = Math.sin(angle + Math.toRadians(rotation)) * 1.2;
            Location pLoc = loc.clone().add(side.clone().multiply(x)).add(0, y, 0);
            w.spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(CRESCENT_PRIMARY, 1.3f));
        }
    }

    private void drawCircle(Location loc, double radius, Color color, World w) {
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            w.spawnParticle(Particle.DUST, loc.clone().add(Math.cos(angle)*radius, 0, Math.sin(angle)*radius), 1, new Particle.DustOptions(color, 1.2f));
        }
    }

    private void checkHit(Player p, Location loc, double radius, double damage, boolean launch) {
        for (Entity en : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                LivingEntity le = (LivingEntity) en;
                le.damage(damage, p);
                if (launch) le.setVelocity(new Vector(0, 0.5, 0));
            }
        }
    }

    private void applyMoonMark(LivingEntity target) {
        moonMarked.put(target.getUniqueId(), System.currentTimeMillis() + 6500);
        new BukkitRunnable() {
            int time = 0;
            public void run() {
                if (time > 130 || !target.isValid() || !moonMarked.containsKey(target.getUniqueId())) {
                    moonMarked.remove(target.getUniqueId());
                    cancel(); return;
                }
                target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 2.5, 0), 2, new Particle.DustOptions(ULT_PRIMARY, 1.5f));
                time += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (isWearingPiece(p, EquipmentSlot.CHEST, GoldenMoon.ARMOR_CHEST_KEY) && e.getEntity() instanceof LivingEntity) {
            applyMoonMark((LivingEntity) e.getEntity());
        }
    }

    private int getArmorTier(Player p) {
        if (plugin.getArmorManager().hasFullEliteSet(p)) return 2;
        if (plugin.getArmorManager().hasCrescentSet(p)) return 1;
        return 0;
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private boolean isWearingPiece(Player p, EquipmentSlot slot, org.bukkit.NamespacedKey key) {
        ItemStack item = p.getInventory().getArmorContents()[slot == EquipmentSlot.HEAD ? 3 : (slot == EquipmentSlot.CHEST ? 2 : (slot == EquipmentSlot.LEGS ? 1 : 0))];
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private void sab(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    private long getDashCooldown(Player p) { return getArmorTier(p) == 2 ? 1000 : 1500; }
    private long getCrescentCooldown(Player p) { return getArmorTier(p) == 2 ? 400 : 600; }

    private PlayerData get(Player p) { return data.computeIfAbsent(p.getUniqueId(), k -> new PlayerData()); }
    private static class PlayerData { long lastSlash = 0, lastDash = 0, lastUlt = 0; }
}
