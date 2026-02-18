package com.ramadhan;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
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
        startPermanentAura();
    }

    private void startPermanentAura() {
        new BukkitRunnable() {
            double rotation = 0;
            @Override
            public void run() {
                rotation += 0.1;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isHoldingSpecial(p)) drawBackCrescent(p, rotation);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void drawBackCrescent(Player p, double rot) {
        Location loc = p.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        Vector rightDir = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
        Color particleColor = (stack >= 5) ? Color.WHITE : Color.fromRGB(255, 215, 0);

        Location center = loc.clone().add(0, 1.3, 0).add(dir.clone().multiply(-0.3));
        for (double t = -1.2; t <= 1.2; t += 0.1) {
            double tx = (Math.cos(t) * 1.2 * Math.cos(rot)) - (t * 1.1 * Math.sin(rot));
            double ty = (Math.cos(t) * 1.2 * Math.sin(rot)) + (t * 1.1 * Math.cos(rot));

            Location pLoc = center.clone().add(rightDir.clone().multiply(tx)).add(0, ty, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(particleColor, 0.8f));
            if (stack >= 5 && random.nextDouble() > 0.8) 
                p.getWorld().spawnParticle(Particle.END_ROD, pLoc, 1, 0, 0, 0, 0.01);
        }
    }

    @EventHandler
    public void onSwing(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (isHoldingSpecial(p) && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            playSlashAnimation(p);
        }
    }

    private void playSlashAnimation(Player p) {
        Location eyeLoc = p.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        boolean fromRight = random.nextBoolean();

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.4f);
        for (double i = -1.2; i <= 1.2; i += 0.1) {
            Location pLoc = eyeLoc.clone()
                    .add(dir.clone().multiply(1.5 + ((1.44 - (i * i)) * 0.4)))
                    .add(right.clone().multiply(i * 1.5))
                    .add(0, (fromRight ? i * 0.8 : -i * 0.8) - 0.5, 0);

            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 200), 0.7f));
            if (random.nextDouble() > 0.9) p.getWorld().spawnParticle(Particle.FIREWORK, pLoc, 1, 0, 0, 0, 0.02);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHoldingSpecial(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        int stack = comboStack.getOrDefault(id, 0);
        if (System.currentTimeMillis() - lastHit.getOrDefault(id, 0L) > 4000) stack = 0;
        
        stack = Math.min(stack + 1, 5);
        comboStack.put(id, stack);
        lastHit.put(id, System.currentTimeMillis());

        if (stack == 5) {
            p.sendTitle("", "§f§lMOONLIGHT READY", 0, 20, 10);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        }

        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
        if (target.getHealth() - e.getFinalDamage() <= 0) {
            drawVerticalStab(target.getLocation());
            p.teleport(target.getLocation().subtract(p.getLocation().getDirection().setY(0).normalize().multiply(1.2)));
            p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.6f, 1.8f);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingSpecial(p) || !e.isSneaking()) return;

        if (comboStack.getOrDefault(p.getUniqueId(), 0) >= 5) {
            executeMoonBurst(p);
            comboStack.put(p.getUniqueId(), 0);
        } else {
            executeRecallHeal(p);
        }
    }

    private void executeMoonBurst(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.5f);
        for (int i = 0; i < 4; i++) {
            double angle = i * (Math.PI / 2);
            new BukkitRunnable() {
                double d = 0;
                @Override
                public void run() {
                    d += 0.6;
                    Location loc = p.getLocation().add(Math.cos(angle) * d, 1, Math.sin(angle) * d);
                    p.getWorld().spawnParticle(Particle.CLOUD, loc, 3, 0.1, 0.1, 0.1, 0.02);
                    for (Entity en : p.getNearbyEntities(6, 6, 6)) {
                        if (en instanceof LivingEntity le && en != p && en.getLocation().distance(loc) < 1.5) {
                            le.damage(18, p);
                            le.setVelocity(en.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.2).setY(0.4));
                        }
                    }
                    if (d > 7) this.cancel();
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    private void executeRecallHeal(Player p) {
        new BukkitRunnable() {
            double h = 0; int t = 0;
            @Override
            public void run() {
                if (!p.isSneaking() || t > 30) {
                    if (t > 30) {
                        p.setHealth(Math.min(p.getHealth() + 4, p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
                        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                    }
                    this.cancel(); return;
                }
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(Math.cos(t * 0.5) * 1.2, h, Math.sin(t * 0.5) * 1.2), 1, new Particle.DustOptions(Color.YELLOW, 1f));
                h += 0.08; t++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private boolean isHoldingSpecial(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private void drawVerticalStab(Location loc) {
        for (double y = 0; y <= 3; y += 0.15) loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 3 - y, 0), 3, new Particle.DustOptions(Color.WHITE, 1.5f));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        ItemStack s = e.getEntity().getInventory().getItemInMainHand();
        if (isHoldingSpecial(e.getEntity())) { e.getDrops().remove(s); savedSwords.put(e.getEntity().getUniqueId(), s); }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (savedSwords.containsKey(e.getPlayer().getUniqueId())) e.getPlayer().getInventory().addItem(savedSwords.remove(e.getPlayer().getUniqueId()));
    }
}
