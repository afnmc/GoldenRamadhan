package com.ramadhan;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private static final Color Y = Color.fromRGB(255, 230, 100);
    private static final Color W = Color.fromRGB(255, 250, 240);
    private static final Color S = Color.fromRGB(220, 220, 230);
    private static final Color G = Color.fromRGB(255, 240, 150);
    private static final Color A = Color.fromRGB(255, 220, 120);
    private static final Color P = Color.fromRGB(180, 160, 220);

    private final GoldenMoon plugin;
    private final ArmorManager armor;
    private final Map<UUID, D> data = new HashMap<>();
    private final Random r = new Random();
    private static final int MAX = 100, GPH = 15, HOLD = 250;

    public SkillListener(GoldenMoon p) {
        this.plugin = p;        this.armor = p.getArmorManager();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        data.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onD(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();
        if (!isB(p)) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity t = (LivingEntity) e.getEntity();
        D d = get(p);
        long n = System.currentTimeMillis();

        if (n - d.lt < 120) return;
        d.lt = n;
        d.g = Math.min(MAX, d.g + GPH);

        if (d.ls == 0) {
            d.ls = n;
        } else if (n - d.ls >= HOLD && !d.s1) {
            d.s1 = true;
            slash(p, t);
            return;
        }
        if (n - d.ls > HOLD + 300) {
            d.ls = 0;
            d.s1 = false;
        }
        t.damage(2, p);
        spr(t.getLocation().add(0, 1, 0), p.getWorld(), G, 25);
    }

    @EventHandler
    public void onPD(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (hasS(p)) {
            e.setDamage(e.getDamage() * 0.85f);
            if (r.nextInt(100) < 30) spr(p.getLocation().add(0, 1.3f, 0), p.getWorld(), S, 20);
        }
    }

    @EventHandler
    public void onI(PlayerInteractEvent e) {
        Player p = e.getPlayer();        if (!isB(p)) return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            D d = get(p);
            if (d.g >= MAX && !d.ch && !d.s3) {
                d.ch = true;
                d.cs = System.currentTimeMillis();
                sab(p, "§6§l✦ §fMenahan... §7(Lepas untuk Ultimate)");
                chg(p);
            } else if (d.ch) {
                long ct = System.currentTimeMillis() - d.cs;
                if (ct >= 1000) {
                    d.ch = false;
                    ult(p);
                    d.g = 0;
                    d.s3 = true;
                    sab(p, "§6§l✦ §f🌕 PANGGILAN BULAN AKTIF!");
                    new BukkitRunnable() {
                        public void run() {
                            if (data.containsKey(p.getUniqueId())) get(p).s3 = false;
                        }
                    }.runTaskLater(plugin, 1200);
                } else {
                    d.ch = false;
                    sab(p, "§c✦ §fTahan minimal 1 detik!");
                }
            }
        }
        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && p.isSneaking()) {
            D d = get(p);
            if (!d.s2) {
                e.setCancelled(true);
                storm(p);
                d.s2 = true;
                sab(p, "§e§l✦ §f☁️ HUJAN BERKAH! §7(3s cooldown)");
                new BukkitRunnable() {
                    public void run() {
                        if (data.containsKey(p.getUniqueId())) get(p).s2 = false;
                    }
                }.runTaskLater(plugin, 60);
            }
        }
    }

    @EventHandler
    public void onM(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isB(p)) return;
        D d = get(p);
        if (d.ch && System.currentTimeMillis() % 100 < 25) spr(p.getLocation().add(0, 1.6f, 0), p.getWorld(), Y, 12);        if (armor.tryMoonStep(p)) {
            d.ms = false;
            new BukkitRunnable() {
                public void run() {
                    if (data.containsKey(p.getUniqueId())) get(p).ms = true;
                }
            }.runTaskLater(plugin, 60);
        }
    }

    // ==========================================
    // ⚔️ SKILL 1: MOONLIGHT SLASH (EXTENDED)
    // ==========================================
    private void slash(final Player p, final LivingEntity t) {
        final World w = p.getWorld();
        final Location pl = p.getLocation().clone();
        final Location tl = t.getLocation().clone();
        final Vector dir = tl.toVector().subtract(pl.toVector()).setY(0).normalize();
        final double dist = pl.distance(tl);

        // PHASE 1: Charge buildup (6 frames, rotating rings)
        for (int f = 0; f < 6; f++) {
            final int fr = f;
            new BukkitRunnable() {
                public void run() {
                    Vector so = p.getLocation().getDirection().multiply(0.9f);
                    Location sl = pl.clone().add(so);
                    
                    // Outer ring - 30 particles
                    for (int i = 0; i < 30; i++) {
                        double a = Math.toRadians(i * 12 + fr * 15);
                        Vector ro = new Vector(Math.cos(a) * 0.8f, 0.45f + (float) (Math.sin(fr * 0.5) * 0.35), Math.sin(a) * 0.8f);
                        Color c = fr % 2 == 0 ? Y : W;
                        w.spawnParticle(Particle.DUST, sl.clone().add(ro), 1, new Particle.DustOptions(c, 1.7f));
                    }
                    
                    // Inner sparkle ring - 20 particles
                    for (int i = 0; i < 20; i++) {
                        double a = Math.toRadians(i * 18 + fr * 20);
                        Vector ro = new Vector(Math.cos(a) * 0.4f, 0.35f, Math.sin(a) * 0.4f);
                        w.spawnParticle(Particle.DUST, sl.clone().add(ro), 1, new Particle.DustOptions(G, 1.4f));
                    }
                    
                    // Flame bursts
                    if (fr % 2 == 0) {
                        for (int i = 0; i < 12; i++) w.spawnParticle(Particle.FLAME, sl, 1, 0.18f, 0.18f, 0.18f, 0);
                        spr(sl, w, G, 10);
                    }
                    
                    // Sound buildup                    if (fr == 3 || fr == 5) w.playSound(sl, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, 1.5f + fr * 0.1f);
                }
            }.runTaskLater(plugin, f * 2);
        }

        // PHASE 2: Short dash with sword trail (8 frames)
        new BukkitRunnable() {
            int df = 0;
            public void run() {
                if (df >= 8) {
                    cancel();
                    return;
                }
                float pr = (float) df / 7f;
                Vector dm = dir.clone().multiply(1.5f * pr);
                Location dl = pl.clone().add(dm);
                Location sl = dl.clone().add(dir.clone().multiply(0.8f));
                
                // Core blade - Yellow (30 particles)
                for (int i = 0; i < 30; i++) w.spawnParticle(Particle.DUST, sl, 1, new Particle.DustOptions(Y, 2.6f));
                
                // Inner blade - White (25 particles)
                for (int i = 0; i < 25; i++) w.spawnParticle(Particle.DUST, sl, 1, new Particle.DustOptions(W, 2.1f));
                
                // Aura glow - Amber (20 particles)
                for (int i = 0; i < 20; i++) w.spawnParticle(Particle.DUST, sl, 1, new Particle.DustOptions(A, 1.8f));
                
                // Wind trail - Silver (20 particles with random spread)
                for (int i = 0; i < 20; i++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    Vector wo = new Vector(Math.cos(a) * 0.7f, (float) (r.nextDouble() * 0.6), Math.sin(a) * 0.7f);
                    w.spawnParticle(Particle.DUST, sl.clone().add(wo), 1, new Particle.DustOptions(S, 1.5f));
                }
                
                // Sparkle bursts every 2 frames
                if (df % 2 == 0) {
                    for (int s = 0; s < 12; s++) {
                        Vector ss = new Vector((float) ((r.nextDouble() - 0.5) * 0.9), (float) (r.nextDouble() * 0.8), (float) ((r.nextDouble() - 0.5) * 0.9));
                        w.spawnParticle(Particle.DUST, sl.clone().add(ss), 1, new Particle.DustOptions(G, 1.4f));
                    }
                }
                
                // Flame accents every 3 frames
                if (df % 3 == 0) {
                    for (int f = 0; f < 8; f++) w.spawnParticle(Particle.FLAME, sl, 1, 0.2f, 0.2f, 0.2f, 0);
                }
                
                df++;
            }
        }.runTaskTimer(plugin, 12, 1);
        // PHASE 3: Grid slash impact (5x5 grid pattern)
        new BukkitRunnable() {
            public void run() {
                w.playSound(tl, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.1f, 2.1f);
                w.playSound(tl, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.6f);
                w.spawnParticle(Particle.EXPLOSION, tl, 3);
                
                // Grid lines - 5x5 with 5 particles each
                for (int row = -4; row <= 4; row++) {
                    for (int col = -4; col <= 4; col++) {
                        Vector go = new Vector(col * 0.6f, row * 0.7f, 0);
                        go = rot(go, dir);
                        Color c = (row + col) % 2 == 0 ? Y : W;
                        for (int i = 0; i < 5; i++) w.spawnParticle(Particle.DUST, tl.clone().add(go), 1, new Particle.DustOptions(c, 1.6f));
                    }
                }
                
                // Massive yellow burst (100 particles)
                for (int i = 0; i < 100; i++) {
                    Vector sp = new Vector((float) ((r.nextDouble() - 0.5) * 3.5), (float) (r.nextDouble() * 3), (float) ((r.nextDouble() - 0.5) * 3.5));
                    w.spawnParticle(Particle.DUST, tl.clone().add(sp), 1, new Particle.DustOptions(Y, 1.5f + (float) (r.nextDouble() * 0.7f)));
                }
                
                // Massive white burst (75 particles)
                for (int i = 0; i < 75; i++) {
                    Vector sp = new Vector((float) ((r.nextDouble() - 0.5) * 3), (float) (r.nextDouble() * 2.5), (float) ((r.nextDouble() - 0.5) * 3));
                    w.spawnParticle(Particle.DUST, tl.clone().add(sp), 1, new Particle.DustOptions(W, 1.4f + (float) (r.nextDouble() * 0.6f)));
                }
                
                // Flame explosion (40 particles)
                for (int i = 0; i < 40; i++) {
                    Vector sp = new Vector((float) ((r.nextDouble() - 0.5) * 2.5), (float) (r.nextDouble() * 2), (float) ((r.nextDouble() - 0.5) * 2.5));
                    w.spawnParticle(Particle.FLAME, tl.clone().add(sp), 1, 0.22f, 0.22f, 0.22f, 0.08f);
                }
                
                // Sparkle shower (30 delayed particles)
                for (int i = 0; i < 30; i++) {
                    final int sk = i;
                    new BukkitRunnable() {
                        public void run() {
                            Vector sp = new Vector((float) ((r.nextDouble() - 0.5) * 2), (float) (r.nextDouble() * 1.5), (float) ((r.nextDouble() - 0.5) * 2));
                            w.spawnParticle(Particle.DUST, tl.clone().add(sp), 1, new Particle.DustOptions(G, 1.3f));
                        }
                    }.runTaskLater(plugin, sk);
                }
                
                t.damage(4, p);
                t.setVelocity(dir.clone().multiply(0.8f).setY(0.5f));
            }        }.runTaskLater(plugin, 20);

        // PHASE 4: Lingering afterglow (18 frames, 7x7 grid fade)
        new BukkitRunnable() {
            int gf = 0;
            public void run() {
                if (gf >= 18) {
                    cancel();
                    return;
                }
                double pu = Math.sin(gf * 0.5) * 0.3 + 0.85;
                for (int row = -5; row <= 5; row++) {
                    for (int col = -5; col <= 5; col++) {
                        if (r.nextInt(2) == 0) continue;
                        Vector go = new Vector(row * 0.6f, 0, col * 0.6f);
                        w.spawnParticle(Particle.DUST, tl.clone().add(go), 1, new Particle.DustOptions(W, (float) (1.4f * pu)));
                    }
                }
                if (gf % 2 == 0) {
                    spr(tl, w, G, 15);
                    spr(tl.clone().add(0.5, 0.4, 0), w, Y, 10);
                }
                if (gf % 3 == 0) {
                    for (int f = 0; f < 6; f++) w.spawnParticle(Particle.FLAME, tl, 1, 0.15f, 0.15f, 0.15f, 0);
                }
                gf++;
            }
        }.runTaskTimer(plugin, 32, 2);

        // Reset skill state
        new BukkitRunnable() {
            public void run() {
                if (data.containsKey(p.getUniqueId())) {
                    get(p).s1 = false;
                    get(p).ls = 0;
                }
            }
        }.runTaskLater(plugin, 50);
    }

    // ==========================================
    // ✨ SKILL 2: BLESSING STORM (EXTENDED)
    // ==========================================
    private void storm(final Player p) {
        final World w = p.getWorld();
        final Location c = p.getLocation().clone();

        // PHASE 1: Sky preparation (28 frames, cloud rings)
        new BukkitRunnable() {
            int pf = 0;            public void run() {
                if (pf >= 28) {
                    cancel();
                    return;
                }
                float cr = 5.5f + (float) (Math.sin(pf * 0.3) * 0.8);
                
                // Outer cloud ring - 25 particles x4
                for (int i = 0; i < 25; i++) {
                    double a = Math.toRadians(i * 14.4 + pf * 3);
                    Vector co = new Vector(Math.cos(a) * cr, 9.5f + (float) (Math.sin(pf * 0.4) * 0.7), Math.sin(a) * cr);
                    w.spawnParticle(Particle.DUST, c.clone().add(co), 4, new Particle.DustOptions(S, 1.6f));
                }
                
                // Inner cloud ring - 16 particles x2
                for (int i = 0; i < 16; i++) {
                    double a = Math.toRadians(i * 22.5 + pf * 4);
                    Vector co = new Vector(Math.cos(a) * (cr * 0.7), 8.8f, Math.sin(a) * (cr * 0.7));
                    w.spawnParticle(Particle.DUST, c.clone().add(co), 2, new Particle.DustOptions(W, 1.4f));
                }
                
                // Lightning flashes
                if (pf % 4 == 0 && r.nextInt(2) == 0) {
                    w.spawnParticle(Particle.FLASH, c.clone().add(0, 10.5, 0), 3);
                    spr(c.clone().add(0, 10, 0), w, G, 8);
                    w.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.8f);
                }
                
                pf++;
            }
        }.runTaskTimer(plugin, 0, 2);

        // PHASE 2: Falling blessing orbs (12 orbs with trails)
        for (int o = 0; o < 12; o++) {
            final int ob = o;
            new BukkitRunnable() {
                public void run() {
                    double an = r.nextDouble() * Math.PI * 2;
                    double di = 1.8 + r.nextDouble() * 5.5;
                    final float tx = (float) ((r.nextDouble() - 0.5) * 1.8);
                    final float tz = (float) ((r.nextDouble() - 0.5) * 1.8);
                    final Location ds = c.clone().add(Math.cos(an) * di + tx, 19, Math.sin(an) * di + tz);
                    final float dx = (float) ((r.nextDouble() - 0.5) * 0.09);
                    final float dz = (float) ((r.nextDouble() - 0.5) * 0.09);
                    
                    new BukkitRunnable() {
                        int ff = 0;
                        public void run() {
                            if (ff >= 38) {
                                Location il = ds.clone();                                il.setY(c.getY());
                                
                                // Impact explosion
                                w.spawnParticle(Particle.EXPLOSION, il, 4);
                                
                                // Yellow burst (60 particles)
                                for (int i = 0; i < 60; i++) w.spawnParticle(Particle.DUST, il, 1, new Particle.DustOptions(Y, 2.1f));
                                
                                // White burst (45 particles)
                                for (int i = 0; i < 45; i++) w.spawnParticle(Particle.DUST, il, 1, new Particle.DustOptions(W, 1.7f));
                                
                                // Flame burst (30 particles)
                                for (int i = 0; i < 30; i++) w.spawnParticle(Particle.FLAME, il, 1, 0.45f, 0.45f, 0.45f, 0.18f);
                                
                                // Sparkle shower (25 delayed particles)
                                for (int i = 0; i < 25; i++) {
                                    final int sk = i;
                                    new BukkitRunnable() {
                                        public void run() {
                                            Vector sp = new Vector((float) ((r.nextDouble() - 0.5) * 2), (float) (r.nextDouble() * 1.5), (float) ((r.nextDouble() - 0.5) * 2));
                                            w.spawnParticle(Particle.DUST, il.clone().add(sp), 1, new Particle.DustOptions(G, 1.4f));
                                        }
                                    }.runTaskLater(plugin, sk);
                                }
                                
                                w.playSound(il, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.9f, 1.8f);
                                
                                // Damage entities in 3.8-block radius
                                for (Entity en : w.getNearbyEntities(il, 3.8f, 3.8f, 3.8f)) {
                                    if (en instanceof LivingEntity && !en.equals(p)) {
                                        LivingEntity le = (LivingEntity) en;
                                        le.damage(6, p);
                                        le.setVelocity(new Vector(0, 0.7f, 0));
                                        spr(le.getLocation().add(0, 1, 0), w, G, 12);
                                    }
                                }
                                
                                // Lingering glow (25 frames)
                                new BukkitRunnable() {
                                    int gf = 0;
                                    public void run() {
                                        if (gf >= 25) {
                                            cancel();
                                            return;
                                        }
                                        float pu = 1f + (float) (Math.sin(gf * 0.4) * 0.35);
                                        for (int i = 0; i < 10; i++) w.spawnParticle(Particle.DUST, il, 1, new Particle.DustOptions(W, pu));
                                        if (gf % 2 == 0) {
                                            spr(il, w, G, 8);
                                            spr(il.clone().add(0.3, 0.2, 0.3), w, Y, 5);                                        }
                                        gf++;
                                    }
                                }.runTaskTimer(plugin, 0, 2);
                                
                                cancel();
                                return;
                            }
                            
                            // Falling orb core (8 particles)
                            Location cl = ds.clone();
                            cl.setY(ds.getY() - ff * 0.52f);
                            cl.add(dx * ff, 0, dz * ff);
                            for (int i = 0; i < 8; i++) w.spawnParticle(Particle.DUST, cl, 1, new Particle.DustOptions(Y, 1.9f));
                            
                            // Amber glow (6 particles)
                            for (int i = 0; i < 6; i++) w.spawnParticle(Particle.DUST, cl, 1, new Particle.DustOptions(A, 1.5f));
                            
                            // Wind trail (8 particles)
                            for (int tr = 0; tr < 8; tr++) {
                                Location tl = cl.clone().add(0, tr * 0.55f + 0.45f, 0);
                                w.spawnParticle(Particle.DUST, tl, 1, new Particle.DustOptions(S, 1.4f));
                            }
                            
                            // Sparkle around orb
                            if (ff % 3 == 0) spr(cl, w, G, 10);
                            
                            // Flame accent
                            if (r.nextInt(3) == 0) {
                                for (int f = 0; f < 5; f++) w.spawnParticle(Particle.FLAME, cl, 1, 0.18f, 0.18f, 0.18f, 0);
                            }
                            
                            ff++;
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
            }.runTaskLater(plugin, ob * 2 + r.nextInt(5));
        }

        // PHASE 3: Ground blessing wave (35 frames, expanding rings)
        new BukkitRunnable() {
            int wf = 0;
            public void run() {
                if (wf >= 35) {
                    cancel();
                    return;
                }
                float ra = 2.6f + wf * 0.19f;
                if (ra <= 7f) {
                    // Outer ring - 50 particles                    for (int a = 0; a < 50; a++) {
                        double an = Math.toRadians(a * 7.2 + wf * 4);
                        Vector ro = new Vector(Math.cos(an) * ra, 0.16f, Math.sin(an) * ra);
                        w.spawnParticle(Particle.DUST, c.clone().add(ro), 1, new Particle.DustOptions(W, 1.7f));
                    }
                    // Inner ring - 30 particles
                    for (int a = 0; a < 30; a++) {
                        double an = Math.toRadians(a * 12 + wf * 5);
                        Vector ro = new Vector(Math.cos(an) * (ra * 0.7), 0.12f, Math.sin(an) * (ra * 0.7));
                        w.spawnParticle(Particle.DUST, c.clone().add(ro), 1, new Particle.DustOptions(Y, 1.5f));
                    }
                }
                // Random sparkle showers
                if (wf % 3 == 0) {
                    for (int s = 0; s < 15; s++) {
                        final int sk = s;
                        new BukkitRunnable() {
                            public void run() {
                                Location sl = c.clone().add((r.nextDouble() - 0.5) * 8, 2.2 + r.nextDouble() * 7, (r.nextDouble() - 0.5) * 8);
                                spr(sl, w, G, 6);
                            }
                        }.runTaskLater(plugin, sk);
                    }
                }
                wf++;
            }
        }.runTaskTimer(plugin, 45, 2);
    }

    // ==========================================
    // 🌕 SKILL 3: MOON DOMAIN ULTIMATE (EXTENDED)
    // ==========================================
    private void ult(final Player p) {
        final World w = p.getWorld();
        final Location c = p.getLocation().clone();
        final float dr = 7.5f;

        p.setVelocity(new Vector(0, 0.55f, 0));
        p.setInvulnerable(true);
        p.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.3f, 0.99f);
        p.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.9f, 0.96f);
        p.sendTitle("§f§l🌕", "§6§l✦ PANGGILAN BULAN ✦", 8, 32, 11);
        sab(p, "§6§l🌙 §fDomain Bulan Aktif...");

        // PHASE 1: Domain boundary summon (35 frames, hexagon corners)
        new BukkitRunnable() {
            int fr = 0;
            public void run() {
                if (fr >= 35) {
                    cancel();                    return;
                }
                float pr = (float) fr / 34f;
                float cr = dr * pr;
                for (int co = 0; co < 6; co++) {
                    double an = Math.toRadians(co * 60 + fr * 4);
                    Vector of = new Vector(Math.cos(an) * cr, 0.35f + pr * 0.8f, Math.sin(an) * cr);
                    Location cl = c.clone().add(of);
                    // Corner core - 8 particles
                    for (int i = 0; i < 8; i++) w.spawnParticle(Particle.DUST, cl, 1, new Particle.DustOptions(Y, 2f));
                    // Corner glow - 6 particles
                    for (int i = 0; i < 6; i++) w.spawnParticle(Particle.DUST, cl, 1, new Particle.DustOptions(A, 1.6f));
                }
                fr++;
            }
        }.runTaskTimer(plugin, 0, 2);

        // PHASE 2: Rising moon blade (38 frames, 9x7 grid)
        new BukkitRunnable() {
            int bf = 0;
            public void run() {
                if (bf >= 38) {
                    cancel();
                    return;
                }
                float y = 6.5f + bf * 0.48f;
                Location bl = c.clone().add(0, y, 0);
                // Blade grid - 9x7 with 3 particles each
                for (int row = -5; row <= 5; row++) {
                    for (int col = -4; col <= 4; col++) {
                        if (r.nextInt(2) == 0) continue;
                        Vector bo = new Vector(col * 0.65f, row * 0.48f, 0);
                        Color cc = (row + col) % 2 == 0 ? Y : W;
                        for (int i = 0; i < 3; i++) w.spawnParticle(Particle.DUST, bl.clone().add(bo), 1, new Particle.DustOptions(cc, 1.7f));
                    }
                }
                // Blade glow aura
                if (bf % 4 == 0) spr(bl, w, Y, 25);
                // Sparkle rain
                if (bf % 3 == 0) {
                    for (int s = 0; s < 10; s++) {
                        final int sk = s;
                        new BukkitRunnable() {
                            public void run() {
                                Vector sp = new Vector((float) ((r.nextDouble() - 0.5) * 4), (float) (r.nextDouble() * 1.8), (float) ((r.nextDouble() - 0.5) * 4));
                                w.spawnParticle(Particle.DUST, bl.clone().add(sp), 1, new Particle.DustOptions(G, 1.5f));
                            }
                        }.runTaskLater(plugin, sk);
                    }
                }                bf++;
            }
        }.runTaskTimer(plugin, 28, 2);

        // PHASE 3: Moon crash impact
        new BukkitRunnable() {
            public void run() {
                w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.2f, 1.6f);
                w.spawnParticle(Particle.DUST, c, 120, new Particle.DustOptions(Y, 2.3f));
                w.spawnParticle(Particle.DUST, c, 90, new Particle.DustOptions(W, 1.9f));
                w.spawnParticle(Particle.EXPLOSION, c, 5);

                // Light pillars - 14 pillars with 25 height each
                for (int i = 0; i < 14; i++) {
                    final int id = i;
                    new BukkitRunnable() {
                        public void run() {
                            double an = Math.toRadians(id * 25.7);
                            Location pl = c.clone().add(Math.cos(an) * dr * 0.92f, 0, Math.sin(an) * dr * 0.92f);
                            new BukkitRunnable() {
                                int h = 0;
                                public void run() {
                                    if (h >= 25) {
                                        cancel();
                                        return;
                                    }
                                    Location pl2 = pl.clone().add(0, h, 0);
                                    // Pillar core - 7 particles
                                    for (int j = 0; j < 7; j++) w.spawnParticle(Particle.DUST, pl2, 1, new Particle.DustOptions(W, 1.8f));
                                    // Pillar glow - 4 particles
                                    for (int j = 0; j < 4; j++) w.spawnParticle(Particle.DUST, pl2, 1, new Particle.DustOptions(A, 1.5f));
                                    // Flame core
                                    if (h % 3 == 0) w.spawnParticle(Particle.FLAME, pl2, 3, 0.15f, 0.12f, 0.15f, 0);
                                    h++;
                                }
                            }.runTaskTimer(plugin, 0, 2);
                        }
                    }.runTaskLater(plugin, i * 2);
                }

                // Damage + effects in 7.5-block radius
                for (Entity en : w.getNearbyEntities(c, dr, dr, dr)) {
                    if (en instanceof LivingEntity && !en.equals(p)) {
                        LivingEntity le = (LivingEntity) en;
                        le.damage(8, p);
                        le.setVelocity(new Vector(0, 0.85f, 0));
                        spr(le.getLocation().add(0, 1.4f, 0), w, G, 10);
                    }
                }
                // Self heal + buffs
                try {
                    if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue())
                        p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(), p.getHealth() + 7.5f));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 240, 1, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, false, false));
                } catch (Exception ignored) {
                }
            }
        }.runTaskLater(plugin, 65);

        // PHASE 4: Finale blessing burst
        new BukkitRunnable() {
            public void run() {
                p.setInvulnerable(false);
                spr(c.clone().add(0, 1.8f, 0), w, Y, 30);
                w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.1f, 1.7f);
                w.playSound(c, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 2.5f);
                sab(p, "§6§l✦ §fBerkah Bulan Menyertaimu!");

                // Rising golden particles (32 frames)
                new BukkitRunnable() {
                    int ff = 0;
                    public void run() {
                        if (ff >= 32) {
                            cancel();
                            return;
                        }
                        for (int i = 0; i < 16; i++) {
                            double an = Math.toRadians(i * 22.5 + ff * 10);
                            Vector of = new Vector(Math.cos(an) * (1.9f + ff * 0.16f), ff * 0.21f, Math.sin(an) * (1.9f + ff * 0.16f));
                            w.spawnParticle(Particle.DUST, p.getLocation().clone().add(of), 4, new Particle.DustOptions(Y, 1.9f));
                        }
                        // Sparkle shower
                        if (ff % 4 == 0) {
                            for (int s = 0; s < 15; s++) {
                                final int sk = s;
                                new BukkitRunnable() {
                                    public void run() {
                                        Vector sp = new Vector((float) ((r.nextDouble() - 0.5) * 3.5), 1.4f + (float) (r.nextDouble() * 2.4), (float) ((r.nextDouble() - 0.5) * 3.5));
                                        w.spawnParticle(Particle.DUST, p.getLocation().clone().add(sp), 1, new Particle.DustOptions(G, 1.6f));
                                    }
                                }.runTaskLater(plugin, sk);
                            }
                        }
                        // Flame accents
                        if (ff % 5 == 0) {
                            for (int f = 0; f < 8; f++) w.spawnParticle(Particle.FLAME, p.getLocation(), 1, 0.2f, 0.2f, 0.2f, 0);
                        }
                        ff++;                    }
                }.runTaskTimer(plugin, 0, 2);
            }
        }.runTaskLater(plugin, 110);
    }

    // ==========================================
    // 🔋 CHARGE ANIMATION
    // ==========================================
    private void chg(final Player p) {
        new BukkitRunnable() {
            int pu = 0;
            public void run() {
                D d = get(p);
                if (!d.ch || !p.isOnline()) {
                    cancel();
                    return;
                }
                double ra = 1.3 + Math.sin(pu * 0.38) * 0.65;
                // Outer aura ring - 24 particles x4
                for (int a = 0; a < 24; a++) {
                    double an = Math.toRadians(a * 15);
                    Location al = p.getLocation().add(Math.cos(an) * ra, 0.75f + (float) (Math.sin(pu * 0.28) * 0.45), Math.sin(an) * ra);
                    p.getWorld().spawnParticle(Particle.DUST, al, 4, new Particle.DustOptions(Y, 1.9f));
                }
                // Inner sparkle ring - 16 particles x2
                for (int a = 0; a < 16; a++) {
                    double an = Math.toRadians(a * 22.5 + pu * 5);
                    Location al = p.getLocation().add(Math.cos(an) * (ra * 0.6), 0.65f, Math.sin(an) * (ra * 0.6));
                    p.getWorld().spawnParticle(Particle.DUST, al, 2, new Particle.DustOptions(G, 1.5f));
                }
                // Sparkle bursts
                if (pu % 4 == 0) spr(p.getLocation(), p.getWorld(), G, 12);
                // Progress bar
                int ba = Math.min(5, pu / 6);
                StringBuilder bar = new StringBuilder("§7[§f");
                for (int i = 0; i < ba; i++) bar.append("▮");
                for (int i = 0; i < 5 - ba; i++) bar.append("▯");
                bar.append("]");
                sab(p, "§6§l✦ §fBerkah Terkumpul §7" + bar.toString());
                pu++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // ==========================================
    // 🎨 PARTICLE HELPERS
    // ==========================================
    private void spr(Location l, World w, Color c, int n) {
        for (int i = 0; i < n; i++) {            Vector sp = new Vector((float) ((r.nextDouble() - 0.5) * 0.7), (float) (r.nextDouble() * 0.8), (float) ((r.nextDouble() - 0.5) * 0.7));
            w.spawnParticle(Particle.DUST, l.clone().add(sp), 1, new Particle.DustOptions(c, 1.4f));
        }
    }

    private Vector rot(Vector v, Vector d) {
        double a = Math.atan2(d.getZ(), d.getX());
        double x = v.getX() * Math.cos(a) - v.getZ() * Math.sin(a);
        double z = v.getX() * Math.sin(a) + v.getZ() * Math.cos(a);
        return new Vector(x, v.getY(), z);
    }

    private boolean isB(Player p) {
        ItemStack it = p.getInventory().getItemInMainHand();
        return it.getType() != Material.AIR && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private boolean hasS(Player p) {
        ItemStack oh = p.getInventory().getItemInOffHand();
        return oh.getType() != Material.AIR && oh.hasItemMeta() && oh.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE);
    }

    private D get(Player p) {
        return data.computeIfAbsent(p.getUniqueId(), k -> new D());
    }

    private void sab(Player p, String m) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(m));
    }

    // ==========================================
    // 📊 PLAYER DATA CLASS
    // ==========================================
    private static class D {
        long lt = 0, ls = 0, cs = 0;
        boolean s1 = false, s2 = false, s3 = false, ch = false, ms = true;
        int g = 0;
    }
          }
