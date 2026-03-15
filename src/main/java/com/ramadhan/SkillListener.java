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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SkillListener implements Listener {
    // Colors per tier
    private static final Color NONE_C = Color.fromRGB(200,200,220);
    private static final Color CRESC_C = Color.fromRGB(50,255,150);
    private static final Color ELITE_C = Color.fromRGB(255,215,0);
    private static final Color ELITE_A = Color.fromRGB(180,140,220);
    
    private final GoldenMoon plugin;
    private final Map<UUID,PD> data = new HashMap<>();
    private final Map<UUID,Long> marked = new HashMap<>();
    private final Random r = new Random();
    
    public SkillListener(GoldenMoon p) { plugin = p; }
    
    @EventHandler public void onQ(PlayerQuitEvent e) { data.remove(e.getPlayer().getUniqueId()); }
    
    @EventHandler public void onI(PlayerInteractEvent e) {        Player p = e.getPlayer();
        if (!hasSword(p)) return;
        PD d = get(p);
        long n = System.currentTimeMillis();
        int t = tier(p);
        
        // ⚡ SKILL 1: DASH (different per tier)
        if (p.isSneaking() && (e.getAction()==Action.LEFT_CLICK_AIR || e.getAction()==Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            if (n-d.ld < (t==2?800:(t==1?1200:1500))) { sab(p,"§cCD"); return; }
            dash(p,t); d.ld = n; return;
        }
        // 🌙 SKILL 2: PROJECTILE (different per tier)
        if (e.getAction()==Action.LEFT_CLICK_AIR || e.getAction()==Action.LEFT_CLICK_BLOCK) {
            if (n-d.ls < (t==2?300:(t==1?450:600))) return;
            projectile(p,t); d.ls = n; return;
        }
        // 🌕 SKILL 3: ULTIMATE (different per tier)
        if (e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (n-d.lu < 12000) { sab(p,"§cUlt CD"); return; }
            ultimate(p,t); d.lu = n;
        }
    }
    
    @EventHandler public void onA(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player)e.getDamager();
        int t = tier(p);
        if (t==2 && hasPiece(p,EquipmentSlot.HEAD,GoldenMoon.ELITE_HELMET_KEY) && p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()*0.7) {
            e.setDamage(e.getDamage()*1.2); spark(e.getEntity().getLocation(),p.getWorld(),ELITE_A,4);
        }
        if (t>=1 && hasPiece(p,EquipmentSlot.CHEST,GoldenMoon.ARMOR_CHEST_KEY) && e.getEntity() instanceof LivingEntity && !e.getEntity().equals(p)) {
            mark((LivingEntity)e.getEntity());
        }
    }
    
    // ==========================================
    // ⚡ SKILL 1: DASH - 3 DISTINCT VERSIONS
    // ==========================================
    private void dash(Player p,int t) {
        World w = p.getWorld();
        Location l = p.getLocation();
        Vector dir = l.getDirection().setY(0).normalize();
        
        if (t==0) { // NO ARMOR: Simple forward blink
            Location tl = l.clone().add(dir.clone().multiply(2));
            p.teleport(tl);
            w.playSound(tl,Sound.ENTITY_ENDERMAN_TELEPORT,0.8f,1f);
            for(int i=0;i<15;i++) {                Vector sp = new Vector((float)((r.nextDouble()-0.5)*0.8),(float)(r.nextDouble()*0.6),(float)((r.nextDouble()-0.5)*0.8));
                w.spawnParticle(Particle.DUST,tl.clone().add(sp),1,new Particle.DustOptions(NONE_C,1.2f));
            }
        }
        else if (t==1) { // CRESCENT: Curved arc dash + slow enemies
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if (f>10) { cancel(); return; }
                    final float pr = (float)f/10f;
                    // Curved trajectory
                    Vector curve = dir.clone().multiply(2.5f*pr).setY((float)(Math.sin(pr*Math.PI)*1.5));
                    Location cl = l.clone().add(curve);
                    p.teleport(cl);
                    // Crescent arc particles
                    for (double a=-1.5;a<=1.5;a+=0.2) {
                        Vector arc = rotate(dir,90).multiply(a*0.6).add(dir.clone().multiply((float)(-a*a*0.3)));
                        w.spawnParticle(Particle.DUST,cl.clone().add(arc),1,new Particle.DustOptions(CRESC_C,1.4f));
                    }
                    // Slow enemies on path
                    for (Entity en:w.getNearbyEntities(cl,2,2,2)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            ((LivingEntity)en).addPotionEffect(new PotionEffect(PotionEffectType.SLOW,40,1,false,false));
                        }
                    }
                    f++;
                }
            }.runTaskTimer(plugin,0,1);
            w.playSound(l,Sound.BLOCK_AMETHYST_BLOCK_STEP,0.9f,1.3f);
        }
        else { // ELITE: Multi-teleport strike + damage
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if (f>3) {
                        // Final strike
                        Location end = l.clone().add(dir.clone().multiply(4));
                        p.teleport(end);
                        w.playSound(end,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,1f,1.2f);
                        for (Entity en:w.getNearbyEntities(end,3,3,3)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                ((LivingEntity)en).damage(5,p);
                                ((LivingEntity)en).setVelocity(dir.clone().multiply(0.6f).setY(0.5f));
                                spark(en.getLocation(),w,ELITE_C,8);
                            }
                        }
                        cancel(); return;
                    }
                    // Multi-teleport with lightning
                    Location tl = l.clone().add(dir.clone().multiply(1+f*0.8));                    p.teleport(tl);
                    w.spawnParticle(Particle.FLASH,tl,1);
                    for(int i=0;i<10;i++) {
                        Vector sp = new Vector((float)((r.nextDouble()-0.5)*1.2),(float)(r.nextDouble()*1.0),(float)((r.nextDouble()-0.5)*1.2));
                        w.spawnParticle(Particle.DUST,tl.clone().add(sp),1,new Particle.DustOptions(i%2==0?ELITE_C:ELITE_A,1.6f));
                    }
                    w.playSound(tl,Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.5f,1.5f+f*0.2f);
                    f++;
                }
            }.runTaskTimer(plugin,0,2);
        }
    }
    
    // ==========================================
    // 🌙 SKILL 2: PROJECTILE - 3 DISTINCT VERSIONS
    // ==========================================
    private void projectile(Player p,int t) {
        World w = p.getWorld();
        Location st = p.getEyeLocation().add(p.getLocation().getDirection());
        Vector dir = p.getLocation().getDirection().normalize();
        
        if (t==0) { // NO ARMOR: Straight line projectile
            new BukkitRunnable() {
                int lf=0;
                public void run() {
                    if (lf>20) { cancel(); return; }
                    Location cur = st.clone().add(dir.clone().multiply(lf*0.9));
                    // Simple straight trail
                    w.spawnParticle(Particle.DUST,cur,3,new Particle.DustOptions(NONE_C,1.1f));
                    // Hit check
                    for (Entity en:w.getNearbyEntities(cur,1.2,1.2,1.2)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            ((LivingEntity)en).damage(4,p);
                            spark(en.getLocation(),w,NONE_C,5);
                            cancel(); return;
                        }
                    }
                    lf++;
                }
            }.runTaskTimer(plugin,0,1);
            w.playSound(st,Sound.ENTITY_ARROW_SHOOT,0.7f,1.2f);
        }
        else if (t==1) { // CRESCENT: Boomerang that returns + chains
            new BukkitRunnable() {
                int lf=0; boolean ret=false;
                LivingEntity hit=null;
                public void run() {
                    if (lf>40) { cancel(); return; }
                    // Forward then return
                    float prog = ret ? (40f-lf)/20f : Math.min(1f,lf/20f);                    if (lf==20 && hit==null) ret=true;
                    Location cur = st.clone().add(dir.clone().multiply((ret?20-lf:lf)*0.8));
                    // Crescent shape particles
                    for (double a=-1.8;a<=1.8;a+=0.18) {
                        double cv = (a*a)*0.35;
                        Vector arc = rotate(dir,90).multiply(a*1.1).add(dir.clone().multiply((float)-cv));
                        w.spawnParticle(Particle.DUST,cur.clone().add(arc),1,new Particle.DustOptions(CRESC_C,1.3f));
                    }
                    // Hit check
                    if (hit==null) {
                        for (Entity en:w.getNearbyEntities(cur,1.5,1.5,1.5)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                hit = (LivingEntity)en;
                                ((LivingEntity)en).damage(6,p);
                                mark((LivingEntity)en);
                                spark(en.getLocation(),w,CRESC_C,7);
                                // Chain to nearest
                                chain(en,p,w);
                                break;
                            }
                        }
                    }
                    lf++;
                }
            }.runTaskTimer(plugin,0,1);
            w.playSound(st,Sound.ENTITY_ARROW_SHOOT,0.6f,1.4f);
            w.playSound(st,Sound.BLOCK_GRASS_BREAK,0.4f,1.7f);
        }
        else { // ELITE: Triple homing orbs + explosion
            for (int orb=0;orb<3;orb++) {
                final int oi=orb;
                final Vector odir = rotate(dir,(orb-1)*12);
                new BukkitRunnable() {
                    int lf=0;
                    public void run() {
                        if (lf>25) {
                            // Explosion on end
                            Location end = st.clone().add(odir.clone().multiply(22));
                            w.spawnParticle(Particle.EXPLOSION,end,1);
                            for (Entity en:w.getNearbyEntities(end,2.5,2.5,2.5)) {
                                if (en instanceof LivingEntity && !en.equals(p)) {
                                    ((LivingEntity)en).damage(7,p);
                                    ((LivingEntity)en).setVelocity(new Vector(0,0.5f,0));
                                    spark(en.getLocation(),w,ELITE_C,6);
                                }
                            }
                            cancel(); return;
                        }
                        Location cur = st.clone().add(odir.clone().multiply(lf*0.85));
                        // Homing behavior                        if (lf>5) {
                            LivingEntity nearest=null; double md=7;
                            for (Entity en:w.getNearbyEntities(cur,6,4,6)) {
                                if (en instanceof LivingEntity && !en.equals(p)) {
                                    double d = en.getLocation().distance(cur);
                                    if (d<md) { md=d; nearest=(LivingEntity)en; }
                                }
                            }
                            if (nearest!=null) {
                                Vector toT = nearest.getLocation().add(0,1,0).toVector().subtract(cur.toVector()).normalize();
                                odir.add(toT.multiply(0.04f)).normalize();
                            }
                        }
                        // Orb core + glow
                        w.spawnParticle(Particle.DUST,cur,2,new Particle.DustOptions(ELITE_C,1.8f));
                        w.spawnParticle(Particle.DUST,cur,1,new Particle.DustOptions(ELITE_A,1.3f));
                        lf++;
                    }
                }.runTaskTimer(plugin,orb*2,1);
            }
            w.playSound(st,Sound.BLOCK_BEACON_ACTIVATE,0.7f,0.9f);
            w.playSound(st,Sound.ENTITY_BLAZE_SHOOT,0.5f,1.1f);
        }
    }
    
    private void chain(LivingEntity from,Player src,World w) {
        LivingEntity nr=null; double md=5;
        for (Entity en:from.getWorld().getNearbyEntities(from.getLocation(),5,3,5)) {
            if (en instanceof LivingEntity && !en.equals(src) && en!=from) {
                double d = en.getLocation().distance(from.getLocation());
                if (d<md) { md=d; nr=(LivingEntity)en; }
            }
        }
        if (nr!=null) {
            Vector cd = nr.getLocation().toVector().subtract(from.getLocation().toVector()).normalize();
            new BukkitRunnable() {
                int cf=0;
                public void run() {
                    if (cf>8) { nr.damage(3,src); spark(nr.getLocation(),w,CRESC_C,4); cancel(); return; }
                    final float pr = (float)cf/8f;
                    for (int i=0;i<10;i++) {
                        Location cl = from.getLocation().clone().add(cd.clone().multiply((float)(i*0.4f*pr)));
                        cl.add(0,(float)(Math.sin(i*0.5+cf*0.4)*0.2f*pr),0);
                        w.spawnParticle(Particle.DUST,cl,1,new Particle.DustOptions(CRESC_C,1f*pr));
                    }
                    cf++;
                }
            }.runTaskTimer(plugin,0,1);
        }
    }    
    // ==========================================
    // 🌕 SKILL 3: ULTIMATE - 3 DISTINCT VERSIONS
    // ==========================================
    private void ultimate(Player p,int t) {
        World w = p.getWorld();
        Location c = p.getLocation();
        
        if (t==0) { // NO ARMOR: Simple AOE burst
            p.sendTitle("§f§l✦ MOON BURST ✦","§7Basic",3,25,8);
            w.playSound(c,Sound.BLOCK_AMETHYST_BLOCK_HIT,1f,1f);
            // Burst particles
            for (int i=0;i<80;i++) {
                Vector sp = new Vector((float)((r.nextDouble()-0.5)*4),(float)(r.nextDouble()*3),(float)((r.nextDouble()-0.5)*4));
                w.spawnParticle(Particle.DUST,c.clone().add(sp),1,new Particle.DustOptions(NONE_C,1.5f));
            }
            // Damage
            for (Entity en:w.getNearbyEntities(c,5,4,5)) {
                if (en instanceof LivingEntity && !en.equals(p)) {
                    ((LivingEntity)en).damage(10,p);
                    ((LivingEntity)en).setVelocity(new Vector(0,0.4f,0));
                }
            }
            // Self heal
            if (p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+4));
        }
        else if (t==1) { // CRESCENT: Moon ring that expands + pulls enemies
            p.sendTitle("§b§l✦ CRESCENT PULL ✦","§aDraw them in",4,28,9);
            w.playSound(c,Sound.BLOCK_AMETHYST_BLOCK_CHIME,1.1f,1.2f);
            // Expanding ring
            new BukkitRunnable() {
                int rf=0;
                public void run() {
                    if (rf>30) {
                        // Pull enemies to center
                        for (Entity en:w.getNearbyEntities(c,7,5,7)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                LivingEntity le = (LivingEntity)en;
                                Vector pull = c.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.8);
                                le.setVelocity(pull.setY(0.3));
                                le.damage(8,p);
                                spark(le.getLocation(),w,CRESC_C,5);
                            }
                        }
                        cancel(); return;
                    }
                    final float pr = (float)rf/30f;
                    final float rad = 2f+pr*6f;
                    // Ring particles
                    for (int i=0;i<35;i++) {                        double a = Math.toRadians(i*10.3+rf*4);
                        Vector off = new Vector((float)(Math.cos(a)*rad),0.15f,(float)(Math.sin(a)*rad));
                        w.spawnParticle(Particle.DUST,c.clone().add(off),1,new Particle.DustOptions(CRESC_C,1.5f*(1f-pr*0.3f)));
                    }
                    // Crescent accents
                    if (rf%4==0) {
                        for (int i=0;i<8;i++) {
                            double a = Math.toRadians(i*45+rf*6);
                            Vector cres = rotate(new Vector(1,0,0),90).multiply(Math.sin(a)*1.2).add(new Vector(-Math.cos(a)*0.4,0,0));
                            w.spawnParticle(Particle.DUST,c.clone().add(cres).add(0,0.2f,0),1,new Particle.DustOptions(Color.fromRGB(100,255,200),1.3f));
                        }
                    }
                    rf++;
                }
            }.runTaskTimer(plugin,0,1);
            // Self buff
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,180,0,false,false));
        }
        else { // ELITE: Multi-phase moon domain
            p.sendTitle("§6§l✦ GOLDEN DOMAIN ✦","§eCelestial Judgment",5,32,10);
            w.playSound(c,Sound.BLOCK_BEACON_ACTIVATE,1.3f,0.8f);
            w.playSound(c,Sound.ENTITY_WITHER_SPAWN,0.6f,0.9f);
            
            final double zr = 8;
            List<LivingEntity> tg = new ArrayList<>();
            for (Entity en:w.getNearbyEntities(c,zr,5,zr)) if (en instanceof LivingEntity && !en.equals(p)) tg.add((LivingEntity)en);
            
            // Phase 1: Summon golden pillars
            new BukkitRunnable() {
                int pf=0;
                public void run() {
                    if (pf>25) { cancel(); return; }
                    final float pr = (float)pf/25f;
                    // 6 pillars in hexagon
                    for (int pi=0;pi<6;pi++) {
                        double pa = Math.toRadians(pi*60+pf*3);
                        Location pl = c.clone().add((float)(Math.cos(pa)*zr*0.8),0,(float)(Math.sin(pa)*zr*0.8));
                        // Rising pillar
                        for (int h=0;h<(int)(pr*20);h++) {
                            w.spawnParticle(Particle.DUST,pl.clone().add(0,h,0),2,new Particle.DustOptions(ELITE_C,1.7f));
                            if (pf%3==0) w.spawnParticle(Particle.DUST,pl.clone().add(0,h,0),1,new Particle.DustOptions(ELITE_A,1.3f));
                        }
                    }
                    pf++;
                }
            }.runTaskTimer(plugin,0,1);
            
            // Phase 2: Moon beams strike targets
            new BukkitRunnable() {
                int bf=0;                public void run() {
                    if (bf>tg.size()*3) {
                        // Phase 3: Final explosion + buffs
                        w.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,1f,0.8f);
                        for (int i=0;i<120;i++) {
                            Vector sp = new Vector((float)((r.nextDouble()-0.5)*6),(float)(r.nextDouble()*4.5),(float)((r.nextDouble()-0.5)*6));
                            w.spawnParticle(Particle.DUST,c.clone().add(sp),1,new Particle.DustOptions(i%3==0?ELITE_C:(i%3==1?ELITE_A:Color.fromRGB(255,240,180)),2f));
                        }
                        for (LivingEntity le:tg) { le.damage(12,p); le.setVelocity(new Vector(0,-0.7f,0)); spark(le.getLocation(),w,ELITE_C,10); }
                        // Elite buffs
                        if (p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+10));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,240,1,false,false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,300,2,false,false));
                        cancel(); return;
                    }
                    if (bf<tg.size() && !tg.isEmpty()) {
                        LivingEntity target = tg.get(bf%tg.size());
                        // Beam from sky
                        new BukkitRunnable() {
                            int beam=0;
                            public void run() {
                                if (beam>15) {
                                    target.damage(6,p); spark(target.getLocation(),w,ELITE_A,6); cancel(); return;
                                }
                                Location beamLoc = target.getLocation().clone().add(0,15-beam,0);
                                w.spawnParticle(Particle.DUST,beamLoc,3,new Particle.DustOptions(ELITE_C,1.9f));
                                w.spawnParticle(Particle.FLAME,beamLoc,1,0.1f,0.1f,0.1f,0.05f);
                                beam++;
                            }
                        }.runTaskTimer(plugin,0,1);
                    }
                    bf++;
                }
            }.runTaskTimer(plugin,26,3);
        }
    }
    
    // ==========================================
    // ✨ HELPERS
    // ==========================================
    private void mark(LivingEntity t) {
        marked.put(t.getUniqueId(),System.currentTimeMillis()+6000);
        new BukkitRunnable() {
            int tm=0;
            public void run() {
                if (tm>120 || !t.isValid() || !marked.containsKey(t.getUniqueId())) { marked.remove(t.getUniqueId()); cancel(); return; }
                Location h = t.getLocation().add(0,2.6f,0);
                final float pl = 1f+(float)(Math.sin(tm*0.25)*0.18f);
                t.getWorld().spawnParticle(Particle.DUST,h,4,new Particle.DustOptions(ELITE_C,1.6f*pl));
                tm+=2;            }
        }.runTaskTimer(plugin,0,2);
    }
    
    private void spark(Location l,World w,Color c,int n) {
        for (int i=0;i<n;i++) {
            Vector sp = new Vector((float)((r.nextDouble()-0.5)*0.55),(float)(r.nextDouble()*0.65),(float)((r.nextDouble()-0.5)*0.55));
            w.spawnParticle(Particle.DUST,l.clone().add(sp),1,new Particle.DustOptions(c,1.3f));
        }
    }
    
    private Vector rotate(Vector v,double deg) {
        double a = Math.toRadians(deg);
        double cs = Math.cos(a), sn = Math.sin(a);
        double x = v.getX()*cs + v.getZ()*sn;
        double z = v.getX()*-sn + v.getZ()*cs;
        return new Vector((float)x,v.getY(),(float)z);
    }
    
    private boolean hasSword(Player p) {
        ItemStack it = p.getInventory().getItemInMainHand();
        return it!=null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY,PersistentDataType.BYTE);
    }
    
    private boolean hasPiece(Player p,EquipmentSlot sl,org.bukkit.NamespacedKey key) {
        ItemStack it = null;
        switch(sl) { case HEAD: it=p.getInventory().getHelmet(); break; case CHEST: it=p.getInventory().getChestplate(); break; case LEGS: it=p.getInventory().getLeggings(); break; case FEET: it=p.getInventory().getBoots(); break; }
        return it!=null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(key,PersistentDataType.BYTE);
    }
    
    private int tier(Player p) {
        if (plugin.getArmorManager().hasFullEliteSet(p)) return 2;
        if (plugin.getArmorManager().hasCrescentSet(p)) return 1;
        return 0;
    }
    
    private void sab(Player p,String m) { p.spigot().sendMessage(ChatMessageType.ACTION_BAR,TextComponent.fromLegacyText(m)); }
    private PD get(Player p) { return data.computeIfAbsent(p.getUniqueId(),k->new PD()); }
    
    private static class PD { long ls=0,ld=0,lu=0; }
                }
