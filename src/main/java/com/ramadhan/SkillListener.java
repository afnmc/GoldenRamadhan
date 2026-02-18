package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> comboStack = new HashMap<>();
    private final Map<UUID, Long> recallCooldown = new HashMap<>();
    private final Random rand = new Random();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startTicking();
    }

    private void startTicking() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isHolding(p)) continue;
                    int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
                    
                    // Partikel Putih di Samping (Makin banyak stack makin rame)
                    drawSideParticles(p, stack);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawSideParticles(Player p, int stack) {
        if (stack <= 0) return;
        Location loc = p.getLocation().add(0, 1, 0);
        for (int i = 0; i < stack * 2; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double x = Math.cos(angle) * 0.6;
            double z = Math.sin(angle) * 0.6;
            // Bedrock Fix: Speed 0 biar gak melayang turun
            p.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(x, (rand.nextDouble() - 0.5) * 1.5, z), 0, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        // Dash Dikit pas mukul
        p.setVelocity(p.getLocation().getDirection().multiply(0.2).setY(0.1));

        drawEpicSlash(target.getLocation());
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§l● READY ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 2f);
        }

        if (target.getHealth() <= e.getFinalDamage()) {
            p.teleport(target.getLocation());
            p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1f, 2f);
            drawThunderFall(target.getLocation());
        }
    }

    private void drawEpicSlash(Location loc) {
        boolean side = rand.nextBoolean();
        for (double i = -1.0; i <= 1.0; i += 0.2) {
            double yOff = side ? i * 0.7 : -i * 0.7; 
            Location pLoc = loc.clone().add(i, yOff + 1.2, 0);
            loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1f));
        }
    }

    private void drawThunderFall(Location loc) {
        for (double y = 0; y <= 5; y += 0.5) {
            loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 1, 0, 0, 0, 0);
        }
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            // ULTI: Nusuk Ke Depan
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.2f);
            p.getWorld().spawnParticle(Particle.SONIC_BOOM, p.getLocation().add(p.getLocation().getDirection().multiply(2)), 1, 0, 0, 0, 0);
            
            for (Entity en : p.getNearbyEntities(5, 5, 5)) {
                if (en instanceof LivingEntity le && en != p) {
                    // Cek FOV biar gak bantai belakang badan
                    Vector toEntity = le.getLocation().toVector().subtract(p.getLocation().toVector());
                    if (p.getLocation().getDirection().dot(toEntity.normalize()) > 0.5) {
                        le.damage(15, p); // Damage dikurangi biar gak OP
                        le.setVelocity(p.getLocation().getDirection().multiply(1.5));
                    }
                }
            }
        } else {
            // RECALL: Dengan Cooldown
            long now = System.currentTimeMillis();
            if (recallCooldown.getOrDefault(p.getUniqueId(), 0L) > now) {
                p.sendMessage("§cRecall masih cooldown!");
                return;
            }
            recallCooldown.put(p.getUniqueId(), now + 10000); // 10 Detik
            
            drawCircleHeal(p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 2));
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1f, 2f);
        }
    }

    private void drawCircleHeal(Player p) {
        Location loc = p.getLocation();
        for (int i = 0; i < 360; i += 20) {
            double angle = Math.toRadians(i);
            double x = Math.cos(angle) * 1.5;
            double z = Math.sin(angle) * 1.5;
            p.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0);
        }
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
