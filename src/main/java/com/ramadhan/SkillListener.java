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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
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
    private static final Color WHITE_C = Color.fromRGB(255,250,240);
    private static final Color RED_C = Color.fromRGB(255,80,80);
    
    private final GoldenMoon plugin;
    private final Map<UUID,PD> data = new HashMap<>();
    private final Map<UUID,Long> marked = new HashMap<>();
    private final Map<UUID,Boolean> flying = new HashMap<>();
    private final Random r = new Random();    
    public SkillListener(GoldenMoon p) { plugin = p; }
    
    @EventHandler public void onQ(PlayerQuitEvent e) { 
        data.remove(e.getPlayer().getUniqueId()); 
        flying.remove(e.getPlayer().getUniqueId());
        marked.remove(e.getPlayer().getUniqueId());
    }
    
    @EventHandler public void onI(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!hasSword(p)) return;
        PD d = get(p);
        long n = System.currentTimeMillis();
        int t = tier(p);
        
        // ⚡ SKILL 1: DASH ATTACK
        if (p.isSneaking() && (e.getAction()==Action.LEFT_CLICK_AIR || e.getAction()==Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            int cd = t==2?800:(t==1?1200:1500);
            if (n-d.ld < cd) { showCD(p,"Dash",cd-(n-d.ld)); return; }
            dashAttack(p,t); d.ld = n; return;
        }
        // 🌙 SKILL 2: PROJECTILE ATTACK
        if (e.getAction()==Action.LEFT_CLICK_AIR || e.getAction()==Action.LEFT_CLICK_BLOCK) {
            int cd = t==2?300:(t==1?450:600);
            if (n-d.ls < cd) { showCD(p,"Skill",cd-(n-d.ls)); return; }
            projectileAttack(p,t); d.ls = n; return;
        }
        // 🌕 SKILL 3: ULTIMATE ATTACK
        if (e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (n-d.lu < 12000) { showCD(p,"Ultimate",12000-(n-d.lu)); return; }
            ultimateAttack(p,t); d.lu = n;
        }
    }
    
    // ==========================================
    // ☁️ SKILL 4: FLY/SLAM (ELITE ONLY)
    // ==========================================
    @EventHandler public void onFly(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!hasSword(p) || tier(p)!=2) return;
        if (!hasPiece(p,EquipmentSlot.CHEST,GoldenMoon.ELITE_CHEST_KEY)) {
            e.setCancelled(true); p.setFlying(false); return;
        }
        PD d = get(p);
        if (e.isFlying()) {
            flying.put(p.getUniqueId(),true);
            p.setAllowFlight(true);            p.setFlying(true);
            Location cl = p.getLocation().clone().add(0,-0.5,0);
            spawnCloud(cl,p.getWorld());
            p.playSound(cl,Sound.BLOCK_CLOUD_SPAWN,1f,1.2f);
            showCD(p,"Fly",99999);
        } else {
            flying.put(p.getUniqueId(),false);
            p.setAllowFlight(false);
            Location gl = getGroundBelow(p);
            if (gl!=null) slamAttack(p,gl);
            showCD(p,"Fly CD",5000);
            d.lu = System.currentTimeMillis()+5000;
        }
    }
    
    private void spawnCloud(Location l,World w) {
        for (int i=0;i<25;i++) {
            Vector sp = new Vector((float)((r.nextDouble()-0.5)*2.5),0.4f+(float)(r.nextDouble()*0.5),(float)((r.nextDouble()-0.5)*2.5));
            w.spawnParticle(Particle.DUST,l.clone().add(sp),1,new Particle.DustOptions(WHITE_C,1.6f));
        }
        for (int i=0;i<20;i++) {
            Vector sp = new Vector((float)((r.nextDouble()-0.5)*2),0.6f+(float)(r.nextDouble()*0.7),(float)((r.nextDouble()-0.5)*2));
            Color c = i%2==0?ELITE_C:WHITE_C;
            w.spawnParticle(Particle.DUST,l.clone().add(sp),1,new Particle.DustOptions(c,1.4f));
        }
    }
    
    private Location getGroundBelow(Player p) {
        Location l = p.getLocation();
        for (int y=0;y<60;y++) {
            Location check = l.clone().add(0,-y,0);
            if (check.getBlock().getType()!=Material.AIR && check.getBlock().getType()!=Material.CAVE_AIR) {
                return check.add(0,1,0);
            }
        }
        return null;
    }
    
    private void slamAttack(Player p,Location gl) {
        World w = p.getWorld();
        p.teleport(gl.clone().add(0,2,0));
        p.setFallDistance(0);
        
        w.playSound(gl,Sound.ENTITY_GENERIC_EXPLODE,1.3f,0.85f);
        w.playSound(gl,Sound.BLOCK_STONE_BREAK,1.1f,1f);
        w.playSound(gl,Sound.ENTITY_ENDER_DRAGON_GROWL,0.8f,0.9f);
        
        for (int i=0;i<50;i++) {            
            Vector sp = new Vector((float)((r.nextDouble()-0.5)*3.5),(float)(r.nextDouble()*5),(float)((r.nextDouble()-0.5)*3.5));
            w.spawnParticle(Particle.CRIT,gl.clone().add(sp),1);
        }
        
        for (int i=0;i<35;i++) {
            final int fi=i;
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if (f>12) { cancel(); return; }
                    Vector base = new Vector((float)((r.nextDouble()-0.5)*4),2f+(float)(r.nextDouble()*3),(float)((r.nextDouble()-0.5)*4));
                    Color c = fi%3==0?ELITE_C:(fi%3==1?WHITE_C:ELITE_A);
                    w.spawnParticle(Particle.DUST,gl.clone().add(base).add(0,-f*0.3,0),1,new Particle.DustOptions(c,1.5f*(1f-f/12f)));
                    f++;
                }
            }.runTaskTimer(plugin,fi,1);
        }
        
        for (int i=0;i<80;i++) {
            double angle = Math.toRadians(i*4.5);
            float speed = 0.4f+(float)(r.nextDouble()*0.5f);
            Vector vel = new Vector((float)(Math.cos(angle)*speed),0.3f+(float)(r.nextDouble()*0.4f),(float)(Math.sin(angle)*speed));
            Location cloudP = gl.clone().add(0,1.5,0);
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if (f>20) { cancel(); return; }
                    w.spawnParticle(Particle.DUST,cloudP.clone().add(vel.clone().multiply(f)),1,new Particle.DustOptions(f%2==0?ELITE_C:WHITE_C,1.3f*(1f-f/20f)));
                    f++;
                }
            }.runTaskTimer(plugin,0,1);
        }
        
        for (int dx=-3;dx<=3;dx++) {
            for (int dz=-3;dz<=3;dz++) {
                if (Math.abs(dx)+Math.abs(dz)<=4) {
                    Location bl = gl.clone().add(dx,0,dz);
                    w.spawnParticle(Particle.BLOCK_CRACK,bl.add(0.5,0.5,0.5),4,0.25f,0.25f,0.25f,0,Material.STONE.createBlockData());
                    w.spawnParticle(Particle.BLOCK_CRACK,bl.add(0.5,0.5,0.5),3,0.2f,0.2f,0.2f,0,Material.COBBLESTONE.createBlockData());
                }
            }
        }
        
        new BukkitRunnable() {
            int rf=0;
            public void run() {                
                if (rf>25) { cancel(); return; }
                final float rad = 1.5f+rf*0.35f;
                for (int i=0;i<35;i++) {
                    double a = Math.toRadians(i*10.3+rf*6);
                    Vector off = new Vector((float)(Math.cos(a)*rad),0.08f,(float)(Math.sin(a)*rad));
                    Color c = rf%3==0?ELITE_C:(rf%3==1?WHITE_C:ELITE_A);
                    w.spawnParticle(Particle.DUST,gl.clone().add(off),1,new Particle.DustOptions(c,1.4f*(1f-rf/25f)));
                }
                rf++;
            }
        }.runTaskTimer(plugin,0,1);
        
        for (Entity en:w.getNearbyEntities(gl,5,4,5)) {
            if (en instanceof LivingEntity && !en.equals(p)) {
                LivingEntity le = (LivingEntity)en;
                le.damage(18,p);
                le.setVelocity(new Vector(0,1f,0));
                spark(le.getLocation(),w,ELITE_C,12);
                new BukkitRunnable() {
                    int bf=0;
                    public void run() {
                        if (bf>10) { cancel(); return; }
                        for (int i=0;i<3;i++) {
                            Vector bsp = new Vector((float)((r.nextDouble()-0.5)*0.8),(float)(r.nextDouble()*0.6),(float)((r.nextDouble()-0.5)*0.8));
                            w.spawnParticle(Particle.DUST,le.getLocation().clone().add(0,1,0).add(bsp),1,new Particle.DustOptions(RED_C,1.2f));
                        }
                        bf++;
                    }
                }.runTaskTimer(plugin,0,3);
            }
        }
    }
    
    @EventHandler public void onDmg(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player)e.getEntity();
        if (flying.containsKey(p.getUniqueId()) && flying.get(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }
    
    @EventHandler public void onA(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player)e.getDamager();
        int t = tier(p);
        if (t==2 && hasPiece(p,EquipmentSlot.HEAD,GoldenMoon.ELITE_HELMET_KEY) && p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()*0.7) {
            e.setDamage(e.getDamage()*1.2); spark(e.getEntity().getLocation(),p.getWorld(),ELITE_A,5);
        }        
        if (t>=1 && hasPiece(p,EquipmentSlot.CHEST,GoldenMoon.ARMOR_CHEST_KEY) && e.getEntity() instanceof LivingEntity && !e.getEntity().equals(p)) {
            mark((LivingEntity)e.getEntity());
        }
    }
    
    private void dashAttack(Player p,int t) {
        World w = p.getWorld();
        Location l = p.getLocation();
        Vector dir = l.getDirection().setY(0).normalize();
        
        if (t==0) {
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if (f>8) {
                        Location end = l.clone().add(dir.clone().multiply(3));
                        p.teleport(end);
                        for (double a=-1;a<=1;a+=0.15) {
                            Vector slash = rotate(dir,90).multiply(a*0.8).add(dir.clone().multiply((float)(-a*a*0.4)));
                            w.spawnParticle(Particle.DUST,end.clone().add(slash),1,new Particle.DustOptions(NONE_C,1.3f));
                        }
                        for (Entity en:w.getNearbyEntities(end,3,2.5,3)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                ((LivingEntity)en).damage(6,p);
                                ((LivingEntity)en).setVelocity(dir.clone().multiply(0.5f).setY(0.4f));
                                spark(en.getLocation(),w,NONE_C,6);
                            }
                        }
                        w.playSound(end,Sound.ENTITY_PLAYER_ATTACK_SWEEP,1f,1.3f);
                        cancel(); return;
                    }
                    Location dl = l.clone().add(dir.clone().multiply(f*0.4));
                    p.teleport(dl);
                    for (int i=0;i<5;i++) {
                        Vector sp = new Vector((float)((r.nextDouble()-0.5)*0.6),(float)(r.nextDouble()*0.5),(float)((r.nextDouble()-0.5)*0.6));
                        w.spawnParticle(Particle.DUST,dl.clone().add(sp),1,new Particle.DustOptions(NONE_C,1.1f));
                    }
                    w.playSound(dl,Sound.ENTITY_ENDERMAN_TELEPORT,0.4f,1.2f);
                    f++;
                }
            }.runTaskTimer(plugin,0,1);
            showCD(p,"Dash",1500);
        }
        else if (t==1) {
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if (f>15) { cancel(); return; }
                    final float pr = (float)f/15f;
                    Vector curve = dir.clone().multiply(4f*pr).setY((float)(Math.sin(pr*Math.PI)*2));
                    Location cl = l.clone().add(curve);
                    p.teleport(cl);
                    for (double a=-2;a<=2;a+=0.12) {
                        double cv = (a*a)*0.4;
                        Vector arc = rotate(dir,90).multiply(a*0.9).add(dir.clone().multiply((float)-cv));
                        Color c = f%3==0?CRESC_C:(f%3==1?Color.fromRGB(100,255,200):WHITE_C);
                        w.spawnParticle(Particle.DUST,cl.clone().add(arc),1,new Particle.DustOptions(c,1.5f));
                    }
                    for (Entity en:w.getNearbyEntities(cl,2.5,2.5,2.5)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            LivingEntity le = (LivingEntity)en;
                            le.damage(4,p);
                            le.setVelocity(dir.clone().multiply(0.4f).setY(0.5f));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,30,1,false,false));
                            spark(le.getLocation(),w,CRESC_C,5);
                        }
                    }
                    if (f%3==0) w.playSound(cl,Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.4f,1.2f+f*0.05f);
                    f++;
                }
            }.runTaskTimer(plugin,0,1);
            w.playSound(l,Sound.ENTITY_PLAYER_ATTACK_SWEEP,0.9f,1.4f);
            showCD(p,"Dash",1200);
        }
        else {
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if (f>5) {
                        Location end = l.clone().add(dir.clone().multiply(5));
                        p.teleport(end);
                        w.playSound(end,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,1.2f,1.1f);
                        w.spawnParticle(Particle.FLASH,end,2);
                        for (int i=0;i<8;i++) {
                            double angle = Math.toRadians(i*45);
                            Location pillar = end.clone().add((float)(Math.cos(angle)*2),0,(float)(Math.sin(angle)*2));
                            new BukkitRunnable() {
                                int pf=0;
                                public void run() {
                                    if (pf>12) { cancel(); return; }
                                    w.spawnParticle(Particle.DUST,pillar.clone().add(0,pf*0.3,0),2,new Particle.DustOptions(pf%2==0?ELITE_C:ELITE_A,1.8f));
                                    pf++;
                                }
                            }.runTaskTimer(plugin,0,1);
                        }
                        for (Entity en:w.getNearbyEntities(end,4,3,4)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                ((LivingEntity)en).damage(8,p);
                                ((LivingEntity)en).setVelocity(dir.clone().multiply(0.7f).setY(0.6f));
                                spark(en.getLocation(),w,ELITE_C,10);
                                new BukkitRunnable() {
                                    int lf=0;
                                    public void run() {
                                        if (lf>8) { cancel(); return; }
                                        w.spawnParticle(Particle.DUST,en.getLocation().clone().add(0,1,0),2,new Particle.DustOptions(ELITE_A,1.5f));
                                        lf++;
                                    }
                                }.runTaskTimer(plugin,0,2);
                            }
                        }
                        cancel(); return;
                    }
                    Location tl = l.clone().add(dir.clone().multiply(1+f*0.9));
                    p.teleport(tl);
                    w.spawnParticle(Particle.FLASH,tl,1);
                    for (int i=0;i<15;i++) {
                        Vector sp = new Vector((float)((r.nextDouble()-0.5)*1.4),(float)(r.nextDouble()*1.2),(float)((r.nextDouble()-0.5)*1.4));
                        Color c = i%2==0?ELITE_C:ELITE_A;
                        w.spawnParticle(Particle.DUST,tl.clone().add(sp),1,new Particle.DustOptions(c,1.7f));
                    }
                    for (Entity en:w.getNearbyEntities(tl,2,2,2)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            ((LivingEntity)en).damage(3,p);
                            spark(en.getLocation(),w,ELITE_C,4);
                        }
                    }
                    w.playSound(tl,Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.5f,1.5f+f*0.15f);
                    f++;
                }
            }.runTaskTimer(plugin,0,2);
            showCD(p,"Dash",800);
        }
    }
    
    private void projectileAttack(Player p,int t) {
        World w = p.getWorld();
        Location st = p.getEyeLocation().add(p.getLocation().getDirection());
        Vector dir = p.getLocation().getDirection().normalize();
        
        if (t==0) {
            new BukkitRunnable() {
                int lf=0;
                public void run() {
                    if (lf>25) { cancel(); return; }
                    Location cur = st.clone().add(dir.clone().multiply(lf*1f));
                    w.spawnParticle(Particle.DUST,cur,4,new Particle.DustOptions(NONE_C,1.3f));
                    w.spawnParticle(Particle.DUST,cur.clone().add(dir.clone().multiply(-0.3)),2,new Particle.DustOptions(WHITE_C,1f));
                    for (int i=0;i<3;i++) {
                        Vector tsp = dir.clone().multiply(-i*0.4);
                        w.spawnParticle(Particle.DUST,cur.clone().add(tsp),1,new Particle.DustOptions(NONE_C,1f-i*0.2f));
                    }
                    for (Entity en:w.getNearbyEntities(cur,1.5,1.5,1.5)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            ((LivingEntity)en).damage(5,p);
                            ((LivingEntity)en).setVelocity(dir.clone().multiply(0.6f).setY(0.3f));
                            spark(en.getLocation(),w,NONE_C,7);
                        }
                    }
                    lf++;
                }
            }.runTaskTimer(plugin,0,1);
            w.playSound(st,Sound.ENTITY_ARROW_SHOOT,0.8f,1.3f);
            w.playSound(st,Sound.ENTITY_PLAYER_ATTACK_SWEEP,0.5f,1.5f);
            showCD(p,"Skill",600);
        }
        else if (t==1) {
            new BukkitRunnable() {
                int lf=0; boolean ret=false;
                LivingEntity hit=null;
                public void run() {
                    if (lf>45) { cancel(); return; }
                    float prog = ret ? (45f-lf)/22f : Math.min(1f,lf/22f);
                    if (lf==22 && hit==null) ret=true;
                    Location cur = st.clone().add(dir.clone().multiply((ret?22-lf:lf)*0.9));
                    for (double a=0;a<Math.PI*2;a+=0.3) {
                        double radius = 0.8 + Math.sin(lf*0.5+a)*0.3;
                        Vector blade = new Vector((float)(Math.cos(a)*radius),(float)(Math.sin(lf*0.3)*0.4),(float)(Math.sin(a)*radius));
                        blade = rotate(blade,90);
                        Color c = lf%4==0?CRESC_C:(lf%4==1?Color.fromRGB(100,255,200):(lf%4==2?WHITE_C:CRESC_C));
                        w.spawnParticle(Particle.DUST,cur.clone().add(blade),1,new Particle.DustOptions(c,1.4f));
                    }
                    w.spawnParticle(Particle.DUST,cur,3,new Particle.DustOptions(CRESC_C,1.6f));
                    if (hit==null) {
                        for (Entity en:w.getNearbyEntities(cur,1.8,1.8,1.8)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                hit = (LivingEntity)en;
                                ((LivingEntity)en).damage(7,p);
                                mark((LivingEntity)en);
                                spark(en.getLocation(),w,CRESC_C,8);
                                ((LivingEntity)en).setVelocity(dir.clone().multiply(0.5f).setY(0.4f));
                                chainAttack(en,p,w);
                                break;
                            }
                        }
                    }
                    lf++;
                }
            }.runTaskTimer(plugin,0,1);
            w.playSound(st,Sound.ENTITY_ARROW_SHOOT,0.7f,1.5f);
            w.playSound(st,Sound.BLOCK_GRASS_BREAK,0.5f,1.8f);
            w.playSound(st,Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.4f,2f);
            showCD(p,"Skill",450);
        }
        else {
            for (int orb=0;orb<3;orb++) {
                final int oi=orb;
                final Vector odir = rotate(dir,(orb-1)*15);
                new BukkitRunnable() {
                    int lf=0;
                    public void run() {
                        if (lf>30) {
                            Location end = st.clone().add(odir.clone().multiply(26));
                            w.spawnParticle(Particle.EXPLOSION,end,2);
                            w.spawnParticle(Particle.DRAGON_BREATH,end,20,0.5f,0.5f,0.5f,0.1f);
                            for (int i=0;i<40;i++) {
                                double angle = Math.toRadians(i*9);
                                Vector esp = new Vector((float)(Math.cos(angle)*3),(float)(r.nextDouble()*2.5),(float)(Math.sin(angle)*3));
                                Color c = i%3==0?ELITE_C:(i%3==1?ELITE_A:WHITE_C);
                                w.spawnParticle(Particle.DUST,end.clone().add(esp),1,new Particle.DustOptions(c,1.8f));
                            }
                            for (Entity en:w.getNearbyEntities(end,3.5,3,3.5)) {
                                if (en instanceof LivingEntity && !en.equals(p)) {
                                    ((LivingEntity)en).damage(9,p);
                                    ((LivingEntity)en).setVelocity(new Vector(0,0.6f,0));
                                    spark(en.getLocation(),w,ELITE_C,8);
                                    new BukkitRunnable() {
                                        int bf=0;
                                        public void run() {
                                            if (bf>12) { cancel(); return; }
                                            for (int i=0;i<4;i++) {
                                                Vector bsp = new Vector((float)((r.nextDouble()-0.5)*1),(float)(r.nextDouble()*0.8),(float)((r.nextDouble()-0.5)*1));
                                                w.spawnParticle(Particle.FLAME,en.getLocation().clone().add(0,1,0).add(bsp),1);
                                            }
                                            bf++;
                                        }
                                    }.runTaskTimer(plugin,0,2);
                                }
                            }
                            cancel(); return;
                        }
                        Location cur = st.clone().add(odir.clone().multiply(lf*0.9f));
                        if (lf>8) {
                            LivingEntity nearest=null; double md=8;
                            for (Entity en:w.getNearbyEntities(cur,7,5,7)) {
                                if (en instanceof LivingEntity && !en.equals(p)) {
                                    double d = en.getLocation().distance(cur);
                                    if (d<md) { md=d; nearest=(LivingEntity)en; }
                                }
                            }
                            if (nearest!=null) {
                                Vector toT = nearest.getLocation().add(0,1,0).toVector().subtract(cur.toVector()).normalize();
                                odir.add(toT.multiply(0.05f)).normalize();
                            }
                        }
                        w.spawnParticle(Particle.DUST,cur,4,new Particle.DustOptions(ELITE_C,2f));
                        w.spawnParticle(Particle.DUST,cur,2,new Particle.DustOptions(ELITE_A,1.5f));
                        w.spawnParticle(Particle.DRAGON_BREATH,cur,2,0.2f,0.2f,0.2f,0.05f);
                        for (int i=0;i<2;i++) {
                            double sa = Math.toRadians(lf*30+i*180);
                            Vector spiral = new Vector((float)(Math.cos(sa)*0.4),(float)(Math.sin(lf*0.4)*0.3),(float)(Math.sin(sa)*0.4));
                            w.spawnParticle(Particle.DUST,cur.clone().add(spiral),1,new Particle.DustOptions(WHITE_C,1.2f));
                        }
                        lf++;
                    }
                }.runTaskTimer(plugin,orb*3,1);
            }
            w.playSound(st,Sound.BLOCK_BEACON_ACTIVATE,0.8f,0.85f);
            w.playSound(st,Sound.ENTITY_BLAZE_SHOOT,0.6f,1f);
            w.playSound(st,Sound.ENTITY_ENDER_DRAGON_GROWL,0.4f,0.9f);
            showCD(p,"Skill",300);
        }
    }
    
    private void chainAttack(LivingEntity from,Player src,World w) {
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
                    if (cf>10) { nr.damage(4,src); spark(nr.getLocation(),w,CRESC_C,5); cancel(); return; }
                    final float pr = (float)cf/10f;
                    for (int i=0;i<12;i++) {
                        Location cl = from.getLocation().clone().add(cd.clone().multiply((float)(i*0.4f*pr)));
                        cl.add(0,(float)(Math.sin(i*0.5+cf*0.4)*0.25f*pr),0);
                        Color c = i%2==0?CRESC_C:Color.fromRGB(100,255,200);
                        w.spawnParticle(Particle.DUST,cl,1,new Particle.DustOptions(c,1.1f*pr));
                        if (i%3==0) w.spawnParticle(Particle.DUST,cl.clone().add(0,0.2f,0),1,new Particle.DustOptions(WHITE_C,0.9f*pr));
                    }
                    cf++;
                }
            }.runTaskTimer(plugin,0,1);
        }
    }
    
    private void ultimateAttack(Player p,int t) {
        World w = p.getWorld();
        Location c = p.getLocation();
        
        if (t==0) {
            p.sendTitle("§f§l✦ MOON BURST ✦","§7Attack Mode",3,25,8);
            w.playSound(c,Sound.BLOCK_AMETHYST_BLOCK_HIT,1.1f,1.1f);
            w.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,0.8f,0.9f);
            for (int i=0;i<100;i++) {
                Vector sp = new Vector((float)((r.nextDouble()-0.5)*5),(float)(r.nextDouble()*4),(float)((r.nextDouble()-0.5)*5));
                Color c_layer = i%3==0?NONE_C:(i%3==1?WHITE_C:Color.fromRGB(180,180,200));
                w.spawnParticle(Particle.DUST,c.clone().add(sp),1,new Particle.DustOptions(c_layer,1.6f));
            }
            new BukkitRunnable() {
                int rf=0;
                public void run() {
                    if (rf>20) { cancel(); return; }
                    final float rad = 2f+rf*0.35f;
                    for (int i=0;i<30;i++) {
                        double a = Math.toRadians(i*12+rf*5);
                        Vector off = new Vector((float)(Math.cos(a)*rad),0.1f,(float)(Math.sin(a)*rad));
                        w.spawnParticle(Particle.DUST,c.clone().add(off),1,new Particle.DustOptions(NONE_C,1.4f*(1f-rf/20f)));
                    }
                    rf++;
                }
            }.runTaskTimer(plugin,0,1);
            for (Entity en:w.getNearbyEntities(c,6,5,6)) {
                if (en instanceof LivingEntity && !en.equals(p)) {
                    ((LivingEntity)en).damage(12,p);
                    ((LivingEntity)en).setVelocity(new Vector(0,0.5f,0));
                    spark(en.getLocation(),w,NONE_C,8);
                }
            }
            if (p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+5));
            showCD(p,"Ultimate",12000);
        }
        else if (t==1) {
            p.sendTitle("§b§l✦ CRESCENT VORTEX ✦","§aPull & Destroy",4,28,9);
            w.playSound(c,Sound.BLOCK_AMETHYST_BLOCK_CHIME,1.2f,1.3f);
            w.playSound(c,Sound.ENTITY_ENDER_DRAGON_GROWL,0.5f,0.8f);
            new BukkitRunnable() {
                int vf=0;
                public void run() {
                    if (vf>35) {
                        for (Entity en:w.getNearbyEntities(c,8,6,8)) {
                            if (en instanceof LivingEntity && !en.equals(p)) {
                                LivingEntity le = (LivingEntity)en;
                                Vector pull = c.toVector().subtract(le.getLocation().toVector()).normalize().multiply(1f);
                                le.setVelocity(pull.setY(0.4f));
                                le.damage(10,p);
                                spark(le.getLocation(),w,CRESC_C,7);
                            }
                        }
                        for (int i=0;i<60;i++) {
                            double angle = Math.toRadians(i*6);
                            Vector esp = new Vector((float)(Math.cos(angle)*6),(float)(r.nextDouble()*4),(float)(Math.sin(angle)*6));
                            w.spawnParticle(Particle.DUST,c.clone().add(esp),1,new Particle.DustOptions(i%2==0?CRESC_C:WHITE_C,1.6f));
                        }
                        cancel(); return;
                    }
                    final float pr = (float)vf/35f;
                    final float rad = 3f+pr*6f;
                    for (int i=0;i<40;i++) {
                        double a = Math.toRadians(i*9+vf*8);
                        double height = Math.sin(vf*0.2+i*0.3)*2;
                        Vector off = new Vector((float)(Math.cos(a)*rad),(float)(height*pr),(float)(Math.sin(a)*rad));
                        Color c_v = vf%4==0?CRESC_C:(vf%4==1?Color.fromRGB(100,255,200):(vf%4==2?WHITE_C:CRESC_C));
                        w.spawnParticle(Particle.DUST,c.clone().add(off),1,new Particle.DustOptions(c_v,1.5f*(1f-pr*0.3f)));
                    }
                    for (Entity en:w.getNearbyEntities(c,8,6,8)) {
                        if (en instanceof LivingEntity && !en.equals(p)) {
                            LivingEntity le = (LivingEntity)en;
                            Vector pull = c.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.5f*pr);
                            le.setVelocity(pull.setY(0.2f*pr));
                        }
                    }
                    vf++;
                }
            }.runTaskTimer(plugin,0,1);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,200,0,false,false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,200,0,false,false));
            showCD(p,"Ultimate",12000);
        }
        else {
            p.sendTitle("§6§l✦ GOLDEN ANNIHILATION ✦","§eDivine Punishment",5,35,10);
            w.playSound(c,Sound.BLOCK_BEACON_ACTIVATE,1.5f,0.75f);
            w.playSound(c,Sound.ENTITY_WITHER_SPAWN,0.8f,0.85f);
            w.playSound(c,Sound.ENTITY_ENDER_DRAGON_GROWL,0.7f,0.9f);
            
            final double zr = 10;
            List<LivingEntity> tg = new ArrayList<>();
            for (Entity en:w.getNearbyEntities(c,zr,6,zr)) if (en instanceof LivingEntity && !en.equals(p)) tg.add((LivingEntity)en);
            
            new BukkitRunnable() {
                int pf=0;
                public void run() {
                    if (pf>30) { cancel(); return; }
                    final float pr = (float)pf/30f;
                    for (int pi=0;pi<8;pi++) {
                        double pa = Math.toRadians(pi*45+pf*2);
                        Location pl = c.clone().add((float)(Math.cos(pa)*zr*0.85),0,(float)(Math.sin(pa)*zr*0.85));
                        for (int h=0;h<(int)(pr*25);h++) {
                            w.spawnParticle(Particle.DUST,pl.clone().add(0,h,0),3,new Particle.DustOptions(ELITE_C,1.8f));
                            if (pf%4==0) w.spawnParticle(Particle.DUST,pl.clone().add(0,h,0),1,new Particle.DustOptions(ELITE_A,1.4f));
                            if (pf%6==0) w.spawnParticle(Particle.FLAME,pl.clone().add(0,h,0),1,0.1f,0.1f,0.1f,0.05f);
                        }
                    }
                    pf++;
                }
            }.runTaskTimer(plugin,0,1);
            
            new BukkitRunnable() {
                int bf=0;
                public void run() {
                    if (bf>tg.size()*4) {
                        w.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,1.3f,0.75f);
                        w.playSound(c,Sound.BLOCK_END_PORTAL_SPAWN,1f,0.8f);
                        for (int i=0;i<150;i++) {
                            Vector sp = new Vector((float)((r.nextDouble()-0.5)*8),(float)(r.nextDouble()*6),(float)((r.nextDouble()-0.5)*8));
                            Color c_exp = i%4==0?ELITE_C:(i%4==1?ELITE_A:(i%4==2?WHITE_C:Color.fromRGB(255,240,180)));
                            w.spawnParticle(Particle.DUST,c.clone().add(sp),1,new Particle.DustOptions(c_exp,2.2f));
                        }
                        w.spawnParticle(Particle.DRAGON_BREATH,c,50,3f,2f,3f,0.2f);
                        for (LivingEntity le:tg) {
                            le.damage(15,p);
                            le.setVelocity(new Vector(0,-0.8f,0));
                            spark(le.getLocation(),w,ELITE_C,12);
                            new BukkitRunnable() {
                                int mf=0;
                                public void run() {
                                    if (mf>15) { cancel(); return; }
                                    for (int i=0;i<5;i++) {
                                        Vector msp = new Vector((float)((r.nextDouble()-0.5)*1.2),(float)(r.nextDouble()*1),(float)((r.nextDouble()-0.5)*1.2));
                                        w.spawnParticle(Particle.DUST,le.getLocation().clone().add(0,1,0).add(msp),1,new Particle.DustOptions(RED_C,1.4f));
                                    }
                                    mf++;
                                }
                            }.runTaskTimer(plugin,0,2);
                        }
                        if (p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+12));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,300,1,false,false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,400,2,false,false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,200,1,false,false));
                        cancel(); return;
                    }
                    if (bf<tg.size() && !tg.isEmpty()) {
                        LivingEntity target = tg.get(bf%tg.size());
                        new BukkitRunnable() {
                            int beam=0;
                            public void run() {
                                if (beam>20) {
                                    target.damage(7,p);
                                    spark(target.getLocation(),w,ELITE_A,8);
                                    cancel(); return;
                                }
                                Location beamLoc = target.getLocation().clone().add(0,20-beam,0);
                                w.spawnParticle(Particle.DUST,beamLoc,5,new Particle.DustOptions(ELITE_C,2f));
                                w.spawnParticle(Particle.FLAME,beamLoc,2,0.15f,0.15f,0.15f,0.05f);
                                w.spawnParticle(Particle.DRAGON_BREATH,beamLoc,1,0.1f,0.1f,0.1f,0.02f);
                                beam++;
                            }
                        }.runTaskTimer(plugin,0,1);
                    }
                    bf++;
                }
            }.runTaskTimer(plugin,31,2);
            showCD(p,"Ultimate",12000);
        }
    }
    
    private void showCD(Player p,String skill,long ms) {
        final long[] sec = {ms/1000};
        new BukkitRunnable() {
            int s = (int)sec[0];
            public void run() {
                if (s<=0 || !p.isOnline()) { cancel(); return; }
                sab(p,"§6"+skill+" §7§l✦ §f"+s+"s");
                s--;
            }
        }.runTaskTimer(plugin,0,20);
    }
    
    private void mark(LivingEntity t) {
        marked.put(t.getUniqueId(),System.currentTimeMillis()+6000);
        new BukkitRunnable() {
            int tm=0;
            public void run() {
                if (tm>120 || !t.isValid() || !marked.containsKey(t.getUniqueId())) { marked.remove(t.getUniqueId()); cancel(); return; }
                Location h = t.getLocation().add(0,2.6f,0);
                final float pl = 1f+(float)(Math.sin(tm*0.25)*0.18f);
                t.getWorld().spawnParticle(Particle.DUST,h,5,new Particle.DustOptions(Color.fromRGB(255,215,0),1.7f*pl));
                tm+=2;
            }
        }.runTaskTimer(plugin,0,2);
    }
    
    private void spark(Location l,World w,Color c,int n) {
        for (int i=0;i<n;i++) {
            Vector sp = new Vector((float)((r.nextDouble()-0.5)*0.6),(float)(r.nextDouble()*0.7),(float)((r.nextDouble()-0.5)*0.6));
            w.spawnParticle(Particle.DUST,l.clone().add(sp),1,new Particle.DustOptions(c,1.4f));
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

