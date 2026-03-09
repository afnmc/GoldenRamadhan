package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {

    private final GoldenMoon plugin;
    private final Map<UUID, Integer> chargeStack = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> ultimateActive = new HashSet<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    // --- UTILS: Cooldown & Pengecekan ---
    private boolean checkCD(Player p, String skill, int sec) {
        long last = cooldowns.getOrDefault(p.getUniqueId() + skill, 0L);
        if (System.currentTimeMillis() - last < (sec * 1000L)) {
            p.sendMessage("§c§l[!] §7Skill " + skill + " sedang cooldown!");
            return false;
        }
        cooldowns.put(p.getUniqueId() + skill, System.currentTimeMillis());
        return true;
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }

    // --- EVENT: COMBAT (Blink & Dash) ---
    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHoldingSword(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        int stack = chargeStack.getOrDefault(p.getUniqueId(), 0);

        // SKILL 1: BLINK (Lompat + Hit)
        if (!p.isOnGround() && stack < 3) {
            if (checkCD(p, "Blink", 3)) executeBlink(p, target);
        }
        // SKILL 2: DASH (Sneak + Hit, Stack 3-4)
        else if (p.isSneaking() && stack >= 3 && stack < 5) {
            if (checkCD(p, "Dash", 5)) {
                executeDash(p);
                chargeStack.put(p.getUniqueId(), 0);
            }
        }
        // STACKING
        else if (stack < 5) {
            stack++;
            chargeStack.put(p.getUniqueId(), stack);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§e§l✦ Stack: " + stack + " / 5"));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingSword(p) || chargeStack.getOrDefault(p.getUniqueId(), 0) < 5) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (checkCD(p, "Domain", 30)) {
                chargeStack.put(p.getUniqueId(), 0);
                executeDomain(p);
            }
        }
    }

    // --- SKILL 1: BLINK ---
    private void executeBlink(Player p, LivingEntity target) {
        target.setVelocity(new Vector(0, 1.2, 0));
        new BukkitRunnable() {
            public void run() {
                p.teleport(target.getLocation().subtract(target.getLocation().getDirection().multiply(1.5)));
                p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation(), 10);
                target.damage(15.0, p);
                target.setVelocity(new Vector(0, -2.5, 0));
            }
        }.runTaskLater(plugin, 3L);
    }

    // --- SKILL 2: DASH ---
    private void executeDash(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        p.setVelocity(dir.multiply(5));
        p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, p.getLocation(), 5);
        p.getNearbyEntities(5, 3, 5).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) le.damage(25.0, p);
        });
    }

    // --- SKILL 3: DOMAIN & PEDANG KHUSUS ---
    private void executeDomain(Player p) {
        Location center = p.getLocation();
        new BukkitRunnable() {
            double r = 0; int ticks = 0;
            public void run() {
                if (r < 12) r += 0.5;
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60 + ticks * 10);
                    Location pos = center.clone().add(Math.cos(a)*r, 0.2, Math.sin(a)*r);
                    p.getWorld().spawnParticle(Particle.DUST, pos, 2, new Particle.DustOptions(Color.YELLOW, 2.0f));
                }
                if (ticks++ > 60) { dropSword(center, p); this.cancel(); }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void dropSword(Location center, Player p) {
        new BukkitRunnable() {
            int frame = 0;
            public void run() {
                double y = 25 - (frame * 2.5);
                Location b = center.clone().add(0, y, 0);

                // 1. BILAH UTAMA (Runcing)
                for(double h=0; h<12; h+=0.2) {
                    double w = (12 - h) * 0.06;
                    for(double x = -w; x <= w; x += 0.2) {
                        center.getWorld().spawnParticle(Particle.DUST, b.clone().add(x, h, 0), 1, new Particle.DustOptions(Color.SILVER, 1.0f));
                    }
                }
                // 2. CROSSGUARD MELENGKUNG (Sesuai Gambar)
                for(double x = -3; x <= 3; x += 0.2) {
                    double curve = Math.sin(Math.abs(x) * 1.5) * 1.5;
                    center.getWorld().spawnParticle(Particle.DUST, b.clone().add(x, 12 + curve, 0), 1, new Particle.DustOptions(Color.YELLOW, 1.5f));
                }
                // 3. GAGANG
                for(double h=12; h<16; h+=0.2) {
                    center.getWorld().spawnParticle(Particle.DUST, b.clone().add(0, h, 0), 1, new Particle.DustOptions(Color.fromRGB(200, 150, 50), 1.0f));
                }

                if (y <= 0) {
                    center.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, center, 1);
                    center.getNearbyEntities(12, 5, 12).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(p)) le.damage(60.0, p);
                    });
                    this.cancel();
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}

