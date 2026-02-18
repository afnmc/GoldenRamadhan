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
            double rot = 0;
            @Override
            public void run() {
                rot += 0.25;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isHolding(p)) continue;
                    int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
                    drawBackMoon(p, rot, stack);
                    if (stack > 0 && stack < 5) drawSideStack(p, stack);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawBackMoon(Player p, double rot, int stack) {
        Location loc = p.getLocation().add(0, 1.2, 0).add(p.getLocation().getDirection().setY(0).normalize().multiply(-0.5));
        Vector right = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize();
        Color col = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);

        for (double t = -1.6; t <= 1.6; t += 0.1) {
            double taper = Math.cos(t / 2.0);
            double rx = (Math.cos(t) * taper * Math.cos(rot)) - (Math.sin(t) * Math.sin(rot));
            double ry = (Math.cos(t) * taper * Math.sin(rot)) + (Math.sin(t) * Math.cos(rot));
            Location pLoc = loc.clone().add(right.clone().multiply(rx)).add(0, ry, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(col, 0.7f));
        }
    }

    private void drawSideStack(Player p, int stack) {
        Location loc = p.getLocation().add(0, 0.8, 0);
        for (int i = 0; i < stack; i++) {
            double side = (i % 2 == 0) ? 0.7 : -0.7;
            Vector v = p.getLocation().getDirection().getCrossProduct(new Vector(0,1,0)).normalize().multiply(side);
            p.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(v), 0, 0, 0, 0, 0);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        p.setVelocity(p.getLocation().getDirection().multiply(0.2).setY(0.1));
        drawEpicSlash(target.getLocation());
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§l● LUNAR READY ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        }

        // TP KILL: Balik lagi dengan visual nusuk atas-bawah
        if (target.getHealth() <= e.getFinalDamage()) {
            p.teleport(target.getLocation());
            executeTPKillEffect(target.getLocation());
        }
    }

    private void executeTPKillEffect(Location loc) {
        new BukkitRunnable() {
            double y = 6.0;
            @Override
            public void run() {
                // Partikel nusuk kuning dari atas
                loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 5, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.YELLOW, 1.5f));
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 1, 0, 0, 0, 0);
                
                y -= 0.5;
                if (y <= 0) {
                    // Ledakan putih di bawah
                    loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
                    loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 10, 0.5, 0.5, 0.5, 0.1);
                    loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawEpicSlash(Location loc) {
        for (double i = -1.3; i <= 1.3; i += 0.3) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(i, 1.2, 0), 1, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1f));
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            executeLunarUlti(p);
        } else {
            long now = System.currentTimeMillis();
            if (recallCooldown.getOrDefault(p.getUniqueId(), 0L) > now) return;
            recallCooldown.put(p.getUniqueId(), now + 10000); 
            drawSpiralRecall(p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0));
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
        }
    }

    private void executeLunarUlti(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.2f);
        
        for (Entity en : p.getNearbyEntities(7, 7, 7)) {
            if (en instanceof LivingEntity le && en != p) {
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        double angle = t * 0.6;
                        Location circle = le.getLocation().add(Math.cos(angle)*1.5, 0.5, Math.sin(angle)*1.5);
                        le.getWorld().spawnParticle(Particle.DUST, circle, 5, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1.2f));
                        
                        if (t >= 10) {
                            // Ledakan Sabit Putih (Ulti)
                            le.getWorld().spawnParticle(Particle.SONIC_BOOM, le.getLocation(), 1, 0, 0, 0, 0);
                            le.getWorld().spawnParticle(Particle.FLASH, le.getLocation(), 2, 0.2, 0.2, 0.2, 0);
                            le.damage(17, p);
                            le.setVelocity(new Vector(0, 0.6, 0));
                            this.cancel();
                        }
                        t++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    private void drawSpiralRecall(Player p) {
        new BukkitRunnable() {
            double y = 0;
            @Override
            public void run() {
                for (int i = 0; i < 8; i++) {
                    double angle = (y * 5) + (i * Math.PI / 4);
                    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(Math.cos(angle)*1.2, y, Math.sin(angle)*1.2), 1, 0, 0, 0, 0);
                }
                y += 0.3;
                if (y > 3) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        // Cek data lunar, bukan cek nama
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
