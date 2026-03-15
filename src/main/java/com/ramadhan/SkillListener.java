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
    private static final Color DASH_CORE = Color.fromRGB(200, 255, 255);
    
    // ==========================================
    // 🎨 SKILL 2: CRESCENT - GREEN/EMERALD THEME
    // ==========================================
    private static final Color CRESCENT_PRIMARY = Color.fromRGB(50, 255, 150);
    private static final Color CRESCENT_SECONDARY = Color.fromRGB(100, 255, 200);
    private static final Color CRESCENT_ACCENT = Color.fromRGB(200, 255, 220);    private static final Color CRESCENT_CORE = Color.fromRGB(150, 255, 180);
    
    // ==========================================
    // 🎨 SKILL 3: ULTIMATE - GOLD/PURPLE THEME
    // ==========================================
    private static final Color ULT_PRIMARY = Color.fromRGB(255, 215, 0);
    private static final Color ULT_SECONDARY = Color.fromRGB(180, 140, 220);
    private static final Color ULT_ACCENT = Color.fromRGB(255, 240, 180);
    private static final Color ULT_CORE = Color.fromRGB(255, 230, 100);

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
        if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {            if (now - d.lastSlash < getCrescentCooldown(p)) {
                sab(p, "§a🌙 Emerald Crescent: " + (getCrescentCooldown(p)/1000 - (now - d.lastSlash)/1000) + "s");
                return;
            }
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
    // ⚡ SKILL 1: THUNDER STEP DASH (ENHANCED ANIMATIONS)
    // ==========================================
    private void performThunderStepDash(Player p) {
        World w = p.getWorld();
        Location loc = p.getLocation();
        int armorTier = getArmorTier(p);
        
        Vector dir = loc.getDirection().setY(0).normalize();
        double dashDistance = 1.5 + armorTier * 0.5;        
        // ==========================================
        // 🎬 PHASE 1: CHARGE UP (0-5 ticks) - Lightning gathering
        // ==========================================
        new BukkitRunnable() {
            int chargeFrame = 0;
            public void run() {
                if (chargeFrame > 5) {
                    // ==========================================
                    // 🎬 PHASE 2: TELEPORT + BURST (tick 6)
                    // ==========================================
                    Location targetLoc = loc.clone().add(dir.multiply(dashDistance));
                    p.teleport(targetLoc);
                    
                    // Impact burst
                    w.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 1.8f + armorTier * 0.2f);
                    w.playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 2.2f);
                    
                    for (int i = 0; i < 30 + armorTier * 12; i++) {
                        Vector spread = new Vector((float)((r.nextDouble()-0.5)*1.3), (float)(r.nextDouble()*1.1), (float)((r.nextDouble()-0.5)*1.3));
                        Color impactColor = i % 5 == 0 ? DASH_ACCENT : (i % 5 == 1 ? DASH_CORE : (i % 5 == 2 ? DASH_PRIMARY : (i % 5 == 3 ? DASH_SECONDARY : DASH_ACCENT)));
                        w.spawnParticle(Particle.DUST, targetLoc.clone().add(spread), 1, new Particle.DustOptions(impactColor, 1.6f + armorTier * 0.3f));
                    }
                    
                    // ==========================================
                    // 🎬 PHASE 3: LINGERING ELECTRIC RINGS (6-20 ticks)
                    // ==========================================
                    new BukkitRunnable() {
                        int ringFrame = 0;
                        public void run() {
                            if (ringFrame > 14) { cancel(); return; }
                            
                            final float progress = (float) ringFrame / 14f;
                            final float ringRadius = 0.6f + progress * 2.0f;
                            final float alpha = 1.0f - progress;
                            
                            // Expanding electric rings
                            for (int i = 0; i < 16; i++) {
                                double angle = Math.toRadians(i * 22.5 + ringFrame * 12);
                                Vector ringOffset = new Vector((float)(Math.cos(angle) * ringRadius), 0.12f, (float)(Math.sin(angle) * ringRadius));
                                Color ringColor = ringFrame % 3 == 0 ? DASH_PRIMARY : (ringFrame % 3 == 1 ? DASH_SECONDARY : DASH_CORE);
                                w.spawnParticle(Particle.DUST, targetLoc.clone().add(ringOffset), 1, new Particle.DustOptions(ringColor, 1.3f * alpha));
                            }
                            
                            // Elite: Lightning strikes
                            if (armorTier == 2 && ringFrame % 4 == 0) {
                                w.spawnParticle(Particle.FLASH, targetLoc.clone().add(0, 2.5f, 0), 1);
                                w.playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, 2.0f + ringFrame * 0.12f);
                                for (int bolt = 0; bolt < 3; bolt++) {
                                    double boltAngle = Math.toRadians(bolt * 120 + ringFrame * 15);                                    Vector boltOffset = new Vector((float)(Math.cos(boltAngle) * 1.8), 0, (float)(Math.sin(boltAngle) * 1.8));
                                    w.spawnParticle(Particle.DUST, targetLoc.clone().add(boltOffset), 2, new Particle.DustOptions(DASH_ACCENT, 1.8f));
                                }
                            }
                            ringFrame++;
                        }
                    }.runTaskTimer(plugin, 0, 1);
                    
                    // ==========================================
                    // 🎬 PHASE 4: ELITE BONUS - DAMAGE + MARK
                    // ==========================================
                    if (armorTier == 2) {
                        for (Entity en : w.getNearbyEntities(targetLoc, 2.8, 2.8, 2.8)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                LivingEntity le = (LivingEntity) en;
                                le.damage(4.5, p);
                                applyMoonMark(le);
                                le.setVelocity(dir.clone().multiply(0.55f).setY(0.45f));
                                spawnSparkle(le.getLocation().add(0, 1, 0), w, DASH_PRIMARY, 10);
                            }
                        }
                    }
                    
                    // ==========================================
                    // 🎬 PHASE 5: CRESCENT+ BONUS - COOLDOWN RESET
                    // ==========================================
                    if (armorTier >= 1) {
                        for (Entity en : w.getNearbyEntities(targetLoc, 2.3, 2.3, 2.3)) {
                            if (en instanceof LivingEntity && moonMarked.containsKey(en.getUniqueId())) {
                                PlayerData d = get(p);
                                d.lastDash = System.currentTimeMillis() - 1000;
                                sab(p, "§b⚡ §fThunder Step Reset!");
                                w.playSound(targetLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.4f);
                                for (int i = 0; i < 20; i++) {
                                    Vector spark = new Vector((float)((r.nextDouble()-0.5)*0.9), (float)(r.nextDouble()*0.8), (float)((r.nextDouble()-0.5)*0.9));
                                    w.spawnParticle(Particle.DUST, targetLoc.clone().add(spark), 1, new Particle.DustOptions(DASH_ACCENT, 1.7f));
                                }
                                break;
                            }
                        }
                    }
                    cancel();
                    return;
                }
                
                // Charge up visuals
                final float chargeProgress = (float) chargeFrame / 5f;
                for (int i = 0; i < 8 + armorTier * 3; i++) {
                    double angle = Math.toRadians(i * 45 + chargeFrame * 20);
                    Vector chargeOffset = new Vector(                            (float)(Math.cos(angle) * (0.5 + chargeProgress)),
                            chargeProgress * 0.6f,
                            (float)(Math.sin(angle) * (0.5 + chargeProgress))
                    );
                    Color chargeColor = chargeFrame % 2 == 0 ? DASH_PRIMARY : DASH_SECONDARY;
                    w.spawnParticle(Particle.DUST, loc.clone().add(chargeOffset), 1, new Particle.DustOptions(chargeColor, 1.3f + chargeProgress));
                }
                
                // Lightning crack effects
                if (chargeFrame % 2 == 0) {
                    for (int crack = 0; crack < 5 + armorTier * 2; crack++) {
                        Vector crackOffset = new Vector(
                                (float)((r.nextDouble()-0.5) * 1.2),
                                (float)(r.nextDouble() * 0.9),
                                (float)((r.nextDouble()-0.5) * 1.2)
                        );
                        w.spawnParticle(Particle.DUST, loc.clone().add(crackOffset), 1, new Particle.DustOptions(DASH_ACCENT, 1.5f));
                    }
                }
                
                // Sound buildup
                if (chargeFrame == 3) {
                    w.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.8f);
                }
                if (chargeFrame == 5) {
                    w.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.6f + armorTier * 0.2f);
                }
                chargeFrame++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private long getDashCooldown(Player p) {
        int tier = getArmorTier(p);
        return tier == 2 ? 1000 : (tier == 1 ? 1200 : 1500);
    }

    // ==========================================
    // 🌙 SKILL 2: EMERALD CRESCENT (ENHANCED ANIMATIONS)
    // ==========================================
    private void spawnEmeraldCrescent(Player p) {
        final World w = p.getWorld();
        final Location start = p.getEyeLocation().add(p.getLocation().getDirection().multiply(1.2));
        final Vector direction = p.getLocation().getDirection().normalize();
        final int armorTier = getArmorTier(p);
        
        int projectileCount = armorTier == 2 ? 3 : (armorTier == 1 ? 2 : 1);
        double speed = 0.85 + armorTier * 0.1;
        double homingStrength = armorTier * 0.035;
        double pierceCount = armorTier == 2 ? 99 : (armorTier == 1 ? 1 : 0);        double range = 15 + armorTier * 3;
        
        // 🌙 SOUND: Nature/Wind themed
        w.playSound(start, Sound.ENTITY_ARROW_SHOOT, 0.65f, 1.4f + armorTier * 0.15f);
        w.playSound(start, Sound.BLOCK_GRASS_BREAK, 0.45f, 1.8f);
        
        for (int proj = 0; proj < projectileCount; proj++) {
            final int projIndex = proj;
            final Vector projDir = rotate(direction, (proj - (projectileCount-1)/2) * 10);
            
            new BukkitRunnable() {
                int life = 0;
                int hits = 0;
                LivingEntity lastHit = null;
                
                public void run() {
                    if (life > range / speed || hits >= pierceCount) {
                        // ==========================================
                        // 🎬 RETURN ANIMATION (Smooth fade out)
                        // ==========================================
                        if (armorTier >= 1 && life <= range / speed + 15) {
                            final float returnProgress = (float) (life - range/speed) / 15f;
                            for (int i = 0; i < 5; i++) {
                                Vector returnOffset = projDir.clone().multiply(-0.4f * returnProgress);
                                Location leafLoc = start.clone().add(returnOffset);
                                leafLoc.add(0, (float)(Math.sin(returnProgress * Math.PI) * 0.5), 0);
                                
                                final float alpha = 1.0f - returnProgress;
                                w.spawnParticle(Particle.DUST, leafLoc, 1, new Particle.DustOptions(CRESCENT_SECONDARY, 1.1f * alpha));
                                w.spawnParticle(Particle.DUST, leafLoc.clone().add(0, 0.15f, 0), 1, new Particle.DustOptions(CRESCENT_ACCENT, 0.9f * alpha));
                            }
                            life++;
                            return;
                        }
                        cancel();
                        return;
                    }
                    
                    Location current = start.clone().add(projDir.clone().multiply((float)(life * speed)));
                    
                    // ==========================================
                    // 🎬 HOMING WITH SMOOTH CURVE
                    // ==========================================
                    if (homingStrength > 0 && life > 5) {
                        LivingEntity nearest = null;
                        double minDist = 8.0;
                        for (Entity en : w.getNearbyEntities(current, 6, 4, 6)) {
                            if (en instanceof LivingEntity && !en.equals(p) && en != lastHit) {
                                double dist = en.getLocation().distance(current);
                                if (dist < minDist) {                                    minDist = dist;
                                    nearest = (LivingEntity) en;
                                }
                            }
                        }
                        if (nearest != null) {
                            Vector toTarget = nearest.getLocation().add(0, 1, 0).toVector().subtract(current.toVector()).normalize();
                            projDir.add(toTarget.multiply((float)homingStrength)).normalize();
                        }
                    }
                    
                    // ==========================================
                    // 🎬 DRAW EMERALD CRESCENT WITH ANIMATION
                    // ==========================================
                    drawEmeraldCrescentAnimated(current, projDir, armorTier, w, life);
                    
                    // ==========================================
                    // 🎬 SPIRAL TRAIL (Smooth, continuous)
                    // ==========================================
                    for (int i = 0; i < 6 + armorTier * 2; i++) {
                        double spiralAngle = Math.toRadians(life * 25 + i * 60 + projIndex * 120);
                        double spiralRadius = 0.35 + Math.sin(life * 0.4) * 0.15;
                        Vector spiralOffset = new Vector(
                                (float)(Math.cos(spiralAngle) * spiralRadius),
                                (float)(Math.sin(life * 0.35 + i) * 0.35),
                                (float)(Math.sin(spiralAngle) * spiralRadius)
                        );
                        Color trailColor = i % 3 == 0 ? CRESCENT_PRIMARY : (i % 3 == 1 ? CRESCENT_SECONDARY : CRESCENT_CORE);
                        w.spawnParticle(Particle.DUST, current.clone().add(spiralOffset), 1, new Particle.DustOptions(trailColor, 1.0f + armorTier * 0.18f));
                    }
                    
                    // ==========================================
                    // 🎬 LEAF PARTICLES (Occasional flutter)
                    // ==========================================
                    if (life % 4 == 0) {
                        for (int leaf = 0; leaf < 3 + armorTier; leaf++) {
                            double leafAngle = Math.toRadians(leaf * 120 + life * 8);
                            Vector leafOffset = new Vector(
                                    (float)(Math.cos(leafAngle) * 0.5),
                                    (float)(Math.sin(life * 0.5 + leaf) * 0.4),
                                    (float)(Math.sin(leafAngle) * 0.5)
                            );
                            w.spawnParticle(Particle.DUST, current.clone().add(leafOffset), 1, new Particle.DustOptions(CRESCENT_ACCENT, 0.9f));
                        }
                    }
                    
                    // Check hit
                    double hitRadius = 1.3 + armorTier * 0.3;
                    for (Entity target : w.getNearbyEntities(current, hitRadius, hitRadius, hitRadius)) {
                        if (target instanceof LivingEntity && !target.equals(p) && target != lastHit) {                            LivingEntity le = (LivingEntity) target;
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
                            
                            // Elite: Create healing/damaging zone
                            if (armorTier == 2) {
                                createEmeraldZone(le.getLocation(), w, p);
                            } else if (armorTier == 1) {
                                chainCrescent(le, p, w, 1);
                            }
                            
                            w.playSound(le.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.75f, 1.5f + armorTier * 0.15f);
                        }
                    }
                    life++;
                }
            }.runTaskTimer(plugin, proj * 4, 1);
        }
    }
    
    private void drawEmeraldCrescentAnimated(Location center, Vector direction, int armorTier, World w, int life) {
        Color mainColor = armorTier == 2 ? CRESCENT_PRIMARY : (armorTier == 1 ? CRESCENT_SECONDARY : CRESCENT_ACCENT);
        float baseSize = armorTier == 2 ? 1.9f : (armorTier == 1 ? 1.6f : 1.3f);
        int layers = armorTier + 1;
        
        // Pulse effect
        final float pulse = 1.0f + (float)(Math.sin(life * 0.5) * 0.15);
        
        Vector forward = direction.clone().normalize();
        Vector right = rotate(forward, 90).normalize();
        
        for (int layer = 0; layer < layers; layer++) {
            final float layerOffset = layer * 0.14f;
            final float size = (baseSize - layer * 0.2f) * pulse;
            
            for (double angle = -2.6; angle <= 2.6; angle += 0.13) {
                final double taper = 1.0 - Math.abs(angle) / 3.0;
                final double curve = (angle * angle) * 0.45;
                Vector arcOffset = right.clone().multiply((float)(angle * 1.35 * taper)).add(forward.clone().multiply((float)-curve));
                Vector layerVec = new Vector(0, (float)(layerOffset * Math.sin(angle)), 0);                
                Location particleLoc = center.clone().add(arcOffset).add(layerVec);
                w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(mainColor, size * (float)taper));
                
                if (layer == 0 && Math.abs(angle) < 1.4) {
                    w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(CRESCENT_CORE, size * 0.75f * (float)taper));
                }
            }
        }
        
        if (armorTier == 2) {
            for (double angle = -2.9; angle <= 2.9; angle += 0.4) {
                final double curve = (angle * angle) * 0.52;
                Vector glowOffset = right.clone().multiply((float)(angle * 1.6)).add(forward.clone().multiply((float)-curve));
                w.spawnParticle(Particle.DUST, center.clone().add(glowOffset), 1, new Particle.DustOptions(CRESCENT_ACCENT, 1.25f * pulse));
            }
        }
    }
    
    private void createEmeraldZone(Location center, World w, Player source) {
        new BukkitRunnable() {
            int duration = 0;
            public void run() {
                if (duration > 50) { cancel(); return; }
                
                final float progress = (float) duration / 50f;
                final float radius = 1.5f + (float)(Math.sin(duration * 0.15) * 0.4);
                
                // Spiral pattern zone
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(i * 30 + duration * 10);
                    Vector offset = new Vector((float)(Math.cos(angle) * radius), 0.18f, (float)(Math.sin(angle) * radius));
                    Color zoneColor = duration % 12 < 6 ? CRESCENT_PRIMARY : (duration % 12 < 9 ? CRESCENT_SECONDARY : CRESCENT_CORE);
                    w.spawnParticle(Particle.DUST, center.clone().add(offset), 1, new Particle.DustOptions(zoneColor, 1.35f * (1.0f - progress * 0.3f)));
                }
                
                // Rising particles from zone
                if (duration % 3 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double angle = Math.toRadians(i * 72 + duration * 5);
                        Vector riseOffset = new Vector((float)(Math.cos(angle) * radius * 0.6), (float)(duration * 0.08), (float)(Math.sin(angle) * radius * 0.6));
                        w.spawnParticle(Particle.DUST, center.clone().add(riseOffset), 1, new Particle.DustOptions(CRESCENT_ACCENT, 1.1f));
                    }
                }
                
                for (Entity en : w.getNearbyEntities(center, 2.2, 2.0, 2.2)) {
                    if (en instanceof LivingEntity && !en.equals(source)) {
                        ((LivingEntity) en).damage(2.0, source);
                    }
                }                duration++;
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
            Vector chainDir = nearest.getLocation().toVector().subtract(from.getLocation().toVector()).normalize();
            
            // Animated vine chain
            new BukkitRunnable() {
                int chainFrame = 0;
                public void run() {
                    if (chainFrame > 12) {
                        nearest.damage(3.5, source);
                        spawnSparkle(nearest.getLocation().add(0, 1, 0), w, CRESCENT_PRIMARY, 5);
                        w.playSound(nearest.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.55f, 1.9f);
                        cancel();
                        return;
                    }
                    
                    final float progress = (float) chainFrame / 12f;
                    for (int i = 0; i < 18; i++) {
                        Location chainLoc = from.getLocation().clone().add(chainDir.clone().multiply((float)(i * 0.35 * progress)));
                        chainLoc.add(0, (float)(Math.sin(i * 0.5 + chainFrame * 0.4) * 0.25 * progress), 0);
                        Color chainColor = i % 3 == 0 ? CRESCENT_PRIMARY : (i % 3 == 1 ? CRESCENT_SECONDARY : CRESCENT_CORE);
                        w.spawnParticle(Particle.DUST, chainLoc, 1, new Particle.DustOptions(chainColor, 1.15f * progress));
                    }
                    chainFrame++;
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }
    
    private long getCrescentCooldown(Player p) {
        int tier = getArmorTier(p);        return tier == 2 ? 400 : (tier == 1 ? 500 : 600);
    }

    // ==========================================
    // 🌕 SKILL 3: GOLDEN MOON PINCH (ENHANCED ANIMATIONS)
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
        
        float soundPitch = armorTier == 2 ? 0.28f : (armorTier == 1 ? 0.45f : 0.55f);
        w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, armorTier == 2 ? 2.2f : 1.6f, soundPitch);
        w.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.55f, 0.8f + armorTier * 0.1f);
        if (armorTier == 2) w.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.75f, 0.85f);

        // ==========================================
        // 🎬 PHASE 1: ARENA EXPANSION (0-28 ticks) - Smooth growth
        // ==========================================
        final double arenaRadius = zoneRadius * 0.85;
        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t > 28) { cancel(); return; }
                
                final float progress = (float) t / 28f;
                final float easeProgress = (float) (1 - Math.pow(1 - progress, 3)); // Ease-out cubic
                final float currentRadius = (float) (arenaRadius * easeProgress);
                final float alpha = 0.5f + easeProgress * 0.5f;
                
                // Main golden ring with rotation
                for (int i = 0; i < 50; i++) {
                    double angle = Math.toRadians(i * 7.2 + t * 3);
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;
                    w.spawnParticle(Particle.DUST, center.clone().add((float)x, 0.2f, (float)z), armorTier == 2 ? 3 : 2, new Particle.DustOptions(ULT_PRIMARY, (armorTier == 2 ? 2.4f : 1.9f) * alpha));
                }                
                // Purple accent ring (counter-rotating)
                if (armorTier >= 1) {
                    for (int i = 0; i < 35; i++) {
                        double angle = Math.toRadians(i * 10.3 + t * 4 + 30);
                        double x = Math.cos(angle) * currentRadius * 0.93;
                        double z = Math.sin(angle) * currentRadius * 0.93;
                        w.spawnParticle(Particle.DUST, center.clone().add((float)x, 0.28f, (float)z), 1, new Particle.DustOptions(ULT_SECONDARY, 1.7f * alpha));
                    }
                }
                
                // Elite: Ornate corner markers with pulse
                if (armorTier == 2 && t % 7 == 0) {
                    for (int corner = 0; corner < 8; corner++) {
                        double angle = Math.toRadians(corner * 45 + t * 2.5);
                        Vector cornerOffset = new Vector((float)(Math.cos(angle) * currentRadius), 0.45f, (float)(Math.sin(angle) * currentRadius));
                        final float cornerPulse = 1.0f + (float)(Math.sin(t * 0.4) * 0.2);
                        w.spawnParticle(Particle.DUST, center.clone().add(cornerOffset), 6, new Particle.DustOptions(ULT_ACCENT, 2.3f * cornerPulse));
                        w.spawnParticle(Particle.DUST, center.clone().add(cornerOffset), 4, new Particle.DustOptions(ULT_PRIMARY, 1.9f * cornerPulse));
                    }
                }
                
                // Sound buildup
                if (t % 7 == 0) {
                    w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.25f, 1.2f + t * 0.04f);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        // ==========================================
        // 🎬 PHASE 2: PLAYER ASCENSION + MOON SUMMON (29-51 ticks)
        // ==========================================
        new BukkitRunnable() {
            int liftFrame = 0;
            public void run() {
                if (liftFrame > 22) { cancel(); return; }
                
                final float liftProgress = (float) liftFrame / 22f;
                final float easeLift = (float) (1 - Math.pow(1 - liftProgress, 2)); // Ease-out quadratic
                
                // Ascend player with golden trail
                if (liftFrame < 17) {
                    p.setVelocity(new Vector(0, 0.35f * (1 - liftProgress), 0));
                    
                    if (liftFrame % 2 == 0) {
                        for (int i = 0; i < 8 + armorTier * 3; i++) {
                            double angle = Math.toRadians(i * 45 + liftFrame * 12);
                            Vector trailOffset = new Vector(
                                    (float)(Math.cos(angle) * (0.7 - liftProgress * 0.3)),                                    -liftFrame * 0.18f,
                                    (float)(Math.sin(angle) * (0.7 - liftProgress * 0.3))
                            );
                            Color trailColor = i % 3 == 0 ? ULT_PRIMARY : (i % 3 == 1 ? ULT_ACCENT : ULT_CORE);
                            w.spawnParticle(Particle.DUST, p.getLocation().clone().add(trailOffset), 1, new Particle.DustOptions(trailColor, 1.5f * (1 - liftProgress * 0.3f)));
                        }
                    }
                }
                
                // Spawn golden moons with corona animation
                for (int m = 0; m < moonCount; m++) {
                    double baseAngle = Math.toRadians(m * (360.0 / moonCount) + liftFrame * 6);
                    final float moonHeight = 3.8f + easeLift * 3.5f;
                    final float moonRadius = (float) (arenaRadius * 0.7 * (0.8 + Math.sin(liftFrame * 0.3 + m) * 0.2));
                    
                    Location moonCenter = center.clone().add(
                            (float)(Math.cos(baseAngle) * moonRadius),
                            moonHeight,
                            (float)(Math.sin(baseAngle) * moonRadius)
                    );
                    
                    Vector inward = center.toVector().subtract(moonCenter.toVector()).normalize();
                    drawGoldenMoonAnimated(moonCenter, Math.toDegrees(Math.atan2(inward.getZ(), inward.getX())), armorTier, w, liftFrame);
                    
                    // Elite: Purple corona with rotation
                    if (armorTier == 2 && liftFrame % 5 == 0) {
                        for (int i = 0; i < 16; i++) {
                            double angle = Math.toRadians(i * 22.5 + liftFrame * 8);
                            Vector coronaOffset = new Vector((float)(Math.cos(angle) * 1.5), 0, (float)(Math.sin(angle) * 1.5));
                            w.spawnParticle(Particle.DUST, moonCenter.clone().add(coronaOffset), 1, new Particle.DustOptions(ULT_SECONDARY, 1.45f));
                        }
                    }
                }
                
                if (liftFrame % 7 == 0) {
                    w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.38f, 1.35f + liftFrame * 0.07f);
                }
                liftFrame++;
            }
        }.runTaskTimer(plugin, 29, 1);

        // ==========================================
        // 🎬 PHASE 3: MOON PINCH (52-84 ticks) - Smooth convergence
        // ==========================================
        new BukkitRunnable() {
            int pinchFrame = 0;
            public void run() {
                if (pinchFrame > 32) {
                    performGoldenSlam(p, center, targets, armorTier, w);
                    cancel();                    return;
                }
                
                final float progress = (float) pinchFrame / 32f;
                final float easePinch = (float) (1 - Math.pow(1 - progress, 4)); // Ease-out quartic for dramatic slowdown
                
                for (int m = 0; m < moonCount; m++) {
                    Location targetLoc = center;
                    if (!targets.isEmpty()) {
                        targetLoc = targets.get(m % targets.size()).getLocation().add(0, 1.7f, 0);
                    }
                    
                    double baseAngle = Math.toRadians(m * (360.0 / moonCount));
                    final float currentRadius = (float) (arenaRadius * 0.7 * (1.0 - easePinch * 0.92));
                    final float height = 4.8f + (float) (Math.sin(pinchFrame * 0.28) * 0.7);
                    
                    Location moonLoc = center.clone().add(
                            (float)(Math.cos(baseAngle) * currentRadius),
                            height,
                            (float)(Math.sin(baseAngle) * currentRadius)
                    );
                    
                    Vector toTarget = targetLoc.toVector().subtract(moonLoc.toVector()).normalize();
                    drawGoldenMoonAnimated(moonLoc, Math.toDegrees(Math.atan2(toTarget.getZ(), toTarget.getX())), armorTier, w, pinchFrame + 50);
                    
                    for (LivingEntity le : targets) {
                        if (moonLoc.distance(le.getLocation()) < 3.2) {
                            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 17.0 : 10.0;
                            double dmg = baseDmg * (1.0 + armorTier * 0.22);
                            le.damage(dmg, p);
                            le.setVelocity(new Vector(0, 0.45f, 0));
                            if (dmg > 12) moonMarked.remove(le.getUniqueId());
                            
                            Color hitColor = armorTier == 2 ? ULT_ACCENT : (armorTier == 1 ? ULT_PRIMARY : ULT_SECONDARY);
                            spawnSparkle(le.getLocation().add(0, 1.4f, 0), w, hitColor, 7 + armorTier * 2);
                        }
                    }
                }
                
                // Arena pulse waves
                if (pinchFrame % 6 == 0) {
                    final float pulseProgress = (float) ((pinchFrame % 6) / 6f);
                    final float pulseRadius = (float) (arenaRadius * 0.5 * (1.0 - easePinch * 0.6));
                    for (int i = 0; i < 40; i++) {
                        double angle = Math.toRadians(i * 9 + pinchFrame * 7);
                        Vector pulseOffset = new Vector((float)(Math.cos(angle) * pulseRadius), 0.15f, (float)(Math.sin(angle) * pulseRadius));
                        Color pulseColor = i % 4 == 0 ? ULT_SECONDARY : (i % 4 == 1 ? ULT_ACCENT : (i % 4 == 2 ? ULT_PRIMARY : ULT_CORE));
                        w.spawnParticle(Particle.DUST, center.clone().add(pulseOffset), 1, new Particle.DustOptions(pulseColor, 1.55f * (1 - pulseProgress * 0.3f)));
                    }
                }                pinchFrame++;
            }
        }.runTaskTimer(plugin, 52, 1);
    }
    
    private void drawGoldenMoonAnimated(Location center, double facingAngleDeg, int armorTier, World w, int life) {
        Color mainColor = armorTier == 2 ? ULT_PRIMARY : (armorTier == 1 ? ULT_ACCENT : ULT_SECONDARY);
        float baseSize = armorTier == 2 ? 2.1f : (armorTier == 1 ? 1.8f : 1.5f);
        int layers = armorTier + 1;
        
        final float pulse = 1.0f + (float)(Math.sin(life * 0.4) * 0.18);
        final float rotation = (float) (life * 0.05);
        
        Vector forward = new Vector((float)Math.cos(Math.toRadians(facingAngleDeg)), 0, (float)Math.sin(Math.toRadians(facingAngleDeg)));
        Vector right = rotate(forward, 90).normalize();
        
        for (int layer = 0; layer < layers; layer++) {
            final float layerOffset = layer * 0.17f;
            final float size = (baseSize - layer * 0.24f) * pulse;
            
            for (double angle = -2.7; angle <= 2.7; angle += 0.11) {
                final double curve = (angle * angle) * 0.58;
                Vector arcOffset = right.clone().multiply((float)(angle * 1.45)).add(forward.clone().multiply((float)-curve));
                Vector layerVec = new Vector(0, (float)(layerOffset * Math.sin(angle + rotation)), 0);
                
                Location particleLoc = center.clone().add(arcOffset).add(layerVec);
                w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(mainColor, size));
                
                if (layer == 0 && Math.abs(angle) < 1.5) {
                    w.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(ULT_CORE, size * 0.88f));
                }
            }
        }
        
        if (armorTier == 2) {
            for (double angle = -3.1; angle <= 3.1; angle += 0.38) {
                final double curve = (angle * angle) * 0.65;
                Vector coronaOffset = right.clone().multiply((float)(angle * 1.7)).add(forward.clone().multiply((float)-curve));
                w.spawnParticle(Particle.DUST, center.clone().add(coronaOffset), 1, new Particle.DustOptions(ULT_SECONDARY, 1.3f * pulse));
            }
        }
    }
    
    private void performGoldenSlam(Player p, Location center, List<LivingEntity> targets, int armorTier, World w) {
        p.setVelocity(new Vector(0, -2.0f, 0));
        
        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, armorTier == 2 ? 2.0f : 1.5f, armorTier == 2 ? 0.65f : 0.85f);
        w.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 0.75f + armorTier * 0.1f);
        if (armorTier == 2) w.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.85f, 0.75f);
                Color burstColor = armorTier == 2 ? ULT_PRIMARY : (armorTier == 1 ? ULT_ACCENT : ULT_SECONDARY);
        int burstCount = armorTier == 2 ? 200 : (armorTier == 1 ? 130 : 100);
        float burstSize = armorTier == 2 ? 3.0f : (armorTier == 1 ? 2.3f : 2.0f);
        
        for (int i = 0; i < burstCount; i++) {
            Vector spread = new Vector(
                    (float)((r.nextDouble() - 0.5) * (armorTier == 2 ? 6.5 : 5.5)),
                    (float)(r.nextDouble() * (armorTier == 2 ? 5.5 : 4.5)),
                    (float)((r.nextDouble() - 0.5) * (armorTier == 2 ? 6.5 : 5.5))
            );
            Color burstParticleColor = i % 6 == 0 ? ULT_SECONDARY : (i % 6 == 1 ? ULT_ACCENT : (i % 6 == 2 ? ULT_CORE : burstColor));
            w.spawnParticle(Particle.DUST, center.clone().add(spread), 1, new Particle.DustOptions(burstParticleColor, burstSize));
        }
        
        for (LivingEntity le : targets) {
            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 26.0 : 15.0;
            double dmg = baseDmg * (1.0 + armorTier * 0.28);
            le.damage(dmg, p);
            le.setVelocity(new Vector(0, -0.75f, 0));
            spawnSparkle(le.getLocation().add(0, 1, 0), w, armorTier == 2 ? ULT_ACCENT : ULT_PRIMARY, 12 + armorTier * 3);
        }
        
        try {
            if (armorTier == 2) {
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 11.0));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 380, 2, false, false));
            } else if (armorTier == 1) {
                if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                    p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 7.5));
                }
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 300, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 1, false, false));
            } else if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 5.5));
            }
        } catch (Exception ignored) {}
        
        new BukkitRunnable() {
            int finaleFrame = 0;
            public void run() {
                if (finaleFrame > 35) { cancel(); return; }
                
                final float finaleProgress = (float) finaleFrame / 35f;
                final float easeFinale = (float) (1 - Math.pow(1 - finaleProgress, 2));
                
                for (int i = 0; i < 16 + armorTier * 6; i++) {
                    double angle = Math.toRadians(i * (360.0 / (16 + armorTier * 6)) + finaleFrame * 11);                    Vector offset = new Vector(
                            (float)(Math.cos(angle) * (2.0 + easeFinale * 2.5)),
                            easeFinale * 3.5f,
                            (float)(Math.sin(angle) * (2.0 + easeFinale * 2.5))
                    );
                    Color finaleColor = i % 5 == 0 ? ULT_SECONDARY : (i % 5 == 1 ? ULT_ACCENT : (i % 5 == 2 ? ULT_CORE : ULT_PRIMARY));
                    w.spawnParticle(Particle.DUST, p.getLocation().clone().add(offset), armorTier + 1, new Particle.DustOptions(finaleColor, 1.9f + armorTier * 0.45f));
                }
                
                if (armorTier == 2 && finaleFrame % 6 == 0) {
                    for (int s = 0; s < 18; s++) {
                        final int spark = s;
                        new BukkitRunnable() {
                            public void run() {
                                Vector spread = new Vector(
                                        (float)((r.nextDouble() - 0.5) * 5.5),
                                        1.7f + (float)(r.nextDouble() * 3.5),
                                        (float)((r.nextDouble() - 0.5) * 5.5)
                                );
                                w.spawnParticle(Particle.DUST, p.getLocation().clone().add(spread), 1, new Particle.DustOptions(ULT_ACCENT, 2.0f));
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
        }.runTaskLater(plugin, 18);
    }

    // ==========================================
    // 🌙 Moon Mark System (GOLD particles)
    // ==========================================
    private void applyMoonMark(LivingEntity target) {
        moonMarked.put(target.getUniqueId(), System.currentTimeMillis() + 7000);
        new BukkitRunnable() {
            int time = 0;
            public void run() {
                if (time > 140 || !target.isValid() || !moonMarked.containsKey(target.getUniqueId())) {
                    moonMarked.remove(target.getUniqueId());
                    cancel(); return;
                }                Location head = target.getLocation().add(0, 2.8f, 0);
                final float pulse = 1.0f + (float)(Math.sin(time * 0.3) * 0.2);
                target.getWorld().spawnParticle(Particle.DUST, head, 5, new Particle.DustOptions(ULT_PRIMARY, 1.8f * pulse));
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
            Vector spread = new Vector((float)((r.nextDouble() - 0.5) * 0.65), (float)(r.nextDouble() * 0.75), (float)((r.nextDouble() - 0.5) * 0.65));
            w.spawnParticle(Particle.DUST, loc.clone().add(spread), 1, new Particle.DustOptions(color, 1.45f));
        }
    }

    private Vector rotate(Vector v, double degrees) {
        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos + v.getZ() * sin;
        double z = v.getX() * -sin + v.getZ() * cos;
        return new Vector((float)x, v.getY(), (float)z);
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
