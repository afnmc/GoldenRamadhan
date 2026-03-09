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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * ============================================================================
 * GOLDEN MOON - SKILL LISTENER (v2.1 Final Fixed)
 * Compatible: Minecraft 1.21.8 - 1.21.10
 * 
 * 🎭 GOLDEN DOMAIN (Ultimate):
 * - Trigger: Stack 5 + Tahan Right Click (1.5 detik = 100%)
 * - Arena: Hexagon mengambang, ukuran sesuai % charge, berputar pelan
 * - Debuff: SLOWNESS 255 (freeze) + DARKNESS ke SEMUA entity (termasuk user)
 * - Visual: Cahaya dari atas → Player TP mundur → Pedang raksasa turun → Impact elegan
 * - Damage: Scaling 10-35 HP berdasarkan % charge
 * ============================================================================
 */
public class SkillListener implements Listener {
    
    // 🔧 CONFIG: GOLDEN DOMAIN SETTINGS
    private static final double DOMAIN_MIN_RANGE = 4.0;
    private static final double DOMAIN_MAX_RANGE = 10.0;
    private static final int DOMAIN_ROTATION_SPEED = 2;
    private static final double DOMAIN_HEIGHT = 2.5;
    private static final int CHARGE_TICKS_TO_FULL = 30;
    private static final double SWORD_DESCENT_SPEED = 0.6;
    private static final double SWORD_SIZE = 4.0;
    private static final int DOMAIN_FREEZE_DURATION = 60;
    private static final int DOMAIN_DARKNESS_DURATION = 80;
    
    // 🔧 CONFIG: ANIME BLINK SETTINGS
    private static final double BLINK_HORIZONTAL_SPREAD = 2.2;
    private static final double BLINK_VERTICAL_BOOST = 1.1;
    private static final int BLINK_DAMAGE = 8;
    private static final int BLINK_MAX_TARGETS = 3;
    
    // 🔧 CONFIG: MAJU MUNDUR SETTINGS
    private static final double MM_BACKWARD_MULTIPLIER = -2.0;
    private static final double MM_FORWARD_MULTIPLIER = 3.5;
    private static final double MM_DAMAGE = 12.0;
    private static final int MM_DELAY_TICKS = 8;
    
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> chargeStack = new HashMap<>();
    private final Map<UUID, Long> clickHoldStart = new HashMap<>();
    private final Set<UUID> domainAffected = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> blinkProtected = Collections.synchronizedSet(new HashSet<>());

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    // ========================================================================
    // EVENT: FALL DAMAGE PROTECTION
    // ========================================================================
    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        
        UUID id = e.getEntity().getUniqueId();
        if (blinkProtected.contains(id) || domainAffected.contains(id)) {
            e.setCancelled(true);
            e.getEntity().setFallDistance(0);
        }
    }

    // ========================================================================
    // EVENT: COMBAT / SKILL TRIGGER
    // ========================================================================
    @EventHandler
    public void onLunamCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID uuid = p.getUniqueId();
        int stack = chargeStack.getOrDefault(uuid, 0);

        // ── STACKING SYSTEM ──
        if (stack < 5) {
            stack++;
            chargeStack.put(uuid, stack);
            sendActionBar(p, "§e§l✦ Golden Stack: §f" + stack + "§7/§f5");
            if (stack == 5) {
                p.sendTitle("§f§l⚡", "§eTahan Right Click untuk Domain", 5, 50, 10);
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 2f);
            }
        }

        // ── SKILL 2: MAJU MUNDUR ──
        if (p.isSneaking() && stack == 3) {
            executeMajuMundur(p);
            chargeStack.put(uuid, 0);
            sendActionBar(p, "§b§l↯ Maju Mundur Activated!");
            return;
        }

        // ── SKILL 1: ANIME BLINK ──
        if (p.isSneaking() && stack < 3) {
            executeAnimeBlink(p, target);
            sendActionBar(p, "§d§l✦ Anime Blink!");
            return;
        }
    }

    // ========================================================================
    // EVENT: HOLD RIGHT CLICK FOR ULTIMATE
    // ========================================================================
    @EventHandler
    public void onHoldUltimate(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || chargeStack.getOrDefault(p.getUniqueId(), 0) < 5) return;

        Action action = e.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            
            UUID uuid = p.getUniqueId();
            if (clickHoldStart.containsKey(uuid)) return;
            
            clickHoldStart.put(uuid, System.currentTimeMillis());
            startChargingDomain(p);
            sendActionBar(p, "§f§l✦ Charging Domain... §7[§e0%§7]");
        }
    }

    // ========================================================================
    // CORE: CHARGING DOMAIN MECHANIC
    // ========================================================================
    private void startChargingDomain(Player p) {
        UUID uuid = p.getUniqueId();
        long startTime = clickHoldStart.get(uuid);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isHolding(p) || !p.isOnline() || !clickHoldStart.containsKey(uuid)) {
                    finishCharging(p, startTime, false);
                    this.cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                int progress = (int) Math.min((elapsed * 100L) / (CHARGE_TICKS_TO_FULL * 50L), 100);
                
                // ── VISUAL: Partikel menyedot ke pusat ──
                // FIX #328: Gunakan constructor baru, hindari chaining mutator
                Location playerLoc = p.getLocation();
                Location center = new Location(
                    playerLoc.getWorld(),
                    playerLoc.getX(),
                    playerLoc.getY() + 1.2,
                    playerLoc.getZ()
                );
                
                double radius = 3.0 - (2.5 * (progress / 100.0));
                
                for (int i = 0; i < 5; i++) {
                    double angle = (Math.PI * 2 / 5) * i + (elapsed / 200.0);
                    
                    // FIX: Buat Location baru tanpa chaining add()
                    double partX = center.getX() + Math.cos(angle) * radius;
                    double partY = center.getY() + (Math.sin(elapsed / 100.0 + i) * 0.8);
                    double partZ = center.getZ() + Math.sin(angle) * radius;
                    Location partLoc = new Location(p.getWorld(), partX, partY, partZ);
                    
                    p.getWorld().spawnParticle(Particle.DUST, partLoc, 1, 
                        new Particle.DustOptions(Color.fromRGB(100, 200, 255), 1.2f));
                    
                    if (progress > 50) {
                        p.getWorld().spawnParticle(Particle.FLASH, partLoc, 0);
                    }
                }
                
                if (progress % 25 == 0 && progress > 0) {
                    p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_STEP, 0.5f, 1f + (progress/50f));
                }
                
                sendActionBar(p, "§f§l✦ Charging Domain: §b" + progress + "%");
                
                if (progress >= 100) {
                    executeGoldenDomain(p, 100);
                    clickHoldStart.remove(uuid);
                    chargeStack.put(uuid, 0);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void finishCharging(Player p, long startTime, boolean success) {
        UUID uuid = p.getUniqueId();
        if (!clickHoldStart.containsKey(uuid)) return;
        
        long held = System.currentTimeMillis() - startTime;
        int progress = (int) Math.min((held * 100L) / (CHARGE_TICKS_TO_FULL * 50L), 100);
        
        if (progress >= 30 && success) {
            executeGoldenDomain(p, progress);
            chargeStack.put(uuid, 0);
        } else {
            sendActionBar(p, "§c✦ Charging cancelled!");
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.5f);
        }
        clickHoldStart.remove(uuid);
    }

    // ========================================================================
    // ⚡ ULTIMATE: GOLDEN DOMAIN
    // ========================================================================
    private void executeGoldenDomain(Player p, int progress) {
        double range = DOMAIN_MIN_RANGE + ((DOMAIN_MAX_RANGE - DOMAIN_MIN_RANGE) * (progress / 100.0));
        Location center = p.getLocation().clone();
        UUID uuid = p.getUniqueId();
        
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 2f, 1.5f);
        sendActionBar(p, "§f§l✦ §bGOLDEN DOMAIN §f§l✦ §7[" + progress + "%]");

        BukkitRunnable domainTask = new BukkitRunnable() {
            int ticks = 0;
            final int duration = 40;
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    // ── FASE 2: PLAYER TP MUNDUR ──
                    // FIX #382: Hindari chaining Vector mutator
                    Vector dir = p.getLocation().getDirection().clone();
                    dir.setY(0);
                    dir.normalize();
                    
                    // FIX: Buat safeSpot dengan constructor, hindari chaining add()
                    Location playerLoc = p.getLocation();
                    Vector offset = dir.clone().multiply(-6);
                    Location safeSpot = new Location(
                        playerLoc.getWorld(),
                        playerLoc.getX() + offset.getX(),
                        playerLoc.getY() + 1,
                        playerLoc.getZ() + offset.getZ()
                    );
                    
                    p.teleport(safeSpot);
                    p.playSound(safeSpot, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
                    
                    startSwordStrike(center, range, progress, p);
                    this.cancel();
                    return;
                }
                
                double rotation = ticks * DOMAIN_ROTATION_SPEED;
                
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    
                    double cornerX = center.getX() + Math.cos(angle) * range;
                    double cornerY = DOMAIN_HEIGHT + (Math.sin(ticks / 4.0 + i) * 0.3);
                    double cornerZ = center.getZ() + Math.sin(angle) * range;
                    Location corner = new Location(p.getWorld(), cornerX, cornerY, cornerZ);
                    
                    Color hexColor = Color.fromRGB(
                        150 + (int)(50 * Math.sin(ticks/5.0 + i)),
                        200 + (int)(30 * Math.cos(ticks/7.0)),
                        255 - (int)(100 * (progress/100.0))
                    );
                    p.getWorld().spawnParticle(Particle.DUST, corner, 2, 
                        new Particle.DustOptions(hexColor, 2.0f));
                    
                    if (i % 2 == 0) {
                        double nextAngle = angle + Math.toRadians(60);
                        double nextX = center.getX() + Math.cos(nextAngle) * range;
                        double nextZ = center.getZ() + Math.sin(nextAngle) * range;
                        Location next = new Location(p.getWorld(), nextX, DOMAIN_HEIGHT, nextZ);
                        
                        drawLine(p.getWorld(), corner, next, Particle.DUST, new Particle.DustOptions(Color.WHITE, 0.8f), 0.6);
                    }
                }
                
                Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, range, range, range);
                for (Entity en : nearby) {
                    if (en instanceof LivingEntity le) {
                        domainAffected.add(le.getUniqueId());
                        
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS, 
                            DOMAIN_FREEZE_DURATION, 
                            255, 
                            false, false
                        ));
                        
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.DARKNESS, 
                            DOMAIN_DARKNESS_DURATION, 
                            0, 
                            false, false
                        ));
                        
                        if (progress > 70 && ticks % 5 == 0) {
                            Location headLoc = le.getLocation().clone();
                            headLoc.setY(headLoc.getY() + 2);
                            le.getWorld().spawnParticle(Particle.DUST, headLoc, 1,
                                new Particle.DustOptions(Color.fromRGB(80, 120, 255), 1f));
                        }
                    }
                }
                
                if (ticks % 3 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.toRadians(i * 120 + ticks * 2);
                        
                        double beamX = center.getX() + Math.cos(angle) * (range * 0.7);
                        double beamZ = center.getZ() + Math.sin(angle) * (range * 0.7);
                        Location beamTop = new Location(p.getWorld(), beamX, 15, beamZ);
                        
                        for (double y = 15; y > DOMAIN_HEIGHT; y -= 1.5) {
                            Location beamPart = new Location(p.getWorld(), beamX, y, beamZ);
                            p.getWorld().spawnParticle(Particle.DUST, beamPart, 0,
                                new Particle.DustOptions(Color.fromRGB(200, 230, 255), 2.5f));
                        }
                    }
                }
                
                ticks++;
            }
        };
        domainTask.runTaskTimer(plugin, 0L, 2L);

        new BukkitRunnable() {
            @Override
            public void run() {
                domainAffected.remove(uuid);
                Iterator<UUID> it = domainAffected.iterator();
                while (it.hasNext()) {
                    Entity en = Bukkit.getEntity(it.next());
                    if (en == null || !en.isValid() || en.isDead()) {
                        it.remove();
                    }
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    // ========================================================================
    // ⚔️ SWORD STRIKE ANIMATION
    // ========================================================================
    private void startSwordStrike(Location center, double range, int progress, Player caster) {
        caster.getWorld().playSound(center, Sound.ENTITY_WARDEN_SNIFF, 1.5f, 0.3f);
        
        new BukkitRunnable() {
            int frame = 0;
            final int descentFrames = (int) (20 / SWORD_DESCENT_SPEED);
            
            @Override
            public void run() {
                if (frame > descentFrames + 15) {
                    triggerSwordImpact(center, range, progress, caster);
                    this.cancel();
                    return;
                }
                
                double swordY = 25 - (frame * SWORD_DESCENT_SPEED);
                
                if (frame <= descentFrames) {
                    Location swordBase = new Location(caster.getWorld(), center.getX(), swordY, center.getZ());
                    drawSwordBlade(caster.getWorld(), swordBase, SWORD_SIZE, frame);
                    
                    if (frame > descentFrames - 10) {
                        double pressureRadius = range * (1.2 - ((frame - (descentFrames-10)) * 0.02));
                        for (int i = 0; i < 8; i++) {
                            double angle = (Math.PI * 2 / 8) * i;
                            Location pressure = new Location(
                                caster.getWorld(),
                                center.getX() + Math.cos(angle) * pressureRadius,
                                1,
                                center.getZ() + Math.sin(angle) * pressureRadius
                            );
                            caster.getWorld().spawnParticle(Particle.CLOUD, pressure, 1, 0.3, 0.1, 0.3, 0.05);
                        }
                    }
                    
                    // FIX: Ganti sound invalid _RESONATE → _HIT
                    if (frame % 3 == 0) {
                        caster.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 
                            0.4f, 0.5f + (frame / 20f));
                    }
                } else {
                    if ((frame - descentFrames) % 4 == 0) {
                        for (int i = 0; i < 12; i++) {
                            double angle = Math.random() * Math.PI * 2;
                            Location residual = new Location(
                                caster.getWorld(),
                                center.getX() + Math.cos(angle) * (Math.random() * range),
                                1 + Math.random() * 2,
                                center.getZ() + Math.sin(angle) * (Math.random() * range)
                            );
                            caster.getWorld().spawnParticle(Particle.DUST, residual, 0,
                                new Particle.DustOptions(Color.fromRGB(255, 220, 100), 1.5f));
                        }
                    }
                }
                
                frame++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // FIX: Tambah parameter World
    private void drawSwordBlade(World world, Location base, double size, int animationProgress) {
        for (double y = 0; y < size * 2; y += 0.3) {
            Location bladePart = new Location(world, base.getX(), base.getY() - y, base.getZ());
            Color bladeColor = Color.fromRGB(
                255,
                230 - (int)(50 * (y / (size*2))),
                180 - (int)(80 * (y / (size*2)))
            );
            // FIX: Cast explicit ke float
            world.spawnParticle(Particle.DUST, bladePart, 1,
                new Particle.DustOptions(bladeColor, (float)(2.8 - (y * 0.1))));
        }
        
        Location guard = new Location(world, base.getX(), base.getY() - size * 2, base.getZ());
        for (double x = -size/2; x <= size/2; x += 0.4) {
            world.spawnParticle(Particle.DUST, new Location(world, guard.getX() + x, guard.getY(), guard.getZ()), 2,
                new Particle.DustOptions(Color.fromRGB(255, 200, 50), 2.2f));
            world.spawnParticle(Particle.DUST, new Location(world, guard.getX(), guard.getY(), guard.getZ() + x), 2,
                new Particle.DustOptions(Color.fromRGB(255, 200, 50), 2.2f));
        }
        
        Location hilt = new Location(world, guard.getX(), guard.getY() - 0.5, guard.getZ());
        world.spawnParticle(Particle.DUST, hilt, 4,
            new Particle.DustOptions(Color.fromRGB(139, 69, 19), 1.8f));
            
        if (animationProgress > 20) {
            world.spawnParticle(Particle.FLASH, base, 0);
            world.spawnParticle(Particle.DUST, base, 3,
                new Particle.DustOptions(Color.WHITE, 3f));
        }
    }

    // ========================================================================
    // FINAL IMPACT
    // ========================================================================
    private void triggerSwordImpact(Location center, double range, int progress, Player caster) {
        World world = center.getWorld();
        
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, range*0.3, 1, range*0.3, 0.1);
        
        for (int ring = 0; ring < 3; ring++) {
            new BukkitRunnable() {
                int r = 0;
                final int maxR = 20;
                @Override
                public void run() {
                    if (r >= maxR) { this.cancel(); return; }
                    double currentRange = (ring * 1.5) + (r * 0.25);
                    if (currentRange > range) { this.cancel(); return; }
                    
                    for (int i = 0; i < 24; i++) {
                        double angle = (Math.PI * 2 / 24) * i;
                        Location ripple = new Location(
                            world,
                            center.getX() + Math.cos(angle) * currentRange,
                            0.2,
                            center.getZ() + Math.sin(angle) * currentRange
                        );
                        Color rippleColor = Color.fromRGB(
                            200 + ring*20,
                            220 - ring*30,
                            255 - ring*40
                        );
                        world.spawnParticle(Particle.DUST, ripple, 0,
                            new Particle.DustOptions(rippleColor, (float)(1.5 - (r/20.0))));
                    }
                    r++;
                }
            }.runTaskTimer(plugin, ring * 2L, 1L);
        }
        
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            Location pillarBase = new Location(
                world,
                center.getX() + Math.cos(angle) * (range * 0.6),
                0.1,
                center.getZ() + Math.sin(angle) * (range * 0.6)
            );
            new BukkitRunnable() {
                int h = 0;
                @Override
                public void run() {
                    if (h > 12) { this.cancel(); return; }
                    world.spawnParticle(Particle.DUST, 
                        new Location(world, pillarBase.getX(), pillarBase.getY() + h, pillarBase.getZ()), 2,
                        new Particle.DustOptions(Color.fromRGB(255, 240, 200), (float)(2.0 - (h/10.0))));
                    h++;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
        
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.6f);
        world.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.5f, 2f);
        world.playSound(center, Sound.ITEM_TRIDENT_RETURN, 1f, 0.8f);
        
        double finalDamage = 10.0 + (25.0 * (progress / 100.0));
        Collection<Entity> nearby = world.getNearbyEntities(center, range, range, range);
        
        for (Entity en : nearby) {
            if (en instanceof LivingEntity le && !en.equals(caster) && !en.isDead()) {
                double vx = (le.getLocation().getX() - center.getX()) * 0.15;
                double vz = (le.getLocation().getZ() - center.getZ()) * 0.15;
                le.setVelocity(new Vector(vx, 0.8, vz));
                
                le.damage(finalDamage, caster);
                Location critLoc = le.getLocation().clone();
                critLoc.setY(critLoc.getY() + 1);
                le.getWorld().spawnParticle(Particle.CRIT, critLoc, 15, 0.3, 0.3, 0.3, 0.1);
                
                domainAffected.add(le.getUniqueId());
            }
        }
        
        caster.sendTitle(
            "§f§l✦ §bDOMAIN COMPLETE §f§l✦",
            "§7Damage: §e" + String.format("%.1f", finalDamage) + " §7| §eRadius: " + String.format("%.1f", range),
            10, 40, 20
        );
        
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity en : nearby) {
                    if (en instanceof LivingEntity le) {
                        le.removePotionEffect(PotionEffectType.DARKNESS);
                    }
                }
            }
        }.runTaskLater(plugin, DOMAIN_DARKNESS_DURATION);
    }

    // ========================================================================
    // ✦ SKILL 1: ANIME BLINK
    // ========================================================================
    private void executeAnimeBlink(Player p, LivingEntity target) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        
        target.getNearbyEntities(7, 4, 7).stream()
            .filter(en -> en instanceof LivingEntity && !en.equals(p) && targets.size() < BLINK_MAX_TARGETS)
            .forEach(en -> targets.add((LivingEntity) en));

        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 50, 0, false, false));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.3f);
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 40, 1.2, 1.2, 1.2, 0.08);

        for (LivingEntity t : targets) {
            blinkProtected.add(t.getUniqueId());
            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
            
            double angle = Math.random() * Math.PI * 2;
            double hSpread = BLINK_HORIZONTAL_SPREAD + Math.random() * 0.8;
            double vBoost = BLINK_VERTICAL_BOOST + Math.random() * 0.5;
            t.setVelocity(new Vector(
                Math.cos(angle) * hSpread,
                vBoost,
                Math.sin(angle) * hSpread
            ));
            
            t.getWorld().spawnParticle(Particle.CLOUD, t.getLocation(), 20, 0.6, 0.4, 0.6, 0.05);
            t.getWorld().spawnParticle(Particle.DUST, t.getLocation(), 10,
                new Particle.DustOptions(Color.fromRGB(200, 180, 255), 1.8f));
        }

        new BukkitRunnable() {
            int i = 0;
            Location last = p.getLocation().clone();
            @Override
            public void run() {
                if (i >= targets.size()) {
                    LivingEntity lastTarget = targets.get(targets.size() - 1);
                    Vector backDir = lastTarget.getLocation().getDirection().clone();
                    backDir.setY(0);
                    backDir.normalize();
                    backDir.multiply(-1.8);
                    
                    Location returnLoc = new Location(
                        p.getWorld(),
                        lastTarget.getX() + backDir.getX(),
                        lastTarget.getY(),
                        lastTarget.getZ() + backDir.getZ()
                    );
                    p.teleport(returnLoc);
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (LivingEntity t : targets) {
                                blinkProtected.remove(t.getUniqueId());
                            }
                        }
                    }.runTaskLater(plugin, 50L);
                    
                    this.cancel();
                    return;
                }
                
                LivingEntity curr = targets.get(i);
                drawTrail(p.getWorld(), last, curr.getLocation(), Color.fromRGB(255, 215, 0));
                
                Location teleportLoc = new Location(p.getWorld(), curr.getX(), curr.getY() + 0.3, curr.getZ());
                p.teleport(teleportLoc);
                curr.damage(BLINK_DAMAGE, p);
                
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);
                p.getWorld().spawnParticle(Particle.CRIT, curr.getLocation(), 25, 0.4, 0.4, 0.4, 0.15);
                p.getWorld().spawnParticle(Particle.DUST, curr.getLocation(), 15,
                    new Particle.DustOptions(Color.fromRGB(255, 100, 100), 2f));
                
                last = curr.getLocation().clone();
                i++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    // ========================================================================
    // ↯ SKILL 2: MAJU MUNDUR
    // ========================================================================
    private void executeMajuMundur(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.9f);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 25, 0.6, 0.5, 0.6, 0.1);
        
        Vector dir = p.getLocation().getDirection().clone();
        dir.setY(0);
        dir.normalize();
        
        p.setVelocity(dir.clone().multiply(MM_BACKWARD_MULTIPLIER));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, 1));
        
        new BukkitRunnable() {
            int trailTicks = 0;
            @Override
            public void run() {
                if (trailTicks >= MM_DELAY_TICKS) { this.cancel(); return; }
                Location trailLoc = new Location(p.getWorld(), p.getX(), p.getY() + 0.5, p.getZ());
                p.getWorld().spawnParticle(Particle.DUST, trailLoc, 4,
                    new Particle.DustOptions(Color.fromRGB(180, 200, 255), 1.5f));
                trailTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setVelocity(dir.clone().multiply(MM_FORWARD_MULTIPLIER));
                
                p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 10);
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 40,
                    new Particle.DustOptions(Color.fromRGB(255, 220, 80), 2.8f));
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.3f);
                
                Vector frontOffset = dir.clone().multiply(2.5);
                Location front = new Location(
                    p.getWorld(),
                    p.getX() + frontOffset.getX(),
                    p.getY(),
                    p.getZ() + frontOffset.getZ()
                );
                
                p.getWorld().getNearbyEntities(front, 3, 2.5, 3).forEach(en -> {
                    if (en instanceof LivingEntity le && !en.equals(p) && !en.isDead()) {
                        le.damage(MM_DAMAGE, p);
                        Vector knockback = dir.clone().multiply(1.2);
                        knockback.setY(0.5);
                        le.setVelocity(knockback);
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation(), 20, 0.3, 0.3, 0.3, 0.1);
                    }
                });
            }
        }.runTaskLater(plugin, MM_DELAY_TICKS);
    }

    // ========================================================================
    // ✨ HELPER METHODS - FIXED
    // ========================================================================
    
    private void drawTrail(World world, Location from, Location to, Color color) {
        Vector diff = to.toVector().subtract(from.toVector());
        Vector step = diff.clone().normalize().multiply(0.35);
        double dist = from.distance(to);
        Location current = from.clone();
        
        for (double d = 0; d < dist; d += 0.35) {
            // FIX: Gunakan add() terpisah, jangan dalam assignment
            current = current.clone();
            current.add(step);
            world.spawnParticle(Particle.DUST, current, 2,
                new Particle.DustOptions(color, 2.0f));
        }
    }
    
    private void drawLine(World world, Location from, Location to, Particle particle, Object options, double stepSize) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);
        Location current = from.clone();
        
        for (double d = 0; d < distance; d += stepSize) {
            if (particle == Particle.DUST && options instanceof Particle.DustOptions) {
                world.spawnParticle(Particle.DUST, current, 0, (Particle.DustOptions) options);
            }
            current = current.clone();
            current.add(direction.clone().multiply(stepSize));
        }
    }

    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    private boolean isHolding(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
