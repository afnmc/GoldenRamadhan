package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class SkillListener implements Listener {
    private final GoldenMoon plugin;
    
    private final Map<UUID, Integer> chargeStack = new HashMap<>();
    private final Map<UUID, Long> healCooldown = new HashMap<>();
    private final Map<UUID, Long> holdStart = new HashMap<>();
    private final Set<UUID> aeternaMarked = new HashSet<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startLunamSoulsTask();
    }

    @EventHandler
    public void onLunamCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID uuid = p.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        // --- PASIF 1: IMPERATORIS LUNA (300% Damage Mark) ---
        if (aeternaMarked.contains(targetUUID)) {
            e.setDamage(e.getDamage() * 3.0);
            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 3);
        } else {
            aeternaMarked.add(targetUUID);
            drawLink(p, target, Color.YELLOW);
        }

        // --- SKILL: ZIG-ZAG DASH (HIT + LOMPAT) ---
        if (!p.isOnGround() && !p.isSneaking()) {
            executeZigZagDash(p, target);
            return;
        }

        // --- STACKING SYSTEM ---
        int stack = chargeStack.getOrDefault(uuid, 0);
        if (stack < 5) {
            stack++;
            chargeStack.put(uuid, stack);
            // FIX: Menggunakan Bungee API agar tidak error saat compile
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                new TextComponent("§e§lGolden Stack: §f" + stack + "/5"));
        }

        // --- SKILL: MAJU MUNDUR (HIT KE-3 + SNEAKING) ---
        if (p.isSneaking() && stack == 3) {
            executeMajuMundur(p);
        }
    }

    private void executeMajuMundur(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        p.setVelocity(dir.clone().multiply(-1.5)); // Mundur

        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                // TRAIL LEBIH BESAR (Kuning & Putih)
                Location loc = p.getLocation().add(0, 0.5, 0);
                p.getWorld().spawnParticle(Particle.DUST, loc, 15, 0.3, 0.3, 0.3, new Particle.DustOptions(Color.YELLOW, 2.0f));
                p.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.2, 0.2, 0.2, new Particle.DustOptions(Color.WHITE, 1.5f));
                
                damageArea(p, 5.0, 3.5); // Damage di sekitar jalur gerak
                
                if (i == 8) {
                    // MAJU + LEDAKAN KECIL
                    p.setVelocity(dir.multiply(2.5));
                    p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 2);
                    p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 2f);
                    this.cancel();
                }
                i++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void executeBigExplosion(Player p) {
        // LEDAKAN EMAS PUTIH (MAIN ARENA)
        p.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, p.getLocation(), 3);
        p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 20, 2, 2, 2, 0.1);
        p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 100, 3, 1, 3, new Particle.DustOptions(Color.YELLOW, 2.2f));
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
        
        for (Entity en : p.getNearbyEntities(6, 6, 6)) {
            if (en instanceof LivingEntity le && en != p) {
                le.damage(25.0, p);
                le.setVelocity(le.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(2.0));
            }
        }
        p.sendMessage("§e§lGOLDEN BURST RELEASED!");
    }

    private void executeZigZagDash(Player p, LivingEntity firstTarget) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(firstTarget);
        for (Entity en : firstTarget.getNearbyEntities(8, 4, 8)) {
            if (en instanceof LivingEntity le && en != p && targets.size() < 3) targets.add(le);
        }

        new BukkitRunnable() {
            int idx = 0;
            Location lastPos = p.getLocation();
            @Override
            public void run() {
                if (idx >= targets.size()) {
                    // FINISH: Berakhir di belakang target terakhir
                    LivingEntity last = targets.get(targets.size()-1);
                    p.teleport(last.getLocation().add(last.getLocation().getDirection().multiply(-1.2)));
                    p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 1);
                    this.cancel();
                    return;
                }
                LivingEntity current = targets.get(idx);
                drawTrail(lastPos, current.getLocation(), Color.YELLOW);
                p.teleport(current.getLocation());
                current.damage(10.0, p);
                lastPos = current.getLocation();
                idx++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!isHolding(p)) return;

        if (e.isSneaking()) {
            // --- HEAL SYSTEM (10S COOLDOWN) ---
            long now = System.currentTimeMillis();
            if (now - healCooldown.getOrDefault(uuid, 0L) > 10000) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0));
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                healCooldown.put(uuid, now);
            }

            // --- CHARGING FOR STACK 5 ---
            if (chargeStack.getOrDefault(uuid, 0) >= 5) {
                holdStart.put(uuid, System.currentTimeMillis());
                p.sendTitle("", "§e§lCHARGING BURST...", 0, 20, 0);
            }
        } else {
            // --- RELEASE BIG EXPLOSION ---
            if (holdStart.containsKey(uuid)) {
                long duration = System.currentTimeMillis() - holdStart.get(uuid);
                if (duration >= 1000 && chargeStack.getOrDefault(uuid, 0) >= 5) {
                    executeBigExplosion(p);
                    chargeStack.put(uuid, 0);
                }
                holdStart.remove(uuid);
            }
        }
    }

    private void startLunamSoulsTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isHolding(p)) continue;
                    p.getNearbyEntities(7, 7, 7).stream()
                        .filter(en -> en instanceof LivingEntity && en != p)
                        .findFirst().ifPresent(target -> {
                            ((LivingEntity) target).damage(3.0, p);
                            p.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 5, new Particle.DustOptions(Color.YELLOW, 1.2f));
                            p.playSound(p.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 0.5f, 2f);
                        });
                }
            }
        }.runTaskTimer(plugin, 0L, 60L);
    }

    private void drawTrail(Location from, Location to, Color color) {
        Vector vec = to.toVector().subtract(from.toVector()).normalize().multiply(0.4);
        double dist = from.distance(to);
        for (double d = 0; d < dist; d += 0.4) {
            from.add(vec);
            from.getWorld().spawnParticle(Particle.DUST, from, 2, new Particle.DustOptions(color, 1.8f));
        }
    }

    private void drawLink(Player p, Entity target, Color color) {
        Location start = p.getLocation().add(0, 1, 0);
        Location end = target.getLocation().add(0, 1, 0);
        Vector vec = end.toVector().subtract(start.toVector()).normalize().multiply(0.4);
        double dist = start.distance(end);
        for (double d = 0; d < dist; d += 0.4) {
            start.add(vec);
            p.getWorld().spawnParticle(Particle.DUST, start, 1, new Particle.DustOptions(color, 0.8f));
        }
    }

    private void damageArea(Player p, double dmg, double range) {
        for (Entity en : p.getNearbyEntities(range, range, range)) {
            if (en instanceof LivingEntity le && en != p) le.damage(dmg, p);
        }
    }

    private boolean isHolding(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
