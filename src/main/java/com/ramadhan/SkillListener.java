package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
    private final Map<UUID, Long> clickHoldStart = new HashMap<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLunamCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID uuid = p.getUniqueId();
        int stack = chargeStack.getOrDefault(uuid, 0);

        // --- SKILL 1: ANIME BLINK (LOMPAT + HIT) ---
        if (!p.isOnGround() && !p.isSneaking()) {
            executeAnimeBlink(p, target);
            return;
        }

        // --- SKILL 2: MAJU MUNDUR (HIT KE-3 + SNEAK) ---
        if (p.isSneaking() && stack == 3) {
            executeMajuMundur(p);
        }

        // --- STACKING SYSTEM ---
        if (stack < 5) {
            stack++;
            chargeStack.put(uuid, stack);
            sendActionBar(p, "§e§lGolden Stack: §f" + stack + "/5");
            if (stack == 5) p.sendTitle("", "§f§lREADY FOR DOMAIN (Hold Right Click)", 0, 40, 10);
        }
    }

    @EventHandler
    public void onHoldUltimate(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || chargeStack.getOrDefault(p.getUniqueId(), 0) < 5) return;

        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!clickHoldStart.containsKey(p.getUniqueId())) {
                clickHoldStart.put(p.getUniqueId(), System.currentTimeMillis());
                startChargingDomain(p);
            }
        }
    }

    private void startChargingDomain(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isHandRaised() || !clickHoldStart.containsKey(p.getUniqueId())) {
                    if (clickHoldStart.containsKey(p.getUniqueId())) {
                        long held = System.currentTimeMillis() - clickHoldStart.get(p.getUniqueId());
                        int progress = (int) Math.min((held / 15), 100);
                        if (progress >= 30) executeGoldenDomain(p, progress);
                    }
                    clickHoldStart.remove(p.getUniqueId());
                    this.cancel();
                    return;
                }

                long time = System.currentTimeMillis() - clickHoldStart.get(p.getUniqueId());
                int progress = (int) Math.min((time / 15), 100);
                
                // Visual Partikel Putih Menyedot
                Location center = p.getLocation().add(0, 1, 0);
                double radius = 3.5 - (3.0 * (progress / 100.0));
                for(int i=0; i<4; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location partLoc = center.clone().add(Math.cos(angle)*radius, (Math.random()-0.5)*2, Math.sin(angle)*radius);
                    p.getWorld().spawnParticle(Particle.DUST, partLoc, 1, new Particle.DustOptions(Color.WHITE, 1.5f));
                }
                
                sendActionBar(p, "§f§lCharging Domain: §e" + progress + "%");
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void executeGoldenDomain(Player p, int progress) {
        double range = 4.0 + (3.0 * (progress / 100.0));
        Location center = p.getLocation();

        // 1. Munculkan Hexagon Berputar (Segi 6)
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + (t * 4));
                    Location corner = center.clone().add(Math.cos(angle) * range, 1.5, Math.sin(angle) * range);
                    p.getWorld().spawnParticle(Particle.DUST, corner, 5, new Particle.DustOptions(Color.YELLOW, 2.0f));
                }
                
                // Efek Lambat & Gelap buat musuh
                center.getWorld().getNearbyEntities(center, range, range, range).forEach(en -> {
                    if (en instanceof LivingEntity le && !en.equals(p)) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
                    }
                });

                if (t++ > 25) { // Durasi Domain
                    // 2. User TP Mundur
                    p.teleport(p.getLocation().add(p.getLocation().getDirection().multiply(-4)));
                    
                    // 3. Pedang Raksasa Nancap
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            p.getWorld().spawnParticle(Particle.FLASH, center, 100, 0.5, 5, 0.5, 0.1);
                            p.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, center, 5);
                            p.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.5f);
                            
                            center.getWorld().getNearbyEntities(center, range, range, range).forEach(en -> {
                                if (en instanceof LivingEntity le && !en.equals(p)) {
                                    le.damage(25.0 * (progress/100.0) + 10, p);
                                }
                            });
                        }
                    }.runTaskLater(plugin, 5L);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
        chargeStack.put(p.getUniqueId(), 0);
    }

    private void executeAnimeBlink(Player p, LivingEntity target) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        target.getNearbyEntities(7, 4, 7).stream()
            .filter(en -> en instanceof LivingEntity && !en.equals(p) && targets.size() < 3)
            .forEach(en -> targets.add((LivingEntity) en));

        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, false));
        
        // Angkat Entity ke udara (Airborne)
        targets.forEach(t -> t.setVelocity(new Vector(0, 0.6 + Math.random()*0.5, 0)));

        new BukkitRunnable() {
            int i = 0;
            Location last = p.getLocation();
            @Override
            public void run() {
                if (i >= targets.size()) {
                    LivingEntity l = targets.get(targets.size()-1);
                    p.teleport(l.getLocation().add(l.getLocation().getDirection().multiply(-1.2)));
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                    this.cancel();
                    return;
                }
                LivingEntity curr = targets.get(i);
                drawTrail(last, curr.getLocation());
                p.teleport(curr.getLocation());
                curr.damage(8.0, p);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
                last = curr.getLocation();
                i++;
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private void executeMajuMundur(Player p) {
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        p.setVelocity(dir.clone().multiply(-1.5));
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setVelocity(dir.multiply(2.5));
                p.getWorld().spawnParticle(Particle.FLASH, p.getLocation(), 5);
                p.getWorld().spawnParticle(Particle.DUST, p.getLocation(), 20, new Particle.DustOptions(Color.YELLOW, 2.0f));
            }
        }.runTaskLater(plugin, 8L);
    }

    private void drawTrail(Location from, Location to) {
        Vector vec = to.toVector().subtract(from.toVector()).normalize().multiply(0.5);
        double dist = from.distance(to);
        for (double d = 0; d < dist; d += 0.5) {
            from.getWorld().spawnParticle(Particle.DUST, from.add(vec), 2, new Particle.DustOptions(Color.YELLOW, 2.5f));
        }
    }

    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    private boolean isHolding(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
