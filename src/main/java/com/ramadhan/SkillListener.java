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

public class SkillListener implements Listener {
    
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> chargeStack = new HashMap<>();
    private final Set<UUID> immunityFrame = Collections.synchronizedSet(new HashSet<>());

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    // --- PROTEKSI FALL DAMAGE (BIAR GAK MATI PAS COMBO) ---
    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && immunityFrame.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
            e.getEntity().setFallDistance(0);
        }
    }

    // --- LOGIC COMBAT & SKILL ---
    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHoldingSword(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        int stack = chargeStack.getOrDefault(id, 0);

        // SYSTEM STACKING (MAX 5)
        if (stack < 5) {
            stack++;
            chargeStack.put(id, stack);
            sendActionBar(p, "§e§l✦ Golden Stack: §f" + stack + "§7/§f5");
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.2f + (stack * 0.2f));
            if (stack == 5) p.sendTitle("§f§l⚡", "§eKLIK KANAN: ULTIMATE", 5, 30, 5);
        }

        // SKILL 1: BLINK LOMPAT (SNEAK + HIT) - Syarat Stack < 3
        if (p.isSneaking() && stack < 3) {
            executeBlinkCombo(p, target);
            return;
        }

        // SKILL 2: MAJU MUNDUR (SNEAK + HIT) - Syarat Stack 3-4
        if (p.isSneaking() && stack >= 3 && stack < 5) {
            executeMajuMundur(p);
            chargeStack.put(id, 0);
        }
    }

    // --- TRIGGER ULTIMATE (KLIK KANAN INSTAN) ---
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingSword(p)) return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            int stack = chargeStack.getOrDefault(p.getUniqueId(), 0);
            if (stack >= 5) {
                e.setCancelled(true);
                chargeStack.put(p.getUniqueId(), 0);
                executeGoldenDomain(p);
            }
        }
    }

    // ==========================================
    // SKILL 1: BLINK (LOMPAT & TEBAS UDARA)
    // ==========================================
    private void executeBlinkCombo(Player p, LivingEntity target) {
        Location startLoc = p.getLocation().clone().add(0, 1, 0); // Trail dari dada
        target.setVelocity(new Vector(0, 1.2, 0)); // Terbangkan musuh
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Location behind = target.getLocation().clone().subtract(target.getLocation().getDirection().multiply(1.5));
                behind.setY(target.getLocation().getY() + 0.8);
                
                drawTrail(startLoc, behind.clone().add(0, 1, 0), Color.WHITE);
                p.teleport(behind);
                
                // Visual Tebasan Udara
                p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
                p.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
                target.damage(9.0, p);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.8f);
                
                // Hempaskan Musuh ke Bawah
                target.setVelocity(new Vector(0, -2.0, 0));
                immunityFrame.add(p.getUniqueId());
                new BukkitRunnable() { @Override public void run() { immunityFrame.remove(p.getUniqueId()); } }.runTaskLater(plugin, 40L);
            }
        }.runTaskLater(plugin, 3L);
    }

    // ==========================================
    // SKILL 2: MAJU MUNDUR (SIDE SLASH AOE)
    // ==========================================
    private void executeMajuMundur(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        
        // FASE 1: MUNDUR + LEDAKAN SONIC
        p.setVelocity(dir.clone().multiply(-1.6).setY(0.3));
        Location bLoc = p.getLocation().subtract(dir.clone().multiply(1));
        p.getWorld().spawnParticle(Particle.EXPLOSION, bLoc, 5, 0.3, 0.3, 0.3, 0.1);
        p.getWorld().spawnParticle(Particle.CLOUD, bLoc, 20, 0.5, 0.5, 0.5, 0.2);
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);

        // FASE 2: MAJU TERJANG + NEBAS SEKITAR
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setVelocity(dir.clone().multiply(3.8));
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.2f);
                
                // Trail Efek Terjang
                for(int i=0; i<10; i++) {
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 5, new Particle.DustOptions(Color.AQUA, 1.5f));
                }

                // AoE Damage (Radius 4 block agar kena samping)
                p.getWorld().getNearbyEntities(p.getLocation(), 4.0, 3.0, 4.0).forEach(en -> {
                    if (en instanceof LivingEntity le && !en.equals(p)) {
                        le.damage(12.0, p);
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                    }
                });
            }
        }.runTaskLater(plugin, 8L);
    }

    // ==========================================
    // SKILL 3: ULTIMATE GOLDEN DOMAIN
    // ==========================================
    private void executeGoldenDomain(Player p) {
        Location center = p.getLocation().clone();
        p.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.5f, 1.2f);
        
        // ANIMASI ARENA HEXAGON
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 25) { 
                    triggerSwordDrop(p, center);
                    this.cancel();
                    return;
                }
                double rotation = ticks * 10;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    Location corner = center.clone().add(Math.cos(angle) * 10, 0.2, Math.sin(angle) * 10);
                    p.getWorld().spawnParticle(Particle.DUST, corner, 3, new Particle.DustOptions(Color.YELLOW, 1.5f));
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void triggerSwordDrop(Player p, Location center) {
        // Player TP mundur bergaya
        p.setVelocity(p.getLocation().getDirection().multiply(-1.8).setY(0.5));
        immunityFrame.add(p.getUniqueId());

        new BukkitRunnable() {
            int frame = 0;
            @Override
            public void run() {
                double y = 18 - (frame * 3);
                Location bladeLoc = center.clone().add(0, y, 0);
                
                // VISUAL PEDANG RAKSASA (FULL RAME)
                for(double h=0; h<8; h+=0.5) {
                    center.getWorld().spawnParticle(Particle.DUST, bladeLoc.clone().add(0, h, 0), 15, new Particle.DustOptions(Color.WHITE, 2.5f));
                    center.getWorld().spawnParticle(Particle.END_ROD, bladeLoc.clone().add(0, h, 0), 2, 0.1, 0.1, 0.1, 0);
                    center.getWorld().spawnParticle(Particle.DUST, bladeLoc.clone().add(0, h, 0), 5, new Particle.DustOptions(Color.YELLOW, 1.0f));
                }

                if (y <= 0) {
                    // IMPACT (LEDAKAN DAHSYAT + TANAH RETAK)
                    center.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 8);
                    center.getWorld().spawnParticle(Particle.FLASH, center, 10, 3, 1, 3, 0);
                    center.getWorld().spawnParticle(Particle.BLOCK, center, 100, 4, 0.5, 4, 0.1, Bukkit.createBlockData(Material.GOLD_BLOCK));
                    center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.5f);
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.7f);
                    
                    center.getWorld().getNearbyEntities(center, 10.0, 10.0, 10.0).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(p)) {
                            le.damage(35.0, p);
                            le.setVelocity(new Vector(0, 1.6, 0));
                        }
                    });
                    new BukkitRunnable() { @Override public void run() { immunityFrame.remove(p.getUniqueId()); } }.runTaskLater(plugin, 40L);
                    this.cancel();
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // --- HELPER UNTUK TRAIL BLINK ---
    private void drawTrail(Location from, Location to, Color color) {
        Vector vector = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        vector.normalize().multiply(0.4);
        for (double i = 0; i < dist; i += 0.4) {
            from.getWorld().spawnParticle(Particle.DUST, from.clone().add(vector.clone().multiply(i)), 2, new Particle.DustOptions(color, 1.5f));
        }
    }

    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}

