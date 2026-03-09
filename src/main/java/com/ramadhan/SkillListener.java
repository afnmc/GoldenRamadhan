package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {
    
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> chargeStack = new HashMap<>();
    private final Set<UUID> immunityFrame = Collections.synchronizedSet(new HashSet<>());

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    // --- PROTEKSI FALL DAMAGE ---
    @EventHandler(ignoreCancelled = true)
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL && immunityFrame.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
            e.getEntity().setFallDistance(0);
        }
    }

    // --- LOGIC COMBAT & SKILL ---
    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHoldingSword(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        int stack = chargeStack.getOrDefault(id, 0);

        // SYSTEM STACKING (MAX 5)
        if (stack < 5) {
            stack++;
            chargeStack.put(id, stack);
            sendActionBar(p, "§e§l✦ Golden Stack: §f" + stack + "§7/§f5");
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1f, 1.2f + (stack * 0.2f));
            if (stack == 5) {
                p.sendTitle("§f§l🌕", "§eKLIK KANAN: GOLDEN MOON DOMAIN", 5, 30, 5);
                p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 1f, 1.5f);
            }
        }

        // SKILL 1: MOONSTEP BLINK (Stack 1-2 + Sneak+Hit)
        if (p.isSneaking() && stack >= 1 && stack <= 2) {
            executeMoonstepBlink(p, target);
            return;
        }

        // SKILL 2: LUNAR CRESCENT (Stack 3-4 + Sneak+Hit)
        if (p.isSneaking() && stack >= 3 && stack <= 4) {
            executeLunarCrescent(p);
            chargeStack.put(id, 0);
        }
    }

    // --- TRIGGER ULTIMATE (Stack 5 + Right Click) ---
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingSword(p)) return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            int stack = chargeStack.getOrDefault(p.getUniqueId(), 0);
            if (stack >= 5) {
                e.setCancelled(true);
                chargeStack.put(p.getUniqueId(), 0);
                executeGoldenMoonDomain(p);
            }
        }
    }

    // ==========================================
    // 🌙 SKILL 1: MOONSTEP BLINK
    // ==========================================
    private void executeMoonstepBlink(Player p, LivingEntity target) {
        Location start = p.getLocation().clone();
        target.setVelocity(new Vector(0, 0.8, 0));
        
        final Player finalP = p;
        final LivingEntity finalTarget = target;
        final World world = p.getWorld();
        
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.8f);
        spawnGoldenParticles(p.getLocation(), 15);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Vector dir = finalTarget.getLocation().getDirection().setY(0).normalize();
                Location behind = finalTarget.getLocation().clone().subtract(dir.multiply(1.2));
                behind.setY(finalTarget.getLocation().getY() + 0.5);
                
                finalP.teleport(behind);
                spawnGoldenParticles(behind, 20);
                finalP.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 2.2f);
                
                // Slash effect
                for(int i = 0; i < 2; i++) {
                    final int effectIndex = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location hitLoc = finalTarget.getLocation().add(0, 1, 0);
                            world.spawnParticle(Particle.CRIT, hitLoc, 15, 0.3, 0.3, 0.3, 0.1);
                            world.spawnParticle(Particle.FLAME, hitLoc, 10, 0.2, 0.2, 0.2, 0);
                            world.spawnParticle(Particle.CLOUD, hitLoc, 8, 0.3, 0.3, 0.3, 0);
                            world.spawnParticle(Particle.DUST, hitLoc, 12, new Particle.DustOptions(Color.YELLOW, 1.5f));
                            world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f + effectIndex * 0.3f);
                        }
                    }.runTaskLater(plugin, i * 4);
                }
                
                finalTarget.damage(8.0, finalP);
                finalTarget.setVelocity(new Vector(0, -1.5, 0));
                spawnGoldenTrail(start, behind, 8);
                
                immunityFrame.add(finalP.getUniqueId());
                new BukkitRunnable() { @Override public void run() { immunityFrame.remove(finalP.getUniqueId()); } }.runTaskLater(plugin, 30L);
            }
        }.runTaskLater(plugin, 4L);
    }

    // ==========================================
    // 🌙 SKILL 2: LUNAR CRESCENT
    // ==========================================
    private void executeLunarCrescent(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        final Player finalP = p;
        final World world = p.getWorld();
        
        finalP.playSound(finalP.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
        for(int i = 0; i < 10; i++) {
            final int chargeIndex = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location chargeLoc = finalP.getLocation().add(0, 1, 0);
                    spawnGoldenParticles(chargeLoc, 5);
                    world.spawnParticle(Particle.FLAME, chargeLoc, 3, 0.2, 0.2, 0.2, 0);
                    world.spawnParticle(Particle.DUST, chargeLoc, 4, new Particle.DustOptions(Color.ORANGE, 1.2f));
                }
            }.runTaskLater(plugin, chargeIndex * 2);
        }
        
        new BukkitRunnable() {
            @Override
            public void run() {
                finalP.setVelocity(dir.clone().multiply(3.2).setY(0.4));
                finalP.playSound(finalP.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.3f);
                
                // Crescent trail
                for(int i = 0; i < 12; i++) {
                    final int trailIndex = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location trailLoc = finalP.getLocation().add(0, 1, 0);
                            for(double angle = -45; angle <= 45; angle += 15) {
                                double rad = Math.toRadians(angle);
                                Vector offset = new Vector(Math.cos(rad) * 0.8, 0, Math.sin(rad) * 0.8);
                                Location particleLoc = trailLoc.clone().add(offset);
                                spawnGoldenParticles(particleLoc, 3);
                                world.spawnParticle(Particle.CRIT, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                            }
                        }
                    }.runTaskLater(plugin, trailIndex);
                }
                
                // Slash impact
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        world.spawnParticle(Particle.SWEEP_ATTACK, finalP.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0);
                        world.spawnParticle(Particle.CRIT, finalP.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1);
                        world.spawnParticle(Particle.FLAME, finalP.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
                        world.spawnParticle(Particle.DUST, finalP.getLocation().add(0, 1, 0), 15, new Particle.DustOptions(Color.YELLOW, 2.0f));
                        finalP.playSound(finalP.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.3f, 1.6f);
                        finalP.playSound(finalP.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.4f);
                        
                        // AOE Damage - FIX: pakai world.playSound, bukan le.playSound
                        world.getNearbyEntities(finalP.getLocation(), 3.5, 2.5, 3.5).forEach(en -> {
                            if (en instanceof LivingEntity le && !en.equals(finalP)) {
                                le.damage(11.0, finalP);
                                le.setVelocity(finalP.getLocation().getDirection().multiply(0.6).setY(0.4));
                                le.setFireTicks(40);
                                spawnGoldenParticles(le.getLocation().add(0, 1, 0), 12);
                                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                                // FIX: world.playSound instead of le.playSound
                                world.playSound(le.getLocation(), Sound.ENTITY_GENERIC_HURT, 1f, 1.4f);
                            }
                        });
                    }
                }.runTaskLater(plugin, 6);
            }
        }.runTaskLater(plugin, 20);
    }

    // ==========================================
    // 🌕 SKILL 3: GOLDEN MOON DOMAIN
    // ==========================================
    private void executeGoldenMoonDomain(Player p) {
        Location center = p.getLocation().clone();
        final Player finalP = p;
        final World world = p.getWorld();
        
        world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.5f, 1.0f);
        world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.9f);
        finalP.sendTitle("§f§l🌕", "§6§lGOLDEN MOON DOMAIN", 10, 40, 10);
        
        // Phase 1: Moon Arena
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 30) {
                    triggerMoonSwordDrop(finalP, center);
                    this.cancel();
                    return;
                }
                
                double rotation = ticks * 8;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    Location corner = center.clone().add(Math.cos(angle) * 9, 0.3, Math.sin(angle) * 9);
                    spawnGoldenParticles(corner, 4);
                    world.spawnParticle(Particle.FLAME, corner, 2, 0.1, 0.1, 0.1, 0);
                    world.spawnParticle(Particle.DUST, corner, 3, new Particle.DustOptions(Color.YELLOW, 1.8f));
                }
                
                if(ticks % 3 == 0) {
                    world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center.clone().add(0, 0.2, 0), 10, 2, 0.5, 2, 0.05);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // --- PHASE 2: GIANT MOON SWORD DROP ---
    private void triggerMoonSwordDrop(Player p, Location center) {
        final Player finalP = p;
        final World world = p.getWorld();
        
        p.setVelocity(p.getLocation().getDirection().multiply(-2).setY(0.6));
        immunityFrame.add(p.getUniqueId());
        
        // ArmorStand dengan Golden Sword
        ArmorStand moonBlade = (ArmorStand) world.spawnEntity(
            center.clone().add(0, 20, 0),
            EntityType.ARMOR_STAND
        );
        moonBlade.setVisible(false);
        moonBlade.setGravity(false);
        moonBlade.setInvulnerable(true);
        moonBlade.setCustomNameVisible(false);
        
        ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName("§6§l🌙 Golden Moon Blade");
        meta.setUnbreakable(true);
        sword.setItemMeta(meta);
        moonBlade.setItemInHand(sword);
        moonBlade.setRightArmPose(new org.bukkit.util.EulerAngle(
            Math.toRadians(-100), Math.toRadians(180), Math.toRadians(0)
        ));
        
        new BukkitRunnable() {
            int frame = 0;
            double currentY = 20;
            
            @Override
            public void run() {
                currentY -= 2.5;
                Location bladeLoc = center.clone().add(0, currentY, 0);
                
                if (!moonBlade.isDead()) {
                    moonBlade.teleport(bladeLoc);
                }
                
                // Falling trail
                for(double h = 0; h < 4; h += 0.7) {
                    Location particleLoc = bladeLoc.clone().add(0, h, 0);
                    spawnGoldenParticles(particleLoc, 6);
                    world.spawnParticle(Particle.FLAME, particleLoc, 4, 0.15, 0.15, 0.15, 0);
                    world.spawnParticle(Particle.CRIT, particleLoc, 3, 0.1, 0.1, 0.1, 0);
                    world.spawnParticle(Particle.DUST, particleLoc, 5, new Particle.DustOptions(Color.YELLOW, 1.5f));
                }
                
                if(frame % 4 == 0) {
                    world.playSound(bladeLoc, Sound.BLOCK_BELL_RESONATE, 0.6f, 2.5f - (frame * 0.08f));
                }
                
                // IMPACT!
                if (currentY <= 0.5) {
                    if (!moonBlade.isDead()) moonBlade.remove();
                    
                    // ===== IMPACT EFFECTS =====
                    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 10);
                    world.spawnParticle(Particle.FLAME, center, 40, 4, 1.5, 4, 0.1);
                    world.spawnParticle(Particle.CLOUD, center, 50, 5, 2, 5, 0.15);
                    world.spawnParticle(Particle.CRIT, center, 60, 5, 2, 5, 0.2);
                    world.spawnParticle(Particle.DUST, center, 30, new Particle.DustOptions(Color.ORANGE, 2.5f));
                    
                    // FIX: BLOCK_CRACK dengan fallback aman
                    spawnBlockCrackEffect(center, Material.GOLD_BLOCK);
                    
                    // Lightning
                    for(int flash = 0; flash < 4; flash++) {
                        final int fDelay = flash * 3;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                world.strikeLightningEffect(center);
                                world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.6f);
                            }
                        }.runTaskLater(plugin, fDelay);
                    }
                    
                    // Epic sounds
                    world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.8f);
                    world.playSound(center, Sound.BLOCK_ANVIL_LAND, 2f, 0.4f);
                    world.playSound(center, Sound.ENTITY_WITHER_DEATH, 1.5f, 0.9f);
                    
                    // AOE Damage - FIX: world.playSound instead of le.playSound
                    world.getNearbyEntities(center, 11.0, 11.0, 11.0).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(finalP)) {
                            le.damage(40.0, finalP);
                            le.setVelocity(new Vector(0, 1.8, 0));
                            le.setFireTicks(120);
                            le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.1);
                            le.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0, 1, 0), 18, 0.3, 0.3, 0.3, 0.1);
                            le.getWorld().spawnParticle(Particle.DUST, le.getLocation().add(0, 1, 0), 10, new Particle.DustOptions(Color.YELLOW, 1.8f));
                            // FIX: world.playSound instead of le.playSound
                            world.playSound(le.getLocation(), Sound.ENTITY_GENERIC_HURT, 1f, 1.3f);
                        }
                    });
                    
                    // After glow
                    new BukkitRunnable() {
                        int glowTicks = 0;
                        @Override
                        public void run() {
                            if(glowTicks >= 20) { this.cancel(); return; }
                            spawnGoldenParticles(center.clone().add(0, 0.5, 0), 8);
                            glowTicks++;
                        }
                    }.runTaskTimer(plugin, 0, 2);
                    
                    immunityFrame.remove(finalP.getUniqueId());
                    this.cancel();
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🎨 HELPER: GOLDEN PARTICLES
    // ==========================================
    private void spawnGoldenParticles(Location loc, int count) {
        loc.getWorld().spawnParticle(Particle.DUST, loc, count, new Particle.DustOptions(Color.YELLOW, 1.5f));
        loc.getWorld().spawnParticle(Particle.FLAME, loc, count / 2, 0.2, 0.2, 0.2, 0);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, count / 3, 0.2, 0.2, 0.2, 0);
    }
    
    private void spawnGoldenTrail(Location from, Location to, int density) {
        Vector vector = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        if(dist < 0.1) return;
        vector.normalize().multiply(0.5);
        
        for (double i = 0; i < dist; i += 0.5) {
            Location particleLoc = from.clone().add(vector.clone().multiply(i));
            spawnGoldenParticles(particleLoc, density / 2);
        }
    }

    // ==========================================
    // 🔨 HELPER: Block Crack Effect (Safe)
    // ==========================================
    private void spawnBlockCrackEffect(Location loc, Material material) {
        try {
            // Coba pakai BLOCK_CRACK dengan BlockData (1.13+)
            loc.getWorld().spawnParticle(
                Particle.valueOf("BLOCK_CRACK"), 
                loc, 120, 6, 0.5, 6, 0.1,
                Bukkit.createBlockData(material)
            );
        } catch (IllegalArgumentException | NullPointerException e) {
            // Fallback kalau BLOCK_CRACK tidak support
            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 80, 5, 1, 5, 0.1);
            loc.getWorld().spawnParticle(Particle.FLAME, loc, 60, 4, 0.5, 4, 0.1);
        }
    }

    // ==========================================
    // 📦 UTILS
    // ==========================================
    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
                        }
