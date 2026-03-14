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

    private final GoldenMoon plugin;
    private final ArmorManager armor;
    private final Map<UUID, D> data = new HashMap<>();
    private final Random r = new Random();
    private static final int MAX = 100, GPH = 15, HOLD = 250;

    public SkillListener(GoldenMoon p) {
        this.plugin = p;
        this.armor = p.getArmorManager();
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
        Player p = e.getPlayer();
        if (!isB(p)) return;
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            D d = get(p);
            if (d.g >= MAX && !d.ch && !d.s3) {
                d.ch = true;
                d.cs = System.currentTimeMillis();
                sab(p, "§6§l✦ §fMenahan...");
                chg(p);
            } else if (d.ch) {
                long ct = System.currentTimeMillis() - d.cs;
                if (ct >= 1000) {
                    d.ch = false;
                    ult(p);
                    d.g = 0;
                    d.s3 = true;
                    sab(p, "§6§l✦ §f🌕 PANGGILAN BULAN!");
                    new BukkitRunnable() {
                        public void run() { if (data.containsKey(p.getUniqueId())) get(p).s3 = false; }
                    }.runTaskLater(plugin, 1200);
                } else {
                    d.ch = false;
                    sab(p, "§c✦ §fTahan 1 detik!");
                }
            }
        }
        if ((e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) && p.isSneaking()) {
            D d = get(p);
            if (!d.s2) {
                e.setCancelled(true);
                storm(p);
                d.s2 = true;
                new BukkitRunnable() {
                    public void run() { if (data.containsKey(p.getUniqueId())) get(p).s2 = false; }
                }.runTaskLater(plugin, 60);
            }
        }
    }

    @EventHandler
    public void onM(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isB(p)) return;
        D d = get(p);
        if (d.ch && System.currentTimeMillis() % 100 < 25) spr(p.getLocation().add(0, 1.6f, 0), p.getWorld(), Y, 12);
        if (armor.tryMoonStep(p)) {
            d.ms = false;
            new BukkitRunnable() {
                public void run() { if (data.containsKey(p.getUniqueId())) get(p).ms = true; }
            }.runTaskLater(plugin, 60);
        }
    }

    private void slash(final Player p, final LivingEntity t) {
        final World w = p.getWorld();
        final Location pl = p.getLocation().clone();
        final Location tl = t.getLocation().clone();
        final Vector dir = tl.toVector().subtract(pl.toVector()).setY(0).normalize();

        for (int f = 0; f < 6; f++) {
            final int fr = f;
            new BukkitRunnable() {
                public void run() {
                    Location sl = pl.clone().add(p.getLocation().getDirection().multiply(0.9f));
                    for (int i = 0; i < 30; i++) {
                        double a = Math.toRadians(i * 12 + fr * 15);
                        Vector ro = new Vector(Math.cos(a) * 0.8f, 0.45f + (float) (Math.sin(fr * 0.5) * 0.35), Math.sin(a) * 0.8f);
                        w.spawnParticle(Particle.DUST, sl.clone().add(ro), 1, new Particle.DustOptions(Y, 1.7f));
                    }
                    for (int i = 0; i < 20; i++) {
                        double a = Math.toRadians(i * 18 + fr * 20);
                        Vector ro = new Vector(Math.cos(a) * 0.4f, 0.35f, Math.sin(a) * 0.4f);
                        w.spawnParticle(Particle.DUST, sl.clone().add(ro), 1, new Particle.DustOptions(W, 1.4f));
                    }
                    w.playSound(sl, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.5f);
                }
            }.runTaskLater(plugin, f * 2);
        }

        new BukkitRunnable() {
            int df = 0;
            public void run() {
                if (df >= 8) { cancel(); return; }
                float pr = (float) df / 7f;
                Location dl = pl.clone().add(dir.clone().multiply(1.5f * pr)).add(dir.clone().multiply(0.8f));
                w.spawnParticle(Particle.DUST, dl, 20, new Particle.DustOptions(Y, 2.6f));
                w.spawnParticle(Particle.DUST, dl, 15, new Particle.DustOptions(W, 2.1f));
                w.spawnParticle(Particle.DUST, dl, 10, new Particle.DustOptions(A, 1.8f));
                if (df % 2 == 0) w.spawnParticle(Particle.FLAME, dl, 5, 0.2, 0.2, 0.2, 0.05);
                df++;
            }
        }.runTaskTimer(plugin, 12, 1);

        new BukkitRunnable() {
            public void run() {
                w.playSound(tl, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1.1f, 2.1f);
                w.playSound(tl, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.5f);
                w.spawnParticle(Particle.EXPLOSION, tl, 3);
                for (int row = -4; row <= 4; row++) {
                    for (int col = -4; col <= 4; col++) {
                        Vector go = rot(new Vector(col * 0.6f, row * 0.7f, 0), dir);
                        Color c = (row + col) % 2 == 0 ? Y : W;
                        for(int i=0; i<3; i++) w.spawnParticle(Particle.DUST, tl.clone().add(go), 1, new Particle.DustOptions(c, 1.6f));
                    }
                }
                for (int i = 0; i < 50; i++) {
                    Vector sp = new Vector((r.nextDouble()-0.5)*3, (r.nextDouble()-0.5)*3, (r.nextDouble()-0.5)*3);
                    w.spawnParticle(Particle.DUST, tl.clone().add(sp), 1, new Particle.DustOptions(G, 1.2f));
                }
                t.damage(4, p);
                t.setVelocity(dir.clone().multiply(0.8f).setY(0.5f));
                if (data.containsKey(p.getUniqueId())) {
                    get(p).s1 = false;
                    get(p).ls = 0;
                }
            }
        }.runTaskLater(plugin, 20);
    }

    private void storm(final Player p) {
        final World w = p.getWorld();
        final Location c = p.getLocation().clone();
        sab(p, "§e§l✦ §fHUJAN BERKAH!");
        new BukkitRunnable() {
            int pf = 0;
            public void run() {
                if (pf >= 28) { cancel(); return; }
                float cr = 5.5f + (float) (Math.sin(pf * 0.3) * 0.8);
                for (int i = 0; i < 25; i++) {
                    double a = Math.toRadians(i * 14.4 + pf * 3);
                    w.spawnParticle(Particle.DUST, c.clone().add(Math.cos(a) * cr, 9.5f, Math.sin(a) * cr), 1, new Particle.DustOptions(S, 1.6f));
                    w.spawnParticle(Particle.DUST, c.clone().add(Math.cos(a) * (cr*0.8), 9.0f, Math.sin(a) * (cr*0.8)), 1, new Particle.DustOptions(W, 1.2f));
                }
                if (pf % 4 == 0) w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.0f);
                pf++;
            }
        }.runTaskTimer(plugin, 0, 2);

        for (int o = 0; o < 12; o++) {
            final int ob = o;
            new BukkitRunnable() {
                public void run() {
                    double an = r.nextDouble() * Math.PI * 2, di = 1.8 + r.nextDouble() * 5.5;
                    final Location ds = c.clone().add(Math.cos(an) * di, 19, Math.sin(an) * di);
                    new BukkitRunnable() {
                        int ff = 0;
                        public void run() {
                            if (ff >= 35) {
                                Location il = ds.clone(); il.setY(c.getY());
                                w.spawnParticle(Particle.EXPLOSION, il, 2);
                                spr(il, w, Y, 20);
                                for (Entity en : w.getNearbyEntities(il, 3.5, 3.5, 3.5)) {
                                    if (en instanceof LivingEntity && !en.equals(p)) {
                                        ((LivingEntity) en).damage(6, p);
                                        en.setVelocity(new Vector(0, 0.5, 0));
                                    }
                                }
                                cancel(); return;
                            }
                            w.spawnParticle(Particle.DUST, ds.clone().subtract(0, ff * 0.55, 0), 3, new Particle.DustOptions(Y, 1.8f));
                            w.spawnParticle(Particle.DUST, ds.clone().subtract(0, (ff-1) * 0.55, 0), 2, new Particle.DustOptions(W, 1.4f));
                            ff++;
                        }
                    }.runTaskTimer(plugin, 0, 1);
                }
            }.runTaskLater(plugin, ob * 3);
        }
    }

    private void ult(final Player p) {
        final World w = p.getWorld();
        final Location c = p.getLocation().clone();
        p.setInvulnerable(true);
        p.sendTitle("§f§l🌕", "§6§l✦ PANGGILAN BULAN ✦", 8, 32, 11);
        new BukkitRunnable() {
            int bf = 0;
            public void run() {
                if (bf >= 25) { cancel(); return; }
                Location bl = c.clone().add(0, 6 + bf * 0.4, 0);
                for (int row = -5; row <= 5; row++) {
                    for (int col = -3; col <= 3; col++) {
                        Color cc = (row + col) % 2 == 0 ? Y : W;
                        w.spawnParticle(Particle.DUST, bl.clone().add(col * 0.7, row * 0.5, 0), 1, new Particle.DustOptions(cc, 1.6f));
                    }
                }
                if(bf % 5 == 0) w.playSound(bl, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 2.0f);
                bf++;
            }
        }.runTaskTimer(plugin, 5, 2);

        new BukkitRunnable() {
            public void run() {
                w.playSound(c, Sound.BLOCK_BEACON_ACTIVATE, 1.3f, 1f);
                w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                w.spawnParticle(Particle.FLASH, c, 5);
                for (int i = 0; i < 100; i++) {
                    double a = Math.toRadians(i * 3.6);
                    w.spawnParticle(Particle.DUST, c.clone().add(Math.cos(a)*8, 0.5, Math.sin(a)*8), 1, new Particle.DustOptions(Y, 2.0f));
                }
                for (Entity en : w.getNearbyEntities(c, 8, 8, 8)) {
                    if (en instanceof LivingEntity && !en.equals(p)) {
                        ((LivingEntity) en).damage(10, p);
                        en.setVelocity(new Vector(0, 1.5, 0));
                    }
                }
                
                // --- BAGIAN PERBAIKAN HEALING ---
                try {
                    // Coba MAX_HEALTH dulu, kalau gagal dia ke catch
                    double maxH = p.getAttribute(Attribute.MAX_HEALTH).getValue();
                    p.setHealth(Math.min(maxH, p.getHealth() + 8));
                } catch (Exception e) {
                    // Cadangan jika versi API berbeda
                    p.setHealth(Math.min(20.0, p.getHealth() + 8));
                }
                
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 1));
                p.setInvulnerable(false);
                sab(p, "§6§l✦ §fBERKAH BULAN AKTIF!");
            }
        }.runTaskLater(plugin, 60);
    }

    private void chg(final Player p) {
        new BukkitRunnable() {
            int pu = 0;
            public void run() {
                D d = get(p);
                if (!d.ch || !p.isOnline()) { cancel(); return; }
                double ra = 1.3 + Math.sin(pu * 0.38) * 0.5;
                for (int a = 0; a < 20; a++) {
                    double an = Math.toRadians(a * 18 + pu * 5);
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(Math.cos(an) * ra, 1, Math.sin(an) * ra), 1, new Particle.DustOptions(Y, 1.8f));
                    p.getWorld().spawnParticle(Particle.DUST, p.getLocation().add(Math.cos(an) * (ra*0.7), 0.5, Math.sin(an) * (ra*0.7)), 1, new Particle.DustOptions(W, 1.4f));
                }
                int progress = Math.min(5, pu / 4);
                StringBuilder bar = new StringBuilder("§f");
                for(int i=0; i<5; i++) bar.append(i < progress ? "▮" : "▯");
                sab(p, "§6§l✦ §fENERGI: " + bar.toString());
                pu++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void spr(Location l, World w, Color c, int n) {
        for (int i = 0; i < n; i++) {
            Vector sp = new Vector((r.nextDouble() - 0.5) * 0.7, r.nextDouble() * 0.8, (r.nextDouble() - 0.5) * 0.7);
            w.spawnParticle(Particle.DUST, l.clone().add(sp), 1, new Particle.DustOptions(c, 1.3f));
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
        return it != null && it.getType() != Material.AIR && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }

    private boolean hasS(Player p) {
        ItemStack oh = p.getInventory().getItemInOffHand();
        return oh != null && oh.getType() != Material.AIR && oh.hasItemMeta() && oh.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE);
    }

    private D get(Player p) {
        return data.computeIfAbsent(p.getUniqueId(), k -> new D());
    }

    private void sab(Player p, String m) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(m));
    }

    private static class D {
        long lt = 0, ls = 0, cs = 0;
        boolean s1 = false, s2 = false, s3 = false, ch = false, ms = true;
        int g = 0;
    }
}
