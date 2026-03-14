package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SkillListener implements Listener {
    
    // 🎨 Color Palette
    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color WHITE = Color.fromRGB(240, 248, 255);
    private static final Color SILVER = Color.fromRGB(192, 192, 192);
    private static final Color PURPLE = Color.fromRGB(180, 140, 220);
    private static final Color CRIMSON = Color.fromRGB(220, 60, 60);

    // 🛡️ Armor Type Constants
    private static final byte TYPE_CRESCENT = 0; // Defensive/Utility
    private static final byte TYPE_GOLDEN = 1;    // Offensive/Aggressive

    private final GoldenMoon plugin;
    private final Map<UUID, PlayerData> data = new HashMap<>();
    private final Map<UUID, Long> moonMarked = new HashMap<>();
    private final Random r = new Random();
    
    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        data.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHoldingSword(p)) return;

        PlayerData d = get(p);
        long now = System.currentTimeMillis();

        // ---------------------------------------------------------
        // SKILL 1: LUNAR DASH & SLASH (SHIFT + LEFT CLICK)
        // ---------------------------------------------------------
        if (p.isSneaking() && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            if (now - d.lastDash < 1500) {
                sab(p, "§cDash Cooldown!");
                return;
            }
            performLunarDash(p);
            d.lastDash = now;
            return;
        }

        // ---------------------------------------------------------
        // SKILL 2: CRESCENT PROJECTILE (LEFT CLICK AIR)
        // ---------------------------------------------------------
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (now - d.lastSlash < 500) return;
            spawnDetailedCrescent(p);
            d.lastSlash = now;
        }

        // ---------------------------------------------------------
        // SKILL 3: LUNAR EXECUTION (RIGHT CLICK - FULL ANIMATION)
        // ---------------------------------------------------------
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (now - d.lastUlt < 12000) {
                sab(p, "§cUltimate: " + (12 - (now - d.lastUlt)/1000) + "s");
                return;
            }
            performLunarExecution(p);
            d.lastUlt = now;
        }
    }
    // ==========================================
    // 🛡️ ARMOR PASSIVE: ON DAMAGE
    // ==========================================
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        
        byte armorType = getDominantArmorType(p);
        
        // CHESTPLATE PASSIVE: Damage Reduction
        if (isWearingPiece(p, EquipmentSlot.CHEST, GoldenMoon.CHEST_KEY)) {
            double reduction;
            if (armorType == TYPE_GOLDEN) {
                // Golden: 10% DR + lifesteal on hit
                reduction = 0.10;
                if (r.nextInt(100) < 25 && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), p.getHealth() + 1.0));
                    spawnArmorSparkle(p.getLocation().add(0, 1.2, 0), p.getWorld(), GOLD, 5);
                }
            } else {
                // Crescent: 20% DR (more defensive)
                reduction = 0.20;
            }
            e.setDamage(e.getDamage() * (1 - reduction));
            
            // Visual feedback
            Color sparkleColor = armorType == TYPE_GOLDEN ? GOLD : SILVER;
            if (r.nextInt(100) < 40) {
                spawnArmorSparkle(p.getLocation().add(0, 1.2, 0), p.getWorld(), sparkleColor, 8);
            }
        }
        
        // HELMET PASSIVE
        if (isWearingPiece(p, EquipmentSlot.HEAD, GoldenMoon.HELMET_KEY)) {
            if (armorType == TYPE_GOLDEN) {
                // Golden: +10% melee damage when below 70% HP (berserk)
                if (p.getHealth() < p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.7) {
                    if (!data.containsKey(p.getUniqueId())) data.put(p.getUniqueId(), new PlayerData());
                    get(p).berserkActive = true;
                    if (r.nextInt(100) < 20) spawnArmorSparkle(p.getLocation().add(0, 2, 0), p.getWorld(), CRIMSON, 4);
                } else {
                    if (data.containsKey(p.getUniqueId())) get(p).berserkActive = false;
                }
            } else {
                // Crescent: Auto-heal when below 50% HP
                if (p.getHealth() < p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * 0.5) {
                    if (r.nextInt(100) < 15) {
                        p.setHealth(Math.min(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), p.getHealth() + 1.5));                        spawnArmorSparkle(p.getLocation().add(0, 2, 0), p.getWorld(), WHITE, 5);
                    }
                }
            }
        }
    }

    // ==========================================
    // 👢 ARMOR PASSIVE: ON MOVE
    // ==========================================
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        byte armorType = getDominantArmorType(p);
        
        // BOOTS PASSIVE
        if (isWearingPiece(p, EquipmentSlot.FEET, GoldenMoon.BOOTS_KEY)) {
            if (armorType == TYPE_GOLDEN) {
                // Golden: Sprint speed boost + attack speed
                if (p.isSprinting() && r.nextInt(100) < 5) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, false));
                }
            } else {
                // Crescent: No fall damage + gentle landing
                if (p.getFallDistance() > 3) {
                    p.setFallDistance(0);
                    if (r.nextInt(100) < 30) {
                        spawnArmorSparkle(p.getLocation().add(0, 0.1, 0), p.getWorld(), SILVER, 4);
                        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_SNOW_STEP, 0.3f, 1.2f);
                    }
                }
                // Crescent: Jump boost when sneaking
                if (p.isSneaking() && p.getLocation().getY() > e.getFrom().getY()) {
                    p.setVelocity(p.getVelocity().setY(p.getVelocity().getY() + 0.12));
                }
            }
        }
        
        // LEGGINGS PASSIVE
        if (isWearingPiece(p, EquipmentSlot.LEGS, GoldenMoon.LEGGINGS_KEY)) {
            if (armorType == TYPE_GOLDEN) {
                // Golden: Dash cooldown reduced visually + crit chance
                PlayerData d = get(p);
                if (System.currentTimeMillis() - d.lastDash < 1500 && r.nextInt(40) == 0) {
                    spawnArmorSparkle(p.getLocation().add(0, 0.5, 0), p.getWorld(), GOLD, 3);
                }
            } else {
                // Crescent: Stealth particles when sneaking
                if (p.isSneaking() && r.nextInt(50) == 0) {
                    spawnArmorSparkle(p.getLocation().add(0, 0.3, 0), p.getWorld(), SILVER, 2);                }
            }
        }
        
        // FULL SET BONUS: Periodic aura
        if (hasFullSetOfType(p, armorType) && System.currentTimeMillis() % 2000 < 100) {
            Color auraColor = armorType == TYPE_GOLDEN ? GOLD : SILVER;
            int particleCount = armorType == TYPE_GOLDEN ? 15 : 10;
            spawnArmorSparkle(p.getLocation().add(0, 1.5, 0), p.getWorld(), auraColor, particleCount);
            
            // Golden: Occasional damage aura
            if (armorType == TYPE_GOLDEN && r.nextInt(4) == 0) {
                for (Entity en : p.getWorld().getNearbyEntities(p.getLocation(), 4, 3, 4)) {
                    if (en instanceof LivingEntity && !en.equals(p)) {
                        ((LivingEntity) en).damage(0.5, p);
                    }
                }
            }
        }
    }

    // ==========================================
    // ⚔️ ATTACK EVENT: Apply berserk bonus
    // ==========================================
    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        
        PlayerData d = get(p);
        // Apply berserk damage bonus if active
        if (d.berserkActive && isHoldingSword(p)) {
            e.setDamage(e.getDamage() * 1.15); // +15% damage
            if (r.nextInt(100) < 30) {
                spawnArmorSparkle(e.getEntity().getLocation(), p.getWorld(), CRIMSON, 3);
            }
        }
    }

    // ==========================================
    // ⚔️ SKILL 1: LUNAR DASH & SLASH
    // ==========================================
    private void performLunarDash(Player p) {
        World w = p.getWorld();
        Location loc = p.getLocation();
        byte armorType = getDominantArmorType(p);
        
        Vector dir = loc.getDirection().setY(0).normalize().multiply(1.8);
        
        // Golden bonus: Extra dash distance        if (armorType == TYPE_GOLDEN && hasFullSetOfType(p, TYPE_GOLDEN)) {
            dir = dir.multiply(1.25);
        }
        
        p.setVelocity(dir);
        w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
        w.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 2.0f);

        new BukkitRunnable() {
            int step = 0;
            public void run() {
                if (step > 6) { cancel(); return; }
                Location pLoc = p.getLocation().add(0, 0.8, 0);
                
                Color trailColor = armorType == TYPE_GOLDEN ? GOLD : SILVER;
                float trailSize = armorType == TYPE_GOLDEN ? 1.4f : 1.2f;
                int trailCount = armorType == TYPE_GOLDEN ? 5 : 3;
                
                // Trail particles
                for (double i = -1.2; i <= 1.2; i += 0.3) {
                    double arc = Math.cos(i) * 0.5;
                    Vector side = rotate(p.getLocation().getDirection(), 90).multiply(i);
                    Vector back = p.getLocation().getDirection().multiply(-arc);
                    w.spawnParticle(Particle.DUST, pLoc.clone().add(side).add(back), trailCount, new Particle.DustOptions(trailColor, trailSize));
                }
                
                // Golden bonus: Extra sparkles
                if (armorType == TYPE_GOLDEN && step % 2 == 0) {
                    for (int s = 0; s < 4; s++) {
                        Vector spark = new Vector((r.nextDouble()-0.5)*0.8, r.nextDouble()*0.6, (r.nextDouble()-0.5)*0.8);
                        w.spawnParticle(Particle.DUST, pLoc.clone().add(spark), 1, new Particle.DustOptions(GOLD, 1.3f));
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🌙 SKILL 2: CRESCENT PROJECTILE
    // ==========================================
    private void spawnDetailedCrescent(Player p) {
        final World w = p.getWorld();
        final Location start = p.getEyeLocation().add(p.getLocation().getDirection());
        final Vector direction = p.getLocation().getDirection().normalize();
        byte armorType = getDominantArmorType(p);
        
        w.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
        if (armorType == TYPE_GOLDEN) w.playSound(start, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 2.0f);
        new BukkitRunnable() {
            int life = 0;
            public void run() {
                if (life > 12) { cancel(); return; }
                Location current = start.clone().add(direction.clone().multiply(life * 0.9));
                
                // Animasi Tebasan Melengkung
                for (double angle = -1.5; angle <= 1.5; angle += 0.15) {
                    double curve = (angle * angle) * 0.25;
                    Vector v = rotate(direction, 90).multiply(angle).add(direction.clone().multiply(-curve));
                    
                    Color mainColor = armorType == TYPE_GOLDEN ? GOLD : SILVER;
                    w.spawnParticle(Particle.DUST, current.clone().add(v), 1, new Particle.DustOptions(mainColor, 1.4f));
                    if (life % 2 == 0) w.spawnParticle(Particle.DUST, current.clone().add(v), 1, new Particle.DustOptions(WHITE, 0.8f));
                }
                
                // Golden bonus: Purple accent particles
                if (armorType == TYPE_GOLDEN && life % 3 == 0) {
                    for (double angle = -1.0; angle <= 1.0; angle += 0.3) {
                        Vector accent = rotate(direction, 90).multiply(angle * 0.7);
                        w.spawnParticle(Particle.DUST, current.clone().add(accent), 1, new Particle.DustOptions(PURPLE, 1.1f));
                    }
                }

                // Check Hit
                double hitRadius = armorType == TYPE_GOLDEN ? 1.6 : 1.3;
                for (Entity target : w.getNearbyEntities(current, hitRadius, hitRadius, hitRadius)) {
                    if (target instanceof LivingEntity && !target.equals(p)) {
                        LivingEntity le = (LivingEntity) target;
                        double baseDmg = armorType == TYPE_GOLDEN ? 7.5 : 6.0;
                        // Berserk bonus
                        if (get(p).berserkActive) baseDmg *= 1.15;
                        
                        le.damage(baseDmg, p);
                        le.setNoDamageTicks(0);
                        applyMoonMark(le);
                        w.playSound(le.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                        
                        // Visual feedback
                        Color hitColor = armorType == TYPE_GOLDEN ? GOLD : SILVER;
                        spawnArmorSparkle(le.getLocation().add(0, 1, 0), w, hitColor, armorType == TYPE_GOLDEN ? 8 : 5);
                        cancel(); return;
                    }
                }
                life++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================    // 🌕 SKILL 3: LUNAR EXECUTION (TYPE-BASED)
    // ==========================================
    private void performLunarExecution(Player p) {
        World w = p.getWorld();
        Location center = p.getLocation();
        byte armorType = getDominantArmorType(p);
        boolean isFullSet = hasFullSetOfType(p, armorType);
        
        // Title based on armor type
        String subtitle = armorType == TYPE_GOLDEN ? "§6§l⚔️ OFFENSIVE MODE" : "§f§l🛡️ DEFENSIVE MODE";
        p.sendTitle("§6§l✦ LUNAR EXECUTION ✦", subtitle, 5, 30, 10);
        
        float soundPitch = armorType == TYPE_GOLDEN ? 0.3f : 0.5f;
        w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, isFullSet ? 1.8f : 1.5f, soundPitch);

        // PHASE 1: SUMMONING RINGS (Type-based colors)
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t > 20) { cancel(); return; }
                Color ringColor = armorType == TYPE_GOLDEN ? GOLD : SILVER;
                float ringSize = armorType == TYPE_GOLDEN ? 2.0f : 1.8f;
                int ringCount = isFullSet ? 8 : 5;
                double ringRadius = isFullSet ? 5 : 4;
                
                for (int i = 0; i < 2; i++) {
                    double angle = Math.toRadians(t * 18 + (i * 180));
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    w.spawnParticle(Particle.DUST, center.clone().add(x, 0.1, z), ringCount, new Particle.DustOptions(ringColor, ringSize));
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        // PHASE 2: TRIPLE CRESCENT STRIKE
        for (int strike = 1; strike <= 3; strike++) {
            final int s = strike;
            new BukkitRunnable() {
                public void run() {
                    w.playSound(center, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.2f, 0.5f + (s * 0.4f));
                    if (isFullSet) w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.8f);
                    
                    int particleDensity = isFullSet ? 7 : 5;
                    float particleSize = isFullSet ? 2.2f : 2.0f;
                    double slashOffset = isFullSet ? 2.5 : 2;
                    
                    for (double y = -3; y <= 3; y += 0.2) {
                        double xOff = (y * y) * 0.3;
                        Vector v = new Vector(xOff - slashOffset, y + 1.5, 0);                        Vector finalV = rotate(v, s * 60); 
                        w.spawnParticle(Particle.DUST, center.clone().add(finalV), particleDensity, new Particle.DustOptions(WHITE, particleSize));
                        w.spawnParticle(Particle.DUST, center.clone().add(finalV), isFullSet ? 3 : 2, new Particle.DustOptions(armorType == TYPE_GOLDEN ? GOLD : SILVER, isFullSet ? 1.7f : 1.5f));
                    }
                    
                    // Golden bonus: Purple accent slashes
                    if (armorType == TYPE_GOLDEN && isFullSet) {
                        for (double y = -2.5; y <= 2.5; y += 0.4) {
                            double xOff = (y * y) * 0.25;
                            Vector v = new Vector(xOff - 2.2, y + 1.5, 0);
                            Vector finalV = rotate(v, s * 60 + 15);
                            w.spawnParticle(Particle.DUST, center.clone().add(finalV), 2, new Particle.DustOptions(PURPLE, 1.4f));
                        }
                    }
                    
                    // Damage & Effects
                    double radius = isFullSet ? 9 : 7;
                    for (Entity en : w.getNearbyEntities(center, radius, 6, radius)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            LivingEntity le = (LivingEntity) en;
                            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 15.0 : 8.0;
                            double dmg = isFullSet ? baseDmg * (armorType == TYPE_GOLDEN ? 1.3 : 1.15) : baseDmg;
                            le.damage(dmg, p);
                            le.setVelocity(new Vector(0, isFullSet ? 0.8 : 0.6, 0));
                            if (dmg > 10) moonMarked.remove(le.getUniqueId());
                            
                            // Visual feedback
                            Color hitColor = armorType == TYPE_GOLDEN ? GOLD : SILVER;
                            spawnArmorSparkle(le.getLocation().add(0, 1.2, 0), w, hitColor, isFullSet ? 6 : 4);
                        }
                    }
                }
            }.runTaskLater(plugin, 20 + (s * 8));
        }
        
        // PHASE 3: SELF BUFF (Type-based)
        new BukkitRunnable() {
            public void run() {
                try {
                    if (armorType == TYPE_GOLDEN) {
                        // Golden: More heal + strength buff
                        double heal = isFullSet ? 7.0 : 5.0;
                        if (p.getHealth() < p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
                            p.setHealth(Math.min(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), p.getHealth() + heal));
                        }
                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, isFullSet ? 200 : 150, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, isFullSet ? 250 : 180, 1, false, false));
                    } else {
                        // Crescent: More sustain + regeneration
                        double heal = isFullSet ? 8.0 : 6.0;                        if (p.getHealth() < p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
                            p.setHealth(Math.min(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), p.getHealth() + heal));
                        }
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, isFullSet ? 200 : 150, 1, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, isFullSet ? 300 : 200, 0, false, false));
                    }
                } catch (Exception ignored) {}
                
                // Visual finale
                Color finaleColor = armorType == TYPE_GOLDEN ? GOLD : SILVER;
                spawnArmorSparkle(p.getLocation().add(0, 1.5, 0), w, finaleColor, isFullSet ? 25 : 15);
            }
        }.runTaskLater(plugin, 45);
    }

    // ==========================================
    // 🌙 MOON MARK SYSTEM
    // ==========================================
    private void applyMoonMark(LivingEntity target) {
        moonMarked.put(target.getUniqueId(), System.currentTimeMillis() + 5000);
        new BukkitRunnable() {
            int time = 0;
            public void run() {
                if (time > 100 || !target.isValid() || !moonMarked.containsKey(target.getUniqueId())) {
                    moonMarked.remove(target.getUniqueId());
                    cancel(); return;
                }
                Location head = target.getLocation().add(0, 2.5, 0);
                target.getWorld().spawnParticle(Particle.DUST, head, 3, new Particle.DustOptions(GOLD, 1.5f));
                time += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // ==========================================
    // 🛡️ ARMOR TYPE UTILITIES
    // ==========================================
    
    /** Get armor type from item PDC */
    private byte getArmorType(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return -1;
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return -1;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
    }
    
    /** Check if player is wearing a specific armor piece with specific type */
    private boolean isWearingPieceOfType(Player p, EquipmentSlot slot, NamespacedKey key, byte type) {
        ItemStack item = switch (slot) {
            case HEAD -> p.getInventory().getHelmet();
            case CHEST -> p.getInventory().getChestplate();            case LEGS -> p.getInventory().getLeggings();
            case FEET -> p.getInventory().getBoots();
            default -> null;
        };
        if (item == null || !item.hasItemMeta()) return false;
        Byte storedType = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return storedType != null && storedType == type;
    }
    
    /** Check if player is wearing any type of a specific armor piece */
    private boolean isWearingPiece(Player p, EquipmentSlot slot, NamespacedKey key) {
        ItemStack item = switch (slot) {
            case HEAD -> p.getInventory().getHelmet();
            case CHEST -> p.getInventory().getChestplate();
            case LEGS -> p.getInventory().getLeggings();
            case FEET -> p.getInventory().getBoots();
            default -> null;
        };
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
    
    /** Get dominant armor type (majority vote, Crescent wins ties) */
    private byte getDominantArmorType(Player p) {
        int crescentCount = 0, goldenCount = 0;
        
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack item = switch (slot) {
                case HEAD -> p.getInventory().getHelmet();
                case CHEST -> p.getInventory().getChestplate();
                case LEGS -> p.getInventory().getLeggings();
                case FEET -> p.getInventory().getBoots();
                default -> null;
            };
            if (item != null && item.hasItemMeta()) {
                Byte type = item.getItemMeta().getPersistentDataContainer().get(GoldenMoon.CHEST_KEY, PersistentDataType.BYTE);
                if (type != null) {
                    if (type == TYPE_CRESCENT) crescentCount++;
                    else if (type == TYPE_GOLDEN) goldenCount++;
                }
            }
        }
        return goldenCount > crescentCount ? TYPE_GOLDEN : TYPE_CRESCENT;
    }
    
    /** Check if player has full set of specific type */
    private boolean hasFullSetOfType(Player p, byte type) {
        return isWearingPieceOfType(p, EquipmentSlot.HEAD, GoldenMoon.HELMET_KEY, type) &&
               isWearingPieceOfType(p, EquipmentSlot.CHEST, GoldenMoon.CHEST_KEY, type) &&
               isWearingPieceOfType(p, EquipmentSlot.LEGS, GoldenMoon.LEGGINGS_KEY, type) &&               isWearingPieceOfType(p, EquipmentSlot.FEET, GoldenMoon.BOOTS_KEY, type);
    }

    // ==========================================
    // ✨ ARMOR SPARKLE HELPER
    // ==========================================
    private void spawnArmorSparkle(Location loc, World w, Color color, int count) {
        for (int i = 0; i < count; i++) {
            Vector spread = new Vector((r.nextDouble() - 0.5) * 0.5, r.nextDouble() * 0.6, (r.nextDouble() - 0.5) * 0.5);
            w.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(color, 1.3f));
        }
    }

    // ==========================================
    // 🔧 VECTOR ROTATION HELPER
    // ==========================================
    private Vector rotate(Vector v, double degrees) {
        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getX() * -sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }

    // ==========================================
    // ⚔️ SWORD CHECK HELPER
    // ==========================================
    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    // ==========================================
    // 💬 ACTION BAR HELPER
    // ==========================================
    private void sab(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    // ==========================================
    // 📊 PLAYER DATA CLASS
    // ==========================================
    private PlayerData get(Player p) {
        return data.computeIfAbsent(p.getUniqueId(), k -> new PlayerData());
    }

    private static class PlayerData {
        long lastSlash = 0, lastDash = 0, lastUlt = 0;        boolean berserkActive = false; // Golden helmet berserk state
    }
    }
