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

import java.util.HashMap;
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

    @EventHandler    public void onQuit(PlayerQuitEvent e) {
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
        PlayerData d = get(p);
        
        if (isWearingPiece(p, EquipmentSlot.HEAD, GoldenMoon.ELITE_HELMET_KEY) && 
            p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue() * 0.7) {
            e.setDamage(e.getDamage() * 1.15);
            if (r.nextInt(100) < 30) spawnSparkle(e.getEntity().getLocation(), p.getWorld(), CRIMSON, 3);        }
        
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

    private void spawnDetailedCrescent(Player p) {        final World w = p.getWorld();
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
                
                for (double angle = -1.5; angle <= 1.5; angle += 0.15) {
                    double curve = (angle * angle) * 0.25;
                    Vector v = rotate(direction, 90).multiply(angle).add(direction.clone().multiply(-curve));
                    Color mainColor = isElite ? GOLD : SILVER;
                    w.spawnParticle(Particle.DUST, current.clone().add(v), 1, new Particle.DustOptions(mainColor, 1.4f));
                    if (life % 2 == 0) w.spawnParticle(Particle.DUST, current.clone().add(v), 1, new Particle.DustOptions(WHITE, 0.8f));
                }
                
                if (isElite && life % 3 == 0) {
                    for (double angle = -1.0; angle <= 1.0; angle += 0.3) {
                        Vector accent = rotate(direction, 90).multiply(angle * 0.7);
                        w.spawnParticle(Particle.DUST, current.clone().add(accent), 1, new Particle.DustOptions(PURPLE, 1.1f));
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
                }
                life++;
            }
        }.runTaskTimer(plugin, 0, 1);    }

    private void performLunarExecution(Player p) {
        World w = p.getWorld();
        Location center = p.getLocation();
        boolean isElite = plugin.getArmorManager().hasFullEliteSet(p);
        boolean hasCrescent = plugin.getArmorManager().hasCrescentSet(p);
        
        String subtitle = isElite ? "§6§l⚔️ ELITE MODE" : (hasCrescent ? "§b§l🛡️ CRESCENT MODE" : "§fMenyegel Takdir...");
        p.sendTitle("§6§l✦ LUNAR EXECUTION ✦", subtitle, 5, 30, 10);
        
        float soundPitch = isElite ? 0.3f : 0.5f;
        w.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, isElite ? 1.8f : 1.5f, soundPitch);

        new BukkitRunnable() {
            int t = 0;
            public void run() {
                if (t > 20) { cancel(); return; }
                Color ringColor = isElite ? GOLD : (hasCrescent ? SILVER : WHITE);
                float ringSize = isElite ? 2.0f : 1.8f;
                int ringCount = isElite ? 8 : 5;
                double ringRadius = isElite ? 5 : 4;
                
                for (int i = 0; i < 2; i++) {
                    double angle = Math.toRadians(t * 18 + (i * 180));
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    w.spawnParticle(Particle.DUST, center.clone().add(x, 0.1, z), ringCount, new Particle.DustOptions(ringColor, ringSize));
                }
                t++;
            }
        }.runTaskTimer(plugin, 0, 1);

        for (int strike = 1; strike <= 3; strike++) {
            final int s = strike;
            new BukkitRunnable() {
                public void run() {
                    w.playSound(center, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.2f, 0.5f + (s * 0.4f));
                    if (isElite) w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 1.8f);
                    
                    int particleDensity = isElite ? 7 : 5;
                    float particleSize = isElite ? 2.2f : 2.0f;
                    double slashOffset = isElite ? 2.5 : 2;
                    
                    for (double y = -3; y <= 3; y += 0.2) {
                        double xOff = (y * y) * 0.3;
                        Vector v = new Vector(xOff - slashOffset, y + 1.5, 0);
                        Vector finalV = rotate(v, s * 60); 
                        w.spawnParticle(Particle.DUST, center.clone().add(finalV), particleDensity, new Particle.DustOptions(WHITE, particleSize));
                        w.spawnParticle(Particle.DUST, center.clone().add(finalV), isElite ? 3 : 2, new Particle.DustOptions(isElite ? GOLD : SILVER, isElite ? 1.7f : 1.5f));                    }
                    
                    if (isElite) {
                        for (double y = -2.5; y <= 2.5; y += 0.4) {
                            double xOff = (y * y) * 0.25;
                            Vector v = new Vector(xOff - 2.2, y + 1.5, 0);
                            Vector finalV = rotate(v, s * 60 + 15);
                            w.spawnParticle(Particle.DUST, center.clone().add(finalV), 2, new Particle.DustOptions(PURPLE, 1.4f));
                        }
                    }
                    
                    double radius = isElite ? 9 : 7;
                    for (Entity en : w.getNearbyEntities(center, radius, 6, radius)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            LivingEntity le = (LivingEntity) en;
                            double baseDmg = moonMarked.containsKey(le.getUniqueId()) ? 15.0 : 8.0;
                            double dmg = isElite ? baseDmg * 1.25 : baseDmg;
                            le.damage(dmg, p);
                            le.setVelocity(new Vector(0, isElite ? 0.8 : 0.6, 0));
                            if (dmg > 10) moonMarked.remove(le.getUniqueId());
                            Color hitColor = isElite ? GOLD : SILVER;
                            spawnSparkle(le.getLocation().add(0, 1.2, 0), w, hitColor, isElite ? 6 : 4);
                        }
                    }
                }
            }.runTaskLater(plugin, 20 + (s * 8));
        }
        
        new BukkitRunnable() {
            public void run() {
                try {
                    if (isElite) {
                        double heal = 7.0;
                        if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                            p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + heal));
                        }
                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 250, 1, false, false));
                    } else if (hasCrescent) {
                        double heal = 6.0;
                        if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                            p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + heal));
                        }
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 200, 0, false, false));
                    }
                } catch (Exception ignored) {}
                Color finaleColor = isElite ? GOLD : (hasCrescent ? SILVER : WHITE);
                spawnSparkle(p.getLocation().add(0, 1.5, 0), w, finaleColor, isElite ? 25 : 15);
            }        }.runTaskLater(plugin, 45);
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
            case LEGS: item = p.getInventory().getLeggings(); break;
            case FEET: item = p.getInventory().getBoots(); break;
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
        return item != null && item.hasItemMeta() &&                item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
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
