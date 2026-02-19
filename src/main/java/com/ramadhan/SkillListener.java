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
    private final Map<UUID, UUID> activeDomainOwner = new HashMap<>();
    private final Set<UUID> playersWithBuffs = new HashSet<>();

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
        // FIX: Gunakan Color.fromRGB, bukan new Color
        Color particleColor = stack >= 5 ? Color.WHITE : Color.fromRGB(255, 200, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(particleColor, 1.5f);

        for (double t = -1.5; t <= 1.5; t += 0.4) { 
            double taper = Math.cos(t / 2.0);
            double rx = (Math.cos(t) * taper * Math.cos(rot)) - (Math.sin(t) * Math.sin(rot));
            double ry = (Math.cos(t) * taper * Math.sin(rot)) + (Math.sin(t) * Math.cos(rot));
            
            Location pLoc = loc.clone().add(right.clone().multiply(rx * radius)).add(0, ry * radius, 0);
            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, dustOptions);
        }
    }

    private void drawSideStack(Player p, int stack) {
        Location loc = p.getLocation().add(0, 0.8, 0);
        // FIX: Color.fromRGB
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 200, 0), 1.2f);
        for (int i = 0; i < stack; i++) {
            double side = (i % 2 == 0) ? 0.7 : -0.7;
            Vector v = p.getLocation().getDirection().getCrossProduct(new Vector(0,1,0)).normalize().multiply(side);
            p.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v), 1, 0, 0, 0, 0, dustOptions);
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
            p.sendTitle("", "§f§l● LUNAR READY ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
            startGroundDomain(p);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            stopGroundDomain(p);
            executeLunarUlti(p);
        }
    }

    private void startGroundDomain(Player activator) {
        UUID activatorId = activator.getUniqueId();
        Location center = activator.getLocation();
        if (domainRunnables.containsKey(activatorId)) stopGroundDomain(activator);
        
        activeDomainOwner.put(activatorId, activatorId);
        applyOwnerBuffs(activator);
        playersWithBuffs.add(activatorId);

        BukkitRunnable domainTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!activator.isOnline() || comboStack.getOrDefault(activatorId, 0) < 5 || ticks > 100) {
                    this.cancel();
                    cleanupDomain(activatorId);
                    return;
                }
                drawDomainEffects(center, activatorId, ticks);
                applyDebuffsToEnemies(center, activator, 15.0);
                if (ticks % 20 == 0) applyOwnerBuffs(activator);
                ticks += 1;
            }
        };
        domainTask.runTaskTimer(plugin, 0L, 1L);
        domainRunnables.put(activatorId, domainTask);
    }

    private void drawDomainEffects(Location center, UUID ownerId, int ticks) {
        double radius = 15.0;
        // FIX: Color.fromRGB
        Particle.DustOptions domainDust = new Particle.DustOptions(Color.fromRGB(255, 220, 50), 2.0f);
        for (double i = 0; i < Math.PI * 2; i += 0.15) {
            double x = Math.cos(i) * radius;
            double z = Math.sin(i) * radius;
            center.getWorld().spawnParticle(Particle.DUST, center.clone().add(x, 0.05, z), 1, 0, 0, 0, 0, domainDust);
        }
        drawLargeCrescent(center, ticks);
    }

    private void drawLargeCrescent(Location loc, int ticks) {
        Color yellow = Color.fromRGB(255, 200, 0);
        double pulse = 1.0 + Math.sin(ticks * 0.15) * 0.1;
        for (double t = -1.5; t <= 1.5; t += 0.08) {
            double taper = Math.cos(t / 2.0);
            double x1 = Math.cos(t) * taper * 7.0 * pulse;
            double z1 = Math.sin(t) * 7.0 * pulse;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x1 - 3.0, 0.05, z1), 1, 0, 0, 0, 0, new Particle.DustOptions(yellow, 3.0f));
        }
    }

    private void applyOwnerBuffs(Player owner) {
        // FIX: Nama PotionEffectType di 1.21.1
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 0, false, false));
        owner.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 25, 0, false, false));
        owner.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 25, 0, false, false));
        owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 25, 0, false, false));
    }

    private void applyDebuffsToEnemies(Location center, Player owner, double radius) {
        for (Entity entity : owner.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Player enemy && enemy != owner) {
                // FIX: SLOWNESS, bukan SLOW
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 25, 0, false, false));
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 25, 0, false, false));
                // FIX: SMOKE, bukan SMOKE_NORMAL
                enemy.spawnParticle(Particle.SMOKE, enemy.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    private void cleanupDomain(UUID playerId) {
        domainRunnables.remove(playerId);
        activeDomainOwner.remove(playerId);
        Player owner = Bukkit.getPlayer(playerId);
        if (owner != null) {
            owner.removePotionEffect(PotionEffectType.SPEED);
            owner.removePotionEffect(PotionEffectType.STRENGTH);
            owner.removePotionEffect(PotionEffectType.ABSORPTION);
            owner.removePotionEffect(PotionEffectType.REGENERATION);
        }
    }

    private void stopGroundDomain(Player p) {
        BukkitRunnable task = domainRunnables.remove(p.getUniqueId());
        if (task != null) task.cancel();
        cleanupDomain(p.getUniqueId());
    }

    private void executeLunarUlti(Player p) {
        Location center = p.getLocation();
        p.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.2f);
        
        for (Entity en : p.getNearbyEntities(15, 15, 15)) {
            if (en instanceof LivingEntity le && en != p) {
                le.getWorld().spawnParticle(Particle.SONIC_BOOM, le.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                // FIX: EXPLOSION, bukan EXPLOSION_LARGE
                le.getWorld().spawnParticle(Particle.EXPLOSION, le.getLocation(), 1, 0.5, 0.5, 0.5, 0.1);
                le.damage(22, p);
                le.setVelocity(new Vector(0, 1.2, 0));
            }
        }
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
