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
    private final Map<UUID, BukkitRunnable> domainRunnables = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startTicking();
    }

    private void startTicking() {
        new BukkitRunnable() {
            double rot = 0;
            @Override
            public void run() {
                rot += 0.20;
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
        Location loc = p.getLocation().add(0, 1.2, 0);
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        loc.add(dir.multiply(-0.4)); 
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        double radius = 0.6; 
        Color col = stack >= 5 ? Color.WHITE : Color.fromRGB(255, 215, 0);
        Particle.DustOptions dust = new Particle.DustOptions(col, 1.2f);

        for (double t = -1.5; t <= 1.5; t += 0.4) { 
            double taper = Math.cos(t / 2.0);
            double rx = (Math.cos(t) * taper * Math.cos(rot)) - (Math.sin(t) * Math.sin(rot));
            double ry = (Math.cos(t) * taper * Math.sin(rot)) + (Math.sin(t) * Math.cos(rot));
            Location pLoc = loc.clone().add(right.clone().multiply(rx * radius)).add(0, ry * radius, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, dust);
        }
    }

    private void drawSideStack(Player p, int stack) {
        Location loc = p.getLocation().add(0, 0.8, 0);
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 255, 200), 1.0f);
        for (int i = 0; i < stack; i++) {
            double side = (i % 2 == 0) ? 0.7 : -0.7;
            Vector v = p.getLocation().getDirection().getCrossProduct(new Vector(0,1,0)).normalize().multiply(side);
            p.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v), 1, 0, 0, 0, 0, dust);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        p.setVelocity(p.getLocation().getDirection().multiply(0.2).setY(0.1));
        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§l● LUNAR CHARGED ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        // LOGIC: Harus 5 stack DAN sedang melompat (tidak di tanah)
        if (stack >= 5 && !((Entity)p).isOnGround()) {
            comboStack.put(p.getUniqueId(), 0);
            startLunarDomain(p);
        }
    }

    private void startLunarDomain(Player p) {
        Location center = p.getLocation().clone();
        p.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1.2f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 160, 0));

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 100) { this.cancel(); return; }

                // 1. Lingkaran Arena (Kuning & Putih)
                drawCircle(center, 8.0, Color.fromRGB(255, 215, 0));
                drawCircle(center, 4.0, Color.WHITE);

                // 2. Efek Pedang Jatuh (Sesuai Video)
                if (ticks % 10 == 0) {
                    double angle = Math.random() * Math.PI * 2;
                    double dist = Math.random() * 7.0;
                    Location swordLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                    spawnLunarSword(swordLoc);
                    
                    // Damage musuh di arena
                    for (Entity en : swordLoc.getWorld().getNearbyEntities(swordLoc, 2, 2, 2)) {
                        if (en instanceof LivingEntity le && en != p) {
                            le.damage(4.0, p);
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        }
                    }
                }
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void drawCircle(Location loc, double radius, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.5f);
        for (double i = 0; i < Math.PI * 2; i += 0.4) {
            double x = Math.cos(i) * radius;
            double z = Math.sin(i) * radius;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0, dust);
        }
    }

    private void spawnLunarSword(Location loc) {
        new BukkitRunnable() {
            double y = 10.0;
            @Override
            public void run() {
                loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 5, 0.1, 0.5, 0.1, 0.01);
                y -= 1.0;
                if (y <= 0) {
                    loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1, 0, 0, 0, 0);
                    loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1f, 1.5f);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
