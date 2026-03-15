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
    private static final Color CYAN = Color.fromRGB(0,255,255);
    private static final Color GREEN = Color.fromRGB(50,255,150);
    private static final Color GOLD = Color.fromRGB(255,215,0);
    private static final Color PURPLE = Color.fromRGB(180,140,220);
    private static final Color WHITE = Color.fromRGB(255,255,255);
    
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
        
        if (p.isSneaking() && (e.getAction()==Action.LEFT_CLICK_AIR || e.getAction()==Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            if (n-d.ld < 1500) { sab(p,"§cDash CD"); return; }
            dash(p); d.ld = n; return;
        }
        if (e.getAction()==Action.LEFT_CLICK_AIR || e.getAction()==Action.LEFT_CLICK_BLOCK) {
            if (n-d.ls < 500) return;
            crescent(p); d.ls = n; return;
        }
        if (e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (n-d.lu < 12000) { sab(p,"§cUlt CD"); return; }
            ult(p); d.lu = n;
        }
    }
    
    @EventHandler public void onA(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player)e.getDamager();
        if (hasPiece(p,EquipmentSlot.HEAD,GoldenMoon.ELITE_HELMET_KEY) && p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()*0.7) {
            e.setDamage(e.getDamage()*1.15);
            if (r.nextInt(100)<30) spark(e.getEntity().getLocation(),p.getWorld(),PURPLE,3);
        }
        if (hasPiece(p,EquipmentSlot.CHEST,GoldenMoon.ARMOR_CHEST_KEY) && e.getEntity() instanceof LivingEntity && !e.getEntity().equals(p)) {
            mark((LivingEntity)e.getEntity());
        }
    }
    
    private void dash(Player p) {
        World w = p.getWorld();
        Location l = p.getLocation();
        int t = tier(p);
        Vector dir = l.getDirection().setY(0).normalize();
        double dist = 1.5 + t*0.5;
        
        new BukkitRunnable() {
            int f = 0;
            public void run() {
                if (f>5) {
                    Location tl = l.clone().add(dir.clone().multiply(dist));
                    p.teleport(tl);
                    w.playSound(tl,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,0.9f,1.8f+t*0.2f);
                    for (int i=0;i<20+t*8;i++) {
                        Vector sp = new Vector((float)((r.nextDouble()-0.5)*1.2),(float)(r.nextDouble()*1.0),(float)((r.nextDouble()-0.5)*1.2));
                        Color c = i%3==0?CYAN:(i%3==1?Color.fromRGB(100,200,255):WHITE);                        w.spawnParticle(Particle.DUST,tl.clone().add(sp),1,new Particle.DustOptions(c,1.5f+t*0.2f));
                    }
                    new BukkitRunnable() {
                        int rf=0;
                        public void run() {
                            if (rf>12) { cancel(); return; }
                            final float pr = (float)rf/12f;
                            final float rad = 0.5f+pr*1.8f;
                            for (int i=0;i<12;i++) {
                                double a = Math.toRadians(i*30+rf*10);
                                Vector off = new Vector((float)(Math.cos(a)*rad),0.1f,(float)(Math.sin(a)*rad));
                                w.spawnParticle(Particle.DUST,tl.clone().add(off),1,new Particle.DustOptions(CYAN,1.2f*(1f-pr)));
                            }
                            rf++;
                        }
                    }.runTaskTimer(plugin,0,1);
                    if (t==2) {
                        for (Entity en:w.getNearbyEntities(tl,2.5,2.5,2.5)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                ((LivingEntity)en).damage(4,p);
                                ((LivingEntity)en).setVelocity(dir.clone().multiply(0.5f).setY(0.4f));
                                mark((LivingEntity)en);
                            }
                        }
                    }
                    cancel(); return;
                }
                final float cp = (float)f/5f;
                for (int i=0;i<6+t*2;i++) {
                    double a = Math.toRadians(i*60+f*15);
                    Vector off = new Vector((float)(Math.cos(a)*(0.4+cp)),cp*0.5f,(float)(Math.sin(a)*(0.4+cp)));
                    w.spawnParticle(Particle.DUST,l.clone().add(off),1,new Particle.DustOptions(CYAN,1.2f+cp));
                }
                f++;
            }
        }.runTaskTimer(plugin,0,1);
    }
    
    private void crescent(Player p) {
        World w = p.getWorld();
        Location st = p.getEyeLocation().add(p.getLocation().getDirection());
        Vector dir = p.getLocation().getDirection().normalize();
        int t = tier(p);
        int cnt = t==2?3:(t==1?2:1);
        double spd = 0.85+t*0.1;
        double rng = 15+t*3;
        
        w.playSound(st,Sound.ENTITY_ARROW_SHOOT,0.6f,1.3f+t*0.15f);
        
        for (int prj=0;prj<cnt;prj++) {            final int pi = prj;
            final Vector pd = rotate(dir,(prj-(cnt-1)/2)*8);
            new BukkitRunnable() {
                int lf=0,hits=0;
                LivingEntity lh=null;
                public void run() {
                    if (lf>rng/spd || hits>(t==2?99:(t==1?1:0))) { cancel(); return; }
                    Location cur = st.clone().add(pd.clone().multiply((float)(lf*spd)));
                    
                    for (double ang=-2.0;ang<=2.0;ang+=0.15) {
                        double cv = (ang*ang)*0.4;
                        Vector arc = rotate(dir,90).multiply(ang*1.2).add(dir.clone().multiply((float)-cv));
                        Color mc = t==2?GREEN:(t==1?Color.fromRGB(100,255,200):Color.fromRGB(200,255,220));
                        w.spawnParticle(Particle.DUST,cur.clone().add(arc),1,new Particle.DustOptions(mc,1.5f-t*0.1f));
                    }
                    
                    for (int i=0;i<4+t;i++) {
                        double sa = Math.toRadians(lf*20+i*72+pi*120);
                        double sr = 0.3+Math.sin(lf*0.3)*0.1;
                        Vector so = new Vector((float)(Math.cos(sa)*sr),(float)(Math.sin(lf*0.25+i)*0.3),(float)(Math.sin(sa)*sr));
                        Color tc = i%2==0?GREEN:Color.fromRGB(100,255,200);
                        w.spawnParticle(Particle.DUST,cur.clone().add(so),1,new Particle.DustOptions(tc,0.9f+t*0.1f));
                    }
                    
                    for (Entity en:w.getNearbyEntities(cur,1.2+t*0.3,1.2+t*0.3,1.2+t*0.3)) {
                        if (en instanceof LivingEntity && !en.equals(p) && en!=lh) {
                            LivingEntity le = (LivingEntity)en;
                            double dmg = t==2?7.5:(t==1?6.0:4.5);
                            if (marked.containsKey(le.getUniqueId())) { dmg*=1.3; marked.remove(le.getUniqueId()); spark(le.getLocation().add(0,1,0),w,Color.fromRGB(200,255,220),6); }
                            le.damage(dmg,p); le.setNoDamageTicks(0); lh=le; hits++;
                            if (t==2) zone(le.getLocation(),w,p);
                            else if (t==1) chain(le,p,w,1);
                            w.playSound(le.getLocation(),Sound.BLOCK_GLASS_BREAK,0.7f,1.4f+t*0.1f);
                        }
                    }
                    lf++;
                }
            }.runTaskTimer(plugin,prj*3,1);
        }
    }
    
    private void ult(Player p) {
        World w = p.getWorld();
        Location c = p.getLocation();
        int t = tier(p);
        List<LivingEntity> tg = new ArrayList<>();
        double zr = 6+t*1.5;
        for (Entity en:w.getNearbyEntities(c,zr,5,zr)) if (en instanceof LivingEntity && !en.equals(p)) tg.add((LivingEntity)en);
        int mc = Math.min(6,Math.max(3,3+tg.size()/2+t));
                p.sendTitle("§6§l✦ LUNAR PINCH ✦",t==2?"§6§l☀️ ELITE":(t==1?"§b§l🌙 CRESCENT":"§fMenyegel..."),5,30,10);
        w.playSound(c,Sound.BLOCK_BEACON_ACTIVATE,t==2?2.0f:1.5f,t==2?0.3f:0.5f);
        
        final double ar = zr*0.85;
        new BukkitRunnable() {
            int tk=0;
            public void run() {
                if (tk>25) { cancel(); return; }
                final float pr = (float)tk/25f;
                final float ep = (float)(1-Math.pow(1-pr,3));
                final float cr = (float)(ar*ep);
                final float al = 0.5f+ep*0.5f;
                for (int i=0;i<40;i++) {
                    double a = Math.toRadians(i*9+tk*3);
                    double x = Math.cos(a)*cr, z = Math.sin(a)*cr;
                    w.spawnParticle(Particle.DUST,c.clone().add((float)x,0.15f,(float)z),t==2?3:2,new Particle.DustOptions(GOLD,(t==2?2.2f:1.7f)*al));
                }
                if (t>=1) {
                    for (int i=0;i<25;i++) {
                        double a = Math.toRadians(i*14.4+tk*4+30);
                        double x = Math.cos(a)*cr*0.93, z = Math.sin(a)*cr*0.93;
                        w.spawnParticle(Particle.DUST,c.clone().add((float)x,0.22f,(float)z),1,new Particle.DustOptions(PURPLE,1.5f*al));
                    }
                }
                tk++;
            }
        }.runTaskTimer(plugin,0,1);
        
        new BukkitRunnable() {
            int lf=0;
            public void run() {
                if (lf>20) { cancel(); return; }
                final float lp = (float)lf/20f;
                final float el = (float)(1-Math.pow(1-lp,2));
                if (lf<15) p.setVelocity(new Vector(0,0.3f*(1-lp),0));
                for (int m=0;m<mc;m++) {
                    double ba = Math.toRadians(m*(360.0/mc)+lf*5);
                    final float mh = 3.5f+el*3f;
                    final float mr = (float)(ar*0.7*(0.8+Math.sin(lf*0.25+m)*0.15));
                    Location mc_loc = c.clone().add((float)(Math.cos(ba)*mr),mh,(float)(Math.sin(ba)*mr));
                    Vector inw = c.toVector().subtract(mc_loc.toVector()).normalize();
                    drawMoon(mc_loc,Math.toDegrees(Math.atan2(inw.getZ(),inw.getX())),t,w,lf);
                }
                lf++;
            }
        }.runTaskTimer(plugin,26,1);
        
        new BukkitRunnable() {
            int pf=0;
            public void run() {                if (pf>30) { slam(p,c,tg,t,w); cancel(); return; }
                final float pr = (float)pf/30f;
                final float ep = (float)(1-Math.pow(1-pr,4));
                for (int m=0;m<mc;m++) {
                    Location tl = c;
                    if (!tg.isEmpty()) tl = tg.get(m%tg.size()).getLocation().add(0,1.5f,0);
                    double ba = Math.toRadians(m*(360.0/mc));
                    final float cr = (float)(ar*0.7*(1f-ep*0.9f));
                    final float ht = 4.5f+(float)(Math.sin(pf*0.25)*0.6);
                    Location ml = c.clone().add((float)(Math.cos(ba)*cr),ht,(float)(Math.sin(ba)*cr));
                    Vector tt = tl.toVector().subtract(ml.toVector()).normalize();
                    drawMoon(ml,Math.toDegrees(Math.atan2(tt.getZ(),tt.getX())),t,w,pf+40);
                    for (LivingEntity le:tg) {
                        if (ml.distance(le.getLocation())<3f) {
                            double bd = marked.containsKey(le.getUniqueId())?16:9;
                            double dmg = bd*(1f+t*0.2f);
                            le.damage(dmg,p); le.setVelocity(new Vector(0,0.4f,0));
                            if (dmg>11) marked.remove(le.getUniqueId());
                            Color hc = t==2?Color.fromRGB(255,240,180):(t==1?GOLD:PURPLE);
                            spark(le.getLocation().add(0,1.2f,0),w,hc,5+t*2);
                        }
                    }
                }
                if (pf%5==0) {
                    final float lpr = (float)((pf%5)/5f);
                    final float lrad = (float)(ar*0.45*(1f-ep*0.55f));
                    for (int i=0;i<30;i++) {
                        double a = Math.toRadians(i*12+pf*6);
                        Vector po = new Vector((float)(Math.cos(a)*lrad),0.12f,(float)(Math.sin(a)*lrad));
                        Color pc = i%3==0?PURPLE:(i%3==1?Color.fromRGB(255,240,180):GOLD);
                        w.spawnParticle(Particle.DUST,c.clone().add(po),1,new Particle.DustOptions(pc,1.4f*(1f-lpr*0.25f)));
                    }
                }
                pf++;
            }
        }.runTaskTimer(plugin,47,1);
    }
    
    private void drawMoon(Location ctr,double angDeg,int t,World w,int lf) {
        Color mc = t==2?GOLD:(t==1?Color.fromRGB(255,240,180):PURPLE);
        float bs = t==2?1.9f:(t==1?1.6f:1.3f);
        int ly = t+1;
        final float pl = 1f+(float)(Math.sin(lf*0.35)*0.15);
        Vector fw = new Vector((float)Math.cos(Math.toRadians(angDeg)),0,(float)Math.sin(Math.toRadians(angDeg)));
        Vector rt = rotate(fw,90).normalize();
        for (int lyi=0;lyi<ly;lyi++) {
            final float lo = lyi*0.15f;
            final float sz = (bs-lyi*0.2f)*pl;
            for (double a=-2.5;a<=2.5;a+=0.12) {
                final double tp = 1-Math.abs(a)/2.8;                final double cv = (a*a)*0.52;
                Vector ao = rt.clone().multiply((float)(a*1.3f*(float)tp)).add(fw.clone().multiply((float)-cv));
                Vector lv = new Vector(0,(float)(lo*Math.sin(a)),0);
                Location pl_loc = ctr.clone().add(ao).add(lv);
                w.spawnParticle(Particle.DUST,pl_loc,1,new Particle.DustOptions(mc,sz*(float)tp));
                if (lyi==0 && Math.abs(a)<1.3) w.spawnParticle(Particle.DUST,pl_loc,1,new Particle.DustOptions(Color.fromRGB(255,230,100),sz*0.8f*(float)tp));
            }
        }
        if (t==2) {
            for (double a=-2.8;a<=2.8;a+=0.35) {
                final double cv = (a*a)*0.58;
                Vector go = rt.clone().multiply((float)(a*1.5f)).add(fw.clone().multiply((float)-cv));
                w.spawnParticle(Particle.DUST,ctr.clone().add(go),1,new Particle.DustOptions(PURPLE,1.2f*pl));
            }
        }
    }
    
    private void slam(Player p,Location c,List<LivingEntity> tg,int t,World w) {
        p.setVelocity(new Vector(0,-1.8f,0));
        w.playSound(c,Sound.BLOCK_AMETHYST_BLOCK_HIT,t==2?1.8f:1.4f,t==2?0.7f:0.9f);
        Color bc = t==2?GOLD:(t==1?Color.fromRGB(255,240,180):PURPLE);
        int bcnt = t==2?150:(t==1?100:70);
        float bsz = t==2?2.6f:(t==1?2.1f:1.8f);
        for (int i=0;i<bcnt;i++) {
            Vector sp = new Vector((float)((r.nextDouble()-0.5)*(t==2?5.5:4.5)),(float)(r.nextDouble()*(t==2?4.5:3.5)),(float)((r.nextDouble()-0.5)*(t==2?5.5:4.5)));
            Color bpc = i%4==0?PURPLE:(i%4==1?Color.fromRGB(255,240,180):(i%4==2?GOLD:bc));
            w.spawnParticle(Particle.DUST,c.clone().add(sp),1,new Particle.DustOptions(bpc,bsz));
        }
        for (LivingEntity le:tg) {
            double bd = marked.containsKey(le.getUniqueId())?22:13;
            double dmg = bd*(1f+t*0.25f);
            le.damage(dmg,p); le.setVelocity(new Vector(0,-0.65f,0));
            spark(le.getLocation().add(0,1,0),w,t==2?Color.fromRGB(255,240,180):GOLD,8+t*2);
        }
        try {
            if (t==2) { if (p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+9)); p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,260,1,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,320,2,false,false)); }
            else if (t==1) { if (p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+6.5f)); p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,260,1,false,false)); p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,260,1,false,false)); }
            else if (p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+4.5f));
        } catch(Exception ignored) {}
        new BukkitRunnable() {
            int ff=0;
            public void run() {
                if (ff>30) { cancel(); return; }
                final float fp = (float)ff/30f;
                final float ef = (float)(1-Math.pow(1-fp,2));
                for (int i=0;i<12+t*4;i++) {
                    double a = Math.toRadians(i*(360.0/(12+t*4))+ff*9);
                    Vector off = new Vector((float)(Math.cos(a)*(1.7f+ef*2f)),ef*3f,(float)(Math.sin(a)*(1.7f+ef*2f)));
                    Color fc = i%4==0?PURPLE:(i%4==1?Color.fromRGB(255,240,180):(i%4==2?GOLD:bc));
                    w.spawnParticle(Particle.DUST,p.getLocation().clone().add(off),t+1,new Particle.DustOptions(fc,1.7f+t*0.35f));                }
                ff++;
            }
        }.runTaskTimer(plugin,0,2);
        new BukkitRunnable() { public void run() { if (p.isOnline()) { p.setVelocity(new Vector(0,0,0)); p.setFallDistance(0); } } }.runTaskLater(plugin,15);
    }
    
    private void zone(Location ctr,World w,Player src) {
        new BukkitRunnable() {
            int d=0;
            public void run() {
                if (d>40) { cancel(); return; }
                final float pr = (float)d/40f;
                final float rad = 1.3f+(float)(Math.sin(d*0.12)*0.3f);
                for (int i=0;i<10;i++) {
                    double a = Math.toRadians(i*36+d*8);
                    Vector off = new Vector((float)(Math.cos(a)*rad),0.15f,(float)(Math.sin(a)*rad));
                    Color zc = d%8<4?GREEN:(d%8<6?Color.fromRGB(100,255,200):Color.fromRGB(150,255,180));
                    w.spawnParticle(Particle.DUST,ctr.clone().add(off),1,new Particle.DustOptions(zc,1.2f*(1f-pr*0.25f)));
                }
                for (Entity en:w.getNearbyEntities(ctr,1.8,1.6,1.8)) if (en instanceof LivingEntity && !en.equals(src)) ((LivingEntity)en).damage(1.5,src);
                d++;
            }
        }.runTaskTimer(plugin,0,1);
    }
    
    private void chain(LivingEntity from,Player src,World w,int cd) {
        if (cd>1) return;
        LivingEntity nr=null; double md=5;
        for (Entity en:from.getWorld().getNearbyEntities(from.getLocation(),5,3,5)) {
            if (en instanceof LivingEntity && !en.equals(src) && en!=from) {
                double d = en.getLocation().distance(from.getLocation());
                if (d<md) { md=d; nr=(LivingEntity)en; }
            }
        }
        if (nr!=null) {
            Vector cd_dir = nr.getLocation().toVector().subtract(from.getLocation().toVector()).normalize();
            new BukkitRunnable() {
                int cf=0;
                public void run() {
                    if (cf>10) { nr.damage(3,src); spark(nr.getLocation().add(0,1,0),w,GREEN,4); w.playSound(nr.getLocation(),Sound.BLOCK_GRASS_BREAK,0.5f,1.8f); cancel(); return; }
                    final float pr = (float)cf/10f;
                    for (int i=0;i<14;i++) {
                        Location cl = from.getLocation().clone().add(cd_dir.clone().multiply((float)(i*0.3f*pr)));
                        cl.add(0,(float)(Math.sin(i*0.4+cf*0.3)*0.2f*pr),0);
                        Color cc = i%2==0?GREEN:Color.fromRGB(100,255,200);
                        w.spawnParticle(Particle.DUST,cl,1,new Particle.DustOptions(cc,1f*pr));
                    }
                    cf++;
                }            }.runTaskTimer(plugin,0,1);
        }
    }
    
    private void mark(LivingEntity t) {
        marked.put(t.getUniqueId(),System.currentTimeMillis()+6000);
        new BukkitRunnable() {
            int tm=0;
            public void run() {
                if (tm>120 || !t.isValid() || !marked.containsKey(t.getUniqueId())) { marked.remove(t.getUniqueId()); cancel(); return; }
                Location h = t.getLocation().add(0,2.6f,0);
                final float pl = 1f+(float)(Math.sin(tm*0.25)*0.18f);
                t.getWorld().spawnParticle(Particle.DUST,h,4,new Particle.DustOptions(GOLD,1.6f*pl));
                tm+=2;
            }
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
