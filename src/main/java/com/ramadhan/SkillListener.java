package com.ramadhan;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent; // Event Mati
import org.bukkit.event.player.PlayerRespawnEvent; // Event Hidup Lagi
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType; // Untuk cek segel
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class SkillListener implements Listener {

    private final GoldenMoon plugin;
    private final Map<UUID, Integer> hitStack = new HashMap<>();
    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, List<BukkitTask>> activeTasks = new HashMap<>();

    // Map untuk menyimpan pedang sementara saat player mati
    private final Map<UUID, ItemStack> savedSwords = new HashMap<>();

    private static final int MAX_STACK = 5;
    private static final long HIT_TIMEOUT = 3000; 

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startComboWatcher();
    }

    // ==========================================
    // BAGIAN 1: FITUR ANTI-HILANG (KEEP ITEM)
    // ==========================================

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        List<ItemStack> drops = e.getDrops();
        
        // Cek drops, kalau ada pedang sakti, hapus dari drops & simpan
        Iterator<ItemStack> iter = drops.iterator();
        while (iter.hasNext()) {
            ItemStack item = iter.next();
            if (isSpecialSword(item)) {
                iter.remove(); // Hapus dari tanah (biar gak diambil orang)
                savedSwords.put(p.getUniqueId(), item); // Simpan di memori plugin
                break; // Asumsi cuma bawa 1, kalau bisa bawa banyak hapus break-nya
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();

        // Kalau dia punya simpenan pedang, balikin
        if (savedSwords.containsKey(id)) {
            ItemStack sword = savedSwords.get(id);
            p.getInventory().addItem(sword);
            savedSwords.remove(id); // Hapus dari map biar bersih
            p.sendMessage(ChatColor.YELLOW + "Pedang Bulan kembali ke tanganmu!");
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_GOLD, 1f, 1f);
        }
    }

    // ==========================================
    // BAGIAN 2: SKILL & COMBO
    // ==========================================

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        
        // Cek pakai fungsi baru (cek Key, bukan Nama)
        if (!isHoldingSpecial(p)) return; 
        
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastHitTime.containsKey(id) && (now - lastHitTime.get(id) > HIT_TIMEOUT)) {
            reset(p);
        }

        int stack = hitStack.getOrDefault(id, 0);
        if (stack >= MAX_STACK) return;

        stack++;
        hitStack.put(id, stack);
        lastHitTime.put(id, now);

        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.5f + (stack * 0.2f));
        spawnOrbitCrescent(p, stack);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;

        Player p = e.getPlayer();
        if (!isHoldingSpecial(p)) return;
        
        UUID id = p.getUniqueId();
        if (hitStack.getOrDefault(id, 0) < MAX_STACK) return;

        executeFinisher(p);
        reset(p);
    }

    // ==========================================
    // HELPER & VISUAL
    // ==========================================

    // Cek apakah item ini adalah pedang khusus (Cek NBT/PDC)
    private boolean isSpecialSword(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        
        // INI KUNCINYA: Cek apakah ada segel "golden_crescent_blade"
        return meta.getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    // Wrapper buat cek player pegang pedang atau enggak
    private boolean isHoldingSpecial(Player p) {
        return isSpecialSword(p.getInventory().getItemInMainHand());
    }

    private void spawnOrbitCrescent(Player p, int stack) {
        UUID id = p.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            double angle = stack * (360.0 / MAX_STACK);
            @Override
            public void run() {
                if (!p.isOnline() || !hitStack.containsKey(id)) { this.cancel(); return; }
                angle += 10; 
                double rad = Math.toRadians(angle);
                Location loc = p.getLocation().clone().add(0, 1.2, 0);
                loc.add(Math.cos(rad) * 0.7, 0, Math.sin(rad) * 0.7);
                p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0, 2);
        activeTasks.computeIfAbsent(id, k -> new ArrayList<>()).add(task);
    }

    private void executeFinisher(Player p) {
        World w = p.getWorld();
        Location center = p.getLocation().add(0, 1, 0);
        double radius = 5.0;
        List<Entity> targets = p.getNearbyEntities(radius, radius, radius);

        drawSpecialCrescent(p.getLocation().add(0, 2.5, 0), p);

        new BukkitRunnable() {
            double t = 0;
            @Override
            public void run() {
                t += 0.3;
                for (double a = 0; a < Math.PI * 2; a += Math.PI / 10) {
                    w.spawnParticle(Particle.END_ROD, center.clone().add(Math.cos(a) * t, 0, Math.sin(a) * t), 1, 0, 0, 0, 0);
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

    private void drawSpecialCrescent(Location center, Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Vector rightDir = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        for (double t = -1.3; t <= 1.3; t += 0.05) {
            double cw = Math.cos(t) * 1.5; 
            double h = t * 1.3; 
            double tx = (cw * Math.cos(0.35)) - (h * Math.sin(0.35));
            double ty = (cw * Math.sin(0.35)) + (h * Math.cos(0.35));
            Location dot = center.clone().add(rightDir.clone().multiply(tx)).add(0, ty, 0);
            p.getWorld().spawnParticle(Particle.DUST, dot, 1, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.5f));
        }
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
                        else { hitStack.remove(id); lastHitTime.remove(id); }
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }
}
