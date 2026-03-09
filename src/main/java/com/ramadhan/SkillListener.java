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
    // Teleport behind enemy + quick slash
    // ==========================================
    private void executeMoonstepBlink(Player p, LivingEntity target) {
        Location start = p.getLocation().clone();
        target.setVelocity(new Vector(0, 0.8, 0));
        
        // Sound & Partikel awal (universal)
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.8f);
        spawnMoonParticles(p.getLocation(), 15, Color.WHITE);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                // Hitung posisi belakang musuh
                Vector dir = target.getLocation().getDirection().setY(0).normalize();
                Location behind = target.getLocation().clone().subtract(dir.multiply(1.2));
                behind.setY(target.getLocation().getY() + 0.5);
                
                // Teleport + efek
                p.teleport(behind);
                spawnMoonParticles(behind, 20, Color.YELLOW);
                p.playSound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 2.2f);
                
                // Slash effect (multi-hit visual)
                for(int i = 0; i < 2; i++) {
                    final int delay = i * 4;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location hitLoc = target.getLocation().add(0, 1, 0);
                            target.getWorld().spawnParticle(Particle.CRIT, hitLoc, 15, 0.3, 0.3, 0.3, 0.1);
                            target.getWorld().spawnParticle(Particle.SPELL, hitLoc, 10, 0.2, 0.2, 0.2, 0);
                            target.getWorld().spawnParticle(Particle.FLAME, hitLoc, 8, 0.2, 0.2, 0.2, 0);
                            target.getWorld().playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f + i * 0.3f);
                        }
                    }.runTaskLater(plugin, delay);
                }
                
                // Damage
                target.damage(8.0, p);
                target.setVelocity(new Vector(0, -1.5, 0));
                
                // Moon trail effect
                spawnMoonTrail(start, behind, Color.WHITE, 8);
                
                immunityFrame.add(p.getUniqueId());
                new BukkitRunnable() { @Override public void run() { immunityFrame.remove(p.getUniqueId()); } }.runTaskLater(plugin, 30L);
            }
        }.runTaskLater(plugin, 4L);
    }

    // ==========================================
    // 🌙 SKILL 2: LUNAR CRESCENT
    // Dash forward + circular slash AOE
    // ==========================================
    private void executeLunarCrescent(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        
        // FASE 1: PERSIAPAN (tarik energi bulan)
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
        for(int i = 0; i < 10; i++) {
            final int delay = i * 2;
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location chargeLoc = p.getLocation().add(0, 1, 0);
                    spawnMoonParticles(chargeLoc, 5, Color.YELLOW);
                    p.getWorld().spawnParticle(Particle.SPELL_WITCH, chargeLoc, 3, 0.2, 0.2, 0.2, 0);
                }
            }.runTaskLater(plugin, delay);
        }
        
        // FASE 2: DASH + SLASH
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setVelocity(dir.clone().multiply(3.2).setY(0.4));
                p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.3f);
                
                // Crescent trail effect
                for(int i = 0; i < 12; i++) {
                    final int idx = i;
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location trailLoc = p.getLocation().add(0, 1, 0);
                            // Arc particle pattern (crescent shape)
                            for(double angle = -45; angle <= 45; angle += 15) {
                                double rad = Math.toRadians(angle);
                                Vector offset = new Vector(Math.cos(rad) * 0.8, 0, Math.sin(rad) * 0.8);
                                Location particleLoc = trailLoc.clone().add(offset);
                                spawnMoonParticles(particleLoc, 3, Color.WHITE);
                                p.getWorld().spawnParticle(Particle.CRIT, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                            }
                        }
                    }.runTaskLater(plugin, idx);
                }
                
                // Slash impact (delay sedikit biar pas di peak dash)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Visual slash
                        p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, p.getLocation().add(0, 1, 0), 6, 0.4, 0.4, 0.4, 0);
                        p.getWorld().spawnParticle(Particle.CRIT_MAGIC, p.getLocation().add(0, 1, 0), 25, 0.5, 0.5, 0.5, 0.1);
                        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.3f, 1.6f);
                        p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.4f);
                        
                        // AOE Damage
                        p.getWorld().getNearbyEntities(p.getLocation(), 3.5, 2.5, 3.5).forEach(en -> {
                            if (en instanceof LivingEntity le && !en.equals(p)) {
                                le.damage(11.0, p);
                                le.setVelocity(p.getLocation().getDirection().multiply(0.6).setY(0.4));
                                le.setFireTicks(40); // Small burn effect
                                spawnMoonParticles(le.getLocation().add(0, 1, 0), 12, Color.YELLOW);
                                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                                le.playSound(le.getLocation(), Sound.ENTITY_GENERIC_HURT, 1f, 1.4f);
                            }
                        });
                    }
                }.runTaskLater(plugin, 6);
            }
        }.runTaskLater(plugin, 20);
    }

    // ==========================================
    // 🌕 SKILL 3: GOLDEN MOON DOMAIN (ULTIMATE)
    // Arena + Giant Moon Sword Drop
    // ==========================================
    private void executeGoldenMoonDomain(Player p) {
        Location center = p.getLocation().clone();
        
        // Sound intro epic
        p.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.5f, 1.0f);
        p.getWorld().playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.9f);
        p.sendTitle("§f§l🌕", "§6§lGOLDEN MOON DOMAIN", 10, 40, 10);
        
        // Phase 1: Create Moon Arena (hexagon ring)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 30) {
                    triggerMoonSwordDrop(p, center);
                    this.cancel();
                    return;
                }
                
                // Rotating hexagon particles
                double rotation = ticks * 8;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    Location corner = center.clone().add(Math.cos(angle) * 9, 0.3, Math.sin(angle) * 9);
                    
                    // Universal particles only
                    spawnMoonParticles(corner, 4, Color.YELLOW);
                    p.getWorld().spawnParticle(Particle.FLAME, corner, 2, 0.1, 0.1, 0.1, 0);
                    p.getWorld().spawnParticle(Particle.SPELL, corner, 2, 0, 0, 0, 0);
                }
                
                // Inner moon glow
                if(ticks % 3 == 0) {
                    p.getWorld().spawnParticle(Particle.SMOKE_NORMAL, center.clone().add(0, 0.2, 0), 10, 2, 0.5, 2, 0.05);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // --- PHASE 2: GIANT MOON SWORD DROP ---
    private void triggerMoonSwordDrop(Player p, Location center) {
        p.setVelocity(p.getLocation().getDirection().multiply(-2).setY(0.6));
        immunityFrame.add(p.getUniqueId());
        
        // Spawn ArmorStand dengan Golden Sword (cross-platform visible)
        ArmorStand moonBlade = (ArmorStand) center.getWorld().spawnEntity(
            center.clone().add(0, 20, 0),
            EntityType.ARMOR_STAND
        );
        moonBlade.setVisible(false);
        moonBlade.setGravity(false);
        moonBlade.setInvulnerable(true);
        moonBlade.setCustomNameVisible(false);
        
        // Equip golden sword
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
                
                // Update ArmorStand position
                if (!moonBlade.isDead()) {
                    moonBlade.teleport(bladeLoc);
                }
                
                // Falling trail particles (universal)
                for(double h = 0; h < 4; h += 0.7) {
                    Location particleLoc = bladeLoc.clone().add(0, h, 0);
                    spawnMoonParticles(particleLoc, 6, Color.YELLOW);
                    p.getWorld().spawnParticle(Particle.FLAME, particleLoc, 4, 0.15, 0.15, 0.15, 0);
                    p.getWorld().spawnParticle(Particle.CRIT, particleLoc, 3, 0.1, 0.1, 0.1, 0);
                    p.getWorld().spawnParticle(Particle.SMOKE_NORMAL, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                }
                
                // Falling sound (pitch rising)
                if(frame % 4 == 0) {
                    p.getWorld().playSound(bladeLoc, Sound.BLOCK_BELL_RESONATE, 0.6f, 2.5f - (frame * 0.08f));
                }
                
                // IMPACT!
                if (currentY <= 0.5) {
                    // Cleanup
                    if (!moonBlade.isDead()) moonBlade.remove();
                    
                    // ===== IMPACT EFFECTS =====
                    // Universal explosion particles
                    center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, center, 10);
                    center.getWorld().spawnParticle(Particle.FLAME, center, 40, 4, 1.5, 4, 0.1);
                    center.getWorld().spawnParticle(Particle.SMOKE_NORMAL, center, 50, 5, 2, 5, 0.15);
                    center.getWorld().spawnParticle(Particle.CRIT, center, 60, 5, 2, 5, 0.2);
                    center.getWorld().spawnParticle(Particle.CRIT_MAGIC, center, 30, 4, 1.5, 4, 0.15);
                    
                    // Block crack (ground effect)
                    center.getWorld().spawnParticle(
                        Particle.BLOCK_CRACK, center, 120, 6, 0.5, 6, 0.1,
                        Bukkit.createBlockData(Material.GOLD_BLOCK)
                    );
                    
                    // Lightning flashes (visible in Bedrock!)
                    for(int flash = 0; flash < 4; flash++) {
                        final int fDelay = flash * 3;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                center.getWorld().strikeLightningEffect(center);
                                center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2f, 0.6f);
                            }
                        }.runTaskLater(plugin, fDelay);
                    }
                    
                    // Epic sounds
                    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.8f);
                    center.getWorld().playSound(center, Sound.BLOCK_ANVIL_LAND, 2f, 0.4f);
                    center.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 1.5f, 0.9f);
                    
                    // AOE Damage + Effects
                    center.getWorld().getNearbyEntities(center, 11.0, 11.0, 11.0).forEach(en -> {
                        if (en instanceof LivingEntity le && !en.equals(p)) {
                            le.damage(40.0, p);
                            le.setVelocity(new Vector(0, 1.8, 0));
                            le.setFireTicks(120); // Burn effect (works in Bedrock)
                            
                            // Hit particles (universal)
                            le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.1);
                            le.getWorld().spawnParticle(Particle.FLAME, le.getLocation().add(0, 1, 0), 18, 0.3, 0.3, 0.3, 0.1);
                            le.getWorld().spawnParticle(Particle.SPELL, le.getLocation().add(0, 1, 0), 12, 0.2, 0.2, 0.2, 0);
                            le.playSound(le.getLocation(), Sound.ENTITY_GENERIC_HURT, 1f, 1.3f);
                        }
                    });
                    
                    // Moon glow after effect
                    new BukkitRunnable() {
                        int glowTicks = 0;
                        @Override
                        public void run() {
                            if(glowTicks >= 20) { this.cancel(); return; }
                            spawnMoonParticles(center.clone().add(0, 0.5, 0), 8, Color.YELLOW);
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
    // 🎨 HELPER: MOON PARTICLES (Cross-Platform)
    // ==========================================
    private void spawnMoonParticles(Location loc, int count, Color color) {
        // Pakai DUST particle tapi fallback aman untuk Bedrock
        // Bedrock akan ignore DUST tapi tetap lihat FLAME/CRIT/SMOKE
        try {
            loc.getWorld().spawnParticle(
                Particle.DUST, loc, count, 
                new Particle.DustOptions(color, 1.5f)
            );
        } catch(Exception e) {
            // Fallback kalau DUST tidak support
            loc.getWorld().spawnParticle(Particle.SPELL, loc, count, 0.3, 0.3, 0.3, 0);
        }
        // Selalu spawn universal particles sebagai backup
        loc.getWorld().spawnParticle(Particle.FLAME, loc, count / 2, 0.2, 0.2, 0.2, 0);
        loc.getWorld().spawnParticle(Particle.CRIT, loc, count / 3, 0.2, 0.2, 0.2, 0);
    }
    
    private void spawnMoonTrail(Location from, Location to, Color color, int density) {
        Vector vector = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        if(dist < 0.1) return;
        vector.normalize().multiply(0.5);
        
        for (double i = 0; i < dist; i += 0.5) {
            Location particleLoc = from.clone().add(vector.clone().multiply(i));
            spawnMoonParticles(particleLoc, density / 2, color);
        }
    }

    // ==========================================
    // 📦 UTILS
    // ==========================================
    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
            }
