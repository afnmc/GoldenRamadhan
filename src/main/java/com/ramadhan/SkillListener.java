package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {
    
    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color MOON_WHITE = Color.fromRGB(255, 250, 240);
    private static final Color CRESCENT_SILVER = Color.fromRGB(200, 200, 220);
    
    private final GoldenMoon plugin;
    private final ArmorManager armorManager;
    private final Map<UUID, LunarPlayerData> playerData = new HashMap<>();
    
    private static final int MAX_COMBO = 3;
    private static final long COMBO_WINDOW_MS = 1200;
    private static final int MAX_LUNAR_GAUGE = 100;
    private static final int GAUGE_PER_HIT = 15;
    private static final int GAUGE_PER_PARRY = 20;
    private static final long PARRY_WINDOW_MS = 300;
    
    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        this.armorManager = plugin.getArmorManager();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isLunarBlade(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;
        
        if (isBlockingWithShield(p)) {
            e.setCancelled(true);
            return;
        }
        
        LunarPlayerData data = getData(p);
        data.addGauge(GAUGE_PER_HIT);
        sendGaugeUpdate(p, data);
        
        long now = System.currentTimeMillis();
        if (now - data.lastHitTime < COMBO_WINDOW_MS) {
            data.combo++;
            if (data.combo > MAX_COMBO) data.combo = MAX_COMBO;
            executeComboSkill(p, target, data.combo);
        } else {
            data.combo = 1;
            executeBasicStrike(p, target);
        }
        data.lastHitTime = now;
        
        if (data.combo >= 2) {
            triggerScreenShake(p, 0.05, 2);
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!hasLunarShield(p)) return;
        
        LunarPlayerData data = getData(p);
        
        if (data.lastBlockStart > 0 && 
            System.currentTimeMillis() - data.lastBlockStart <= PARRY_WINDOW_MS) {
            e.setCancelled(true);
            data.addGauge(GAUGE_PER_PARRY);
            sendGaugeUpdate(p, data);
            executePerfectParry(p, e.getCause());
            data.lastBlockStart = 0;
            data.isBlocking = false;
            return;
        }
        
        if (data.isBlocking) {
            e.setDamage(e.getDamage() * 0.2);
            spawnShieldBarrier(p.getLocation(), CRESCENT_SILVER, 8);
        }
    }
    
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        
        boolean hasShield = hasLunarShield(p);
        
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            LunarPlayerData data = getData(p);
            
            if (hasShield) {
                data.isBlocking = true;
                data.lastBlockStart = System.currentTimeMillis();
                startBlockEffect(p);
            } else if (data.lunarGauge >= MAX_LUNAR_GAUGE && !data.isCharging) {
                data.isCharging = true;
                data.chargeTicks = 0;
                startChargeEffect(p);
            } else if (data.isCharging) {
                data.isCharging = false;
                if (data.chargeTicks >= 20) {
                    executeLunarEclipse(p);
                    data.chargeTicks = 0;
                    data.lunarGauge = 0;
                    sendGaugeUpdate(p, data);
                }
            } else {
                sendActionBar(p, "§7✦ §eCharge Lunar Gauge: §f" + data.lunarGauge + "§7/§f" + MAX_LUNAR_GAUGE);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
            }
        }
        
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (hasShield && getData(p).isBlocking) {
                e.setCancelled(true);
                executeShieldBash(p);
            }
        }
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        
        LunarPlayerData data = getData(p);
        
        if (data.isCharging) {
            data.chargeTicks++;
            updateChargeVisual(p, data.chargeTicks);
            if (data.chargeTicks > 100) {
                data.isCharging = false;
                data.chargeTicks = 0;
                sendActionBar(p, "§c✦ Charge cancelled");
            }
        }
        
        if (armorManager.tryMoonStep(p)) {
            data.moonStepReady = false;
            new BukkitRunnable() {
                @Override
                public void run() {
                    data.moonStepReady = true;
                }
            }.runTaskLater(plugin, 60);
        }
    }
    
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isLunarBlade(p)) return;
        if (!e.isSneaking()) return;
        
        LunarPlayerData data = getData(p);
        if (data.isBlocking) {
            data.isBlocking = false;
            data.lastBlockStart = 0;
        }
    }
    
    private void executeBasicStrike(Player p, LivingEntity target) {
        target.damage(2.5, p);
        target.setVelocity(p.getLocation().getDirection().multiply(0.2).setY(0.1));
        spawnLunarSpark(target.getLocation().add(0, 1, 0), MOON_WHITE, 4);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.8f);
    }
    
    private void executeComboSkill(Player p, LivingEntity target, int combo) {
        final Player finalP = p;
        final LivingEntity finalTarget = target;
        final World world = p.getWorld();
        
        if (combo == 2) {
            world.playSound(p.getLocation(), Sound.BLOCK_CHISELED_DEEPSLATE_BREAK, 0.5f, 1.4f);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    spawnCrescentTrail(p.getLocation(), p.getLocation().getDirection(), CRESCENT_SILVER, 10);
                    
                    finalTarget.damage(4.0, finalP);
                    finalTarget.setVelocity(new Vector(0, 0.8, 0));
                    finalTarget.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 0, false, false));
                    
                    Location impactLoc = finalTarget.getLocation().add(0, 0.5, 0);
                    world.spawnParticle(Particle.DUST, impactLoc, 15, new Particle.DustOptions(CRESCENT_SILVER, 1.5f));
                    world.spawnParticle(Particle.FLAME, impactLoc, 6, 0.2, 0.2, 0.2, 0.05);
                    
                    spawnCrescentCrater(impactLoc, world, 25);
                    
                    if (armorManager.hasCrescentChestplate(finalP)) {
                        finalP.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 0, false, false));
                        spawnLunarSpark(finalP.getLocation().add(0, 1.5, 0), GOLD, 8);
                    }
                }
            }.runTaskLater(plugin, 6L);
            
        } else if (combo == 3) {
            world.playSound(p.getLocation(), Sound.BLOCK_CHIME_HIT, 0.6f, 1.6f);
            
            Vector dir = p.getLocation().getDirection().setY(0).normalize();
            
            new BukkitRunnable() {
                int dashTicks = 0;
                final Vector dashVel = dir.clone().multiply(2.0);
                
                @Override
                public void run() {
                    if (dashTicks >= 12) {
                        executeStarfallSlash(finalP, dir);
                        this.cancel();
                        return;
                    }
                    
                    finalP.setVelocity(dashVel.clone().setY(0.05));
                    spawnStarTrail(finalP.getLocation(), CRESCENT_SILVER, 3);
                    
                    world.getNearbyEntities(finalP.getLocation(), 1.2, 1.0, 1.2).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(finalP) && !le.hasMetadata("starHit")) {
                            le.setMetadata("starHit", new FixedMetadataValue(plugin, true));
                            le.damage(1.5, finalP);
                            le.setVelocity(dir.clone().multiply(0.5).setY(0.2));
                            spawnLunarSpark(le.getLocation().add(0, 1, 0), MOON_WHITE, 4);
                        }
                    });
                    
                    dashTicks++;
                }
            }.runTaskTimer(plugin, 0, 1);
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    world.getEntities().forEach(en -> en.removeMetadata("starHit", plugin));
                }
            }.runTaskLater(plugin, 50L);
        }
        
        getData(p).combo = 0;
    }
    
    private void executeStarfallSlash(Player p, Vector direction) {
        World world = p.getWorld();
        Location slashLoc = p.getLocation().add(0, 1, 0);
        
        for(double angle = -70; angle <= 70; angle += 10) {
            double rad = Math.toRadians(angle);
            Vector offset = new Vector(Math.cos(rad) * 3.0, 0, Math.sin(rad) * 3.0);
            Location particleLoc = slashLoc.clone().add(offset.rotateAroundY(Math.toRadians(90)));
            world.spawnParticle(Particle.DUST, particleLoc, 2, new Particle.DustOptions(MOON_WHITE, 1.8f));
            world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0);
        }
        
        world.playSound(slashLoc, Sound.BLOCK_CHIME_HIT, 0.8f, 1.5f);
        world.playSound(slashLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 2.0f);
        
        world.getNearbyEntities(slashLoc, 4.0, 2.5, 4.0).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                Vector toEnemy = le.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize();
                double dot = toEnemy.dot(direction);
                if (dot > 0.2) {
                    le.damage(2.0, p);
                    le.setVelocity(direction.clone().multiply(0.8).setY(0.4));
                    spawnLunarSpark(le.getLocation().add(0, 1, 0), CRESCENT_SILVER, 6);
                    if (armorManager.hasCrescentLeggings(p)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 1, false, false));
                    }
                }
            }
        });
        
        triggerScreenShake(p, 0.08, 4);
    }

    private void startBlockEffect(Player p) {
        sendActionBar(p, "§b✦ §fBlocking... §7(Release on hit to Parry!)");
        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_PLACE, 0.3f, 1.2f);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                LunarPlayerData data = getData(p);
                if(!data.isBlocking) { this.cancel(); return; }
                spawnShieldBarrier(p.getLocation(), CRESCENT_SILVER, 4);
            }
        }.runTaskTimer(plugin, 0, 3);
    }
    
    private void executePerfectParry(Player p, EntityDamageEvent.DamageCause cause) {
        World world = p.getWorld();
        Location parryLoc = p.getLocation().add(0, 1, 0);
        
        world.spawnParticle(Particle.FLASH, parryLoc, 1);
        world.spawnParticle(Particle.DUST, parryLoc, 25, new Particle.DustOptions(GOLD, 2.5f));
        for(double angle = 0; angle < 360; angle += 20) {
            double rad = Math.toRadians(angle);
            Vector spread = new Vector(Math.cos(rad) * 1.5, 0.3, Math.sin(rad) * 1.5);
            world.spawnParticle(Particle.DUST, parryLoc.clone().add(spread), 1, 
                new Particle.DustOptions(MOON_WHITE, 1.5f));
        }
        
        world.playSound(parryLoc, Sound.BLOCK_CHIME_HIT, 1.0f, 1.8f);
        world.playSound(parryLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 2.2f);
        
        p.setInvulnerable(true);
        sendActionBar(p, "§a✦ §fPERFECT PARRY! §7(Counter window open)");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setInvulnerable(false);
                getData(p).parryBonus = true;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        getData(p).parryBonus = false;
                    }
                }.runTaskLater(plugin, 40L);
            }
        }.runTaskLater(plugin, 10L);
    }
    
    private void executeShieldBash(Player p) {
        World world = p.getWorld();
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        
        world.spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 10, 0.5, 0.3, 0.5, 0.1);
        world.spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 15, 
            new Particle.DustOptions(CRESCENT_SILVER, 2.0f));
        world.playSound(p.getLocation(), Sound.BLOCK_ANVIL_HIT, 0.6f, 1.0f);
        
        world.getNearbyEntities(p.getLocation(), 2.5, 2.0, 2.5).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                le.damage(1.0, p);
                le.setVelocity(dir.clone().multiply(1.2).setY(0.3));
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 3, false, false));
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15, 0, false, false));
            }
        });
        
        getData(p).addGauge(10);
        sendGaugeUpdate(p, getData(p));
    }
    
    private void spawnShieldBarrier(Location loc, Color color, int count) {
        World world = loc.getWorld();
        for(double angle = 0; angle < 360; angle += 30) {
            double rad = Math.toRadians(angle);
            Vector offset = new Vector(Math.cos(rad) * 1.2, 0.8, Math.sin(rad) * 1.2);
            Location barrierLoc = loc.clone().add(offset);
            world.spawnParticle(Particle.DUST, barrierLoc, 1, new Particle.DustOptions(color, 1.5f));
        }
        world.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.8, 0), count / 2, 0.3, 0.2, 0.3, 0.05);
    }

    private void executeLunarEclipse(Player p) {
        final Player finalP = p;
        final World world = p.getWorld();
        final Location center = p.getLocation().clone();
        
        boolean isElite = armorManager.hasFullEliteSet(p);
        String title = isElite ? "§f§l🌑" : "§f§l🌕";
        String subtitle = isElite ? "§7§lLUNAR ECLIPSE" : "§6§lGOLDEN MOON BLESSING";
        
        p.playSound(center, Sound.BLOCK_CHIME_HIT, 1.2f, 1.0f);
        p.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.9f);
        p.sendTitle(title, subtitle, 5, 25, 10);
        
        p.setVelocity(new Vector(0, 0.4, 0));
        p.setInvulnerable(true);
        
        new BukkitRunnable() {
            int phase = 0;
            @Override
            public void run() {
                if (phase == 0) {
                    spawnBlessingArena(center, world, isElite ? Color.fromRGB(100, 100, 150) : GOLD);
                    world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.7f);
                } else if (phase == 1) {
                    spawnRisingCrescent(center, world);
                    world.playSound(center, Sound.BLOCK_CHIME_HIT, 1.0f, 1.2f);
                } else if (phase == 2) {
                    world.spawnParticle(Particle.DUST, center.clone().add(0, 2, 0), 40, 
                        3, 1.5, 3, 0.08, new Particle.DustOptions(MOON_WHITE, 2.5f));
                    world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.9f, 0.8f);
                } else if (phase == 3) {
                    executeBlessingImpact(finalP, center, world, isElite);
                } else {
                    p.setInvulnerable(false);
                    this.cancel();
                    return;
                }
                phase++;
            }
        }.runTaskTimer(plugin, 0, 30);
    }
    
    private void executeBlessingImpact(Player p, Location center, World world, boolean isElite) {
        double radius = isElite ? 14.0 : 10.0;
        int duration = isElite ? 180 : 120;
        
        for(Player viewer : center.getWorld().getPlayers()) {
            if(viewer.getLocation().distance(center) < radius + 5) {
                viewer.sendTitle("§f§l✦", "§b§lBlessing Descends", 3, 12, 5);
            }
        }
        
        for(int i = 0; i < (isElite ? 8 : 5); i++) {
            final int delay = i * 5;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location pillarLoc = center.clone().add(
                        (Math.random() - 0.5) * radius, 0, (Math.random() - 0.5) * radius
                    );
                    spawnLightPillar(pillarLoc, world, isElite ? CRESCENT_SILVER : GOLD);
                    world.playSound(pillarLoc, Sound.BLOCK_CHIME_HIT, 0.6f, 1.3f);
                }
            }.runTaskLater(plugin, delay);
        }
        
        world.spawnParticle(Particle.DUST, center, 60, radius/2, 1, radius/2, 0.15, 
            new Particle.DustOptions(MOON_WHITE, 2.0f));
        world.spawnParticle(Particle.FLAME, center, 30, radius/3, 0.8, radius/3, 0.1);
        world.spawnParticle(Particle.CLOUD, center, 25, radius/2, 1.5, radius/2, 0.2);
        
        world.playSound(center, Sound.BLOCK_CHIME_HIT, 1.5f, 0.9f);
        world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.2f, 0.8f);
        world.playSound(center, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        
        world.getNearbyEntities(center, radius, radius, radius).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(p)) {
                double dist = le.getLocation().distance(center);
                le.damage(isElite ? 6.0 : 4.0, p);
                le.setVelocity(new Vector(0, 1.2, 0));
                le.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 0, false, false));
                le.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, false, false));
                le.getWorld().spawnParticle(Particle.DUST, le.getLocation().add(0, 1, 0), 12, 
                    new Particle.DustOptions(MOON_WHITE, 1.5f));
                world.playSound(le.getLocation(), Sound.BLOCK_CHIME_HIT, 0.5f, 1.4f);
            }
        });
        
        spawnBlessingZone(center, world, p, duration, isElite);
        
        p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, isElite ? 1 : 0, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 150, 1, false, false));
    }
    
    private void spawnLightPillar(Location loc, World world, Color color) {
        new BukkitRunnable() {
            int height = 0;
            @Override
            public void run() {
                if(height >= 15) { this.cancel(); return; }
                world.spawnParticle(Particle.DUST, loc.clone().add(0, height, 0), 3, 
                    0.2, 0.1, 0.2, 0, new Particle.DustOptions(color, 1.8f));
                world.spawnParticle(Particle.FLAME, loc.clone().add(0, height, 0), 1, 0.1, 0.05, 0.1, 0);
                height++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnBlessingZone(Location center, World world, Player caster, int durationTicks, boolean isElite) {
        Color zoneColor = isElite ? CRESCENT_SILVER : GOLD;
        double zoneRadius = isElite ? 14.0 : 10.0;
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if(ticks >= durationTicks) { 
                    world.spawnParticle(Particle.DUST, center, 25, 3, 1, 3, 0,
                        new Particle.DustOptions(zoneColor, 1.8f));
                    this.cancel(); 
                    return; 
                }
                
                for(int i = 0; i < 8; i++) {
                    Location particleLoc = center.clone().add(
                        (Math.random() - 0.5) * zoneRadius,
                        0.5 + Math.random() * 3,
                        (Math.random() - 0.5) * zoneRadius
                    );
                    world.spawnParticle(Particle.DUST, particleLoc, 1,
                        new Particle.DustOptions(zoneColor, 1.2f));
                    world.spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0);
                }
                
                if(ticks % 20 == 0) {
                    world.getNearbyEntities(center, zoneRadius, zoneRadius, zoneRadius).forEach(en -> {
                        if(en instanceof LivingEntity le && !en.equals(caster)) {
                            if(le.getHealth() < le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
                                le.setHealth(Math.min(le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), 
                                    le.getHealth() + 0.5));
                                spawnLunarSpark(le.getLocation().add(0, 1.5, 0), MOON_WHITE, 3);
                            }
                        }
                    });
                }
                
                if(ticks % 5 == 0) {
                    for(double angle = 0; angle < 360; angle += 15) {
                        double rad = Math.toRadians(angle);
                        Location edge = center.clone().add(
                            Math.cos(rad) * zoneRadius, 0.02, Math.sin(rad) * zoneRadius
                        );
                        world.spawnParticle(Particle.DUST, edge, 1, 
                            new Particle.DustOptions(zoneColor, 1.5f));
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 4);
    }

    private void spawnLunarSpark(Location loc, Color color, int count) {
        World world = loc.getWorld();
        for(int i = 0; i < count; i++) {
            Vector spread = new Vector(
                (Math.random() - 0.5) * 0.3,
                Math.random() * 0.4,
                (Math.random() - 0.5) * 0.3
            );
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, 
                new Particle.DustOptions(color, 1.5f));
        }
    }
    
    private void spawnCrescentTrail(Location from, Vector direction, Color color, int density) {
        World world = from.getWorld();
        for(int i = 0; i < density; i++) {
            double progress = (double) i / density;
            Vector offset = direction.clone().multiply(progress * 2.0);
            double arc = Math.sin(progress * Math.PI) * 0.6;
            offset.rotateAroundY(Math.toRadians(90 + arc * 25));
            Location particleLoc = from.clone().add(0, 1, 0).add(offset);
            world.spawnParticle(Particle.DUST, particleLoc, 1, 
                new Particle.DustOptions(color, 1.8f));
        }
    }
    
    private void spawnStarTrail(Location loc, Color color, int count) {
        World world = loc.getWorld();
        for(int i = 0; i < count; i++) {
            Vector spread = new Vector(
                (Math.random() - 0.5) * 0.5,
                Math.random() * 0.6,
                (Math.random() - 0.5) * 0.5
            );
            world.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, 
                new Particle.DustOptions(color, 1.6f));
            world.spawnParticle(Particle.FLAME, loc.clone().add(spread), 1, 0.05, 0.05, 0.05, 0);
        }
    }
    
    private void spawnCrescentCrater(Location center, World world, int durationTicks) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= durationTicks) { this.cancel(); return; }
                double radius = 2.5 + Math.sin(ticks * 0.25) * 0.4;
                for(double angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    Location edge = center.clone().add(Math.cos(rad) * radius, 0.02, Math.sin(rad) * radius);
                    world.spawnParticle(Particle.DUST, edge, 1, new Particle.DustOptions(CRESCENT_SILVER, 1.3f));
                }
                if(ticks % 10 == 0) {
                    world.getNearbyEntities(center, 3.0, 2.0, 3.0).forEach(en -> {
                        if(en instanceof LivingEntity le && !le.hasPotionEffect(PotionEffectType.SLOW)) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20, 0, false, false));
                        }
                    });
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnBlessingArena(Location center, World world, Color color) {
        for(int corner = 0; corner < 6; corner++) {
            double baseAngle = corner * 60;
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if(ticks >= 50) { this.cancel(); return; }
                    double rotation = ticks * 2;
                    double angle = Math.toRadians(baseAngle + rotation);
                    Location cornerLoc = center.clone().add(
                        Math.cos(angle) * 10, 0.3, Math.sin(angle) * 10
                    );
                    world.spawnParticle(Particle.DUST, cornerLoc, 2, 
                        new Particle.DustOptions(color, 1.8f));
                    world.spawnParticle(Particle.FLAME, cornerLoc, 1, 0.1, 0.1, 0.1, 0);
                    ticks++;
                }
            }.runTaskTimer(plugin, corner * 3, 1);
        }
    }
    
    private void spawnRisingCrescent(Location center, World world) {
        new BukkitRunnable() {
            int frame = 0;
            @Override
            public void run() {
                if(frame >= 30) { this.cancel(); return; }
                double y = frame * 0.6;
                Location crescentCenter = center.clone().add(0, y, 0);
                for(double angle = -45; angle <= 45; angle += 8) {
                    double rad = Math.toRadians(angle);
                    double radius = 2.0 + Math.sin(frame * 0.2) * 0.5;
                    Vector offset = new Vector(Math.cos(rad) * radius, 0, Math.sin(rad) * radius * 0.3);
                    world.spawnParticle(Particle.DUST, crescentCenter.clone().add(offset), 2,
                        new Particle.DustOptions(GOLD, 2.0f));
                }
                world.spawnParticle(Particle.DUST, crescentCenter, 6, 0.4, 0.8, 0.4, 0,
                    new Particle.DustOptions(MOON_WHITE, 2.5f));
                frame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void triggerScreenShake(Player p, double intensity, int ticks) {
        if(intensity <= 0) return;
        Location original = p.getLocation().clone();
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if(count >= ticks) {
                    p.teleport(original);
                    this.cancel();
                    return;
                }
                double shakeX = (Math.random() - 0.5) * intensity;
                double shakeZ = (Math.random() - 0.5) * intensity;
                Location shaken = original.clone().add(shakeX, 0, shakeZ);
                p.teleport(shaken);
                count++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void startChargeEffect(Player p) {
        sendActionBar(p, "§e§l✦ CHARGING BLESSING... §7[§f▮▮▮▮▮§7]");
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.0f);
        
        new BukkitRunnable() {
            int chargeFrame = 0;
            @Override
            public void run() {
                LunarPlayerData data = getData(p);
                if(!data.isCharging) { this.cancel(); return; }
                
                double radius = 1.3 + Math.sin(chargeFrame * 0.35) * 0.6;
                for(double angle = 0; angle < 360; angle += 25) {
                    double rad = Math.toRadians(angle);
                    Location auraLoc = p.getLocation().add(
                        Math.cos(rad) * radius, 0.6, Math.sin(rad) * radius
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 1,
                        new Particle.DustOptions(GOLD, 1.8f));
                }
                
                int bars = Math.min(5, data.chargeTicks / 20);
                String bar = "§7[§f" + "▮".repeat(bars) + "§7" + "▯".repeat(5 - bars) + "]";
                sendActionBar(p, "§e§l✦ CHARGING... §7" + bar + " §f" + (data.chargeTicks / 20) + "s");
                
                chargeFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    
    private void updateChargeVisual(Player p, int chargeTicks) {
        if(chargeTicks % 12 == 0) {
            float pitch = 0.9f + (chargeTicks * 0.015f);
            p.playSound(p.getLocation(), Sound.BLOCK_CHIME_HIT, 0.25f, Math.min(pitch, 2.0f));
        }
        if(chargeTicks >= 40) {
            p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 4,
                new Particle.DustOptions(GOLD, 2.0f));
        }
    }
    
    private void sendGaugeUpdate(Player p, LunarPlayerData data) {
        int bars = (int) Math.ceil(data.lunarGauge / 20.0);
        String bar = "§7[§f" + "▮".repeat(bars) + "§7" + "▯".repeat(5 - bars) + "]";
        sendActionBar(p, "§b✦ Lunar Gauge: §7" + bar + " §f" + data.lunarGauge + "%");
    }

    private boolean hasLunarShield(Player p) {
        ItemStack offhand = p.getInventory().getItemInOffHand();
        return offhand != null && offhand.hasItemMeta() && 
               offhand.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE);
    }
    
    private boolean isBlockingWithShield(Player p) {
        return hasLunarShield(p) && getData(p).isBlocking;
    }
    
    private LunarPlayerData getData(Player p) {
        return playerData.computeIfAbsent(p.getUniqueId(), k -> new LunarPlayerData());
    }
    
    private boolean isLunarBlade(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
    
    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }
    
    private static class LunarPlayerData {
        int combo = 0;
        long lastHitTime = 0;
        int lunarGauge = 0;
        boolean isCharging = false;
        int chargeTicks = 0;
        boolean isBlocking = false;
        long lastBlockStart = 0;
        boolean parryBonus = false;
        boolean moonStepReady = true;
        
        void addGauge(int amount) {
            lunarGauge = Math.min(MAX_LUNAR_GAUGE, lunarGauge + amount);
        }
    }
                }
