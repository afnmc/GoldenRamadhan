package com.ramadhan;

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
    private final Map<UUID, Integer> comboStack = new HashMap<>();
    private final Map<UUID, BukkitRunnable> domainRunnables = new HashMap<>();
    private final Map<UUID, UUID> activeDomainOwner = new HashMap<>(); // Track siapa yang aktivasi domain
    private final Set<UUID> playersWithBuffs = new HashSet<>();

    public SkillListener(GoldenMoon plugin) {
        this.plugin = plugin;
        startTicking();
    }

    private void startTicking() {
        new BukkitRunnable() {
            double rot = 0;
            @Override
            public void run() {
                rot += 0.20;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!isHolding(p)) continue;
                    int stack = comboStack.getOrDefault(p.getUniqueId(), 0);
                    
                    drawBackMoon(p, rot, stack);
                    
                    if (stack > 0 && stack < 5) drawSideStack(p, stack);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawBackMoon(Player p, double rot, int stack) {
        Location loc = p.getLocation().add(0, 1.2, 0);
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        loc.add(dir.multiply(-0.4)); 

        Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
        
        double radius = 0.6; 
        Color particleColor = stack >= 5 ? Color.WHITE : new Color(255, 200, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(particleColor, 1.5f);

        for (double t = -1.5; t <= 1.5; t += 0.4) { 
            double taper = Math.cos(t / 2.0);
            double rx = (Math.cos(t) * taper * Math.cos(rot)) - (Math.sin(t) * Math.sin(rot));
            double ry = (Math.cos(t) * taper * Math.sin(rot)) + (Math.sin(t) * Math.cos(rot));
            
            Location pLoc = loc.clone().add(right.clone().multiply(rx * radius)).add(0, ry * radius, 0);

            p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0, 0, 0, 0, dustOptions);
            
            if (stack >= 5) {
                p.getWorld().spawnParticle(Particle.DUST, pLoc, 1, 0.05, 0.05, 0.05, 0, 
                    new Particle.DustOptions(Color.fromRGB(255, 255, 200), 1.0f));
            }
        }
    }

    private void drawSideStack(Player p, int stack) {
        Location loc = p.getLocation().add(0, 0.8, 0);
        Color yellow = new Color(255, 200, 0);
        Particle.DustOptions dustOptions = new Particle.DustOptions(yellow, 1.2f);
        
        for (int i = 0; i < stack; i++) {
            double side = (i % 2 == 0) ? 0.7 : -0.7;
            Vector v = p.getLocation().getDirection().getCrossProduct(new Vector(0,1,0)).normalize().multiply(side);
            p.getWorld().spawnParticle(Particle.DUST, loc.clone().add(v), 1, 0, 0, 0, 0, dustOptions);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !isHolding(p)) return;
        if (!(e.getEntity() instanceof LivingEntity target)) return;

        p.setVelocity(p.getLocation().getDirection().multiply(0.2).setY(0.1));
        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        
        int stack = Math.min(comboStack.getOrDefault(p.getUniqueId(), 0) + 1, 5);
        comboStack.put(p.getUniqueId(), stack);

        if (stack == 5) {
            p.sendTitle("", "§f§l● LUNAR READY ●", 0, 10, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 2f);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.5f);
            
            startGroundDomain(p);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (!isHolding(p) || !e.isSneaking()) return;
        int stack = comboStack.getOrDefault(p.getUniqueId(), 0);

        if (stack >= 5) {
            comboStack.put(p.getUniqueId(), 0);
            stopGroundDomain(p);
            executeLunarUlti(p);
        }
    }

    private void startGroundDomain(Player activator) {
        UUID activatorId = activator.getUniqueId();
        Location center = activator.getLocation();
        
        // Verifikasi: hanya satu domain aktif per pemain
        if (domainRunnables.containsKey(activatorId)) {
            stopGroundDomain(activator);
        }
        
        // Simpan siapa owner domain ini
        activeDomainOwner.put(activatorId, activatorId);
        
        // Beri efek buff ke pemilik
        applyOwnerBuffs(activator);
        playersWithBuffs.add(activatorId);

        BukkitRunnable domainTask = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!activator.isOnline()) {
                    this.cancel();
                    cleanupDomain(activatorId);
                    return;
                }
                
                // Cek apakah masih stack 5
                if (comboStack.getOrDefault(activatorId, 0) < 5) {
                    this.cancel();
                    cleanupDomain(activatorId);
                    return;
                }
                
                if (ticks > 100) { 
                    this.cancel(); 
                    cleanupDomain(activatorId);
                    return; 
                }

                // Gambar domain
                drawDomainEffects(center, activatorId, ticks);
                
                // Apply efek ke musuh di dalam domain
                applyDebuffsToEnemies(center, activator, 15.0);
                
                // Refresh buff owner setiap 20 tick (1 detik)
                if (ticks % 20 == 0 && activator.isOnline()) {
                    applyOwnerBuffs(activator);
                }
                
                ticks += 1;
            }
        };
        
        domainTask.runTaskTimer(plugin, 0L, 1L);
        domainRunnables.put(activatorId, domainTask);
        
        // Broadcast efek visual
        activator.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.0f);
    }

    private void drawDomainEffects(Location center, UUID ownerId, int ticks) {
        // Lingkaran luar - radius 15
        double radius = 15.0;
        Color domainColor = new Color(255, 220, 50);
        Particle.DustOptions domainDust = new Particle.DustOptions(domainColor, 2.0f);
        
        for (double i = 0; i < Math.PI * 2; i += 0.15) {
            double x = Math.cos(i) * radius;
            double z = Math.sin(i) * radius;
            Location particleLoc = center.clone().add(x, 0.05, z);
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, domainDust);
        }
        
        // Lingkaran dalam (efek berdenyut)
        double innerRadius = 11.0 + Math.sin(ticks * 0.1) * 2.0;
        for (double i = 0; i < Math.PI * 2; i += 0.2) {
            double x = Math.cos(i) * innerRadius;
            double z = Math.sin(i) * innerRadius;
            Location particleLoc = center.clone().add(x, 0.05, z);
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, 
                new Particle.DustOptions(Color.WHITE, 1.5f));
        }

        // Lingkaran tengah
        double midRadius = 6.0 + Math.cos(ticks * 0.15) * 1.0;
        for (double i = 0; i < Math.PI * 2; i += 0.25) {
            double x = Math.cos(i) * midRadius;
            double z = Math.sin(i) * midRadius;
            Location particleLoc = center.clone().add(x, 0.05, z);
            particleLoc.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                new Particle.DustOptions(new Color(255, 180, 0), 1.8f));
        }

        // Logo bulan sabit besar di tengah
        drawLargeCrescent(center, ticks);
    }

    private void drawLargeCrescent(Location loc, int ticks) {
        Color yellow = new Color(255, 200, 0);
        Color white = Color.WHITE;
        double pulse = 1.0 + Math.sin(ticks * 0.15) * 0.1;
        
        for (double t = -1.5; t <= 1.5; t += 0.08) {
            double taper = Math.cos(t / 2.0);
            double x1 = Math.cos(t) * taper * 7.0 * pulse;
            double z1 = Math.sin(t) * 7.0 * pulse;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x1 - 3.0, 0.05, z1), 1, 0, 0, 0, 0,
                new Particle.DustOptions(yellow, 3.0f));
        }
        
        for (double t = -1.3; t <= 1.3; t += 0.1) {
            double x2 = Math.cos(t) * 5.5 * pulse;
            double z2 = Math.sin(t) * 5.5 * pulse;
            loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x2 - 2.2, 0.05, z2), 1, 0, 0, 0, 0,
                new Particle.DustOptions(white, 2.5f));
        }
    }

    private void applyOwnerBuffs(Player owner) {
        // Hapus efek lama dulu
        clearOwnerBuffs(owner);
        
        // BUFF UNTUK PEMILIK (Tidak OP, balanced untuk PvP):
        // Speed I - gerak lebih cepat sedikit
        owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 25, 0, false, false));
        
        // Strength I - damage naik sedikit
        owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 25, 0, false, false));
        
        // Absorption I - tambahan 2 heart shield
        owner.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 25, 0, false, false));
        
        // Regeneration I - heal pelan-pelan
        owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 25, 0, false, false));
        
        // Visual indicator
        owner.spawnParticle(Particle.HAPPY_VILLAGER, owner.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.01);
    }

    private void applyDebuffsToEnemies(Location center, Player owner, double radius) {
        for (Entity entity : owner.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof Player enemy && enemy != owner) {
                // Cek apakah enemy ada dalam line of sight (optional, bisa dihapus)
                if (owner.hasLineOfSight(enemy) || enemy.getLocation().distance(owner.getLocation()) <= radius) {
                    
                    // DEBUFF UNTUK MUSUH (Balanced untuk PvP):
                    // Slowness I - gerak lebih lambat 15%
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 25, 0, false, false));
                    
                    // Weakness I - damage turun sedikit
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 25, 0, false, false));
                    
                    // Blindness singkat (setiap 2 detik selama 0.5 detik) - tidak terlalu OP
                    if (System.currentTimeMillis() % 4000 < 1000) {
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10, 0, false, false));
                    }
                    
                    // Visual indicator untuk musuh
                    enemy.spawnParticle(Particle.SMOKE_NORMAL, enemy.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }
    }

    private void clearOwnerBuffs(Player owner) {
        owner.removePotionEffect(PotionEffectType.SPEED);
        owner.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        owner.removePotionEffect(PotionEffectType.ABSORPTION);
        owner.removePotionEffect(PotionEffectType.REGENERATION);
    }

    private void cleanupDomain(UUID playerId) {
        domainRunnables.remove(playerId);
        activeDomainOwner.remove(playerId);
        
        // Hapus buff dari pemilik
        Player owner = Bukkit.getPlayer(playerId);
        if (owner != null && owner.isOnline()) {
            clearOwnerBuffs(owner);
            playersWithBuffs.remove(playerId);
            
            owner.sendTitle("", "§c§lDOMAIN ENDED", 0, 10, 5);
            owner.playSound(owner.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.8f);
        }
    }

    private void stopGroundDomain(Player p) {
        UUID playerId = p.getUniqueId();
        BukkitRunnable task = domainRunnables.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        cleanupDomain(playerId);
    }

    private void executeLunarUlti(Player p) {
        Location center = p.getLocation();
        
        // Clear buff sebelum ultimate
        clearOwnerBuffs(p);
        playersWithBuffs.remove(p.getUniqueId());
        
        // Efek suara
        p.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.2f);
        p.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
        
        // Visual ledakan
        drawLunarArenaExplosion(center);

        // Damage ke musuh dalam radius 15
        int hitCount = 0;
        for (Entity en : p.getNearbyEntities(15, 15, 15)) {
            if (en instanceof LivingEntity le && en != p) {
                // Visual hit
                le.getWorld().spawnParticle(Particle.SONIC_BOOM, le.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
                le.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, le.getLocation(), 1, 0.5, 0.5, 0.5, 0.1);
                
                // Damage (22 = 11 hearts - cukup kuat tapi tidak one-shot)
                le.damage(22, p);
                
                // Knockup vertical
                le.setVelocity(new Vector(0, 1.2, 0));
                
                hitCount++;
            }
        }
        
        // Feedback ke player
        p.sendTitle("§f§lLUNAR ULTIMATE", String.format("§eHit %d enemies!", hitCount), 0, 20, 10);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        
        // Cooldown message
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline()) {
                    p.sendMessage("§7[§bGolden Moon§7] §fSkill ready again!");
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2f);
                }
            }
        }.runTaskLater(plugin, 200L); // 10 detik cooldown message
    }

    private void drawLunarArenaExplosion(Location loc) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 40) { 
                    this.cancel(); 
                    return; 
                }

                double expandRadius = 15.0 + (ticks * 0.5);
                Color pulseColor = new Color(255, 255 - (ticks * 3), 100);
                
                for (double i = 0; i < Math.PI * 2; i += 0.2) {
                    double x = Math.cos(i) * expandRadius;
                    double z = Math.sin(i) * expandRadius;
                    loc.getWorld().spawnParticle(Particle.DUST, loc.clone().add(x, 0.1, z), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(pulseColor, 2.0f));
                }
                
                // Partikel tambahan
                if (ticks % 5 == 0) {
                    loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc.clone().add(0, 0.5, 0), 
                        10, 3, 1, 3, 0.1);
                }

                ticks += 1;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean isHolding(Player p) {
        ItemStack i = p.getInventory().getItemInMainHand();
        return i != null && i.hasItemMeta() && i.getItemMeta().getPersistentDataContainer().has(plugin.SWORD_KEY, PersistentDataType.BYTE);
    }
    
    // Getter untuk combo stack (bisa digunakan class lain)
    public int getComboStack(Player p) {
        return comboStack.getOrDefault(p.getUniqueId(), 0);
    }
}
