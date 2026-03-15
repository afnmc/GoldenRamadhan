package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static final Color CYAN = Color.fromRGB(100, 220, 255);

    private final GoldenMoon plugin;
    private final Map<UUID, PlayerData> data = new HashMap<>();
    private final Map<UUID, Long> moonMarked = new HashMap<>();
    private final Random r = new Random();
    
    public SkillListener(GoldenMoon plugin) {        this.plugin = plugin;
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

        // =====================================================
        // ⚡ SKILL 1: LUNAR PHASE DASH (Mobility/Reposition)
        // =====================================================
        if (p.isSneaking() && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            if (now - d.lastDash < getDashCooldown(p)) {
                sab(p, "§cDash: " + (getDashCooldown(p)/1000 - (now - d.lastDash)/1000) + "s");
                return;
            }
            performLunarPhaseDash(p);
            d.lastDash = now;
            return;
        }

        // =====================================================
        // 🌙 SKILL 2: CRESCENT BOOMERANG (Ranged/Homing)
        // =====================================================
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (now - d.lastSlash < getCrescentCooldown(p)) {
                sab(p, "§cCrescent: " + (getCrescentCooldown(p)/1000 - (now - d.lastSlash)/1000) + "s");
                return;
            }
            spawnCrescentBoomerang(p);
            d.lastSlash = now;
            return;
        }

        // =====================================================
        // 🌕 SKILL 3: LUNAR PINCH EXECUTION (Ultimate/AOE Control)
        // =====================================================
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (now - d.lastUlt < 12000) {
                sab(p, "§cUltimate: " + (12 - (now - d.lastUlt)/1000) + "s");                return;
            }
            performLunarPinchExecution(p);
            d.lastUlt = now;
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        
        // Elite helmet berserk: +15% damage when <70% HP
        if (isWearingPiece(p, EquipmentSlot.HEAD, GoldenMoon.ELITE_HELMET_KEY) && 
            p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.7) {
            e.setDamage(e.getDamage() * 1.15);
            if (r.nextInt(100) < 30) spawnSparkle(e.getEntity().getLocation(), p.getWorld(), CRIMSON, 3);
        }
        
        // Crescent chest: Apply Moon Mark on hit (for ultimate bonus)
        if (isWearingPiece(p, EquipmentSlot.CHEST, GoldenMoon.ARMOR_CHEST_KEY) && 
            e.getEntity() instanceof LivingEntity && !e.getEntity().equals(p)) {
            applyMoonMark((LivingEntity) e.getEntity());
        }
    }

    // ==========================================
    // ⚡ SKILL 1: LUNAR PHASE DASH (Mobility)
    // ==========================================
    private void performLunarPhaseDash(Player p) {
        World w = p.getWorld();
        Location loc = p.getLocation();
        int armorTier = getArmorTier(p);
        
        Vector dir = loc.getDirection().setY(0).normalize();
        double dashDistance = 1.5 + armorTier * 0.5; // 1.5/2.0/2.5 blocks
        
        // Play dash sound based on tier
        Sound dashSound = armorTier == 2 ? Sound.ENTITY_ENDERMAN_TELEPORT : 
                         armorTier == 1 ? Sound.BLOCK_AMETHYST_BLOCK_STEP : Sound.ENTITY_PLAYER_ATTACK_SWEEP;
        w.playSound(loc, dashSound, 1.0f, armorTier == 2 ? 1.5f : 1.0f);
        
        // Visual: Phase particles BEFORE teleport
        for (int i = 0; i < 15 + armorTier * 5; i++) {
            Vector spread = new Vector((r.nextDouble()-0.5)*0.8, r.nextDouble()*0.6, (r.nextDouble()-0.5)*0.8);
            Color phaseColor = armorTier == 2 ? PURPLE : (armorTier == 1 ? SILVER : WHITE);
            w.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(phaseColor, 1.2f + armorTier * 0.2f));
        }
        
        // TELEPORT (not velocity) for instant reposition        Location targetLoc = loc.clone().add(dir.multiply(dashDistance));
        p.teleport(targetLoc);
        
        // Visual: Arrival particles
        for (int i = 0; i < 10 + armorTier * 4; i++) {
            Vector spread = new Vector((r.nextDouble()-0.5)*0.6, r.nextDouble()*0.5, (r.nextDouble()-0.5)*0.6);
            Color arrivalColor = armorTier == 2 ? GOLD : (armorTier == 1 ? CYAN : SILVER);
            w.spawnParticle(Particle.DUST, targetLoc.clone().add(spread), 1, new Particle.DustOptions(arrivalColor, 1.3f + armorTier * 0.2f));
        }
        
        // Elite bonus: Dash through entities, apply mark to all hit
        if (armorTier == 2) {
            for (Entity en : w.getNearbyEntities(targetLoc, 2.0, 2.0, 2.0)) {
                if (en instanceof LivingEntity && !en.equals(p)) {
                    LivingEntity le = (LivingEntity) en;
                    le.damage(3.0, p);
                    applyMoonMark(le);
                    le.setVelocity(dir.clone().multiply(0.4).setY(0.3));
                }
            }
            // Crimson trail after elite dash
            new BukkitRunnable() {
                int trailStep = 0;
                public void run() {
                    if (trailStep > 5) { cancel(); return; }
                    w.spawnParticle(Particle.DUST, p.getLocation().add(0, 0.5, 0), 3, new Particle.DustOptions(CRIMSON, 1.4f));
                    trailStep++;
                }
            }.runTaskTimer(plugin, 0, 2);
        }
        
        // Crescent bonus: Dash reset if hitting marked target
        if (armorTier >= 1) {
            for (Entity en : w.getNearbyEntities(targetLoc, 1.5, 1.5, 1.5)) {
                if (en instanceof LivingEntity && moonMarked.containsKey(en.getUniqueId())) {
                    PlayerData d = get(p);
                    d.lastDash = System.currentTimeMillis() - 1000; // Reset cooldown
                    sab(p, "§b✦ §fDash Reset!");
                    w.playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 2.0f);
                    break;
                }
            }
        }
    }
    
    private long getDashCooldown(Player p) {
        int tier = getArmorTier(p);
        return tier == 2 ? 1000 : (tier == 1 ? 1200 : 1500); // Elite: 1s, Crescent: 1.2s, None: 1.5s
    }
    // ==========================================
    // 🌙 SKILL 2: CRESCENT BOOMERANG (Ranged/Homing)
    // ==========================================
    private void spawnCrescentBoomerang(Player p) {
        final World w = p.getWorld();
        final Location start = p.getEyeLocation().add(p.getLocation().getDirection().multiply(1.2));
        final Vector direction = p.getLocation().getDirection().normalize();
        final int armorTier = getArmorTier(p);
        
        // Projectile properties based on tier
        int projectileCount = armorTier == 2 ? 3 : (armorTier == 1 ? 2 : 1);
        double speed = 0.9 + armorTier * 0.1;
        double homingStrength = armorTier * 0.03;
        double pierceCount = armorTier == 2 ? 99 : (armorTier == 1 ? 1 : 0);
        double range = 15 + armorTier * 3;
        
        w.playSound(start, Sound.ENTITY_ARROW_SHOOT, 0.7f, 1.2f + armorTier * 0.2f);
        
        // Spawn multiple projectiles for higher tiers
        for (int proj = 0; proj < projectileCount; proj++) {
            final int projIndex = proj;
            final Vector projDir = rotate(direction, (proj - (projectileCount-1)/2) * 8); // Spread slightly
            
            new BukkitRunnable() {
                int life = 0;
                int hits = 0;
                LivingEntity lastHit = null;
                
                public void run() {
                    if (life > range / speed || hits >= pierceCount) {
                        // Return animation (boomerang effect)
                        if (armorTier >= 1 && life <= range / speed + 10) {
                            // Return trail
                            for (int i = 0; i < 3; i++) {
                                Vector returnOffset = projDir.clone().multiply(-0.3 * (life - range/speed));
                                w.spawnParticle(Particle.DUST, start.clone().add(returnOffset), 1, new Particle.DustOptions(SILVER, 0.9f));
                            }
                            life++;
                            return;
                        }
                        cancel();
                        return;
                    }
                    
                    Location current = start.clone().add(projDir.clone().multiply(life * speed));
                    
                    // HOMING: Seek nearest unhit enemy
                    if (homingStrength > 0 && life > 5) {
                        LivingEntity nearest = null;
                        double minDist = 8.0;                        for (Entity en : w.getNearbyEntities(current, 6, 4, 6)) {
                            if (en instanceof LivingEntity && !en.equals(p) && en != lastHit) {
                                double dist = en.getLocation().distance(current);
                                if (dist < minDist) {
                                    minDist = dist;
                                    nearest = (LivingEntity) en;
                                }
                            }
                        }
                        if (nearest != null) {
                            Vector toTarget = nearest.getLocation().add(0, 1, 0).toVector().subtract(current.toVector()).normalize();
                            projDir.add(toTarget.multiply(homingStrength)).normalize();
                        }
                    }
                    
                    // Draw THICK, CURVED crescent blade
                    drawCrescentBlade(current, projDir, armorTier, w);
                    
                    // Check hit
                    double hitRadius = 1.2 + armorTier * 0.3;
                    for (Entity target : w.getNearbyEntities(current, hitRadius, hitRadius, hitRadius)) {
                        if (target instanceof LivingEntity && !target.equals(p) && target != lastHit) {
                            LivingEntity le = (LivingEntity) target;
                            double baseDmg = armorTier == 2 ? 7.0 : (armorTier == 1 ? 5.5 : 4.0);
                            
                            // Bonus damage if target is marked
                            if (moonMarked.containsKey(le.getUniqueId())) {
                                baseDmg *= 1.3;
                                moonMarked.remove(le.getUniqueId()); // Consume mark for bonus
                                spawnSparkle(le.getLocation().add(0, 1, 0), w, GOLD, 6);
                            }
                            
                            le.damage(baseDmg, p);
                            le.setNoDamageTicks(0);
                            lastHit = le;
                            hits++;
                            
                            // Crescent-specific effects
                            if (armorTier == 2) {
                                // Elite: Leave lingering damage zone
                                createLingeringZone(le.getLocation(), w, p);
                            } else if (armorTier == 1) {
                                // Crescent: Chain to nearest enemy
                                chainToNearest(le, p, w, 1);
                            }
                            
                            w.playSound(le.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.4f + armorTier * 0.2f);
                        }
                    }
                    life++;                }
            }.runTaskTimer(plugin, proj * 3, 1); // Stagger spawn for multi-projectile
        }
    }
    
    private void drawCrescentBlade(Location center, Vector direction, int armorTier, World w) {
        Color mainColor = armorTier == 2 ? GOLD : (armorTier == 1 ? SILVER : WHITE);
        float baseSize = armorTier == 2 ? 1.7f : (armorTier == 1 ? 1.4f : 1.1f);
        int layers = armorTier + 1;
        
        Vector forward = direction.clone().normalize();
        Vector right = rotate(forward, 90).normalize();
        
        // Multiple layers for thickness
        for (int layer = 0; layer < layers; layer++) {
            float layerOffset = layer * 0.12f;
            float size = baseSize - layer * 0.15f;
            
            // Curved crescent arc (MORE curved than before)
            for (double angle = -2.3; angle <= 2.3; angle += 0.11) {
                double curve = (angle * angle) * 0.48; // Pronounced curve
                Vector arcOffset = right.clone().multiply(angle * 1.25).add(forward.clone().multiply(-curve));
                Vector layerVec = new Vector(0, layerOffset * Math.sin(angle), 0);
                
                Location particleLoc = center.clone().add(arcOffset).add(layerVec);
                w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(mainColor, size));
                
                // Inner glow
                if (layer == 0 && Math.abs(angle) < 1.0) {
                    w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(WHITE, size * 0.75f));
                }
            }
        }
        
        // Elite: Purple outer glow
        if (armorTier == 2) {
            for (double angle = -2.6; angle <= 2.6; angle += 0.35) {
                double curve = (angle * angle) * 0.52;
                Vector glowOffset = right.clone().multiply(angle * 1.45).add(forward.clone().multiply(-curve));
                w.spawnParticle(Particle.DUST, center.clone().add(glowOffset), 1, new Particle.DustOptions(PURPLE, 1.1f));
            }
        }
    }
    
    private void createLingeringZone(Location center, World w, Player source) {
        // Elite crescent: Leave damaging zone for 2 seconds
        new BukkitRunnable() {
            int duration = 0;
            public void run() {
                if (duration > 40) { cancel(); return; }                
                // Zone particles
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45 + duration * 5);
                    Vector offset = new Vector(Math.cos(angle) * 1.5, 0.1, Math.sin(angle) * 1.5);
                    w.spawnParticle(Particle.DUST, center.clone().add(offset), 1, new Particle.DustOptions(PURPLE, 1.2f));
                }
                
                // Damage entities in zone
                for (Entity en : w.getNearbyEntities(center, 1.8, 1.5, 1.8)) {
                    if (en instanceof LivingEntity && !en.equals(source)) {
                        ((LivingEntity) en).damage(1.5, source);
                    }
                }
                duration++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void chainToNearest(LivingEntity from, Player source, World w, int chainDepth) {
        // Crescent: Chain to nearest enemy (max 1 chain)
        if (chainDepth > 1) return;
        
        LivingEntity nearest = null;
        double minDist = 5.0;
        for (Entity en : from.getWorld().getNearbyEntities(from.getLocation(), 5, 3, 5)) {
            if (en instanceof LivingEntity && !en.equals(source) && en != from) {
                double dist = en.getLocation().distance(from.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = (LivingEntity) en;
                }
            }
        }
        
        if (nearest != null) {
            // Chain visual
            Vector chainDir = nearest.getLocation().toVector().subtract(from.getLocation().toVector()).normalize();
            for (int i = 0; i < 12; i++) {
                Location chainLoc = from.getLocation().clone().add(chainDir.clone().multiply(i * 0.4));
                w.spawnParticle(Particle.DUST, chainLoc, 1, new Particle.DustOptions(CYAN, 1.0f));
            }
            // Chain damage
            nearest.damage(3.0, source);
            spawnSparkle(nearest.getLocation().add(0, 1, 0), w, CYAN, 4);
            w.playSound(nearest.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
        }
    }
    
    private long getCrescentCooldown(Player p) {        int tier = getArmorTier(p);
        return tier == 2 ? 400 : (tier == 1 ? 500 : 600); // Elite: 0.4s, Crescent: 0.5s, None: 0.6s
    }

    // ==========================================
    // 🌕 SKILL 3: LUNAR PINCH EXECUTION (Ultimate/AOE Control)
    // ==========================================
    private void performLunarPinchExecution(Player p) {
        World w = p.getWorld();
        Location center = p.getLocation();
        int armorTier = getArmorTier(p);
        
        // Gather targets for dynamic behavior
        List<LivingEntity> targets = new ArrayList<>();
        double zoneRadius = 6.0 + armorTier * 1.5;
        for (Entity en : w.getNearbyEntities(center, zoneRadius, 5, zoneRadius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                targets.add((LivingEntity) en);
            }
        }
        
        // Dynamic moon count: 3 base + entities/2 + armor bonus
        int moonCount = Math.min(6, Math.max(3, 3 + targets.size()/2 + armorTier));
        
        // Title & sound based on tier
        String[] subtitles = {"§fMenyegel Takdir...", "§b§l🛡️ CRESCENT MODE", "§6§l⚔️ ELITE MODE"};
        p.sendTitle("§6§l✦ LUNAR PINCH ✦", subtitles[armorTier], 5, 30, 10);
        
        float soundPitch = armorTier == 2 ? 0.25f : (armorTier == 1 ? 0.4f : 0.5f);
        w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, armorTier == 2 ? 2.0f : 1.5f, soundPitch);
        if (armorTier == 2) w.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.9f);

        // PHASE 1: ARENA EXPANSION (Visual setup)
        double arenaRadius = zoneRadius * 0.85;
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t > 25) { cancel(); return; }
                float progress = (float) t / 25f;
                float currentRadius = (float) (arenaRadius * progress);
                
                Color ringColor = armorTier == 2 ? GOLD : (armorTier == 1 ? SILVER : WHITE);
                float ringSize = armorTier == 2 ? 2.2f : (armorTier == 1 ? 1.9f : 1.7f);
                
                // Expanding arena ring
                for (int i = 0; i < 40; i++) {
                    double angle = Math.toRadians(i * 9 + t * 3);
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    w.spawnParticle(Particle.DUST, center.clone().add(x, 0.15, z), armorTier == 2 ? 3 : 2, new Particle.DustOptions(ringColor, ringSize));                }
                
                // Elite: Corner markers
                if (armorTier == 2 && t % 5 == 0) {
                    for (int corner = 0; corner < 8; corner++) {
                        double angle = Math.toRadians(corner * 45 + t * 2);
                        Vector cornerOffset = new Vector(Math.cos(angle) * currentRadius, 0.3, Math.sin(angle) * currentRadius);
                        w.spawnParticle(Particle.DUST, center.clone().add(cornerOffset), 4, new Particle.DustOptions(GOLD, 2.0f));
                    }
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        // PHASE 2: PLAYER LIFT + MOON SUMMON (Thick curved crescents)
        new BukkitRunnable() {
            int liftFrame = 0;
            public void run() {
                if (liftFrame > 20) { cancel(); return; }
                
                // Lift player
                if (liftFrame < 15) p.setVelocity(new Vector(0, 0.28, 0));
                
                // Spawn thick curved crescents
                for (int m = 0; m < moonCount; m++) {
                    double baseAngle = Math.toRadians(m * (360.0 / moonCount) + liftFrame * 4);
                    Location moonCenter = center.clone().add(
                            Math.cos(baseAngle) * (arenaRadius * 0.7),
                            3.0 + liftFrame * 0.25,
                            Math.sin(baseAngle) * (arenaRadius * 0.7)
                    );
                    
                    // Draw THICK, CURVED crescent facing inward
                    Vector inward = center.toVector().subtract(moonCenter.toVector()).normalize();
                    drawThickCrescent(moonCenter, Math.toDegrees(Math.atan2(inward.getZ(), inward.getX())), armorTier, w);
                    
                    // Elite: Purple accent glow
                    if (armorTier == 2 && liftFrame % 3 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double angle = Math.toRadians(i * 45);
                            Vector glowOffset = new Vector(Math.cos(angle) * 1.2, 0, Math.sin(angle) * 1.2);
                            w.spawnParticle(Particle.DUST, moonCenter.clone().add(glowOffset), 1, new Particle.DustOptions(PURPLE, 1.3f));
                        }
                    }
                }
                
                // Sound buildup
                if (liftFrame % 5 == 0) {
                    w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.2f + liftFrame * 0.05f);
                }                liftFrame++;
            }
        }.runTaskTimer(plugin, 26, 1);

        // PHASE 3: MOON PINCH (Dynamic targeting)
        new BukkitRunnable() {
            int pinchFrame = 0;
            public void run() {
                if (pinchFrame > 30) {
                    performSlamDown(p, center, targets, armorTier, w);
                    cancel();
                    return;
                }
                
                float progress = (float) pinchFrame / 30f;
                
                for (int m = 0; m < moonCount; m++) {
                    // Target selection: nearest entity or center
                    Location targetLoc = center;
                    if (!targets.isEmpty()) {
                        targetLoc = targets.get(m % targets.size()).getLocation().add(0, 1.5, 0);
                    }
                    
                    // Pinch motion: moons move inward
                    double baseAngle = Math.toRadians(m * (360.0 / moonCount));
                    float currentRadius = (float) (arenaRadius * 0.7 * (1.0 - progress * 0.85));
                    float height = 4.0f + (float) (Math.sin(pinchFrame * 0.2) * 0.5);
                    
                    Location moonLoc = center.clone().add(
                            Math.cos(baseAngle) * currentRadius,
                            height,
                            Math.sin(baseAngle) * currentRadius
                    );
                    
                    // Draw crescent facing target
                    Vector toTarget = targetLoc.toVector().subtract(moonLoc.toVector()).normalize();
                    drawThickCrescent(moonLoc, Math.toDegrees(Math.atan2(toTarget.getZ(), toTarget.getX())), armorTier, w);
                    
                    // Damage entities in pinch path
                    for (LivingEntity le : targets) {
                        if (moonLoc.distance(le.getLocation()) < 2.8) {
                            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 15.0 : 8.0;
                            double dmg = baseDmg * (1.0 + armorTier * 0.18);
                            le.damage(dmg, p);
                            le.setVelocity(new Vector(0, 0.35, 0));
                            if (dmg > 10) moonMarked.remove(le.getUniqueId());
                            
                            Color hitColor = armorTier == 2 ? GOLD : (armorTier == 1 ? SILVER : WHITE);
                            spawnSparkle(le.getLocation().add(0, 1.2, 0), w, hitColor, 5 + armorTier * 2);
                        }                    }
                }
                
                // Arena pulse
                if (pinchFrame % 4 == 0) {
                    float pulseRadius = (float) (arenaRadius * 0.5 * (1.0 - progress * 0.5));
                    for (int i = 0; i < 30; i++) {
                        double angle = Math.toRadians(i * 12 + pinchFrame * 5);
                        Vector pulseOffset = new Vector(Math.cos(angle) * pulseRadius, 0.1, Math.sin(angle) * pulseRadius);
                        w.spawnParticle(Particle.DUST, center.clone().add(pulseOffset), 1, new Particle.DustOptions(WHITE, 1.4f));
                    }
                }
                pinchFrame++;
            }
        }.runTaskTimer(plugin, 47, 1);
    }
    
    // Helper: Draw THICK, CURVED crescent for ultimate
    private void drawThickCrescent(Location center, double facingAngleDeg, int armorTier, World w) {
        Color mainColor = armorTier == 2 ? GOLD : (armorTier == 1 ? SILVER : WHITE);
        float baseSize = armorTier == 2 ? 1.9f : (armorTier == 1 ? 1.6f : 1.3f);
        int layers = armorTier + 1;
        
        Vector forward = new Vector(Math.cos(Math.toRadians(facingAngleDeg)), 0, Math.sin(Math.toRadians(facingAngleDeg)));
        Vector right = rotate(forward, 90).normalize();
        
        for (int layer = 0; layer < layers; layer++) {
            float layerOffset = layer * 0.14f;
            float size = baseSize - layer * 0.2f;
            
            for (double angle = -2.4; angle <= 2.4; angle += 0.09) {
                double curve = (angle * angle) * 0.52;
                Vector arcOffset = right.clone().multiply(angle * 1.35).add(forward.clone().multiply(-curve));
                Vector layerVec = new Vector(0, layerOffset * Math.sin(angle), 0);
                
                Location particleLoc = center.clone().add(arcOffset).add(layerVec);
                w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(mainColor, size));
                
                if (layer == 0 && Math.abs(angle) < 1.2) {
                    w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(WHITE, size * 0.8f));
                }
            }
        }
        
        if (armorTier == 2) {
            for (double angle = -2.7; angle <= 2.7; angle += 0.32) {
                double curve = (angle * angle) * 0.58;
                Vector glowOffset = right.clone().multiply(angle * 1.55).add(forward.clone().multiply(-curve));
                w.spawnParticle(Particle.DUST, center.clone().add(glowOffset), 1, new Particle.DustOptions(PURPLE, 1.15f));
            }        }
    }
    
    // Helper: Slam down finale
    private void performSlamDown(Player p, Location center, List<LivingEntity> targets, int armorTier, World w) {
        // Slam player down
        p.setVelocity(new Vector(0, -1.6, 0));
        
        // Impact sound
        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, armorTier == 2 ? 1.6f : 1.3f, armorTier == 2 ? 0.75f : 0.95f);
        if (armorTier == 2) w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 0.85f);
        
        // Impact burst
        Color burstColor = armorTier == 2 ? GOLD : (armorTier == 1 ? SILVER : WHITE);
        int burstCount = armorTier == 2 ? 160 : (armorTier == 1 ? 110 : 80);
        float burstSize = armorTier == 2 ? 2.6f : (armorTier == 1 ? 2.1f : 1.8f);
        
        for (int i = 0; i < burstCount; i++) {
            Vector spread = new Vector(
                    (r.nextDouble() - 0.5) * (armorTier == 2 ? 5.5 : 4.5),
                    r.nextDouble() * (armorTier == 2 ? 4.5 : 3.5),
                    (r.nextDouble() - 0.5) * (armorTier == 2 ? 5.5 : 4.5)
            );
            w.spawnParticle(Particle.DUST, center.clone().add(spread), 1, new Particle.DustOptions(burstColor, burstSize));
        }
        
        // Slam damage
        for (LivingEntity le : targets) {
            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 22.0 : 13.0;
            double dmg = baseDmg * (1.0 + armorTier * 0.22);
            le.damage(dmg, p);
            le.setVelocity(new Vector(0, -0.6, 0));
            spawnSparkle(le.getLocation().add(0, 1, 0), w, armorTier == 2 ? GOLD : SILVER, 8 + armorTier * 3);
        }
        
        // Self buff based on tier
        try {
            if (armorTier == 2) {
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 9.0));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 260, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 320, 2, false, false));
            } else if (armorTier == 1) {
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 6.5));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 260, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 260, 1, false, false));
            } else if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 4.5));
            }
        } catch (Exception ignored) {}
        
        // Finale particles
        new BukkitRunnable() {
            int finaleFrame = 0;
            public void run() {
                if (finaleFrame > 28) { cancel(); return; }
                
                for (int i = 0; i < 12 + armorTier * 4; i++) {
                    double angle = Math.toRadians(i * (360.0 / (12 + armorTier * 4)) + finaleFrame * 9);
                    Vector offset = new Vector(
                            Math.cos(angle) * (1.6 + finaleFrame * 0.13),
                            finaleFrame * 0.2,
                            Math.sin(angle) * (1.6 + finaleFrame * 0.13)
                    );
                    w.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), armorTier + 1, new Particle.DustOptions(burstColor, 1.7f + armorTier * 0.35f));
                }
                
                if (armorTier == 2 && finaleFrame % 4 == 0) {
                    for (int s = 0; s < 12; s++) {
                        final int spark = s;
                        new BukkitRunnable() {
                            public void run() {
                                Vector spread = new Vector(
                                        (r.nextDouble() - 0.5) * 4.5,
                                        1.3 + r.nextDouble() * 2.8,
                                        (r.nextDouble() - 0.5) * 4.5
                                );
                                w.spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, new Particle.DustOptions(GOLD, 1.8f));
                            }
                        }.runTaskLater(plugin, spark);
                    }
                }
                finaleFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // Reset velocity
        new BukkitRunnable() {
            public void run() {
                if (p.isOnline()) {
                    p.setVelocity(new Vector(0, 0, 0));
                    p.setFallDistance(0);
                }
            }
        }.runTaskLater(plugin, 12);
    }
    // ==========================================
    // 🌙 Moon Mark System
    // ==========================================
    private void applyMoonMark(LivingEntity target) {
        moonMarked.put(target.getUniqueId(), System.currentTimeMillis() + 6000);
        new BukkitRunnable() {
            int time = 0;
            public void run() {
                if (time > 120 || !target.isValid() || !moonMarked.containsKey(target.getUniqueId())) {
                    moonMarked.remove(target.getUniqueId());
                    cancel(); return;
                }
                Location head = target.getLocation().add(0, 2.6, 0);
                target.getWorld().spawnParticle(Particle.DUST, head, 3, new Particle.DustOptions(GOLD, 1.6f));
                time += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // ==========================================
    // 🛡️ Armor Utilities
    // ==========================================
    private int getArmorTier(Player p) {
        if (plugin.getArmorManager().hasFullEliteSet(p)) return 2;
        if (plugin.getArmorManager().hasCrescentSet(p)) return 1;
        return 0;
    }
    
    private boolean isWearingPiece(Player p, EquipmentSlot slot, org.bukkit.NamespacedKey key) {
        ItemStack item = null;
        switch (slot) {
            case HEAD: item = p.getInventory().getHelmet(); break;
            case CHEST: item = p.getInventory().getChestplate(); break;
            case LEGS: item = p.getInventory().getLeggings(); break;
            case FEET: item = p.getInventory().getBoots(); break;
        }
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    // ==========================================
    // ✨ Helpers
    // ==========================================
    private void spawnSparkle(Location loc, World w, Color color, int count) {
        for (int i = 0; i < count; i++) {
            Vector spread = new Vector((r.nextDouble() - 0.5) * 0.55, r.nextDouble() * 0.65, (r.nextDouble() - 0.5) * 0.55);
            w.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(color, 1.35f));
        }
    }
    private Vector rotate(Vector v, double degrees) {
        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getX() * -sin + v.getZ() * cos;
        return new Vector(x, v.getY(), z);
    }

    private boolean isHoldingSword(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private void sab(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    private PlayerData get(Player p) {
        return data.computeIfAbsent(p.getUniqueId(), k -> new PlayerData());
    }

    private static class PlayerData {
        long lastSlash = 0, lastDash = 0, lastUlt = 0;
    }
                                                                          }
