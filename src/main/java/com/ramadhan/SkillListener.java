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
                    
                    // Sabit Punggung (Visual Dasar)
                    drawBackMoon(p, stack);
                    // Combo Visual (Samping Badan)
                    if (stack > 0) drawComboSide(p, stack);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawBackMoon(Player p, int stack) {
        Location loc = p.getLocation().add(0, 1.2, 0).subtract(p.getLocation().getDirection().setY(0).normalize().multiply(0.4));
        Vector right = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize();
        Color col = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);

        for (double t = -1.2; t <= 1.2; t += 0.2) {
            Location pLoc = loc.clone().add(right.clone().multiply(t * 0.8)).add(0, Math.cos(t) * 0.5, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(col, 0.6f));
        }
    }

    private void drawComboSide(Player p, int stack) {
        Location loc = p.getLocation().add(0, 0.8, 0);
        for (int i = 0; i < stack; i++) {
            double side = (i % 2 == 0) ? 0.7 : -0.7;
            Vector v = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize().multiply(side);
            p.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(v).add(0, (rand.nextDouble() - 0.5) * 0.5, 0), 0, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        // Dash & Air Attack Effect
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        p.setVelocity(dir.multiply(0.25).setY(0.1));
        
        if (!p.isOnGround()) {
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.01);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        }

        drawEpicSlash(target.getLocation());
        p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 1.8f);
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§lREADY", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 2f);
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

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            
            // ULTI: Lunar Pierce (Nusuk Depan)
            p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.2f);
            
            Vector dashDir = p.getLocation().getDirection().multiply(1.5);
            p.setVelocity(dashDir);

            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0,1,0), 1, 0,0,0,0);
                    for (Entity en : p.getNearbyEntities(3, 3, 3)) {
                        if (en instanceof LivingEntity le && en != p) {
                            Vector toTarget = le.getLocation().toVector().subtract(p.getLocation().toVector());
                            if (p.getLocation().getDirection().dot(toTarget.normalize()) > 0.7) {
                                le.damage(15, p); // Damage lebih manusiawi
                                le.setVelocity(p.getLocation().getDirection().multiply(1.2).setY(0.4));
                            }
                        }
                    }
                    if (t++ > 3) this.cancel();
                }
            }.runTaskTimer(plugin, 0L, 1L);

        } else {
            // RECALL: Cooldown 10s
            long now = System.currentTimeMillis();
            if (recallCooldown.getOrDefault(p.getUniqueId(), 0L) > now) return;
            
            recallCooldown.put(p.getUniqueId(), now + 10000);
            drawRecallVisual(p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 2));
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
        }
    }

    private void drawRecallVisual(Player p) {
        new BukkitRunnable() {
            double y = 0;
            @Override
            public void run() {
                for (int i = 0; i < 8; i++) {
                    double angle = (y * 5) + (i * Math.PI / 4);
                    Location l = p.getLocation().add(Math.cos(angle) * 1.2, y, Math.sin(angle) * 1.2);
                    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, l, 1, 0,0,0,0);
                }
                y += 0.3;
                if (y > 2.5) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
