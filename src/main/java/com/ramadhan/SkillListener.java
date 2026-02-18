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
                    
                    // Sabit Punggung: Jadi Putih Terang kalau Stack 5
                    drawBackMoon(p, rot, stack);
                    
                    // Partikel samping badan pas nyicil stack
                    if (stack > 0 && stack < 5) drawSideStack(p, stack);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawBackMoon(Player p, double rot, int stack) {
        Location loc = p.getLocation().add(0, 1.2, 0).add(p.getLocation().getDirection().setY(0).normalize().multiply(-0.5));
        Vector right = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize();
        
        // Stack 5 = Putih, <5 = Kuning (Build Lama)
        Color col = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);

        for (double t = -1.6; t <= 1.6; t += 0.1) {
            double taper = Math.cos(t / 2.0);
            double rx = (Math.cos(t) * taper * Math.cos(rot)) - (Math.sin(t) * Math.sin(rot));
            double ry = (Math.cos(t) * taper * Math.sin(rot)) + (Math.sin(t) * Math.cos(rot));
            Location pLoc = loc.clone().add(right.clone().multiply(rx)).add(0, ry, 0);
            
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(col, 0.8f));
            if (stack >= 5) p.getWorld().spawnParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0, 0);
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

        // Dash Attack
        p.setVelocity(p.getLocation().getDirection().multiply(0.25).setY(0.1));

        // Air Attack
        if (!p.isOnGround()) {
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 5, 0, 0, 0, 0);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        }

        drawEpicSlash(target.getLocation());
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§l● LUNAR READY ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        }
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
                // Efek Serbu Sabit Melingkar ke Mob
                new BukkitRunnable() {
                    int t = 0;
                    @Override
                    public void run() {
                        double angle = t * 0.6;
                        // Sabit model tidur melingkar
                        Location circle = le.getLocation().add(Math.cos(angle)*1.5, 0.5, Math.sin(angle)*1.5);
                        le.getWorld().spawnParticle(Particle.DUST, circle, 5, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1.2f));
                        le.getWorld().spawnParticle(Particle.END_ROD, circle, 1, 0, 0, 0, 0);
                        
                        if (t >= 10) { // Menyatukan (Nusuk) dan Meledak
                            le.getWorld().spawnParticle(Particle.SONIC_BOOM, le.getLocation(), 1, 0, 0, 0, 0);
                            le.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, le.getLocation(), 1, 0, 0, 0, 0);
                            le.damage(16, p);
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
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
