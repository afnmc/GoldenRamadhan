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
                    if (stack >= 5) drawGodMode(p, rot); 
                    
                    // Fitur Baru: Partikel putih samping badan pas stack ngumpul
                    if (stack > 0 && stack < 5) drawSideStack(p, stack);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawBackMoon(Player p, double rot, int stack) {
        Location loc = p.getLocation().add(0, 1.2, 0).add(p.getLocation().getDirection().setY(0).normalize().multiply(-0.5));
        Vector right = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize();
        Color col = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);

        for (double t = -1.6; t <= 1.6; t += 0.05) {
            double taper = Math.cos(t / 2.0);
            double rx = (Math.cos(t) * 1.0 * taper * Math.cos(rot)) - (Math.sin(t) * 1.0 * Math.sin(rot));
            double ry = (Math.cos(t) * 1.0 * taper * Math.sin(rot)) + (Math.sin(t) * 1.0 * Math.cos(rot));
            Location pLoc = loc.clone().add(right.clone().multiply(rx)).add(0, ry, 0);
            // Bedrock Fix: Speed 0 (angka terakhir)
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(col, 0.7f));
            if (stack >= 5) p.getWorld().spawnParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0, 0);
        }
    }

    private void drawSideStack(Player p, int stack) {
        Location loc = p.getLocation().add(0, 0.8, 0);
        for (int i = 0; i < stack; i++) {
            double side = (i % 2 == 0) ? 0.6 : -0.6;
            Vector v = new Vector(-p.getLocation().getDirection().getZ(), 0, p.getLocation().getDirection().getX()).normalize().multiply(side);
            p.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(v).add(0, (rand.nextDouble() - 0.5) * 0.5, 0), 0, 0, 0, 0, 0);
        }
    }

    private void drawGodMode(Player p, double rot) {
        Location loc = p.getLocation().add(0, 1, 0);
        for (int i = 0; i < 3; i++) {
            double offset = i * (Math.PI * 2 / 3);
            for (double a = 0; a < Math.PI * 2; a += 0.6) {
                double x = Math.cos(a + rot + offset) * 1.2;
                double z = Math.sin(a + rot + offset) * 1.2;
                double y = Math.sin(a * 2 + rot) * 0.7;
                p.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
                p.getWorld().spawnParticle(Particle.FIREWORK, loc.clone().add(x, y, z), 1, 0, 0, 0, 0);
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        // Dash Dikit pas mukul
        p.setVelocity(p.getLocation().getDirection().multiply(0.2).setY(0.1));

        // Air Attack (Bonus damage & sound pas di udara)
        if (!p.isOnGround()) {
            target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0,1,0), 5, 0.1, 0.1, 0.1, 0);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        }

        drawEpicSlash(target.getLocation());
        p.playSound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.5f, 1.8f);
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§lREADY", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
        }

        if (target.getHealth() <= e.getFinalDamage()) {
            p.teleport(target.getLocation());
            drawThunderFall(target.getLocation());
        }
    }

    private void drawEpicSlash(Location loc) {
        boolean side = rand.nextBoolean();
        for (double i = -1.3; i <= 1.3; i += 0.15) {
            double yOff = side ? i * 0.8 : -i * 0.8; 
            Location pLoc = loc.clone().add(i, yOff + 1.3, 0);
            loc.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1.2f));
        }
    }

    private void drawThunderFall(Location loc) {
        for (double y = 0; y <= 10; y += 0.5) {
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, y, 0), 1, 0, 0, 0, 0, new Particle.DustOptions(Color.YELLOW, 2f));
            loc.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, y, 0), 1, 0, 0, 0, 0);
        }
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
        loc.getWorld().spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            
            // Visual Arena Tetap Sama (Sesuai Permintaan)
            p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 1, 0, 0, 0, 0);
            p.getWorld().spawnParticle(Particle.SONIC_BOOM, p.getLocation().add(p.getLocation().getDirection().multiply(2)), 1, 0, 0, 0, 0);
            p.playSound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1f, 1f);
            
            for (Entity en : p.getNearbyEntities(6, 6, 6)) { // Range dikecilkan biar gak terlalu OP
                if (en instanceof LivingEntity le && en != p) {
                    le.damage(15, p); // Damage dikurangi biar gak one-hit player full armor
                    le.setVelocity(p.getLocation().getDirection().multiply(1.5).setY(0.4));
                }
            }
        } else {
            // Recall Delay 10 Detik
            long now = System.currentTimeMillis();
            if (recallCooldown.getOrDefault(p.getUniqueId(), 0L) > now) return;
            
            recallCooldown.put(p.getUniqueId(), now + 10000); // 10s CD
            drawSpiralRecall(p);
            p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 2));
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
        }
    }

    private void drawSpiralRecall(Player p) {
        new BukkitRunnable() {
            double y = 0;
            @Override
            public void run() {
                for (int i = 0; i < 8; i++) { // Partikel diperbanyak biar lebih bagus
                    double angle = (y * 5) + (i * Math.PI / 4);
                    double x = Math.cos(angle) * 1.2;
                    double z = Math.sin(angle) * 1.2;
                    p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(x, y, z), 1, 0, 0, 0, 0);
                }
                y += 0.2;
                if (y > 3) this.cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
