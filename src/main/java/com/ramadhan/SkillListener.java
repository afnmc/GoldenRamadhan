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
 * GOLDEN MOON - SKILL LISTENER (v2.0 Furina Edition) - FIXED
 * Compatible: Minecraft 1.21.8 - 1.21.10
 * 
 * 🎭 GOLDEN DOMAIN (Ultimate):
 * - Trigger: Stack 5 + Tahan Right Click (1.5 detik = 100%)
 * - Arena: Hexagon mengambang, ukuran sesuai % charge, berputar pelan
 * - Debuff: SLOWNESS 255 (freeze) + DARKNESS ke SEMUA entity (termasuk user)
 * - Visual: Cahaya dari atas → Player TP mundur → Pedang raksasa turun → Impact elegan
 * - Damage: Scaling 10-35 HP berdasarkan % charge
 * 
 * ⚙️ CONFIGURABLE VALUES (cari komentar "🔧 CONFIG"):
 * ============================================================================
 */
public class SkillListener implements Listener {
    
    // 🔧 CONFIG: GOLDEN DOMAIN SETTINGS
    private static final double DOMAIN_MIN_RANGE = 4.0;      // Radius minimal (30% charge)
    private static final double DOMAIN_MAX_RANGE = 10.0;     // Radius maksimal (100% charge)
    private static final int DOMAIN_ROTATION_SPEED = 2;      // Derajat per tick putaran hexagon
    private static final double DOMAIN_HEIGHT = 2.5;         // Ketinggian arena dari tanah
    private static final int CHARGE_TICKS_TO_FULL = 30;      // 30 tick = 1.5 detik untuk 100%
    private static final double SWORD_DESCENT_SPEED = 0.6;   // Kecepatan pedang turun (blok per tick)
    private static final double SWORD_SIZE = 4.0;            // Ukuran visual pedang
    private static final int DOMAIN_FREEZE_DURATION = 60;    // Durasi freeze (3 detik)
    private static final int DOMAIN_DARKNESS_DURATION = 80;  // Durasi darkness (4 detik)
    
    // 🔧 CONFIG: ANIME BLINK SETTINGS
    private static final double BLINK_HORIZONTAL_SPREAD = 2.2;  // Sebaran horizontal enemy
    private static final double BLINK_VERTICAL_BOOST = 1.1;     // Ketinggian lompatan enemy
    private static final int BLINK_DAMAGE = 8;                  // Damage per hit
    private static final int BLINK_MAX_TARGETS = 3;             // Maksimal target yang di-blink
    
    // 🔧 CONFIG: MAJU MUNDUR SETTINGS
    private static final double MM_BACKWARD_MULTIPLIER = -2.0;  // Kekuatan mundur
    private static final double MM_FORWARD_MULTIPLIER = 3.5;    // Kekuatan menerjang
    private static final double MM_DAMAGE = 12.0;               // Damage saat menerjang
    private static final int MM_DELAY_TICKS = 8;                // Delay antara mundur → maju
    
    private final GoldenMoon plugin;
    private final Map<UUID, Integer> chargeStack = new HashMap<>();
    private final Map<UUID, Long> clickHoldStart = new HashMap<>();
    private final Set<UUID> domainAffected = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> blinkProtected = Collections.synchronizedSet(new HashSet<>());

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    // ========================================================================
    // EVENT: FALL DAMAGE PROTECTION (untuk Blink & Domain)
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

        // ── STACKING SYSTEM (selalu jalan duluan) ──
        if (stack < 5) {
            stack++;
            chargeStack.put(uuid, stack);
            sendActionBar(p, "§e§l✦ Golden Stack: §f" + stack + "§7/§f5");
            if (stack == 5) {
                p.sendTitle("§f§l⚡", "§eTahan Right Click untuk Domain", 5, 50, 10);
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 2f);
            }
        }

        // ── SKILL 2: MAJU MUNDUR (Stack 3 + Sneak + Hit) ──
        if (p.isSneaking() && stack == 3) {
            executeMajuMundur(p);
            chargeStack.put(uuid, 0);
            sendActionBar(p, "§b§l↯ Maju Mundur Activated!");
            return;
        }

        // ── SKILL 1: ANIME BLINK (Sneak + Hit, Stack < 3) ──
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
            if (clickHoldStart.containsKey(uuid)) return; // Already charging
            
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
                // Cancel jika player ganti item / mati / logout
                if (!isHolding(p) || !p.isOnline() || !clickHoldStart.containsKey(uuid)) {
                    finishCharging(p, startTime, false);
                    this.cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                int progress = (int) Math.min((elapsed * 100L) / (CHARGE_TICKS_TO_FULL * 50L), 100);
                
                // ── VISUAL: Partikel menyedot ke pusat (Furina style: aqua + white) ──
                Location center = p.getLocation().clone().add(0, 1.2, 0); // FIX: clone() before add()
                double radius = 3.0 - (2.5 * (progress / 100.0));
                
                for (int i = 0; i < 5; i++) {
                    double angle = (Math.PI * 2 / 5) * i + (elapsed / 200.0);
                    Location partLoc = center.clone().add(
                        Math.cos(angle) * radius,
                        (Math.sin(elapsed / 100.0 + i) * 0.8),
                        Math.sin(angle) * radius
                    );
                    // Partikel aqua elegan
                    p.getWorld().spawnParticle(Particle.DUST, partLoc, 1, 
                        new Particle.DustOptions(Color.fromRGB(100, 200, 255), 1.2f));
                    // Partikel spark putih
                    if (progress > 50) {
                        p.getWorld().spawnParticle(Particle.FLASH, partLoc, 0);
                    }
                }
                
                // Sound charging bertahap
                if (progress % 25 == 0 && progress > 0) {
                    p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_STEP, 0.5f, 1f + (progress/50f));
                }
                
                sendActionBar(p, "§f§l✦ Charging Domain: §b" + progress + "%");
                
                // Auto-exec di 100% (karena sulit deteksi release click)
                if (progress >= 100) {
                    executeGoldenDomain(p, 100);
                    clickHoldStart.remove(uuid);
                    chargeStack.put(uuid, 0);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // 1 tick = 50ms, smooth charging
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
    // ⚡ ULTIMATE: GOLDEN DOMAIN (FURINA STYLE)
    // ========================================================================
    private void executeGoldenDomain(Player p, int progress) {
        // 🔧 CONFIG: Range scaling linear 4.0 → 10.0
        double range = DOMAIN_MIN_RANGE + ((DOMAIN_MAX_RANGE - DOMAIN_MIN_RANGE) * (progress / 100.0));
        Location center = p.getLocation().clone();
        UUID uuid = p.getUniqueId();
        
        // Sound start domain
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 2f, 1.5f);
        sendActionBar(p, "§f§l✦ §bGOLDEN DOMAIN §f§l✦ §7[" + progress + "%]");

        // ── FASE 1: HEXAGON ARENA MENGAMBANG & BERPUTAR ──
        BukkitRunnable domainTask = new BukkitRunnable() {
            int ticks = 0;
            final int duration = 40; // 2 detik animasi arena
            
            @Override
            public void run() {
                if (ticks >= duration) {
                    // ── FASE 2: PLAYER TP MUNDUR SEBELUM SWORD IMPACT ──
                    Vector dir = p.getLocation().getDirection().setY(0).normalize();
                    Location safeSpot = p.getLocation().clone().add(dir.clone().multiply(-6)).add(0, 1, 0); // FIX: clone() before modify
                    p.teleport(safeSpot);
                    p.playSound(safeSpot, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
                    
                    // ── FASE 3: PEDANG RAKSASA TURUN DARI LANGIT ──
                    startSwordStrike(center, range, progress, p);
                    
                    this.cancel();
                    return;
                }
                
                // Rotasi hexagon
                double rotation = ticks * DOMAIN_ROTATION_SPEED;
                
                // Gambar 6 titik hexagon mengambang
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    Location corner = center.clone().add(
                        Math.cos(angle) * range,
                        DOMAIN_HEIGHT + (Math.sin(ticks / 4.0 + i) * 0.3), // Efek melayang naik-turun
                        Math.sin(angle) * range
                    );
                    
                    // Partikel hexagon: aqua gold gradient
                    Color hexColor = Color.fromRGB(
                        150 + (int)(50 * Math.sin(ticks/5.0 + i)),
                        200 + (int)(30 * Math.cos(ticks/7.0)),
                        255 - (int)(100 * (progress/100.0))
                    );
                    p.getWorld().spawnParticle(Particle.DUST, corner, 2, 
                        new Particle.DustOptions(hexColor, 2.0f));
                    
                    // Garis penghubung hexagon (opsional, elegan)
                    if (i % 2 == 0) {
                        Location next = center.clone().add(
                            Math.cos(angle + Math.toRadians(60)) * range,
                            DOMAIN_HEIGHT,
                            Math.sin(angle + Math.toRadians(60)) * range
                        );
                        drawLine(p.getWorld(), corner, next, Particle.DUST, new Particle.DustOptions(Color.WHITE, 0.8f), 0.6); // FIX: pass world
                    }
                }
                
                // ── DEBUFF: SLOW + DARKNESS KE SEMUA ENTITY (termasuk user) ──
                Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, range, range, range);
                for (Entity en : nearby) {
                    if (en instanceof LivingEntity le) {
                        // Tandai agar dapat proteksi fall damage
                        domainAffected.add(le.getUniqueId());
                        
                        // Freeze total (Slowness 255 = tidak bisa gerak)
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.SLOWNESS, 
                            DOMAIN_FREEZE_DURATION, 
                            255, 
                            false, false
                        ));
                        
                        // Gelap total (Darkness)
                        le.addPotionEffect(new PotionEffect(
                            PotionEffectType.DARKNESS, 
                            DOMAIN_DARKNESS_DURATION, 
                            0, 
                            false, false
                        ));
                        
                        // Efek visual kecil di atas kepala entity
                        if (progress > 70 && ticks % 5 == 0) {
                            le.getWorld().spawnParticle(Particle.DUST, 
                                le.getLocation().add(0, 2, 0), 1,
                                new Particle.DustOptions(Color.fromRGB(80, 120, 255), 1f));
                        }
                    }
                }
                
                // Cahaya pillar dari atas (Furina style: beam of light)
                if (ticks % 3 == 0) {
                    for (int i = 0; i < 3; i++) {
                        double angle = Math.toRadians(i * 120 + ticks * 2);
                        Location beamTop = center.clone().add(
                            Math.cos(angle) * (range * 0.7),
                            15,
                            Math.sin(angle) * (range * 0.7)
                        );
                        // Beam turun
                        for (double y = 15; y > DOMAIN_HEIGHT; y -= 1.5) {
                            Location beamPart = beamTop.clone().setY(y);
                            p.getWorld().spawnParticle(Particle.DUST, beamPart, 0,
                                new Particle.DustOptions(Color.fromRGB(200, 230, 255), 2.5f));
                        }
                    }
                }
                
                ticks++;
            }
        };
        domainTask.runTaskTimer(plugin, 0L, 2L);

        // ── CLEANUP: Hapus proteksi fall damage setelah efek selesai ──
        new BukkitRunnable() {
            @Override
            public void run() {
                domainAffected.remove(uuid); // Keep player protected a bit longer
                // Hapus semua entity yang sudah tidak perlu
                Iterator<UUID> it = domainAffected.iterator();
                while (it.hasNext()) {
                    Entity en = Bukkit.getEntity(it.next());
                    if (en == null || !en.isValid() || en.isDead()) {
                        it.remove();
                    }
                }
            }
        }.runTaskLater(plugin, 100L); // 5 detik
    }

    // ========================================================================
    // ⚔️ SWORD STRIKE ANIMATION (FURINA ENDING STYLE)
    // ========================================================================
    private void startSwordStrike(Location center, double range, int progress, Player caster) {
        // Sound dramatis sebelum sword muncul
        caster.getWorld().playSound(center, Sound.ENTITY_WARDEN_SNIFF, 1.5f, 0.3f);
        
        new BukkitRunnable() {
            int frame = 0;
            final int descentFrames = (int) (20 / SWORD_DESCENT_SPEED); // ~33 frames dari 20 blok
            
            @Override
            public void run() {
                if (frame > descentFrames + 15) {
                    // ── FINAL IMPACT: LEDAKAN ELEGAN (bukan explosion biasa) ──
                    triggerSwordImpact(center, range, progress, caster);
                    this.cancel();
                    return;
                }
                
                // ── VISUAL: PEDANG TURUN PELAN DARI LANGIT ──
                double swordY = 25 - (frame * SWORD_DESCENT_SPEED);
                
                if (frame <= descentFrames) {
                    // Bentuk pedang: garis vertikal + hilt + guard
                    drawSwordBlade(caster.getWorld(), center.clone().setY(swordY), SWORD_SIZE, frame); // FIX: pass world
                    
                    // Partikel "tekanan udara" saat pedang mendekat
                    if (frame > descentFrames - 10) {
                        double pressureRadius = range * (1.2 - ((frame - (descentFrames-10)) * 0.02));
                        for (int i = 0; i < 8; i++) {
                            double angle = (Math.PI * 2 / 8) * i;
                            Location pressure = center.clone().add(
                                Math.cos(angle) * pressureRadius,
                                1,
                                Math.sin(angle) * pressureRadius
                            );
                            caster.getWorld().spawnParticle(Particle.CLOUD, pressure, 1, 0.3, 0.1, 0.3, 0.05);
                        }
                    }
                    
                    // Sound descending pitch naik - FIX: ganti sound yang tidak valid
                    if (frame % 3 == 0) {
                        caster.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, // FIX: was _RESONATE (invalid)
                            0.4f, 0.5f + (frame / 20f));
                    }
                } else {
                    // ── POST-IMPACT: Residual particles ──
                    if ((frame - descentFrames) % 4 == 0) {
                        for (int i = 0; i < 12; i++) {
                            double angle = Math.random() * Math.PI * 2;
                            Location residual = center.clone().add(
                                Math.cos(angle) * (Math.random() * range),
                                1 + Math.random() * 2,
                                Math.sin(angle) * (Math.random() * range)
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

    // Gambar visual pedang (blade + guard + hilt) - FIX: tambah parameter World
    private void drawSwordBlade(World world, Location base, double size, int animationProgress) {
        // Blade utama (garis vertikal)
        for (double y = 0; y < size * 2; y += 0.3) {
            Location bladePart = base.clone().subtract(0, y, 0);
            // Warna: dari putih terang (ujung) ke gold (pangkal)
            Color bladeColor = Color.fromRGB(
                255,
                230 - (int)(50 * (y / (size*2))),
                180 - (int)(80 * (y / (size*2)))
            );
            // FIX: cast ke float untuk size parameter
            world.spawnParticle(Particle.DUST, bladePart, 1,
                new Particle.DustOptions(bladeColor, (float)(2.8 - (y * 0.1))));
        }
        
        // Guard (pelindung tangan) - horizontal
        Location guard = base.clone().subtract(0, size * 2, 0);
        for (double x = -size/2; x <= size/2; x += 0.4) {
            world.spawnParticle(Particle.DUST, guard.clone().add(x, 0, 0), 2,
                new Particle.DustOptions(Color.fromRGB(255, 200, 50), 2.2f));
            world.spawnParticle(Particle.DUST, guard.clone().add(0, 0, x), 2,
                new Particle.DustOptions(Color.fromRGB(255, 200, 50), 2.2f));
        }
        
        // Hilt (pegangan) - kecil di bawah guard
        Location hilt = guard.clone().subtract(0, 0.5, 0);
        world.spawnParticle(Particle.DUST, hilt, 4,
            new Particle.DustOptions(Color.fromRGB(139, 69, 19), 1.8f));
            
        // Spark efek di ujung pedang (semakin dekat impact, semakin intens)
        if (animationProgress > 20) {
            Location tip = base.clone();
            world.spawnParticle(Particle.FLASH, tip, 0);
            world.spawnParticle(Particle.DUST, tip, 3,
                new Particle.DustOptions(Color.WHITE, 3f));
        }
    }

    // ── FINAL IMPACT: Ledakan elegan ala Furina ──
    private void triggerSwordImpact(Location center, double range, int progress, Player caster) {
        World world = center.getWorld();
        
        // 1. Flash + screen shake effect (via particle burst)
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, range*0.3, 1, range*0.3, 0.1);
        
        // 2. Shockwave ring horizontal (seperti air ripple)
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
                        Location ripple = center.clone().add(
                            Math.cos(angle) * currentRange,
                            0.2,
                            Math.sin(angle) * currentRange
                        );
                        Color rippleColor = Color.fromRGB(
                            200 + ring*20,
                            220 - ring*30,
                            255 - ring*40
                        );
                        world.spawnParticle(Particle.DUST, ripple, 0,
                            new Particle.DustOptions(rippleColor, (float)(1.5 - (r/20.0)))); // FIX: cast float
                    }
                    r++;
                }
            }.runTaskTimer(plugin, ring * 2L, 1L);
        }
        
        // 3. Light pillars dari titik impact (vertical beams)
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(i * 60);
            Location pillarBase = center.clone().add(
                Math.cos(angle) * (range * 0.6),
                0.1,
                Math.sin(angle) * (range * 0.6)
            );
            new BukkitRunnable() {
                int h = 0;
                @Override
                public void run() {
                    if (h > 12) { this.cancel(); return; }
                    world.spawnParticle(Particle.DUST, pillarBase.clone().add(0, h, 0), 2,
                        new Particle.DustOptions(Color.fromRGB(255, 240, 200), (float)(2.0 - (h/10.0)))); // FIX: cast float
                    h++;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
        
        // 4. Sound impact layered
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.6f);
        world.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.5f, 2f);
        world.playSound(center, Sound.ITEM_TRIDENT_RETURN, 1f, 0.8f);
        
        // 5. DAMAGE SCALING: 10 + (25 * progress%)
        double finalDamage = 10.0 + (25.0 * (progress / 100.0));
        Collection<Entity> nearby = world.getNearbyEntities(center, range, range, range);
        
        for (Entity en : nearby) {
            if (en instanceof LivingEntity le && !en.equals(caster) && !en.isDead()) {
                // Knockup elegan (terbang sedikit lalu jatuh aman)
                le.setVelocity(new Vector(
                    (le.getLocation().getX() - center.getX()) * 0.15,
                    0.8,
                    (le.getLocation().getZ() - center.getZ()) * 0.15
                ));
                
                // Damage + indicator particle
                le.damage(finalDamage, caster);
                le.getWorld().spawnParticle(Particle.CRIT, le.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                
                // Tandai untuk proteksi fall damage
                domainAffected.add(le.getUniqueId());
            }
        }
        
        // 6. Title notification untuk caster
        caster.sendTitle(
            "§f§l✦ §bDOMAIN COMPLETE §f§l✦",
            "§7Damage: §e" + String.format("%.1f", finalDamage) + " §7| §eRadius: " + String.format("%.1f", range),
            10, 40, 20
        );
        
        // 7. Cleanup: hapus darkness setelah beberapa detik
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity en : nearby) {
                    if (en instanceof LivingEntity le) {
                        le.removePotionEffect(PotionEffectType.DARKNESS);
                        // Optional: hapus slow juga jika ingin
                        // le.removePotionEffect(PotionEffectType.SLOWNESS);
                    }
                }
            }
        }.runTaskLater(plugin, DOMAIN_DARKNESS_DURATION);
    }

    // ========================================================================
    // ✦ SKILL 1: ANIME BLINK (Sneak + Hit)
    // ========================================================================
    private void executeAnimeBlink(Player p, LivingEntity target) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        
        // Cari target tambahan dalam radius
        target.getNearbyEntities(7, 4, 7).stream()
            .filter(en -> en instanceof LivingEntity && !en.equals(p) && targets.size() < BLINK_MAX_TARGETS)
            .forEach(en -> targets.add((LivingEntity) en));

        // Visual start: player invis + partikel portal
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 50, 0, false, false));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.3f);
        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 40, 1.2, 1.2, 1.2, 0.08);

        // Launch targets: spread X/Y/Z + anti fall
        for (LivingEntity t : targets) {
            blinkProtected.add(t.getUniqueId());
            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0));
            
            // Velocity menyebar 3D
            double angle = Math.random() * Math.PI * 2;
            double hSpread = BLINK_HORIZONTAL_SPREAD + Math.random() * 0.8;
            double vBoost = BLINK_VERTICAL_BOOST + Math.random() * 0.5;
            t.setVelocity(new Vector(
                Math.cos(angle) * hSpread,
                vBoost,
                Math.sin(angle) * hSpread
            ));
            
            // Partikel cloud saat terlempar
            t.getWorld().spawnParticle(Particle.CLOUD, t.getLocation(), 20, 0.6, 0.4, 0.6, 0.05);
            t.getWorld().spawnParticle(Particle.DUST, t.getLocation(), 10,
                new Particle.DustOptions(Color.fromRGB(200, 180, 255), 1.8f));
        }

        // Sequence teleport + hit
        new BukkitRunnable() {
            int i = 0;
            Location last = p.getLocation().clone(); // FIX: clone location
            @Override
            public void run() {
                if (i >= targets.size()) {
                    // Return to last target position
                    LivingEntity lastTarget = targets.get(targets.size() - 1);
                    p.teleport(lastTarget.getLocation().clone().add(
                        lastTarget.getLocation().getDirection().clone().multiply(-1.8).setY(0)
                    ));
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                    
                    // Cleanup fall protection after delay
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
                drawTrail(p.getWorld(), last, curr.getLocation(), Color.fromRGB(255, 215, 0)); // FIX: pass world
                p.teleport(curr.getLocation().clone().add(0, 0.3, 0)); // Sedikit di atas
                curr.damage(BLINK_DAMAGE, p);
                
                // Hit feedback
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);
                p.getWorld().spawnParticle(Particle.CRIT, curr.getLocation(), 25, 0.4, 0.4, 0.4, 0.15);
                p.getWorld().spawnParticle(Particle.DUST, curr.getLocation(), 15,
                    new Particle.DustOptions(Color.fromRGB(255, 100, 100), 2f));
                
                last = curr.getLocation().clone(); // FIX: clone location
                i++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    // ========================================================================
    // ↯ SKILL 2: MAJU MUNDUR (Stack 3 + Sneak + Hit)
    // ========================================================================
    private void executeMajuMundur(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.9f);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 25, 0.6, 0.5, 0.6, 0.1);
        
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        
        // Fase 1: Mundur cepat
        p.setVelocity(dir.clone().multiply(MM_BACKWARD_MULTIPLIER));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, 1));
        
        // Visual mundur: trail partikel
        new BukkitRunnable() {
            int trailTicks = 0;
            @Override
            public void run() {
                if (trailTicks >= MM_DELAY_TICKS) { this.cancel(); return; }
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation().clone().add(0, 0.5, 0), 4, // FIX: clone
                    new Particle.DustOptions(Color.fromRGB(180, 200, 255), 1.5f));
                trailTicks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        // Fase 2: Maju menerjang + impact
        new BukkitRunnable() {
            @Override
            public void run() {
                // Burst forward
                p.setVelocity(dir.clone().multiply(MM_FORWARD_MULTIPLIER));
                
                // Impact visual
                p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 10);
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 40,
                    new Particle.DustOptions(Color.fromRGB(255, 220, 80), 2.8f));
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.3f);
                
                // Damage ke entity di depan
                Location front = p.getLocation().clone().add(dir.clone().multiply(2.5)); // FIX: clone
                p.getWorld().getNearbyEntities(front, 3, 2.5, 3).forEach(en -> {
                    if (en instanceof LivingEntity le && !en.equals(p) && !en.isDead()) {
                        le.damage(MM_DAMAGE, p);
                        le.setVelocity(dir.clone().multiply(1.2).setY(0.5));
                        le.getWorld().spawnParticle(Particle.CRIT, le.getLocation(), 20, 0.3, 0.3, 0.3, 0.1);
                    }
                });
            }
        }.runTaskLater(plugin, MM_DELAY_TICKS);
    }

    // ========================================================================
    // ✨ HELPER METHODS - FIXED
    // ========================================================================
    
    // FIX #1: Tambahkan parameter World, hapus referensi 'p' yang tidak valid
    private void drawTrail(World world, Location from, Location to, Color color) {
        Vector step = to.toVector().subtract(from.toVector()).normalize().multiply(0.35);
        double dist = from.distance(to);
        Location current = from.clone();
        
        for (double d = 0; d < dist; d += 0.35) {
            current.add(step);
            world.spawnParticle(Particle.DUST, current, 2,
                new Particle.DustOptions(color, 2.0f));
        }
    }
    
    // FIX #2: Tambahkan parameter World, hapus referensi 'p' yang tidak valid
    private void drawLine(World world, Location from, Location to, Particle particle, Object options, double stepSize) {
        Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);
        Location current = from.clone();
        
        for (double d = 0; d < distance; d += stepSize) {
            if (particle == Particle.DUST && options instanceof Particle.DustOptions) {
                world.spawnParticle(Particle.DUST, current, 0, (Particle.DustOptions) options);
            }
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
