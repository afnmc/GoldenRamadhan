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
 * GOLDEN MOON - SKILL LISTENER (v2.1 Full Visual Fixed)
 * Fixed for Spigot API Compatibility (Location access fixed)
 */
public class SkillListener implements Listener {
    
    private static final double DOMAIN_MIN_RANGE = 4.0;
    private static final double DOMAIN_MAX_RANGE = 10.0;
    private static final int DOMAIN_ROTATION_SPEED = 2;
    private static final double DOMAIN_HEIGHT = 2.5;
    private static final int CHARGE_TICKS_TO_FULL = 30;
    private static final double SWORD_DESCENT_SPEED = 0.6;
    private static final double SWORD_SIZE = 4.0;
    private static final int DOMAIN_FREEZE_DURATION = 60;
    private static final int DOMAIN_DARKNESS_DURATION = 80;
    
    private static final double BLINK_HORIZONTAL_SPREAD = 2.2;
    private static final double BLINK_VERTICAL_BOOST = 1.1;
    private static final int BLINK_DAMAGE = 8;
    private static final int BLINK_MAX_TARGETS = 3;
    
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

    @EventHandler(ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        UUID id = e.getEntity().getUniqueId();
        if (blinkProtected.contains(id) || domainAffected.contains(id)) {
            e.setCancelled(true);
            e.getEntity().setFallDistance(0);
        }
    }

    @EventHandler
    public void onLunamCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        UUID uuid = p.getUniqueId();
        int stack = chargeStack.getOrDefault(uuid, 0);

        if (stack < 5) {
            stack++;
            chargeStack.put(uuid, stack);
            sendActionBar(p, "§e§l✦ Golden Stack: §f" + stack + "§7/§f5");
            if (stack == 5) {
                p.sendTitle("§f§l⚡", "§eTahan Right Click untuk Domain", 5, 50, 10);
                p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1f, 2f);
            }
        }

        if (p.isSneaking() && stack == 3) {
            executeMajuMundur(p);
            chargeStack.put(uuid, 0);
            sendActionBar(p, "§b§l↯ Maju Mundur Activated!");
            return;
        }

        if (p.isSneaking() && stack < 3) {
            executeAnimeBlink(p, target);
            sendActionBar(p, "§d§l✦ Anime Blink!");
            return;
        }
    }

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
                
                Location center = p.getLocation().clone().add(0, 1.2, 0);
                double radius = 3.0 - (2.5 * (progress / 100.0));
                
                for (int i = 0; i < 5; i++) {
                    double angle = (Math.PI * 2 / 5) * i + (elapsed / 200.0);
                    Location partLoc = center.clone().add(Math.cos(angle) * radius, (Math.sin(elapsed / 100.0 + i) * 0.8), Math.sin(angle) * radius);
                    
                    p.getWorld().spawnParticle(Particle.DUST, partLoc, 1, 
                        new Particle.DustOptions(Color.fromRGB(100, 200, 255), 1.2f));
                    if (progress > 50) p.getWorld().spawnParticle(Particle.FLASH, partLoc, 0);
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
        } else if (!success) {
            sendActionBar(p, "§c✦ Charging cancelled!");
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.5f);
        }
        clickHoldStart.remove(uuid);
    }

    private void executeGoldenDomain(Player p, int progress) {
        double range = DOMAIN_MIN_RANGE + ((DOMAIN_MAX_RANGE - DOMAIN_MIN_RANGE) * (progress / 100.0));
        Location center = p.getLocation().clone();
        UUID uuid = p.getUniqueId();
        
        p.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 2f, 1.5f);
        sendActionBar(p, "§f§l✦ §bGOLDEN DOMAIN §f§l✦ §7[" + progress + "%]");

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 40) {
                    Vector dir = p.getLocation().getDirection().clone().setY(0).normalize();
                    Location safeSpot = p.getLocation().clone().add(dir.multiply(-6)).add(0, 1, 0);
                    p.teleport(safeSpot);
                    p.playSound(safeSpot, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
                    startSwordStrike(center, range, progress, p);
                    this.cancel();
                    return;
                }
                
                double rotation = ticks * DOMAIN_ROTATION_SPEED;
                for (int i = 0; i < 6; i++) {
                    double angle = Math.toRadians(i * 60 + rotation);
                    Location corner = center.clone().add(Math.cos(angle) * range, (DOMAIN_HEIGHT - 1.5) + (Math.sin(ticks / 4.0 + i) * 0.3), Math.sin(angle) * range);
                    
                    Color hexColor = Color.fromRGB(150 + (int)(50 * Math.sin(ticks/5.0)), 200, 255);
                    p.getWorld().spawnParticle(Particle.DUST, corner, 2, new Particle.DustOptions(hexColor, 2.0f));
                    
                    if (i % 2 == 0) {
                        double nextA = angle + Math.toRadians(60);
                        Location next = center.clone().add(Math.cos(nextA) * range, DOMAIN_HEIGHT - 1.5, Math.sin(nextA) * range);
                        drawLine(p.getWorld(), corner, next, Particle.DUST, new Particle.DustOptions(Color.WHITE, 0.8f), 0.6);
                    }
                }
                
                center.getWorld().getNearbyEntities(center, range, range, range).forEach(en -> {
                    if (en instanceof LivingEntity le) {
                        domainAffected.add(le.getUniqueId());
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 255, false, false));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, false));
                    }
                });
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void startSwordStrike(Location center, double range, int progress, Player caster) {
        caster.getWorld().playSound(center, Sound.ENTITY_WARDEN_SNIFF, 1.5f, 0.3f);
        new BukkitRunnable() {
            int frame = 0;
            @Override
            public void run() {
                if (frame > 45) {
                    triggerSwordImpact(center, range, progress, caster);
                    this.cancel();
                    return;
                }
                double swordY = 25 - (frame * SWORD_DESCENT_SPEED);
                if (frame <= 33) {
                    drawSwordBlade(caster.getWorld(), center.clone().add(0, swordY, 0), SWORD_SIZE, frame);
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawSwordBlade(World world, Location base, double size, int anim) {
        for (double y = 0; y < size * 2; y += 0.3) {
            Location part = base.clone().subtract(0, y, 0);
            Color c = Color.fromRGB(255, 230 - (int)(y*10), 150);
            world.spawnParticle(Particle.DUST, part, 1, new Particle.DustOptions(c, (float)(2.5 - (y * 0.1))));
        }
    }

    private void triggerSwordImpact(Location center, double range, int progress, Player caster) {
        World world = center.getWorld();
        world.spawnParticle(Particle.FLASH, center, 5);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, range*0.3, 1, range*0.3, 0.1);
        world.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2f, 0.6f);
        
        double finalDamage = 10.0 + (25.0 * (progress / 100.0));
        world.getNearbyEntities(center, range, range, range).forEach(en -> {
            if (en instanceof LivingEntity le && !en.equals(caster)) {
                le.damage(finalDamage, caster);
                le.setVelocity(le.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.5).setY(0.8));
                domainAffected.add(le.getUniqueId());
            }
        });
        
        caster.sendTitle("§f§l✦ §bDOMAIN COMPLETE §f§l✦", "§eDamage: " + String.format("%.1f", finalDamage), 10, 40, 20);
    }

    private void executeAnimeBlink(Player p, LivingEntity target) {
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        target.getNearbyEntities(7, 4, 7).stream()
            .filter(en -> en instanceof LivingEntity && !en.equals(p) && targets.size() < BLINK_MAX_TARGETS)
            .forEach(en -> targets.add((LivingEntity) en));

        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 50, 0, false, false));
        
        for (LivingEntity t : targets) {
            blinkProtected.add(t.getUniqueId());
            t.setVelocity(new Vector((Math.random()-0.5)*2, BLINK_VERTICAL_BOOST, (Math.random()-0.5)*2));
        }

        new BukkitRunnable() {
            int i = 0;
            Location last = p.getLocation().clone();
            @Override
            public void run() {
                if (i >= targets.size()) {
                    LivingEntity lastT = targets.get(targets.size() - 1);
                    p.teleport(lastT.getLocation().clone().add(lastT.getLocation().getDirection().multiply(-1.5)));
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);
                    new BukkitRunnable() { @Override public void run() { blinkProtected.clear(); } }.runTaskLater(plugin, 40L);
                    this.cancel();
                    return;
                }
                LivingEntity curr = targets.get(i);
                drawTrail(p.getWorld(), last, curr.getLocation(), Color.YELLOW);
                p.teleport(curr.getLocation().clone().add(0, 0.5, 0));
                curr.damage(BLINK_DAMAGE, p);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2f);
                last = curr.getLocation().clone();
                i++;
            }
        }.runTaskTimer(plugin, 0L, 3L);
    }

    private void executeMajuMundur(Player p) {
        Vector dir = p.getLocation().getDirection().clone().setY(0).normalize();
        p.setVelocity(dir.clone().multiply(MM_BACKWARD_MULTIPLIER));
        
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setVelocity(dir.clone().multiply(MM_FORWARD_MULTIPLIER));
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);
                p.getWorld().getNearbyEntities(p.getLocation(), 3, 3, 3).forEach(en -> {
                    if (en instanceof LivingEntity le && !en.equals(p)) {
                        le.damage(MM_DAMAGE, p);
                        le.setVelocity(dir.clone().multiply(1.5).setY(0.4));
                    }
                });
            }
        }.runTaskLater(plugin, MM_DELAY_TICKS);
    }

    private void drawTrail(World world, Location from, Location to, Color color) {
        Vector diff = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        for (double d = 0; d < dist; d += 0.4) {
            world.spawnParticle(Particle.DUST, from.clone().add(diff.clone().normalize().multiply(d)), 1, new Particle.DustOptions(color, 1.5f));
        }
    }
    
    private void drawLine(World world, Location from, Location to, Particle particle, Object options, double step) {
        Vector dir = to.toVector().subtract(from.toVector());
        double dist = from.distance(to);
        for (double d = 0; d < dist; d += step) {
            world.spawnParticle(particle, from.clone().add(dir.clone().normalize().multiply(d)), 1, options);
        }
    }

    private void sendActionBar(Player p, String msg) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    private boolean isHolding(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
}
