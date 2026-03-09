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
            p.playSound(p.getLocation(), Sound.valueOf("AMETHYST_BLOCK_HIT"), 1f, 1.2f + (stack * 0.2f));
            if (stack == 5) {
                p.sendTitle("§f§l🌕", "§eKLIK KANAN: GOLDEN MOON DOMAIN", 5, 30, 5);
                playSoundSafe(p.getLocation(), Sound.valueOf("BELL_RESONATE"), 1f, 1.5f);
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
        
        // Sound & Partikel awal
        playSoundSafe(p.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), 0.7f, 1.8f);
        spawnGoldenParticles(p.getLocation(), 15);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                Vector dir = target.getLocation().getDirection().setY(0).normalize();
                Location behind = target.getLocation().clone().subtract(dir.multiply(1.2));
                behind.setY(target.getLocation().getY() + 0.5);
                
                p.teleport(behind);
                spawnGoldenParticles(behind, 20);
                playSoundSafe(behind, Sound.valueOf("ENDERMAN_TELEPORT"), 0.7f, 2.2f);
                
                // Slash effect (multi-hit visual)
                for(int i = 0; i < 2; i++) {
                    final int delay = i * 4;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location hitLoc = target.getLocation().add(0, 1, 0);
                            target.getWorld().spawnParticle(Particle.CRIT, hitLoc, 15, 0.3, 0.3, 0.3, 0.1);
                            target.getWorld().spawnParticle(Particle.FLAME, hitLoc, 10, 0.2, 0.2, 0.2, 0);
                            target.getWorld().spawnParticle(Particle.CLOUD, hitLoc, 8, 0.3, 0.3, 0.3, 0);
                            playSoundSafe(hitLoc, Sound.valueOf("ENTITY_PLAYER_ATTACK_SWEEP"), 1f, 1.5f + i * 0.3f);
                        }
                    }.runTaskLater(plugin, delay);
                }
                
                target.damage(8.0, p);
                target.setVelocity(new Vector(0, -1.5, 0));
                spawnGoldenTrail(start, behind, 8);
                
                immunityFrame.add(p.getUniqueId());
                new BukkitRunnable() { @Override public void run() { immunityFrame.remove(p.getUniqueId()); } }.runTaskLater(plugin, 30L);
            }
        }.runTaskLater(plugin, 4L);
    }

    // ==========================================
    // 🌙 SKILL 2: LUNAR CRESCENT
    // ==========================================
    private void executeLunarCrescent(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        
        // FASE 1: Charge effect
        playSoundSafe(p.getLocation(), Sound.valueOf("BLOCK_BEACON_ACTIVATE"), 0.8f, 1.2f);
        for(int i = 0; i < 10; i++) {
            final int delay = i * 2;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location chargeLoc = p.getLocation().add(0, 1, 0);
                    spawnGoldenParticles(chargeLoc, 5);
                    p.getWorld().spawnParticle(Particle.FLAME, chargeLoc, 3, 0.2, 0.2, 0.2, 0);
                }
            }.runTaskLater(plugin, delay);
        }
        
        // FASE 2: DASH + SLASH
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setVelocity(dir.clone().multiply(3.2).setY(0.4));
                playSoundSafe(p.getLocation(), Sound.valueOf("ITEM_TRIDENT_RIPTIDE_3"), 1f, 1.3f);
                
                // Crescent trail
                for(int i = 0; i < 12; i++) {
                    final int idx = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location trailLoc = p.getLocation().add(0, 1, 0);
                            for(double angle = -45; angle <= 45; angle += 15) {
                                double rad = Math.toRadians(angle);
                                Vector offset = new Vector(Math.cos(rad) * 0.8, 0, Math.sin(rad) * 0.8);
                                Location particleLoc = trailLoc.clone().add(offset);
                                spawnGoldenParticles(particleLoc, 3);
                                p.getWorld().spawnParticle(Particle.CRIT, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                            }
                        }
                    }.runTaskLater(plugin, idx);
                }
                
                // Slash impact
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0);
                        p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1);
                        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
                        playSoundSafe(p.getLocation(), Sound.valueOf("ENTITY_PLAYER_ATTACK_SWEEP"), 1.3f, 1.6f);
                        playSoundSafe(p.getLocation(), Sound.valueOf("BLOCK_GLASS_BREAK"), 0.7f, 1.4f);
                        
                        // AOE Damage
                        p.getWorld().getNearbyEntities(p.getLocation(), 3.5, 2.5, 3.5).forEach(en -> {
                            if (en instanceof LivingEntity le && !en.equals(p)) {
                                le.damage(11.0, p);
                                le.setVelocity(p.getLocation().getDirection().multiply(0.6).setY(0.4));
                                le.setFireTicks(40);
                                spawnGoldenParticles(le.getLocation().add(0, 1, 0), 12);
                                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                                playSoundSafe(le.getLocation(), Sound.valueOf("ENTITY_GENERIC_HURT"), 1f, 1.4f);
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
        
        playSoundSafe(center, Sound.valueOf("ENTITY_WITHER_SPAWN"), 1.5f, 1.0f);
        playSoundSafe(center, Sound.valueOf("BLOCK_END_PORTAL_SPAWN"), 1.0f, 0.9f);
        p.sendTitle("§f§l🌕", "§6§lGOLDEN MOON DOMAIN", 10, 40, 10);
        
        // Phase 1: Moon Arena
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 30) {
                    triggerMoonSwordDrop(p, center);
                    this.cancel();
                    return;
                }
                
                double rotation = ticks * 8;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    Location corner = center.clone().add(Math.cos(angle) * 9, 0.3, Math.sin(angle) * 9);
                    spawnGoldenParticles(corner, 4);
                    p.getWorld().spawnParticle(Particle.FLAME, corner, 2, 0.1, 0.1, 0.1, 0);
                }
                
                if(ticks % 3 == 0) {
                    p.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, 0.2, 0), 10, 2, 0.5, 2, 0.05);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // --- PHASE 2: GIANT MOON SWORD DROP ---
    private void triggerMoonSwordDrop(Player p, Location center) {
        p.setVelocity(p.getLocation().getDirection().multiply(-2).setY(0.6));
        immunityFrame.add(p.getUniqueId());
        
        // ArmorStand dengan Golden Sword (cross-platform)
        ArmorStand moonBlade = (ArmorStand) center.getWorld().spawnEntity(
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
        
        // Set arm pose (compatible)
        try {
            moonBlade.setRightArmPose(new org.bukkit.util.EulerAngle(
                Math.toRadians(-100), Math.toRadians(180), Math.toRadians(0)
            ));
        } catch(Exception ignored) {}
        
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
                    p.getWorld().spawnParticle(Particle.FLAME, particleLoc, 4, 0.15, 0.15, 0.15, 0);
                    p.getWorld().spawnParticle(Particle.CRIT, particleLoc, 3, 0.1, 0.1, 0.1, 0);
                    p.getWorld().spawnParticle(Particle.SMOKE, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                }
                
                if(frame % 4 == 0) {
                    playSoundSafe(bladeLoc, Sound.valueOf("BLOCK_BELL_RESONATE"), 0.6f, 2.5f - (frame * 0.08f));
                }
                
                // IMPACT!
                if (currentY <= 0.5) {
                    if (!moonBlade.isDead()) moonBlade.remove();
                    
                    // ===== IMPACT EFFECTS =====
                    center.getWorld().spawnParticle(Particle.EXPLOSION, center, 10);
                    center.getWorld().spawnParticle(Particle.FLAME, center, 40, 4, 1.5, 4, 0.1);
                    center.getWorld().spawnParticle(Particle.SMOKE, center, 50, 5, 2, 5, 0.15);
                    center.getWorld().spawnParticle(Particle.CRIT, center, 60, 5, 2, 5, 0.2);
                    
                    // Block crack (ground)
                    try {
                        center.getWorld().spawnParticle(
                            Particle.valueOf("BLOCK_CRACK"), center, 120, 6, 0.5, 6, 0.1,
                            Bukkit.createBlockData(Material.GOLD_BLOCK)
                        );
                    } catch(Exception ignored) {
                        center.getWorld().spawnParticle(Particle.CLOUD, center, 80, 5, 1, 5, 0.1);
                    }
                    
                    // Lightning
                    for(int flash = 0; flash < 4; flash++) {
                        final int fDelay = flash * 3;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                center.getWorld().strikeLightningEffect(center);
                                playSoundSafe(center, Sound.valueOf("ENTITY_LIGHTNING_BOLT_THUNDER"), 2f, 0.6f);
                            }
                        }.runTaskLater(plugin, fDelay);
                    }
                    
                    // Epic sounds
                    playSoundSafe(center, Sound.valueOf("ENTITY_GENERIC_EXPLODE"), 2.5f, 0.8f);
                    playSoundSafe(center, Sound.valueOf("BLOCK_ANVIL_LAND"), 2f, 0.4f);
                    playSoundSafe(center, Sound.valueOf("ENTITY_WITHER_DEATH"), 1.5f, 0.9f);
                    
                    // AOE Damage
                    center.getWorld().getNearbyEntities(center, 11.0, 11.0, 11.0).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(p)) {
                            le.damage(40.0, p);
                            le.setVelocity(new Vector(0, 1.8, 0));
                            le.setFireTicks(120);
                            le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.1);
                            le.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0, 1, 0), 18, 0.3, 0.3, 0.3, 0.1);
                            playSoundSafe(le.getLocation(), Sound.valueOf("ENTITY_GENERIC_HURT"), 1f, 1.3f);
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
                    
                    immunityFrame.remove(p.getUniqueId());
                    this.cancel();
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🎨 HELPER: GOLDEN PARTICLES (Universal)
    // ==========================================
    private void spawnGoldenParticles(Location loc, int count) {
        // DUST particle dengan fallback
        try {
            loc.getWorld().spawnParticle(
                Particle.valueOf("REDSTONE"), loc, count, 
                new Particle.DustOptions(Color.YELLOW, 1.5f)
            );
        } catch(Exception e) {
            loc.getWorld().spawnParticle(Particle.FLAME, loc, count, 0.3, 0.3, 0.3, 0);
        }
        // Universal backup particles
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
    // 🔊 HELPER: playSound Safe (Old API)
    // ==========================================
    private void playSoundSafe(Location loc, Sound sound, float volume, float pitch) {
        try {
            loc.getWorld().playSound(loc, sound, volume, pitch);
        } catch(Exception e) {
            // Fallback untuk sound name yang tidak ada
        }
    }

    // ==========================================
    // 📦 UTILS
    // ==========================================
    private void sendActionBar(Player p, String msg) {
        try {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
        } catch(Exception e) {
            p.sendMessage(msg); // Fallback chat
        }
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
                            }
