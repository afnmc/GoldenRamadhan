package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    
    // Data Store buat Skill
    private final Map<UUID, Integer> chargeStack = new HashMap<>();
    private final Set<UUID> aeternaMarked = new HashSet<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startLunamSoulsTask();
    }

    @EventHandler
    public void onLunamCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID targetUUID = target.getUniqueId();

        // --- SKILL 1: IMPERATORIS LUNA (300% Damage Mark) ---
        if (aeternaMarked.contains(targetUUID)) {
            e.setDamage(e.getDamage() * 3.0); // Triple damage!
            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1.2, 0), 3, 0.1, 0.1, 0.1, 0.05);
        } else {
            // Kasih Mark & Visual Garis Energi
            aeternaMarked.add(targetUUID);
            drawLunamLink(p, target);
        }

        // --- SKILL 2: LUNAM BLADE (Charge & Slide Back) ---
        int stack = chargeStack.getOrDefault(p.getUniqueId(), 0) + 1;
        
        // Trigger: Pas hit ke-3 sambil Shift (Jongkok)
        if (p.isSneaking() && stack >= 3) {
            executeLunamSlide(p);
            chargeStack.put(p.getUniqueId(), 0); // Reset
        } else {
            chargeStack.put(p.getUniqueId(), stack);
        }
        
        // Visual Hit (Kuning-Putih)
        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
    }

    // --- SKILL 2 LOGIC: SLIDE BACK & SNAP ---
    private void executeLunamSlide(Player p) {
        Location startPos = p.getLocation().clone();
        Vector backward = p.getLocation().getDirection().setY(0).normalize().multiply(-1.5);
        
        p.setVelocity(backward);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.5f);

        new BukkitRunnable() {
            int timer = 0;
            @Override
            public void run() {
                if (timer > 10) {
                    // SNAP BACK: Kalau masih nahan Shift di akhir slide, balik ke awal
                    if (p.isSneaking()) {
                        p.teleport(startPos);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
                    }
                    this.cancel();
                    return;
                }
                
                // Visual Slide (Kuning-Putih)
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 5, new Particle.DustOptions(Color.YELLOW, 1.2f));
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 5, new Particle.DustOptions(Color.WHITE, 1.0f));
                timer++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // --- SKILL 3: LUNAM SWORDS STORM (Right Click) ---
    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.getAction().name().contains("RIGHT")) return;
        
        Location targetLoc = p.getTargetBlock(null, 10).getLocation();
        p.sendMessage("§b§lSWORD STORM!");

        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 15) { this.cancel(); return; }
                
                double x = (Math.random() * 6) - 3;
                double z = (Math.random() * 6) - 3;
                Location dropLoc = targetLoc.clone().add(x, 8, z);
                
                // Visual Pedang Jatuh (End Rod)
                dropLoc.getWorld().spawnParticle(Particle.END_ROD, dropLoc, 10, 0, -2, 0, 0.5);
                
                // Damage & Slow di titik jatuh
                for (Entity en : dropLoc.getWorld().getNearbyEntities(dropLoc.clone().subtract(0, 8, 0), 2, 2, 2)) {
                    if (en instanceof LivingEntity le && en != p) {
                        le.damage(5.0, p);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                    }
                }
                count++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // --- SKILL 4: LUNAM SOULS (Passive Auto-Attack) ---
    private void startLunamSoulsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isHolding(p)) continue;
                    
                    // Cari musuh terdekat buat ditembak "Soul"
                    p.getNearbyEntities(7, 7, 7).stream()
                        .filter(en -> en instanceof LivingEntity && en != p)
                        .findFirst().ifPresent(target -> {
                            p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p.getLocation().add(0, 1, 0), 5);
                            ((LivingEntity) target).damage(3.0, p);
                            p.playSound(p.getLocation(), Sound.ENTITY_WISP_AMBIENT, 0.5f, 2f);
                        });
                }
            }
        }.runTaskTimer(plugin, 0L, 60L); // Tiap 3 detik
    }

    private void drawLunamLink(Player p, Entity target) {
        Location start = p.getLocation().add(0, 1, 0);
        Location end = target.getLocation().add(0, 1, 0);
        Vector vec = end.toVector().subtract(start.toVector()).normalize().multiply(0.3);
        double dist = start.distance(end);
        
        for (double d = 0; d < dist; d += 0.3) {
            start.add(vec);
            p.getWorld().spawnParticle(Particle.DUST, start, 1, new Particle.DustOptions(Color.AQUA, 0.6f));
        }
    }

    private boolean isHolding(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
