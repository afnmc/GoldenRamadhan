package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * ============================================================================
 * GOLDEN MOON - ULTIMATE SKILL LISTENER (v2.5 Full Visuals)
 * 🚀 Fitur: Golden Domain, Anime Blink, Maju Mundur (Explosive Dash)
 * 🛠 Fix: API Compatibility for Spigot/Paper 1.21+
 * ============================================================================
 */
public class SkillListener implements Listener {
    
    // --- CONFIGURATION: DOMAIN ---
    private static final double DOMAIN_MIN_RANGE = 4.0;
    private static final double DOMAIN_MAX_RANGE = 12.0;
    private static final int DOMAIN_ROTATION_SPEED = 3;
    private static final double SWORD_SIZE = 5.0;
    
    // --- CONFIGURATION: SKILLS ---
    private static final int BLINK_MAX_TARGETS = 4;
    private static final double MM_FORWARD_POWER = 3.8;

    private final GoldenMoon plugin;
    private final Map<UUID, Integer> chargeStack = new HashMap<>();
    private final Map<UUID, Long> clickHoldStart = new HashMap<>();
    private final Set<UUID> immunityFrame = Collections.synchronizedSet(new HashSet<>());

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    // --- [ EVENT: FALL PROTECTION ] ---
    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && immunityFrame.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
            e.getEntity().setFallDistance(0);
        }
    }

    // --- [ EVENT: COMBAT & STACKING ] ---
    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHoldingSword(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        int stack = chargeStack.getOrDefault(id, 0);

        // Logic Stacking
        if (stack < 5) {
            stack++;
            chargeStack.put(id, stack);
            sendActionBar(p, "§e§l✦ Golden Stack: §f" + stack + "§7/§f5");
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.5f + (stack * 0.1f));
            
            if (stack == 5) {
                p.sendTitle("§f§l⚡", "§eTAHAN KLIK KANAN UNTUK DOMAIN", 5, 40, 10);
                p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0, 1, 0), 1);
            }
        }

        // Skill 2: Maju Mundur (Sneak + Stack 3)
        if (p.isSneaking() && stack >= 3 && stack < 5) {
            executeMajuMundur(p);
            chargeStack.put(id, 0);
            return;
        }

        // Skill 1: Anime Blink (Sneak + Stack 1-2)
        if (p.isSneaking() && stack < 3) {
            executeAnimeBlink(p, target);
        }
    }

    // --- [ EVENT: ULTIMATE CHARGING ] ---
    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingSword(p) || chargeStack.getOrDefault(p.getUniqueId(), 0) < 5) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (clickHoldStart.containsKey(p.getUniqueId())) return;
            
            clickHoldStart.put(p.getUniqueId(), System.currentTimeMillis());
            runChargingTask(p);
        }
    }

    private void runChargingTask(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID id = p.getUniqueId();
                if (!p.isOnline() || !clickHoldStart.containsKey(id) || !isHoldingSword(p)) {
                    this.cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - clickHoldStart.get(id);
                int progress = (int) Math.min((elapsed / 1500.0) * 100, 100); // 1.5 detik full

                // Visual Suction Particles
                double radius = 4.0 * (1.0 - (progress / 100.0));
                for (int i = 0; i < 3; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location partLoc = p.getLocation().add(Math.cos(angle) * radius, 0.5 + Math.random(), Math.sin(angle) * radius);
                    p.getWorld().spawnParticle(Particle.DUST, partLoc, 1, new Particle.DustOptions(Color.AQUA, 1.2f));
                }

                sendActionBar(p, "§f§lDOMINATING: §b" + progress + "% " + "§8[" + "§b" + "|".repeat(progress/10) + "§8" + ".".repeat(10 - progress/10) + "§8]");

                if (progress >= 100 || !p.isHandRaised()) { // Jika dilepas atau penuh
                    executeGoldenDomain(p, progress);
                    clickHoldStart.remove(id);
                    chargeStack.put(id, 0);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // --- [ CORE: GOLDEN DOMAIN ] ---
    private void executeGoldenDomain(Player p, int progress) {
        if (progress < 40) {
            sendActionBar(p, "§cGagal! Charge tidak cukup.");
            return;
        }

        Location center = p.getLocation().clone();
        double range = DOMAIN_MIN_RANGE + ((DOMAIN_MAX_RANGE - DOMAIN_MIN_RANGE) * (progress / 100.0));
        
        p.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2f, 1f);
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 40) { // Durasi ancang-ancang arena
                    triggerFinalImpact(p, center, range, progress);
                    this.cancel();
                    return;
                }

                // Gambar Hexagon Berputar
                double rotation = ticks * DOMAIN_ROTATION_SPEED;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    Location corner = center.clone().add(Math.cos(angle) * range, 0.2, Math.sin(angle) * range);
                    p.getWorld().spawnParticle(Particle.DUST, corner, 5, new Particle.DustOptions(Color.YELLOW, 2.0f));
                    
                    // Hubungkan garis antar sudut
                    double nextAngle = Math.toRadians((i + 1) * 60 + rotation);
                    Location nextCorner = center.clone().add(Math.cos(nextAngle) * range, 0.2, Math.sin(nextAngle) * range);
                    drawVisualLine(corner, nextCorner, Color.WHITE);
                }

                // Freeze musuh di sekitar
                center.getWorld().getNearbyEntities(center, range, range, range).forEach(en -> {
                    if (en instanceof LivingEntity le && !en.equals(p)) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 255, false, false));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 10, 0, false, false));
                    }
                });
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void triggerFinalImpact(Player p, Location center, double range, int progress) {
        // Step 1: Player mundur bergaya
        Vector back = p.getLocation().getDirection().multiply(-1).setY(0.5);
        p.setVelocity(back);
        immunityFrame.add(p.getUniqueId());

        // Step 2: Animasi Pedang Jatuh
        new BukkitRunnable() {
            int frame = 0;
            @Override
            public void run() {
                double y = 20 - (frame * 2);
                Location bladeLoc = center.clone().add(0, y, 0);
                
                // Gambar bentuk pedang sederhana pakai partikel
                for(double i=0; i<SWORD_SIZE; i+=0.5) {
                    center.getWorld().spawnParticle(Particle.DUST, bladeLoc.clone().add(0, i, 0), 5, new Particle.DustOptions(Color.WHITE, 2f));
                }

                if (y <= 0) {
                    // Step 3: LEDAKAN AKHIR
                    center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 3);
                    center.getWorld().spawnParticle(Particle.FLASH, center, 10, 2, 0, 2, 0);
                    center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.5f);
                    
                    double damage = 15.0 + (progress * 0.2);
                    center.getWorld().getNearbyEntities(center, range, range, range).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(p)) {
                            le.damage(damage, p);
                            le.setVelocity(new Vector(0, 1.2, 0));
                        }
                    });
                    
                    new BukkitRunnable() { @Override public void run() { immunityFrame.remove(p.getUniqueId()); } }.runTaskLater(plugin, 40L);
                    this.cancel();
                }
                frame++;
            }
        }.runTaskTimer(plugin, 5, 1);
    }

    // --- [ SKILL: ANIME BLINK ] ---
    private void executeAnimeBlink(Player p, LivingEntity target) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        target.getNearbyEntities(6, 3, 6).stream()
            .filter(en -> en instanceof LivingEntity && !en.equals(p) && targets.size() < BLINK_MAX_TARGETS)
            .forEach(en -> targets.add((LivingEntity) en));

        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0));
        
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i >= targets.size()) {
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                    this.cancel();
                    return;
                }
                LivingEntity curr = targets.get(i);
                p.teleport(curr.getLocation().add(curr.getLocation().getDirection().multiply(-1)));
                curr.damage(7.0, p);
                curr.getWorld().spawnParticle(Particle.SWEEP_ATTACK, curr.getLocation().add(0, 1, 0), 1);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
                i++;
            }
        }.runTaskTimer(plugin, 0, 3);
    }

    // --- [ SKILL: MAJU MUNDUR ] ---
    private void executeMajuMundur(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        p.setVelocity(dir.clone().multiply(-1.5).setY(0.3)); // Mundur dulu
        
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setVelocity(dir.multiply(MM_FORWARD_POWER)); // Terjang maju
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 2f);
                p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 2);
            }
        }.runTaskLater(plugin, 7L);
    }

    // --- [ HELPERS ] ---
    private void drawVisualLine(Location start, Location end, Color color) {
        Vector vector = end.toVector().subtract(start.toVector());
        double length = start.distance(end);
        vector.normalize().multiply(0.5);
        for (double i = 0; i < length; i += 0.5) {
            start.getWorld().spawnParticle(Particle.DUST, start.clone().add(vector.clone().multiply(i)), 1, new Particle.DustOptions(color, 1f));
        }
    }

    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}

