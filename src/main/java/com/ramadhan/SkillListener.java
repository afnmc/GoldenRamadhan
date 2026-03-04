package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> chargedAttackStack = new HashMap<>();
    private final Map<UUID, Location> originalPosition = new HashMap<>();
    private final Map<UUID, BukkitRunnable> soulTask = new HashMap<>();
    private final Set<UUID> markedEnemies = new HashSet<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        // IMPERATORIS LUNA - Mark enemies for bonus damage
        markEnemy(target);
        
        // LUNAM BLADE - Charged Attack System
        handleChargedAttack(p, target);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        // LUNAM BLADE - Slide back on 3rd charged attack
        int stack = chargedAttackStack.getOrDefault(p.getUniqueId(), 0);
        if (stack >= 3) {
            executeSlideBack(p);
            chargedAttackStack.put(p.getUniqueId(), 0);
        }
    }

    private void markEnemy(LivingEntity target) {
        markedEnemies.add(target.getUniqueId());
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 
            20, 0.5, 0.5, 0.5, 0.1, new Particle.DustOptions(Color.fromRGB(0, 150, 255), 1.0f));
        
        // Remove mark after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                markedEnemies.remove(target.getUniqueId());
            }
        }.runTaskLater(plugin, 200L);
    }

    private void handleChargedAttack(Player p, LivingEntity target) {
        UUID playerId = p.getUniqueId();
        int stack = chargedAttackStack.getOrDefault(playerId, 0);
        stack++;
        chargedAttackStack.put(playerId, stack);

        // Visual effect for charged attack
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(0, 1, 0), 
            30, 0.3, 0.5, 0.3, 0.05, new Particle.DustOptions(Color.CYAN, 1.5f));
        
        // Bonus damage for marked enemies (300%)
        if (markedEnemies.contains(target.getUniqueId())) {
            target.damage(target.getHealth() * 0.3, p);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 10);
        }

        // Play sound for charged attack
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);

        // Reset stack after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                chargedAttackStack.put(playerId, 0);
            }
        }.runTaskLater(plugin, 100L);
    }

    private void executeSlideBack(Player p) {
        Location startLoc = p.getLocation();
        originalPosition.put(p.getUniqueId(), startLoc);
        Vector direction = startLoc.getDirection().setY(0).normalize().multiply(-1);

        // Slide back with damage
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step > 15) {
                    this.cancel();
                    return;
                }

                p.setVelocity(direction.multiply(1.2));
                Location trail = p.getLocation();

                // Blue particle trail
                p.getWorld().spawnParticle(Particle.DUST, trail.add(0, 1, 0), 
                    15, 0.4, 0.2, 0.4, 0.05, new Particle.DustOptions(Color.CYAN, 2.0f));
                p.getWorld().spawnParticle(Particle.WHITE_ASH, trail, 10, 0.3, 0.3, 0.3, 0.05);

                // Damage nearby enemies
                for (Entity en : p.getNearbyEntities(4, 4, 4)) {
                    if (en instanceof LivingEntity le && en != p) {
                        le.damage(15.0, p);
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation(), 5);
                    }
                }

                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // LUNAM SWORDS STORM - Ultimate skill
    public void activateSwordsStorm(Player p) {
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.0f);
        
        Location center = p.getLocation();
        
        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration > 60) { // 3 seconds
                    this.cancel();
                    return;
                }

                // Spawn falling swords
                for (int i = 0; i < 5; i++) {
                    double offsetX = (Math.random() - 0.5) * 20;
                    double offsetZ = (Math.random() - 0.5) * 20;
                    Location swordLoc = center.clone().add(offsetX, 30, offsetZ);
                    
                    spawnFallingSword(swordLoc, p);
                }
                duration++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void spawnFallingSword(Location loc, Player owner) {
        // Visual sword falling
        new BukkitRunnable() {
            int height = 30;
            @Override
            public void run() {
                if (height <= 0) {
                    // Impact
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 
                        50, 3, 1, 3, 0.1, new Particle.DustOptions(Color.CYAN, 2.0f));
                    loc.getWorld().playSound(loc, Sound.BLOCK_ANVIL_LAND, 1f, 1.5f);

                    // Damage and slow enemies
                    for (Entity en : loc.getNearbyEntities(5, 1, 5)) {
                        if (en instanceof LivingEntity le && en != owner) {
                            le.damage(20.0, owner);
                            // Apply slowness
                            le.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.SLOW, 100, 2));
                            le.getWorld().spawnParticle(Particle.CRIT, le.getLocation(), 10);
                        }
                    }
                    this.cancel();
                    return;
                }

                // Sword trail
                loc.getWorld().spawnParticle(Particle.DUST, loc.clone().subtract(0, height, 0), 
                    5, 0.2, 0.2, 0.2, 0, new Particle.DustOptions(Color.CYAN, 1.5f));
                
                height -= 2;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // LUNAM SOULS - Summon sword that releases souls
    public void activateLunamSouls(Player p) {
        UUID playerId = p.getUniqueId();
        
        // Cancel existing task if any
        if (soulTask.containsKey(playerId)) {
            soulTask.get(playerId).cancel();
        }

        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        p.sendMessage(ChatColor.AQUA + "Lunam Souls activated!");

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) {
                    this.cancel();
                    soulTask.remove(playerId);
                    return;
                }

                // Release soul projectile
                Location soulLoc = p.getLocation().add(0, 1, 0);
                Vector direction = p.getLocation().getDirection().setY(0).normalize();
                
                new BukkitRunnable() {
                    int distance = 0;
                    @Override
                    public void run() {
                        if (distance > 30) {
                            this.cancel();
                            return;
                        }

                        soulLoc.add(direction.multiply(1));
                        
                        // Soul visual
                        p.getWorld().spawnParticle(Particle.DUST, soulLoc, 
                            10, 0.3, 0.3, 0.3, 0.05, new Particle.DustOptions(Color.fromRGB(100, 200, 255), 1.5f));

                        // Check collision with enemies
                        for (Entity en : soulLoc.getNearbyEntities(1.5, 1.5, 1.5)) {
                            if (en instanceof LivingEntity le && en != p) {
                                le.damage(25.0, p);
                                le.getWorld().spawnParticle(Particle.SOUL, le.getLocation(), 20);
                                p.getWorld().playSound(soulLoc, Sound.BLOCK_SOUL_SAND_BREAK, 0.5f, 1.5f);
                                this.cancel();
                                return;
                            }
                        }
                        distance++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);

            }
        };
        
        task.runTaskTimer(plugin, 0L, 60L); // Every 3 seconds
        soulTask.put(playerId, task);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && 
            i.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        chargedAttackStack.remove(id);
        originalPosition.remove(id);
        if (soulTask.containsKey(id)) {
            soulTask.get(id).cancel();
            soulTask.remove(id);
        }
    }
                            }
