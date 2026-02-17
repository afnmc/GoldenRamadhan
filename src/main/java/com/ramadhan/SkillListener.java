package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SkillListener implements Listener {

    private final GoldenMoon plugin; // Pakai referensi main class lama lo
    private final Map<UUID, Integer> hitStack = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, List<BukkitTask>> activeTasks = new HashMap<>();

    private static final int MAX_STACK = 5;
    private static final long HIT_TIMEOUT = 3000; 

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startComboWatcher();
    }

    // --- 1. DETEKSI HIT & COMBO ---
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isHolding(p)) return; // Cek pedang sabit lo
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();

        // Reset kalau kelamaan gak hit
        if (lastHitTime.containsKey(id) && (now - lastHitTime.get(id) > HIT_TIMEOUT)) {
            reset(p);
        }

        int stack = hitStack.getOrDefault(id, 0);
        if (stack >= MAX_STACK) return;

        stack++;
        hitStack.put(id, stack);
        lastHitTime.put(id, now);

        // Sound naik pitch tiap hit
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.5f + (stack * 0.2f));
        spawnCrescent(p, stack);
    }

    // --- 2. SNEAK UNTUK MELEDAKKAN COMBO ---
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;

        Player p = e.getPlayer();
        if (!isHolding(p)) return;
        
        UUID id = p.getUniqueId();
        if (hitStack.getOrDefault(id, 0) < MAX_STACK) return;

        finisher(p);
        reset(p);
    }

    // --- 3. VISUAL SABIT MUTER (Orbit) ---
    private void spawnCrescent(Player p, int stack) {
        UUID id = p.getUniqueId();
        
        BukkitTask task = new BukkitRunnable() {
            double angle = stack * (360.0 / MAX_STACK);

            @Override
            public void run() {
                if (!p.isOnline() || !hitStack.containsKey(id)) {
                    this.cancel();
                    return;
                }

                angle += 10; // Kecepatan putar
                double rad = Math.toRadians(angle);
                double radius = 0.7; 

                Location loc = p.getLocation().clone().add(0, 1.2, 0);
                double x = Math.cos(rad) * radius;
                double z = Math.sin(rad) * radius;
                loc.add(x, 0, z);

                // Partikel soul fire biar serem
                p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0, 2);

        activeTasks.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
    }

    // --- 4. FINISHER AOE ---
    private void finisher(Player p) {
        World w = p.getWorld();
        Location center = p.getLocation().add(0, 1, 0);
        double radius = 5.0;

        List<Entity> targets = p.getNearbyEntities(radius, radius, radius);

        new BukkitRunnable() {
            double t = 0;
            @Override
            public void run() {
                t += 0.3;
                // Partikel cincin meluas
                for (double a = 0; a < Math.PI * 2; a += Math.PI / 10) {
                    double x = Math.cos(a) * t;
                    double z = Math.sin(a) * t;
                    w.spawnParticle(Particle.END_ROD, center.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }

                if (t >= 3.0) {
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                    w.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);

                    for (Entity e : targets) {
                        if (e instanceof LivingEntity le && le != p) {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 10));
                            le.damage(40.0, p);
                        }
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void reset(Player p) {
        UUID id = p.getUniqueId();
        hitStack.remove(id);
        lastHitTime.remove(id);
        if (activeTasks.containsKey(id)) {
            activeTasks.get(id).forEach(BukkitTask::cancel);
            activeTasks.remove(id);
        }
    }

    private void startComboWatcher() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (UUID id : new HashSet<>(lastHitTime.keySet())) {
                    if (now - lastHitTime.get(id) > HIT_TIMEOUT) {
                        Player p = Bukkit.getPlayer(id);
                        if (p != null) reset(p);
                        else {
                            hitStack.remove(id);
                            lastHitTime.remove(id);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    private boolean isHolding(Player p) {
        var i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
            target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.1);
            
            // Efek Debuff
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));

            // Kill Effect (Dash & Thunder)
            if (target.getHealth() - e.getFinalDamage() <= 0) {
                drawVerticalStab(target.getLocation());
                p.teleport(target.getLocation().add(target.getLocation().getDirection().multiply(-1.5)));
                p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2f);
                p.sendMessage("§6§l⚡ LUNAR EXECUTION!");
            }
        }
    }

    // Method Animasi Slash Diagonal Random
    private void playSlashAnimation(Player p) {
        Location eyeLoc = p.getEyeLocation();
        Vector dir = eyeLoc.getDirection().normalize();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize(); // Vektor kanan
        
        // Randomize arah: true = KananAtas ke KiriBawah, false = KiriAtas ke KananBawah
        boolean slashFromRight = random.nextBoolean();
        
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.4f);

        // Loop menggambar garis tebasan
        for (double i = -1.2; i <= 1.2; i += 0.1) {
            
            // 1. Curve (Lengkungan ke depan) -> Biar gak gepeng
            double forwardCurve = (1.44 - (i * i)) * 0.4; 
            
            // 2. Tentukan posisi Horizontal (Kanan/Kiri)
            // Kalau i negatif dia di kiri, positif di kanan
            double horizontalOffset = i * 1.5; 

            // 3. Tentukan posisi Vertikal (Atas/Bawah) -> Biar Diagonal
            // Jika slashFromRight: Saat i positif (kanan), posisi harus tinggi.
            double verticalSlope = slashFromRight ? (i * 0.8) : (-i * 0.8);
            
            // Hitung lokasi partikel
            Location particleLoc = eyeLoc.clone()
                    .add(dir.clone().multiply(1.5 + forwardCurve)) // Jarak dari mata + lengkungan
                    .add(right.clone().multiply(horizontalOffset)) // Geser Kanan/Kiri
                    .add(0, verticalSlope, 0) // Geser Atas/Bawah (Miring)
                    .subtract(0, 0.5, 0); // Turunkan dikit biar pas di tengah layar

            p.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.YELLOW, 0.6f));
            
            // Tambah partikel putih di ujung biar tajam
            if (Math.abs(i) > 0.8) {
                p.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
        }
    }

    private void drawVerticalStab(Location loc) {
        for (double y = 0; y <= 3; y += 0.15) {
            double curve = (Math.pow(y - 1.5, 2) - 2.25) * 0.2;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(curve, 3 - y, 0), 3, new Particle.DustOptions(Color.ORANGE, 2.0f));
        }
    }

    // --- 3. RECALL (Tetap sama) ---
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;

        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

        new BukkitRunnable() {
            double angle = 0;
            double radius = 1.5; 
            double height = 0;   
            int t = 0;

            @Override
            public void run() {
                if (!p.isOnline() || !p.isSneaking() || t > 50) { 
                    if (t > 50) { 
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1));
                        drawBackCrescent(p, true); 
                        p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0, 2.3, 0), 3);
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    }
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 2; i++) {
                    angle += 0.4;
                    radius -= 0.025;
                    height += 0.04; 
                    if (radius < 0.2) radius = 0.2;
                    if (height > 2.2) height = 2.2; 
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(x, height, z), 1, new Particle.DustOptions(Color.ORANGE, 1.2f));
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L); 
    }

    private boolean isHolding(Player p) {
        var i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getDisplayName().contains("Golden Crescent Blade");
    }
}
