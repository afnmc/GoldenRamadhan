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
    
    private static final Color GOLD = Color.fromRGB(255, 215, 0);
    private static final Color WHITE = Color.fromRGB(240, 248, 255);
    private static final Color SILVER = Color.fromRGB(192, 192, 192);
    private static final Color PURPLE = Color.fromRGB(180, 140, 220);
    private static final Color CRIMSON = Color.fromRGB(220, 60, 60);

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

        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (now - d.lastSlash < 500) return;
            spawnDetailedCrescent(p);
            d.lastSlash = now;
        }

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

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        
        if (isWearingPiece(p, EquipmentSlot.HEAD, GoldenMoon.ELITE_HELMET_KEY) && 
            p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.7) {
            e.setDamage(e.getDamage() * 1.15);            if (r.nextInt(100) < 30) spawnSparkle(e.getEntity().getLocation(), p.getWorld(), CRIMSON, 3);
        }
        
        if (isWearingPiece(p, EquipmentSlot.CHEST, GoldenMoon.ARMOR_CHEST_KEY) && 
            e.getEntity() instanceof LivingEntity && !e.getEntity().equals(p)) {
            applyMoonMark((LivingEntity) e.getEntity());
        }
    }

    private void performLunarDash(Player p) {
        World w = p.getWorld();
        Location loc = p.getLocation();
        boolean isElite = plugin.getArmorManager().hasFullEliteSet(p);
        boolean hasCrescent = plugin.getArmorManager().hasCrescentSet(p);
        
        Vector dir = loc.getDirection().setY(0).normalize().multiply(1.8f);
        if (isElite) dir = dir.multiply(1.25f);
        
        p.setVelocity(dir);
        w.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.8f);
        w.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 2.0f);

        new BukkitRunnable() {
            int step = 0;
            public void run() {
                if (step > 6) { cancel(); return; }
                Location pLoc = p.getLocation().add(0, 0.8, 0);
                
                Color trailColor = isElite ? GOLD : (hasCrescent ? SILVER : WHITE);
                float trailSize = isElite ? 1.5f : 1.2f;
                int trailCount = isElite ? 6 : 3;
                
                for (double i = -1.2; i <= 1.2; i += 0.3) {
                    double arc = Math.cos(i) * 0.5;
                    Vector side = rotate(p.getLocation().getDirection(), 90).multiply(i);
                    Vector back = p.getLocation().getDirection().multiply(-arc);
                    w.spawnParticle(Particle.DUST, pLoc.clone().add(side).add(back), trailCount, new Particle.DustOptions(trailColor, trailSize));
                }
                
                if (isElite && step % 2 == 0) {
                    for (int s = 0; s < 4; s++) {
                        Vector spark = new Vector((r.nextDouble()-0.5)*0.8, r.nextDouble()*0.6, (r.nextDouble()-0.5)*0.8);
                        w.spawnParticle(Particle.DUST, pLoc.clone().add(spark), 1, new Particle.DustOptions(GOLD, 1.4f));
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    private void spawnDetailedCrescent(Player p) {
        final World w = p.getWorld();
        final Location start = p.getEyeLocation().add(p.getLocation().getDirection());
        final Vector direction = p.getLocation().getDirection().normalize();
        boolean isElite = plugin.getArmorManager().hasFullEliteSet(p);
        
        w.playSound(start, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
        if (isElite) w.playSound(start, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 2.0f);

        new BukkitRunnable() {
            int life = 0;
            public void run() {
                if (life > 12) { cancel(); return; }
                Location current = start.clone().add(direction.clone().multiply(life * 0.9));
                
                // THICKER, MORE CURVED crescent
                for (double angle = -2.0; angle <= 2.0; angle += 0.12) {
                    double curve = (angle * angle) * 0.35; // More pronounced curve
                    Vector v = rotate(direction, 90).multiply(angle * 1.2).add(direction.clone().multiply(-curve));
                    Color mainColor = isElite ? GOLD : SILVER;
                    // Multiple layers for thickness
                    w.spawnParticle(Particle.DUST, current.clone().add(v), 2, new Particle.DustOptions(mainColor, 1.6f));
                    if (life % 2 == 0) w.spawnParticle(Particle.DUST, current.clone().add(v).add(0, 0.1, 0), 1, new Particle.DustOptions(WHITE, 1.0f));
                }
                
                if (isElite && life % 3 == 0) {
                    for (double angle = -1.5; angle <= 1.5; angle += 0.25) {
                        Vector accent = rotate(direction, 90).multiply(angle * 0.9);
                        w.spawnParticle(Particle.DUST, current.clone().add(accent), 1, new Particle.DustOptions(PURPLE, 1.2f));
                    }
                }

                double hitRadius = isElite ? 1.8 : 1.3;
                for (Entity target : w.getNearbyEntities(current, hitRadius, hitRadius, hitRadius)) {
                    if (target instanceof LivingEntity && !target.equals(p)) {
                        LivingEntity le = (LivingEntity) target;
                        double baseDmg = isElite ? 8.0 : 6.0;
                        if (isWearingPiece(p, EquipmentSlot.HEAD, GoldenMoon.ELITE_HELMET_KEY) && 
                            p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.7) {
                            baseDmg *= 1.15;
                        }
                        le.damage(baseDmg, p);
                        le.setNoDamageTicks(0);
                        applyMoonMark(le);
                        w.playSound(le.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
                        Color hitColor = isElite ? GOLD : SILVER;
                        spawnSparkle(le.getLocation().add(0, 1, 0), w, hitColor, isElite ? 8 : 5);
                        cancel(); return;
                    }
                }                life++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    // ==========================================
    // 🌕 SKILL 3: LUNAR EXECUTION (REVISED PER FEEDBACK)
    // ==========================================
    private void performLunarExecution(Player p) {
        World w = p.getWorld();
        Location center = p.getLocation();
        
        // Determine armor tier
        boolean isElite = plugin.getArmorManager().hasFullEliteSet(p);
        boolean hasCrescent = plugin.getArmorManager().hasCrescentSet(p);
        int armorTier = isElite ? 2 : (hasCrescent ? 1 : 0);
        
        // Get entities in zone for dynamic moon behavior
        List<LivingEntity> targets = new ArrayList<>();
        double zoneRadius = 6.0 + (armorTier * 1.5); // Larger zone with better armor
        for (Entity en : w.getNearbyEntities(center, zoneRadius, 5, zoneRadius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                targets.add((LivingEntity) en);
            }
        }
        int entityCount = targets.size();
        
        // Dynamic moon count based on entities + armor tier
        int moonCount = Math.min(5, Math.max(3, 3 + (entityCount / 2) + armorTier));
        
        // Title based on armor tier
        String[] subtitles = {"§fMenyegel Takdir...", "§b§l🛡️ CRESCENT MODE", "§6§l⚔️ ELITE MODE"};
        p.sendTitle("§6§l✦ LUNAR EXECUTION ✦", subtitles[armorTier], 5, 30, 10);
        
        // Sound based on tier
        float soundPitch = isElite ? 0.25f : (hasCrescent ? 0.4f : 0.5f);
        w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, isElite ? 2.0f : 1.5f, soundPitch);
        if (isElite) w.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.6f, 0.9f);

        // PHASE 1: ARENA EXPANSION (Dynamic size based on entity count)
        double arenaRadius = zoneRadius * 0.8;
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t > 25) { cancel(); return; }
                float progress = (float) t / 25f;
                float currentRadius = (float) (arenaRadius * progress);
                
                Color ringColor = isElite ? GOLD : (hasCrescent ? SILVER : WHITE);
                float ringSize = isElite ? 2.2f : (hasCrescent ? 1.9f : 1.7f);                
                // Draw expanding arena ring
                for (int i = 0; i < 40; i++) {
                    double angle = Math.toRadians(i * 9 + t * 3);
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    w.spawnParticle(Particle.DUST, center.clone().add(x, 0.15, z), isElite ? 3 : 2, new Particle.DustOptions(ringColor, ringSize));
                }
                
                // Corner markers for elite
                if (isElite && t % 5 == 0) {
                    for (int corner = 0; corner < 8; corner++) {
                        double angle = Math.toRadians(corner * 45 + t * 2);
                        Vector cornerOffset = new Vector(Math.cos(angle) * currentRadius, 0.3, Math.sin(angle) * currentRadius);
                        w.spawnParticle(Particle.DUST, center.clone().add(cornerOffset), 4, new Particle.DustOptions(GOLD, 2.0f));
                    }
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        // PHASE 2: PLAYER LIFT + MOON SUMMON (Thicker, more curved crescents)
        new BukkitRunnable() {
            int liftFrame = 0;
            public void run() {
                if (liftFrame > 20) { cancel(); return; }
                
                // Lift player gradually
                float liftHeight = liftFrame * 0.35f;
                p.setVelocity(new Vector(0, 0.25, 0));
                
                // Spawn THICK, CURVED crescent moons around arena
                for (int m = 0; m < moonCount; m++) {
                    double baseAngle = Math.toRadians(m * (360.0 / moonCount) + liftFrame * 4);
                    
                    // Moon position: orbits around center at player height
                    Location moonCenter = center.clone().add(
                            Math.cos(baseAngle) * (arenaRadius * 0.7),
                            3.0 + liftHeight,
                            Math.sin(baseAngle) * (arenaRadius * 0.7)
                    );
                    
                    // Draw THICK, CURVED crescent (multiple particle layers)
                    drawThickCrescent(moonCenter, baseAngle, armorTier, w);
                    
                    // Elite: Add purple accent glow
                    if (isElite && liftFrame % 3 == 0) {
                        for (int i = 0; i < 8; i++) {
                            double angle = Math.toRadians(i * 45);
                            Vector glowOffset = new Vector(Math.cos(angle) * 1.2, 0, Math.sin(angle) * 1.2);                            w.spawnParticle(Particle.DUST, moonCenter.clone().add(glowOffset), 1, new Particle.DustOptions(PURPLE, 1.3f));
                        }
                    }
                }
                
                // Sound buildup
                if (liftFrame % 5 == 0) {
                    w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.2f + liftFrame * 0.05f);
                }
                liftFrame++;
            }
        }.runTaskTimer(plugin, 26, 1);

        // PHASE 3: MOON PINCH/SQUEEZE (Dynamic targeting)
        new BukkitRunnable() {
            int pinchFrame = 0;
            public void run() {
                if (pinchFrame > 30) {
                    // PHASE 4: SLAM DOWN FINALE
                    performSlamDown(p, center, targets, armorTier, w);
                    cancel();
                    return;
                }
                
                float progress = (float) pinchFrame / 30f;
                
                for (int m = 0; m < moonCount; m++) {
                    // Each moon targets nearest entity or center if no targets
                    Location targetLoc = center;
                    if (!targets.isEmpty()) {
                        LivingEntity nearest = targets.get(m % targets.size());
                        targetLoc = nearest.getLocation().add(0, 1.5, 0);
                    }
                    
                    // Moon moves inward (pinch motion)
                    double baseAngle = Math.toRadians(m * (360.0 / moonCount));
                    float currentRadius = (float) (arenaRadius * 0.7 * (1.0 - progress * 0.8));
                    float height = 4.0f + (float) (Math.sin(pinchFrame * 0.2) * 0.5);
                    
                    Location moonLoc = center.clone().add(
                            Math.cos(baseAngle) * currentRadius,
                            height,
                            Math.sin(baseAngle) * currentRadius
                    );
                    
                    // Draw crescent facing target
                    Vector toTarget = targetLoc.toVector().subtract(moonLoc.toVector()).normalize();
                    drawThickCrescent(moonLoc, Math.toDegrees(Math.atan2(toTarget.getZ(), toTarget.getX())), armorTier, w);
                    
                    // Damage entities in moon path                    for (LivingEntity le : targets) {
                        if (moonLoc.distance(le.getLocation()) < 2.5) {
                            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 15.0 : 8.0;
                            double dmg = baseDmg * (1.0 + armorTier * 0.15); // Scale with armor tier
                            le.damage(dmg, p);
                            le.setVelocity(new Vector(0, 0.3, 0)); // Slight lift for slam setup
                            if (dmg > 10) moonMarked.remove(le.getUniqueId());
                            
                            // Hit particles
                            Color hitColor = isElite ? GOLD : (hasCrescent ? SILVER : WHITE);
                            spawnSparkle(le.getLocation().add(0, 1.2, 0), w, hitColor, 5 + armorTier * 2);
                        }
                    }
                }
                
                // Arena pulse effect
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
    
    // Helper: Draw THICK, CURVED crescent moon
    private void drawThickCrescent(Location center, double facingAngleDeg, int armorTier, World w) {
        Color mainColor = armorTier == 2 ? GOLD : (armorTier == 1 ? SILVER : WHITE);
        float baseSize = armorTier == 2 ? 1.8f : (armorTier == 1 ? 1.5f : 1.3f);
        int layers = armorTier + 1; // More layers = thicker
        
        // Vector for crescent orientation
        Vector forward = new Vector(Math.cos(Math.toRadians(facingAngleDeg)), 0, Math.sin(Math.toRadians(facingAngleDeg)));
        Vector right = rotate(forward, 90).normalize();
        
        // Draw multiple layers for thickness
        for (int layer = 0; layer < layers; layer++) {
            float layerOffset = layer * 0.15f;
            float size = baseSize - layer * 0.2f;
            
            // Crescent arc: thicker curve
            for (double angle = -2.2; angle <= 2.2; angle += 0.10) {
                // More pronounced curve for "moon pinch" visual
                double curve = (angle * angle) * 0.45;
                Vector arcOffset = right.clone().multiply(angle * 1.3).add(forward.clone().multiply(-curve));
                                // Add layer offset perpendicular to crescent plane
                Vector layerVec = new Vector(0, layerOffset * Math.sin(angle), 0);
                
                Location particleLoc = center.clone().add(arcOffset).add(layerVec);
                w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(mainColor, size));
                
                // Inner glow layer
                if (layer == 0 && angle % 0.4 < 0.1) {
                    w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(WHITE, size * 0.7f));
                }
            }
        }
        
        // Elite: Add outer purple glow ring
        if (armorTier == 2) {
            for (double angle = -2.5; angle <= 2.5; angle += 0.3) {
                double curve = (angle * angle) * 0.5;
                Vector glowOffset = right.clone().multiply(angle * 1.5).add(forward.clone().multiply(-curve));
                w.spawnParticle(Particle.DUST, center.clone().add(glowOffset), 1, new Particle.DustOptions(PURPLE, 1.0f));
            }
        }
    }
    
    // Helper: Slam down finale
    private void performSlamDown(Player p, Location center, List<LivingEntity> targets, int armorTier, World w) {
        // Slam player down dramatically
        p.setVelocity(new Vector(0, -1.5, 0));
        
        // Impact sound
        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, armorTier == 2 ? 1.5f : 1.2f, armorTier == 2 ? 0.8f : 1.0f);
        if (armorTier == 2) w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.9f);
        
        // Massive impact burst
        Color burstColor = armorTier == 2 ? GOLD : (armorTier == 1 ? SILVER : WHITE);
        int burstCount = armorTier == 2 ? 150 : (armorTier == 1 ? 100 : 70);
        float burstSize = armorTier == 2 ? 2.5f : (armorTier == 1 ? 2.0f : 1.7f);
        
        for (int i = 0; i < burstCount; i++) {
            Vector spread = new Vector(
                    (r.nextDouble() - 0.5) * (armorTier == 2 ? 5.0 : 4.0),
                    r.nextDouble() * (armorTier == 2 ? 4.0 : 3.0),
                    (r.nextDouble() - 0.5) * (armorTier == 2 ? 5.0 : 4.0)
            );
            w.spawnParticle(Particle.DUST, center.clone().add(spread), 1, new Particle.DustOptions(burstColor, burstSize));
        }
        
        // Slam damage to all targets
        for (LivingEntity le : targets) {
            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 20.0 : 12.0;
            double dmg = baseDmg * (1.0 + armorTier * 0.2); // Elite: +40% damage            le.damage(dmg, p);
            le.setVelocity(new Vector(0, -0.5, 0)); // Slam effect
            
            // Visual feedback
            spawnSparkle(le.getLocation().add(0, 1, 0), w, armorTier == 2 ? GOLD : SILVER, 8 + armorTier * 3);
        }
        
        // Self buff based on armor tier
        try {
            if (armorTier == 2) {
                // Elite: Strength + Absorption + Heal
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 8.0));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 240, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 2, false, false));
            } else if (armorTier == 1) {
                // Crescent: Regeneration + Absorption + Heal
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 6.0));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 240, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 240, 1, false, false));
            }
            // No armor: Minor heal only
            else if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 4.0));
            }
        } catch (Exception ignored) {}
        
        // Finale particles
        new BukkitRunnable() {
            int finaleFrame = 0;
            public void run() {
                if (finaleFrame > 25) { cancel(); return; }
                
                // Rising particles around player
                for (int i = 0; i < 12 + armorTier * 4; i++) {
                    double angle = Math.toRadians(i * (360.0 / (12 + armorTier * 4)) + finaleFrame * 8);
                    Vector offset = new Vector(
                            Math.cos(angle) * (1.5 + finaleFrame * 0.12),
                            finaleFrame * 0.18,
                            Math.sin(angle) * (1.5 + finaleFrame * 0.12)
                    );
                    w.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), armorTier + 1, new Particle.DustOptions(burstColor, 1.6f + armorTier * 0.3f));
                }
                
                // Elite: Extra sparkle shower
                if (armorTier == 2 && finaleFrame % 4 == 0) {
                    for (int s = 0; s < 10; s++) {                        final int spark = s;
                        new BukkitRunnable() {
                            public void run() {
                                Vector spread = new Vector(
                                        (r.nextDouble() - 0.5) * 4.0,
                                        1.2 + r.nextDouble() * 2.5,
                                        (r.nextDouble() - 0.5) * 4.0
                                );
                                w.spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, new Particle.DustOptions(GOLD, 1.7f));
                            }
                        }.runTaskLater(plugin, spark);
                    }
                }
                finaleFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // Reset player velocity after slam
        new BukkitRunnable() {
            public void run() {
                if (p.isOnline()) {
                    p.setVelocity(new Vector(0, 0, 0));
                    p.setFallDistance(0);
                }
            }
        }.runTaskLater(plugin, 10);
    }

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

    private boolean isWearingPiece(Player p, EquipmentSlot slot, org.bukkit.NamespacedKey key) {
        ItemStack item = null;
        switch (slot) {
            case HEAD: item = p.getInventory().getHelmet(); break;
            case CHEST: item = p.getInventory().getChestplate(); break;
            case LEGS: item = p.getInventory().getLeggings(); break;            case FEET: item = p.getInventory().getBoots(); break;
        }
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private void spawnSparkle(Location loc, World w, Color color, int count) {
        for (int i = 0; i < count; i++) {
            Vector spread = new Vector((r.nextDouble() - 0.5) * 0.5, r.nextDouble() * 0.6, (r.nextDouble() - 0.5) * 0.5);
            w.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(color, 1.3f));
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
