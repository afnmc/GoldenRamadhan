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
    
    // ==========================================
    // 🎨 SKILL 1: DASH - CYAN/LIGHTNING THEME
    // ==========================================
    private static final Color DASH_PRIMARY = Color.fromRGB(0, 255, 255);
    private static final Color DASH_SECONDARY = Color.fromRGB(100, 200, 255);
    private static final Color DASH_ACCENT = Color.fromRGB(255, 255, 255);
    
    // ==========================================
    // 🎨 SKILL 2: CRESCENT - GREEN/EMERALD THEME
    // ==========================================
    private static final Color CRESCENT_PRIMARY = Color.fromRGB(50, 255, 150);
    private static final Color CRESCENT_SECONDARY = Color.fromRGB(100, 255, 200);
    private static final Color CRESCENT_ACCENT = Color.fromRGB(200, 255, 220);
        // ==========================================
    // 🎨 SKILL 3: ULTIMATE - GOLD/PURPLE THEME
    // ==========================================
    private static final Color ULT_PRIMARY = Color.fromRGB(255, 215, 0);
    private static final Color ULT_SECONDARY = Color.fromRGB(180, 140, 220);
    private static final Color ULT_ACCENT = Color.fromRGB(255, 240, 180);

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

        // =====================================================
        // ⚡ SKILL 1: THUNDER STEP DASH (CYAN/LIGHTNING)
        // =====================================================
        if (p.isSneaking() && (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            if (now - d.lastDash < getDashCooldown(p)) {
                sab(p, "§b⚡ Thunder Step: " + (getDashCooldown(p)/1000 - (now - d.lastDash)/1000) + "s");
                return;
            }
            performThunderStepDash(p);
            d.lastDash = now;
            return;
        }

        // =====================================================
        // 🌙 SKILL 2: EMERALD CRESCENT (GREEN/PROJECTILE)
        // =====================================================
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (now - d.lastSlash < getCrescentCooldown(p)) {
                sab(p, "§a🌙 Emerald Crescent: " + (getCrescentCooldown(p)/1000 - (now - d.lastSlash)/1000) + "s");
                return;            }
            spawnEmeraldCrescent(p);
            d.lastSlash = now;
            return;
        }

        // =====================================================
        // 🌕 SKILL 3: GOLDEN MOON PINCH (GOLD/PURPLE ULTIMATE)
        // =====================================================
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (now - d.lastUlt < 12000) {
                sab(p, "§6🌕 Golden Moon: " + (12 - (now - d.lastUlt)/1000) + "s");
                return;
            }
            performGoldenMoonPinch(p);
            d.lastUlt = now;
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        
        if (isWearingPiece(p, EquipmentSlot.HEAD, GoldenMoon.ELITE_HELMET_KEY) && 
            p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.7) {
            e.setDamage(e.getDamage() * 1.15);
            if (r.nextInt(100) < 30) spawnSparkle(e.getEntity().getLocation(), p.getWorld(), ULT_PRIMARY, 3);
        }
        
        if (isWearingPiece(p, EquipmentSlot.CHEST, GoldenMoon.ARMOR_CHEST_KEY) && 
            e.getEntity() instanceof LivingEntity && !e.getEntity().equals(p)) {
            applyMoonMark((LivingEntity) e.getEntity());
        }
    }

    // ==========================================
    // ⚡ SKILL 1: THUNDER STEP DASH
    // Visual: CYAN/LIGHTNING, instant teleport, electric cracks
    // ==========================================
    private void performThunderStepDash(Player p) {
        World w = p.getWorld();
        Location loc = p.getLocation();
        int armorTier = getArmorTier(p);
        
        Vector dir = loc.getDirection().setY(0).normalize();
        double dashDistance = 1.5 + armorTier * 0.5;
        
        // ⚡ SOUND: Lightning/Thunder themed        w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.8f + armorTier * 0.2f);
        w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.2f);
        
        // ⚡ VISUAL: Electric crack particles BEFORE teleport
        for (int i = 0; i < 20 + armorTier * 8; i++) {
            Vector spread = new Vector((r.nextDouble()-0.5)*1.0, r.nextDouble()*0.8, (r.nextDouble()-0.5)*1.0);
            Color crackColor = i % 3 == 0 ? DASH_ACCENT : (i % 3 == 1 ? DASH_PRIMARY : DASH_SECONDARY);
            w.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(crackColor, 1.4f + armorTier * 0.2f));
        }
        
        // ⚡ LIGHTNING BOLTS around player (elite only)
        if (armorTier == 2) {
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(i * 90);
                Vector boltOffset = new Vector(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
                w.spawnParticle(Particle.FLASH, loc.clone().add(boltOffset), 1);
            }
        }
        
        // ⚡ TELEPORT
        Location targetLoc = loc.clone().add(dir.multiply(dashDistance));
        p.teleport(targetLoc);
        
        // ⚡ VISUAL: Impact explosion with lightning spread
        for (int i = 0; i < 25 + armorTier * 10; i++) {
            Vector spread = new Vector(
                    (r.nextDouble()-0.5)*1.2,
                    r.nextDouble()*1.0,
                    (r.nextDouble()-0.5)*1.2
            );
            Color impactColor = i % 4 == 0 ? DASH_ACCENT : (i % 4 == 1 ? DASH_PRIMARY : (i % 4 == 2 ? DASH_SECONDARY : Color.fromRGB(150, 255, 255)));
            w.spawnParticle(Particle.DUST, targetLoc.clone().add(spread), 1, new Particle.DustOptions(impactColor, 1.5f + armorTier * 0.25f));
        }
        
        // ⚡ LIGHTNING TRAIL lingering effect
        new BukkitRunnable() {
            int trailStep = 0;
            public void run() {
                if (trailStep > 8) { cancel(); return; }
                
                // Electric ring expanding from player
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30 + trailStep * 15);
                    Vector ringOffset = new Vector(Math.cos(angle) * (0.5 + trailStep * 0.3), 0.1, Math.sin(angle) * (0.5 + trailStep * 0.3));
                    Color ringColor = trailStep % 2 == 0 ? DASH_PRIMARY : DASH_SECONDARY;
                    w.spawnParticle(Particle.DUST, targetLoc.clone().add(ringOffset), 1, new Particle.DustOptions(ringColor, 1.2f));
                }
                
                // Elite: Additional lightning strikes
                if (armorTier == 2 && trailStep % 3 == 0) {                    w.spawnParticle(Particle.FLASH, targetLoc.clone().add(0, 2, 0), 1);
                    w.playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 2.0f + trailStep * 0.1f);
                }
                trailStep++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // ⚡ Elite: Damage + mark entities in path
        if (armorTier == 2) {
            for (Entity en : w.getNearbyEntities(targetLoc, 2.5, 2.5, 2.5)) {
                if (en instanceof LivingEntity && !en.equals(p)) {
                    LivingEntity le = (LivingEntity) en;
                    le.damage(4.0, p);
                    applyMoonMark(le);
                    le.setVelocity(dir.clone().multiply(0.5).setY(0.4));
                    spawnSparkle(le.getLocation().add(0, 1, 0), w, DASH_PRIMARY, 8);
                }
            }
        }
        
        // ⚡ Crescent: Dash reset on marked hit
        if (armorTier >= 1) {
            for (Entity en : w.getNearbyEntities(targetLoc, 2.0, 2.0, 2.0)) {
                if (en instanceof LivingEntity && moonMarked.containsKey(en.getUniqueId())) {
                    PlayerData d = get(p);
                    d.lastDash = System.currentTimeMillis() - 1000;
                    sab(p, "§b⚡ §fThunder Step Reset!");
                    w.playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 2.3f);
                    // Visual feedback
                    for (int i = 0; i < 15; i++) {
                        Vector spark = new Vector((r.nextDouble()-0.5)*0.8, r.nextDouble()*0.7, (r.nextDouble()-0.5)*0.8);
                        w.spawnParticle(Particle.DUST, targetLoc.clone().add(spark), 1, new Particle.DustOptions(DASH_ACCENT, 1.6f));
                    }
                    break;
                }
            }
        }
    }
    
    private long getDashCooldown(Player p) {
        int tier = getArmorTier(p);
        return tier == 2 ? 1000 : (tier == 1 ? 1200 : 1500);
    }

    // ==========================================
    // 🌙 SKILL 2: EMERALD CRESCENT
    // Visual: GREEN/EMERALD, curved projectile, leaf-like trail
    // ==========================================
    private void spawnEmeraldCrescent(Player p) {
        final World w = p.getWorld();        final Location start = p.getEyeLocation().add(p.getLocation().getDirection().multiply(1.2));
        final Vector direction = p.getLocation().getDirection().normalize();
        final int armorTier = getArmorTier(p);
        
        int projectileCount = armorTier == 2 ? 3 : (armorTier == 1 ? 2 : 1);
        double speed = 0.85 + armorTier * 0.1;
        double homingStrength = armorTier * 0.035;
        double pierceCount = armorTier == 2 ? 99 : (armorTier == 1 ? 1 : 0);
        double range = 15 + armorTier * 3;
        
        // 🌙 SOUND: Nature/Wind themed
        w.playSound(start, Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.4f + armorTier * 0.15f);
        w.playSound(start, Sound.BLOCK_GRASS_BREAK, 0.4f, 1.8f);
        
        for (int proj = 0; proj < projectileCount; proj++) {
            final int projIndex = proj;
            final Vector projDir = rotate(direction, (proj - (projectileCount-1)/2) * 10);
            
            new BukkitRunnable() {
                int life = 0;
                int hits = 0;
                LivingEntity lastHit = null;
                
                public void run() {
                    if (life > range / speed || hits >= pierceCount) {
                        // 🌙 Return animation with leaf particles
                        if (armorTier >= 1 && life <= range / speed + 12) {
                            for (int i = 0; i < 4; i++) {
                                Vector returnOffset = projDir.clone().multiply(-0.35 * (life - range/speed));
                                Location leafLoc = start.clone().add(returnOffset);
                                w.spawnParticle(Particle.DUST, leafLoc, 1, new Particle.DustOptions(CRESCENT_SECONDARY, 1.0f));
                                // Leaf flutter effect
                                if (i % 2 == 0) leafLoc.add(0, Math.sin(life * 0.5) * 0.3, 0);
                                w.spawnParticle(Particle.DUST, leafLoc, 1, new Particle.DustOptions(CRESCENT_ACCENT, 0.8f));
                            }
                            life++;
                            return;
                        }
                        cancel();
                        return;
                    }
                    
                    Location current = start.clone().add(projDir.clone().multiply(life * speed));
                    
                    // 🌙 HOMING with gentle curve
                    if (homingStrength > 0 && life > 5) {
                        LivingEntity nearest = null;
                        double minDist = 8.0;
                        for (Entity en : w.getNearbyEntities(current, 6, 4, 6)) {
                            if (en instanceof LivingEntity && !en.equals(p) && en != lastHit) {                                double dist = en.getLocation().distance(current);
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
                    
                    // 🌙 Draw EMERALD CRESCENT with leaf-like shape
                    drawEmeraldCrescent(current, projDir, armorTier, w);
                    
                    // 🌙 Spiral particle trail (unique to this skill)
                    if (life % 2 == 0) {
                        for (int i = 0; i < 5 + armorTier * 2; i++) {
                            double spiralAngle = Math.toRadians(life * 20 + i * 72);
                            Vector spiralOffset = new Vector(
                                    Math.cos(spiralAngle) * 0.4,
                                    Math.sin(life * 0.3) * 0.3,
                                    Math.sin(spiralAngle) * 0.4
                            );
                            Color trailColor = i % 2 == 0 ? CRESCENT_PRIMARY : CRESCENT_SECONDARY;
                            w.spawnParticle(Particle.DUST, current.clone().add(spiralOffset), 1, new Particle.DustOptions(trailColor, 0.9f + armorTier * 0.15f));
                        }
                    }
                    
                    // Check hit
                    double hitRadius = 1.3 + armorTier * 0.3;
                    for (Entity target : w.getNearbyEntities(current, hitRadius, hitRadius, hitRadius)) {
                        if (target instanceof LivingEntity && !target.equals(p) && target != lastHit) {
                            LivingEntity le = (LivingEntity) target;
                            double baseDmg = armorTier == 2 ? 7.5 : (armorTier == 1 ? 6.0 : 4.5);
                            
                            if (moonMarked.containsKey(le.getUniqueId())) {
                                baseDmg *= 1.35;
                                moonMarked.remove(le.getUniqueId());
                                spawnSparkle(le.getLocation().add(0, 1, 0), w, CRESCENT_ACCENT, 8);
                            }
                            
                            le.damage(baseDmg, p);
                            le.setNoDamageTicks(0);
                            lastHit = le;
                            hits++;
                            
                            // 🌙 Elite: Create healing/damaging zone
                            if (armorTier == 2) {                                createEmeraldZone(le.getLocation(), w, p);
                            } 
                            // 🌙 Crescent: Chain to nearest
                            else if (armorTier == 1) {
                                chainCrescent(le, p, w, 1);
                            }
                            
                            // 🌙 Hit sound - glass/nature themed
                            w.playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.7f, 1.5f + armorTier * 0.15f);
                        }
                    }
                    life++;
                }
            }.runTaskTimer(plugin, proj * 4, 1);
        }
    }
    
    private void drawEmeraldCrescent(Location center, Vector direction, int armorTier, World w) {
        Color mainColor = armorTier == 2 ? CRESCENT_PRIMARY : (armorTier == 1 ? CRESCENT_SECONDARY : CRESCENT_ACCENT);
        float baseSize = armorTier == 2 ? 1.8f : (armorTier == 1 ? 1.5f : 1.2f);
        int layers = armorTier + 1;
        
        Vector forward = direction.clone().normalize();
        Vector right = rotate(forward, 90).normalize();
        
        // 🌙 Multiple layers for thickness (emerald cut effect)
        for (int layer = 0; layer < layers; layer++) {
            float layerOffset = layer * 0.13f;
            float size = baseSize - layer * 0.18f;
            
            // 🌙 Curved crescent with leaf-like taper
            for (double angle = -2.5; angle <= 2.5; angle += 0.12) {
                // Taper at ends for leaf shape
                double taper = 1.0 - Math.abs(angle) / 2.8;
                double curve = (angle * angle) * 0.42;
                Vector arcOffset = right.clone().multiply(angle * 1.3 * taper).add(forward.clone().multiply(-curve));
                Vector layerVec = new Vector(0, layerOffset * Math.sin(angle), 0);
                
                Location particleLoc = center.clone().add(arcOffset).add(layerVec);
                w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(mainColor, size * taper));
                
                // 🌙 Inner glow (brighter core)
                if (layer == 0 && Math.abs(angle) < 1.3) {
                    w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(CRESCENT_ACCENT, size * 0.7f * taper));
                }
            }
        }
        
        // 🌙 Elite: Outer emerald sparkle ring
        if (armorTier == 2) {            for (double angle = -2.8; angle <= 2.8; angle += 0.38) {
                double curve = (angle * angle) * 0.48;
                Vector glowOffset = right.clone().multiply(angle * 1.55).add(forward.clone().multiply(-curve));
                w.spawnParticle(Particle.DUST, center.clone().add(glowOffset), 1, new Particle.DustOptions(CRESCENT_ACCENT, 1.2f));
            }
        }
    }
    
    private void createEmeraldZone(Location center, World w, Player source) {
        // 🌙 Elite: Healing zone for allies, damage for enemies
        new BukkitRunnable() {
            int duration = 0;
            public void run() {
                if (duration > 45) { cancel(); return; }
                
                // 🌙 Zone particles - spiral pattern
                for (int i = 0; i < 10; i++) {
                    double angle = Math.toRadians(i * 36 + duration * 8);
                    double radius = 1.2 + Math.sin(duration * 0.2) * 0.3;
                    Vector offset = new Vector(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius);
                    Color zoneColor = duration % 10 < 5 ? CRESCENT_PRIMARY : CRESCENT_SECONDARY;
                    w.spawnParticle(Particle.DUST, center.clone().add(offset), 1, new Particle.DustOptions(zoneColor, 1.3f));
                }
                
                // 🌙 Damage enemies in zone
                for (Entity en : w.getNearbyEntities(center, 2.0, 1.8, 2.0)) {
                    if (en instanceof LivingEntity && !en.equals(source)) {
                        ((LivingEntity) en).damage(1.8, source);
                    }
                }
                duration++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void chainCrescent(LivingEntity from, Player source, World w, int chainDepth) {
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
            // 🌙 Chain visual - vine-like connection
            Vector chainDir = nearest.getLocation().toVector().subtract(from.getLocation().toVector()).normalize();
            for (int i = 0; i < 15; i++) {
                Location chainLoc = from.getLocation().clone().add(chainDir.clone().multiply(i * 0.35));
                // Vine curve effect
                chainLoc.add(0, Math.sin(i * 0.4) * 0.2, 0);
                Color chainColor = i % 3 == 0 ? CRESCENT_PRIMARY : (i % 3 == 1 ? CRESCENT_SECONDARY : CRESCENT_ACCENT);
                w.spawnParticle(Particle.DUST, chainLoc, 1, new Particle.DustOptions(chainColor, 1.1f));
            }
            nearest.damage(3.5, source);
            spawnSparkle(nearest.getLocation().add(0, 1, 0), w, CRESCENT_PRIMARY, 5);
            w.playSound(nearest.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.5f, 1.9f);
        }
    }
    
    private long getCrescentCooldown(Player p) {
        int tier = getArmorTier(p);
        return tier == 2 ? 400 : (tier == 1 ? 500 : 600);
    }

    // ==========================================
    // 🌕 SKILL 3: GOLDEN MOON PINCH
    // Visual: GOLD/PURPLE, celestial moons, dramatic slam
    // ==========================================
    private void performGoldenMoonPinch(Player p) {
        World w = p.getWorld();
        Location center = p.getLocation();
        int armorTier = getArmorTier(p);
        
        List<LivingEntity> targets = new ArrayList<>();
        double zoneRadius = 6.0 + armorTier * 1.5;
        for (Entity en : w.getNearbyEntities(center, zoneRadius, 5, zoneRadius)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                targets.add((LivingEntity) en);
            }
        }
        
        int moonCount = Math.min(6, Math.max(3, 3 + targets.size()/2 + armorTier));
        
        String[] subtitles = {"§fMenyegel Takdir...", "§b§l🌙 CRESCENT BLESSING", "§6§l☀️ GOLDEN DOMINION"};
        p.sendTitle("§6§l✦ LUNAR PINCH ✦", subtitles[armorTier], 5, 30, 10);
        
        // 🌕 SOUND: Celestial/Divine themed
        float soundPitch = armorTier == 2 ? 0.28f : (armorTier == 1 ? 0.45f : 0.55f);
        w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, armorTier == 2 ? 2.2f : 1.6f, soundPitch);
        w.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 0.8f + armorTier * 0.1f);
        if (armorTier == 2) w.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.85f);

        // 🌕 PHASE 1: ARENA EXPANSION (Golden ring with purple accents)        double arenaRadius = zoneRadius * 0.85;
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t > 28) { cancel(); return; }
                float progress = (float) t / 28f;
                float currentRadius = (float) (arenaRadius * progress);
                
                // 🌕 Main golden ring
                for (int i = 0; i < 45; i++) {
                    double angle = Math.toRadians(i * 8 + t * 3);
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    w.spawnParticle(Particle.DUST, center.clone().add(x, 0.18, z), armorTier == 2 ? 3 : 2, new Particle.DustOptions(ULT_PRIMARY, armorTier == 2 ? 2.3f : 1.8f));
                }
                
                // 🌕 Purple accent ring (offset)
                if (armorTier >= 1) {
                    for (int i = 0; i < 30; i++) {
                        double angle = Math.toRadians(i * 12 + t * 4 + 30);
                        double x = Math.cos(angle) * currentRadius * 0.95;
                        double z = Math.sin(angle) * currentRadius * 0.95;
                        w.spawnParticle(Particle.DUST, center.clone().add(x, 0.25, z), 1, new Particle.DustOptions(ULT_SECONDARY, 1.6f));
                    }
                }
                
                // 🌕 Elite: Ornate corner markers
                if (armorTier == 2 && t % 6 == 0) {
                    for (int corner = 0; corner < 8; corner++) {
                        double angle = Math.toRadians(corner * 45 + t * 2);
                        Vector cornerOffset = new Vector(Math.cos(angle) * currentRadius, 0.4, Math.sin(angle) * currentRadius);
                        w.spawnParticle(Particle.DUST, center.clone().add(cornerOffset), 5, new Particle.DustOptions(ULT_ACCENT, 2.2f));
                        w.spawnParticle(Particle.DUST, center.clone().add(cornerOffset), 3, new Particle.DustOptions(ULT_PRIMARY, 1.8f));
                    }
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        // 🌕 PHASE 2: PLAYER ASCENSION + MOON SUMMON
        new BukkitRunnable() {
            int liftFrame = 0;
            public void run() {
                if (liftFrame > 22) { cancel(); return; }
                
                // 🌕 Ascend player with golden trail
                if (liftFrame < 17) {
                    p.setVelocity(new Vector(0, 0.32, 0));
                    // Golden trail behind ascending player
                    if (liftFrame % 2 == 0) {                        for (int i = 0; i < 6 + armorTier * 2; i++) {
                            double angle = Math.toRadians(i * 60 + liftFrame * 10);
                            Vector trailOffset = new Vector(Math.cos(angle) * 0.6, -liftFrame * 0.15, Math.sin(angle) * 0.6);
                            w.spawnParticle(Particle.DUST, p.getLocation().clone().add(trailOffset), 1, new Particle.DustOptions(ULT_ACCENT, 1.4f));
                        }
                    }
                }
                
                // 🌕 Spawn THICK golden moons with purple glow
                for (int m = 0; m < moonCount; m++) {
                    double baseAngle = Math.toRadians(m * (360.0 / moonCount) + liftFrame * 5);
                    Location moonCenter = center.clone().add(
                            Math.cos(baseAngle) * (arenaRadius * 0.7),
                            3.5 + liftFrame * 0.28,
                            Math.sin(baseAngle) * (arenaRadius * 0.7)
                    );
                    
                    // 🌕 Draw GOLDEN MOON facing inward (different from crescent skill)
                    Vector inward = center.toVector().subtract(moonCenter.toVector()).normalize();
                    drawGoldenMoon(moonCenter, Math.toDegrees(Math.atan2(inward.getZ(), inward.getX())), armorTier, w);
                    
                    // 🌕 Elite: Purple corona effect
                    if (armorTier == 2 && liftFrame % 4 == 0) {
                        for (int i = 0; i < 12; i++) {
                            double angle = Math.toRadians(i * 30);
                            Vector coronaOffset = new Vector(Math.cos(angle) * 1.4, 0, Math.sin(angle) * 1.4);
                            w.spawnParticle(Particle.DUST, moonCenter.clone().add(coronaOffset), 1, new Particle.DustOptions(ULT_SECONDARY, 1.4f));
                        }
                    }
                }
                
                // 🌕 Sound buildup - celestial choir effect
                if (liftFrame % 6 == 0) {
                    w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, 1.3f + liftFrame * 0.06f);
                }
                liftFrame++;
            }
        }.runTaskTimer(plugin, 29, 1);

        // 🌕 PHASE 3: MOON PINCH (Converging moons)
        new BukkitRunnable() {
            int pinchFrame = 0;
            public void run() {
                if (pinchFrame > 32) {
                    performGoldenSlam(p, center, targets, armorTier, w);
                    cancel();
                    return;
                }
                
                float progress = (float) pinchFrame / 32f;                
                for (int m = 0; m < moonCount; m++) {
                    Location targetLoc = center;
                    if (!targets.isEmpty()) {
                        targetLoc = targets.get(m % targets.size()).getLocation().add(0, 1.6, 0);
                    }
                    
                    // 🌕 Pinch motion
                    double baseAngle = Math.toRadians(m * (360.0 / moonCount));
                    float currentRadius = (float) (arenaRadius * 0.7 * (1.0 - progress * 0.88));
                    float height = 4.5f + (float) (Math.sin(pinchFrame * 0.25) * 0.6);
                    
                    Location moonLoc = center.clone().add(
                            Math.cos(baseAngle) * currentRadius,
                            height,
                            Math.sin(baseAngle) * currentRadius
                    );
                    
                    Vector toTarget = targetLoc.toVector().subtract(moonLoc.toVector()).normalize();
                    drawGoldenMoon(moonLoc, Math.toDegrees(Math.atan2(toTarget.getZ(), toTarget.getX())), armorTier, w);
                    
                    // 🌕 Damage in pinch path
                    for (LivingEntity le : targets) {
                        if (moonLoc.distance(le.getLocation()) < 3.0) {
                            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 16.0 : 9.0;
                            double dmg = baseDmg * (1.0 + armorTier * 0.2);
                            le.damage(dmg, p);
                            le.setVelocity(new Vector(0, 0.4, 0));
                            if (dmg > 11) moonMarked.remove(le.getUniqueId());
                            
                            Color hitColor = armorTier == 2 ? ULT_ACCENT : (armorTier == 1 ? ULT_PRIMARY : ULT_SECONDARY);
                            spawnSparkle(le.getLocation().add(0, 1.3, 0), w, hitColor, 6 + armorTier * 2);
                        }
                    }
                }
                
                // 🌕 Arena pulse - golden waves
                if (pinchFrame % 5 == 0) {
                    float pulseRadius = (float) (arenaRadius * 0.5 * (1.0 - progress * 0.55));
                    for (int i = 0; i < 35; i++) {
                        double angle = Math.toRadians(i * 10.3 + pinchFrame * 6);
                        Vector pulseOffset = new Vector(Math.cos(angle) * pulseRadius, 0.12, Math.sin(angle) * pulseRadius);
                        Color pulseColor = i % 3 == 0 ? ULT_PRIMARY : (i % 3 == 1 ? ULT_SECONDARY : ULT_ACCENT);
                        w.spawnParticle(Particle.DUST, center.clone().add(pulseOffset), 1, new Particle.DustOptions(pulseColor, 1.5f));
                    }
                }
                pinchFrame++;
            }
        }.runTaskTimer(plugin, 52, 1);
    }    
    private void drawGoldenMoon(Location center, double facingAngleDeg, int armorTier, World w) {
        Color mainColor = armorTier == 2 ? ULT_PRIMARY : (armorTier == 1 ? ULT_ACCENT : ULT_SECONDARY);
        float baseSize = armorTier == 2 ? 2.0f : (armorTier == 1 ? 1.7f : 1.4f);
        int layers = armorTier + 1;
        
        Vector forward = new Vector(Math.cos(Math.toRadians(facingAngleDeg)), 0, Math.sin(Math.toRadians(facingAngleDeg)));
        Vector right = rotate(forward, 90).normalize();
        
        // 🌕 Multiple layers for celestial depth
        for (int layer = 0; layer < layers; layer++) {
            float layerOffset = layer * 0.16f;
            float size = baseSize - layer * 0.22f;
            
            // 🌕 Fuller, rounder moon shape (different from crescent skill)
            for (double angle = -2.6; angle <= 2.6; angle += 0.10) {
                double curve = (angle * angle) * 0.55;
                Vector arcOffset = right.clone().multiply(angle * 1.4).add(forward.clone().multiply(-curve));
                Vector layerVec = new Vector(0, layerOffset * Math.sin(angle), 0);
                
                Location particleLoc = center.clone().add(arcOffset).add(layerVec);
                w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(mainColor, size));
                
                // 🌕 Bright core
                if (layer == 0 && Math.abs(angle) < 1.4) {
                    w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(ULT_ACCENT, size * 0.85f));
                }
            }
        }
        
        // 🌕 Elite: Outer purple corona
        if (armorTier == 2) {
            for (double angle = -3.0; angle <= 3.0; angle += 0.35) {
                double curve = (angle * angle) * 0.62;
                Vector coronaOffset = right.clone().multiply(angle * 1.65).add(forward.clone().multiply(-curve));
                w.spawnParticle(Particle.DUST, center.clone().add(coronaOffset), 1, new Particle.DustOptions(ULT_SECONDARY, 1.25f));
            }
        }
    }
    
    private void performGoldenSlam(Player p, Location center, List<LivingEntity> targets, int armorTier, World w) {
        // 🌕 Slam down
        p.setVelocity(new Vector(0, -1.8, 0));
        
        // 🌕 SOUND: Massive celestial impact
        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, armorTier == 2 ? 1.8f : 1.4f, armorTier == 2 ? 0.7f : 0.9f);
        w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 0.7f + armorTier * 0.1f);
        if (armorTier == 2) w.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.8f);
        
        // 🌕 Impact burst - golden explosion with purple accents        Color burstColor = armorTier == 2 ? ULT_PRIMARY : (armorTier == 1 ? ULT_ACCENT : ULT_SECONDARY);
        int burstCount = armorTier == 2 ? 180 : (armorTier == 1 ? 120 : 90);
        float burstSize = armorTier == 2 ? 2.8f : (armorTier == 1 ? 2.2f : 1.9f);
        
        for (int i = 0; i < burstCount; i++) {
            Vector spread = new Vector(
                    (r.nextDouble() - 0.5) * (armorTier == 2 ? 6.0 : 5.0),
                    r.nextDouble() * (armorTier == 2 ? 5.0 : 4.0),
                    (r.nextDouble() - 0.5) * (armorTier == 2 ? 6.0 : 5.0)
            );
            Color burstParticleColor = i % 5 == 0 ? ULT_SECONDARY : (i % 5 == 1 ? ULT_ACCENT : burstColor);
            w.spawnParticle(Particle.DUST, center.clone().add(spread), 1, new Particle.DustOptions(burstParticleColor, burstSize));
        }
        
        // 🌕 Slam damage
        for (LivingEntity le : targets) {
            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 24.0 : 14.0;
            double dmg = baseDmg * (1.0 + armorTier * 0.25);
            le.damage(dmg, p);
            le.setVelocity(new Vector(0, -0.7, 0));
            spawnSparkle(le.getLocation().add(0, 1, 0), w, armorTier == 2 ? ULT_ACCENT : ULT_PRIMARY, 10 + armorTier * 3);
        }
        
        // 🌕 Self buff
        try {
            if (armorTier == 2) {
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 10.0));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 280, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 350, 2, false, false));
            } else if (armorTier == 1) {
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 7.0));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 280, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 280, 1, false, false));
            } else if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 5.0));
            }
        } catch (Exception ignored) {}
        
        // 🌕 Finale - ascending golden pillars
        new BukkitRunnable() {
            int finaleFrame = 0;
            public void run() {
                if (finaleFrame > 32) { cancel(); return; }
                
                for (int i = 0; i < 14 + armorTier * 5; i++) {
                    double angle = Math.toRadians(i * (360.0 / (14 + armorTier * 5)) + finaleFrame * 10);                    Vector offset = new Vector(
                            Math.cos(angle) * (1.8 + finaleFrame * 0.14),
                            finaleFrame * 0.22,
                            Math.sin(angle) * (1.8 + finaleFrame * 0.14)
                    );
                    Color finaleColor = i % 4 == 0 ? ULT_SECONDARY : (i % 4 == 1 ? ULT_ACCENT : ULT_PRIMARY);
                    w.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), armorTier + 1, new Particle.DustOptions(finaleColor, 1.8f + armorTier * 0.4f));
                }
                
                // 🌕 Elite: Golden rain finale
                if (armorTier == 2 && finaleFrame % 5 == 0) {
                    for (int s = 0; s < 15; s++) {
                        final int spark = s;
                        new BukkitRunnable() {
                            public void run() {
                                Vector spread = new Vector(
                                        (r.nextDouble() - 0.5) * 5.0,
                                        1.5 + r.nextDouble() * 3.0,
                                        (r.nextDouble() - 0.5) * 5.0
                                );
                                w.spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, new Particle.DustOptions(ULT_ACCENT, 1.9f));
                            }
                        }.runTaskLater(plugin, spark);
                    }
                }
                finaleFrame++;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        new BukkitRunnable() {
            public void run() {
                if (p.isOnline()) {
                    p.setVelocity(new Vector(0, 0, 0));
                    p.setFallDistance(0);
                }
            }
        }.runTaskLater(plugin, 15);
    }

    // ==========================================
    // 🌙 Moon Mark System (GOLD particles)
    // ==========================================
    private void applyMoonMark(LivingEntity target) {
        moonMarked.put(target.getUniqueId(), System.currentTimeMillis() + 6500);
        new BukkitRunnable() {
            int time = 0;
            public void run() {
                if (time > 130 || !target.isValid() || !moonMarked.containsKey(target.getUniqueId())) {
                    moonMarked.remove(target.getUniqueId());
                    cancel(); return;                }
                Location head = target.getLocation().add(0, 2.7, 0);
                target.getWorld().spawnParticle(Particle.DUST, head, 4, new Particle.DustOptions(ULT_PRIMARY, 1.7f));
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
            Vector spread = new Vector((r.nextDouble() - 0.5) * 0.6, r.nextDouble() * 0.7, (r.nextDouble() - 0.5) * 0.6);
            w.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(color, 1.4f));
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
        ItemStack item = p.getInventory().getItemInMainHand();        return item != null && item.hasItemMeta() && 
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
