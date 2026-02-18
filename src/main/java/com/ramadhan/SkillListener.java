package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final Random random = new Random();
    private final Map<UUID, Integer> comboStack = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();
    private final Map<UUID, ItemStack> savedSwords = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startAuraTask();
    }

    private void startAuraTask() {
        new BukkitRunnable() {
            double rot = 0;
            @Override
            public void run() {
                rot += 0.15;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHolding(p)) drawBackMoon(p, rot);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void drawBackMoon(Player p, double rot) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        Color col = (stack >= 5) ? Color.WHITE : Color.YELLOW;

        Location center = loc.clone().add(0, 1.3, 0).add(dir.multiply(-0.35));
        for (double t = -1.2; t <= 1.2; t += 0.15) { // Optimalisasi step biar gak lag
            double tx = (Math.cos(t) * 1.1 * Math.cos(rot)) - (t * 1.0 * Math.sin(rot));
            double ty = (Math.cos(t) * 1.1 * Math.sin(rot)) + (t * 1.0 * Math.cos(rot));
            Location pLoc = center.clone().add(right.clone().multiply(tx)).add(0, ty, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(col, 0.7f));
        }
    }

    @EventHandler
    public void onSwing(PlayerInteractEvent e) {
        if (!isHolding(e.getPlayer())) return;
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            playMoonSlash(e.getPlayer());
        }
    }

    private void playMoonSlash(Player p) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        boolean flip = random.nextBoolean();

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
        for (double i = -1.0; i <= 1.0; i += 0.1) {
            Location loc = eye.clone().add(dir.clone().multiply(1.2 + (1 - i*i)*0.4))
                .add(right.clone().multiply(i * 1.3))
                .add(0, (flip ? i : -i) * 0.7 - 0.4, 0);
            p.getWorld().spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(Color.fromRGB(255,255,200), 0.6f));
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        int stack = comboStack.getOrDefault(id, 0);
        if (System.currentTimeMillis() - lastHit.getOrDefault(id, 0L) > 4000) stack = 0;
        
        stack = Math.min(stack + 1, 5);
        comboStack.put(id, stack);
        lastHit.put(id, System.currentTimeMillis());

        if (stack == 5) p.sendTitle("", "§f§lMOONLIGHT READY", 0, 15, 5);
        
        if (target.getHealth() - e.getFinalDamage() <= 0) {
            p.teleport(target.getLocation().subtract(p.getLocation().getDirection().setY(0).normalize()));
            p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.5f, 2f);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        if (comboStack.getOrDefault(p.getUniqueId(), 0) >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            executeBurst(p);
        }
    }

    private void executeBurst(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.5f);
        for (int i = 0; i < 4; i++) {
            double angle = i * Math.PI / 2;
            new BukkitRunnable() {
                double d = 0;
                @Override
                public void run() {
                    d += 0.7;
                    Location loc = p.getLocation().add(Math.cos(angle)*d, 1, Math.sin(angle)*d);
                    p.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.02);
                    for (Entity en : p.getNearbyEntities(5, 5, 5)) {
                        if (en instanceof LivingEntity le && en != p && en.getLocation().distance(loc) < 1.5) {
                            le.damage(15, p);
                            le.setVelocity(en.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.2).setY(0.4));
                        }
                    }
                    if (d > 6) this.cancel();
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (isHolding(e.getEntity())) {
            ItemStack s = e.getEntity().getInventory().getItemInMainHand();
            e.getDrops().remove(s);
            savedSwords.put(e.getEntity().getUniqueId(), s);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (savedSwords.containsKey(e.getPlayer().getUniqueId())) {
            e.getPlayer().getInventory().addItem(savedSwords.remove(e.getPlayer().getUniqueId()));
        }
    }
}
