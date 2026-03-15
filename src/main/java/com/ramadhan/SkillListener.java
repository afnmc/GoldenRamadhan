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
import org.bukkit.event.entity.EntityDamageEvent;
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
    
    private static final Color NONE_C = Color.fromRGB(200,200,220);
    private static final Color CRESC_C = Color.fromRGB(50,255,150);
    private static final Color ELITE_C = Color.fromRGB(255,215,0);
    private static final Color ELITE_A = Color.fromRGB(180,140,220);
    private static final Color WHITE_C = Color.fromRGB(255,250,240);
    
    private final GoldenMoon plugin;
    private final Map<UUID,PD> data = new HashMap<>();
    private final Map<UUID,Long> marked = new HashMap<>();
    private final Random r = new Random();
    
    public SkillListener(GoldenMoon p) { plugin = p; }
    
    @EventHandler public void onQ(PlayerQuitEvent e) { 
        data.remove(e.getPlayer().getUniqueId()); 
        marked.remove(e.getPlayer().getUniqueId());
    }
    
    @EventHandler public void onI(PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        if (!hasSword(p)) return;
        PD d = get(p);
        long n = System.currentTimeMillis();
        int t = tier(p);
        
        if (p.isSneaking() && (e.getAction()==Action.LEFT_CLICK_AIR || e.getAction()==Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            int cd = t==2?1000:(t==1?1500:2000);
            if (n-d.s1 < cd) { showCD(p,getSkillName(1,t),cd-(n-d.s1)); return; }
            skill1(p,t); d.s1 = n; return;
        }
        if (e.getAction()==Action.LEFT_CLICK_AIR || e.getAction()==Action.LEFT_CLICK_BLOCK) {
            int cd = t==2?500:(t==1?700:1000);
            if (n-d.s2 < cd) { showCD(p,getSkillName(2,t),cd-(n-d.s2)); return; }
            skill2(p,t); d.s2 = n; return;
        }
        if (e.getAction()==Action.RIGHT_CLICK_AIR || e.getAction()==Action.RIGHT_CLICK_BLOCK) {
            e.setCancelled(true);
            if (n-d.s3 < 15000) { showCD(p,getSkillName(3,t),15000-(n-d.s3)); return; }
            skill3(p,t); d.s3 = n;
        }
    }
    
    @EventHandler public void onDmg(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player)e.getEntity();
        int t = tier(p);
        if (t==1 && hasPiece(p,EquipmentSlot.CHEST,GoldenMoon.ARMOR_CHEST_KEY)) {
            e.setDamage(e.getDamage() * 0.85);
        }
        if (t==2 && hasPiece(p,EquipmentSlot.CHEST,GoldenMoon.ELITE_CHEST_KEY)) {
            e.setDamage(e.getDamage() * 0.75);
            if (r.nextInt(100)<20) {
                spark(p.getLocation().add(0,1,0),p.getWorld(),ELITE_C,5);
            }
        }
    }
    
    @EventHandler public void onA(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;        
        Player p = (Player)e.getDamager();
        int t = tier(p);
        if (t==2 && hasPiece(p,EquipmentSlot.HEAD,GoldenMoon.ELITE_HELMET_KEY) && p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue()*0.7) {
            e.setDamage(e.getDamage()*1.25);
            spark(e.getEntity().getLocation(),p.getWorld(),ELITE_A,4);
        }
        if (t>=1 && e.getEntity() instanceof LivingEntity && !e.getEntity().equals(p)) {
            mark((LivingEntity)e.getEntity());
        }
    }
    
    private void skill1(final Player p, int t) {
        final World w = p.getWorld();
        final Location l = p.getLocation();
        final Vector dir = l.getDirection().setY(0).normalize();
        
        if (t==0) { 
            p.sendTitle("§f§l✦ SHADOW STEP ✦","§7Teleport & Strike",2,15,5);
            Location tl = l.clone().add(dir.clone().multiply(3));
            p.teleport(tl);
            w.playSound(tl,Sound.ENTITY_ENDERMAN_TELEPORT,0.9f,1f);
            for(int i=0;i<25;i++) {
                Vector sp = new Vector((float)((r.nextDouble()-0.5)*1),(float)(r.nextDouble()*0.8),(float)((r.nextDouble()-0.5)*1));
                w.spawnParticle(Particle.DUST,tl.clone().add(sp),1,new Particle.DustOptions(NONE_C,1.3f));
            }
            for(Entity en:w.getNearbyEntities(tl,3,3,3)) {
                if(en instanceof LivingEntity && !en.equals(p)) {
                    ((LivingEntity)en).damage(7,p);
                    ((LivingEntity)en).setVelocity(dir.clone().multiply(0.5f).setY(0.4f));
                }
            }
        }
        else if (t==1) { 
            p.sendTitle("§b§l✦ MOONLIGHT DASH ✦","§aCurved Strike + Slow",2,15,5);
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if(f>15) { cancel(); return; }
                    float pr = (float)f/15f;
                    Location cl = l.clone().add(dir.clone().multiply(4f*pr));
                    cl.setY(l.getY()+(float)(Math.sin(pr*Math.PI)*2));
                    p.teleport(cl);
                    for(double a=-1.8;a<=1.8;a+=0.12) {
                        Vector arc = rotate(dir,90).multiply(a*0.8).add(dir.clone().multiply((float)(-a*a*0.35)));
                        w.spawnParticle(Particle.DUST,cl.clone().add(arc),1,new Particle.DustOptions(CRESC_C,1.5f));
                    }
                    for(Entity en:w.getNearbyEntities(cl,2.5,2.5,2.5)) {
                        if(en instanceof LivingEntity && !en.equals(p)) {                            
                            ((LivingEntity)en).damage(4,p);
                            ((LivingEntity)en).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,1,false,false));
                        }
                    }
                    if(f%3==0) w.playSound(cl,Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.4f,1.2f+f*0.05f);
                    f++;
                }
            }.runTaskTimer(plugin,0,1);
        }
        else { 
            p.sendTitle("§6§l✦ GOLDEN THUNDER ✦","§eMulti-Strike + Lightning",2,15,5);
            new BukkitRunnable() {
                int f=0;
                public void run() {
                    if(f>5) {
                        Location end = l.clone().add(dir.clone().multiply(6));
                        p.teleport(end);
                        w.playSound(end,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,1.3f,1f);
                        w.spawnParticle(Particle.FLASH,end,2);
                        for(Entity en:w.getNearbyEntities(end,5,4,5)) {
                            if(en instanceof LivingEntity && !en.equals(p)) {
                                ((LivingEntity)en).damage(10,p);
                                ((LivingEntity)en).setVelocity(dir.clone().multiply(0.7f).setY(0.6f));
                                spark(en.getLocation(),w,ELITE_C,8);
                            }
                        }
                        cancel(); return;
                    }
                    Location tl = l.clone().add(dir.clone().multiply(1.5f+f));
                    p.teleport(tl);
                    w.spawnParticle(Particle.FLASH,tl,1);
                    for(int i=0;i<12;i++) {
                        Vector sp = new Vector((float)((r.nextDouble()-0.5)*1.5),(float)(r.nextDouble()*1.2),(float)((r.nextDouble()-0.5)*1.5));
                        w.spawnParticle(Particle.DUST,tl.clone().add(sp),1,new Particle.DustOptions(i%2==0?ELITE_C:ELITE_A,1.7f));
                    }
                    for(Entity en:w.getNearbyEntities(tl,2.5,2.5,2.5)) {
                        if(en instanceof LivingEntity && !en.equals(p)) {
                            ((LivingEntity)en).damage(4,p);
                        }
                    }
                    w.playSound(tl,Sound.BLOCK_AMETHYST_BLOCK_CHIME,0.5f,1.5f+f*0.1f);
                    f++;
                }
            }.runTaskTimer(plugin,0,2);
        }
    }
    
    private void skill2(final Player p, int t) {
        final World w = p.getWorld();
        final Location st = p.getEyeLocation().add(p.getLocation().getDirection());
        final Vector dir = p.getLocation().getDirection().normalize();
        
        if (t==0) { 
            p.sendTitle("§f§l✦ VOID SPEAR ✦","§7Piercing Projectile",2,15,5);
            new BukkitRunnable() {
                int lf=0;
                public void run() {
                    if(lf>25) { cancel(); return; }
                    Location cur = st.clone().add(dir.clone().multiply(lf*1.1f));
                    w.spawnParticle(Particle.DUST,cur,5,new Particle.DustOptions(NONE_C,1.4f));
                    for(Entity en:w.getNearbyEntities(cur,1.8,1.8,1.8)) {
                        if(en instanceof LivingEntity && !en.equals(p)) {
                            ((LivingEntity)en).damage(6,p);
                            ((LivingEntity)en).setVelocity(dir.clone().multiply(0.7f).setY(0.4f));
                        }
                    }
                    lf++;
                }
            }.runTaskTimer(plugin,0,1);
            w.playSound(st,Sound.ENTITY_ARROW_SHOOT,0.8f,1.3f);
        }
        else if (t==1) { 
            p.sendTitle("§b§l✦ EMERALD SCYTHE ✦","§aSpinning + Chain",2,15,5);
            // Using array to bypass effectively final restriction for hit entity
            final LivingEntity[] hitContainer = {null};
            new BukkitRunnable() {
                int lf=0;
                public void run() {
                    if(lf>40) { cancel(); return; }
                    Location cur = st.clone().add(dir.clone().multiply(lf*0.9f));
                    for(double a=0;a<Math.PI*2;a+=0.25) {
                        Vector blade = new Vector((float)(Math.cos(a)*0.9),(float)(Math.sin(lf*0.3)*0.4),(float)(Math.sin(a)*0.9));
                        w.spawnParticle(Particle.DUST,cur.clone().add(rotate(blade,90)),1,new Particle.DustOptions(CRESC_C,1.4f));
                    }
                    if(hitContainer[0] == null) {
                        for(Entity en : w.getNearbyEntities(cur,2,2,2)) {
                            if(en instanceof LivingEntity && !en.equals(p)) {
                                hitContainer[0] = (LivingEntity)en;
                                hitContainer[0].damage(8,p);
                                mark(hitContainer[0]);
                                chain(hitContainer[0],p,w);
                            }
                        }
                    }
                    lf++;
                }
            }.runTaskTimer(plugin,0,1);
            w.playSound(st,Sound.ENTITY_ARROW_SHOOT,0.7f,1.5f);            
            w.playSound(st,Sound.BLOCK_GRASS_BREAK,0.5f,1.8f);
        }
        else { 
            p.sendTitle("§6§l✦ DRAGON ORBS ✦","§eTriple Homing + Explosion",2,15,5);
            for(int orb=0;orb<3;orb++) {
                final Vector odir = rotate(dir,(orb-1)*15);
                new BukkitRunnable() {
                    int lf=0;
                    public void run() {
                        if(lf>30) {
                            Location end = st.clone().add(odir.clone().multiply(26));
                            w.spawnParticle(Particle.EXPLOSION,end,2);
                            w.spawnParticle(Particle.DRAGON_BREATH,end,15,0.5f,0.5f,0.5f,0.1f);
                            for(Entity en:w.getNearbyEntities(end,4,3,4)) {
                                if(en instanceof LivingEntity && !en.equals(p)) {
                                    ((LivingEntity)en).damage(10,p);
                                    ((LivingEntity)en).setVelocity(new Vector(0,0.7f,0));
                                }
                            }
                            cancel(); return;
                        }
                        Location cur = st.clone().add(odir.clone().multiply(lf*0.95f));
                        if(lf>10) {
                            LivingEntity nr=null; double md=10;
                            for(Entity en:w.getNearbyEntities(cur,8,6,8)) {
                                if(en instanceof LivingEntity && !en.equals(p)) {
                                    double d=en.getLocation().distance(cur);
                                    if(d<md) { md=d; nr=(LivingEntity)en; }
                                }
                            }
                            if(nr!=null) {
                                Vector toT = nr.getLocation().add(0,1,0).toVector().subtract(cur.toVector()).normalize();
                                odir.add(toT.multiply(0.06f)).normalize();
                            }
                        }
                        w.spawnParticle(Particle.DUST,cur,5,new Particle.DustOptions(ELITE_C,2f));
                        w.spawnParticle(Particle.DUST,cur,2,new Particle.DustOptions(ELITE_A,1.5f));
                        lf++;
                    }
                }.runTaskTimer(plugin,orb*3,1);
            }
            w.playSound(st,Sound.BLOCK_BEACON_ACTIVATE,0.8f,0.85f);
            w.playSound(st,Sound.ENTITY_ENDER_DRAGON_GROWL,0.5f,0.9f);
        }
    }
    
    private void chain(final LivingEntity from, final Player src, final World w) {
        LivingEntity nr=null; double md=6;
        for(Entity en : from.getWorld().getNearbyEntities(from.getLocation(),6,4,6)) {
            if(en instanceof LivingEntity && !en.equals(src) && en!=from) {                
                double d=en.getLocation().distance(from.getLocation());
                if(d<md) { md=d; nr=(LivingEntity)en; }
            }
        }
        if(nr!=null) {
            final LivingEntity targetNr = nr;
            final Vector cd = targetNr.getLocation().toVector().subtract(from.getLocation().toVector()).normalize();
            new BukkitRunnable() {
                int cf=0;
                public void run() {
                    if(cf>10) { targetNr.damage(5,src); cancel(); return; }
                    float pr = (float)cf/10f;
                    for(int i=0;i<10;i++) {
                        Location cl = from.getLocation().clone().add(cd.clone().multiply((float)(i*0.5f*pr)));
                        w.spawnParticle(Particle.DUST,cl,1,new Particle.DustOptions(CRESC_C,1.2f*pr));
                    }
                    cf++;
                }
            }.runTaskTimer(plugin,0,1);
        }
    }
    
    private void skill3(final Player p, int t) {
        final World w = p.getWorld();
        final Location c = p.getLocation();
        
        if (t==0) { 
            p.sendTitle("§f§l✦ MOON BURST ✦","§7AOE Explosion",3,25,8);
            w.playSound(c,Sound.BLOCK_AMETHYST_BLOCK_HIT,1.2f,1.1f);
            w.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,0.9f,0.9f);
            for(int i=0;i<100;i++) {
                Vector sp = new Vector((float)((r.nextDouble()-0.5)*6),(float)(r.nextDouble()*5),(float)((r.nextDouble()-0.5)*6));
                w.spawnParticle(Particle.DUST,c.clone().add(sp),1,new Particle.DustOptions(NONE_C,1.6f));
            }
            for(Entity en:w.getNearbyEntities(c,7,5,7)) {
                if(en instanceof LivingEntity && !en.equals(p)) {
                    ((LivingEntity)en).damage(14,p);
                    ((LivingEntity)en).setVelocity(new Vector(0,0.6f,0));
                }
            }
            if(p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+6));
            }
        }
        else if (t==1) { 
            p.sendTitle("§b§l✦ CRESCENT VORTEX ✦","§aPull Enemies + Damage",3,25,8);
            w.playSound(c,Sound.BLOCK_AMETHYST_BLOCK_CHIME,1.3f,1.3f);
            w.playSound(c,Sound.ENTITY_ENDER_DRAGON_GROWL,0.6f,0.8f);            
            new BukkitRunnable() {
                int vf=0;
                public void run() {
                    if(vf>35) {
                        for(Entity en:w.getNearbyEntities(c,9,6,9)) {
                            if(en instanceof LivingEntity && !en.equals(p)) {
                                LivingEntity le=(LivingEntity)en;
                                Vector pull = c.toVector().subtract(le.getLocation().toVector()).normalize().multiply(0.9);
                                le.setVelocity(pull.setY(0.4f));
                                le.damage(12,p);
                            }
                        }
                        for(int i=0;i<70;i++) {
                            double angle = Math.toRadians(i*5);
                            Vector esp = new Vector((float)(Math.cos(angle)*7),(float)(r.nextDouble()*5),(float)(Math.sin(angle)*7));
                            w.spawnParticle(Particle.DUST,c.clone().add(esp),1,new Particle.DustOptions(i%2==0?CRESC_C:WHITE_C,1.7f));
                        }
                        cancel(); return;
                    }
                    float pr = (float)vf/35f;
                    float rad = 4f+pr*7f;
                    for(int i=0;i<40;i++) {
                        double a = Math.toRadians(i*9+vf*7);
                        double h = Math.sin(vf*0.2+i*0.3)*2.5;
                        Vector off = new Vector((float)(Math.cos(a)*rad),(float)(h*pr),(float)(Math.sin(a)*rad));
                        w.spawnParticle(Particle.DUST,c.clone().add(off),1,new Particle.DustOptions(vf%3==0?CRESC_C:(vf%3==1?Color.fromRGB(100,255,200):WHITE_C),1.6f*(1f-pr*0.3f)));
                    }
                    vf++;
                }
            }.runTaskTimer(plugin,0,1);
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,200,0,false,false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,200,0,false,false));
        }
        else { 
            p.sendTitle("§6§l✦ CELESTIAL ANNIHILATION ✦","§eDivine Punishment",4,30,10);
            w.playSound(c,Sound.BLOCK_BEACON_ACTIVATE,1.6f,0.7f);
            w.playSound(c,Sound.ENTITY_WITHER_SPAWN,0.9f,0.8f);
            final List<LivingEntity> tg = new ArrayList<>();
            for(Entity en:w.getNearbyEntities(c,12,7,12)) {
                if(en instanceof LivingEntity && !en.equals(p)) tg.add((LivingEntity)en);
            }
            new BukkitRunnable() {
                int pf=0;
                public void run() {
                    if(pf>30) { cancel(); return; }
                    float pr = (float)pf/30f;
                    for(int pi=0;pi<8;pi++) {
                        double pa = Math.toRadians(pi*45+pf*2);
                        Location pl = c.clone().add((float)(Math.cos(pa)*10),0,(float)(Math.sin(pa)*10));                        
                        for(int h=0;h<(int)(pr*25);h++) {
                            w.spawnParticle(Particle.DUST,pl.clone().add(0,h,0),3,new Particle.DustOptions(ELITE_C,1.9f));
                            if(pf%4==0) w.spawnParticle(Particle.DUST,pl.clone().add(0,h,0),1,new Particle.DustOptions(ELITE_A,1.5f));
                        }
                    }
                    pf++;
                }
            }.runTaskTimer(plugin,0,1);
            new BukkitRunnable() {
                int bf=0;
                public void run() {
                    if(bf>tg.size()*3+10) {
                        w.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,1.4f,0.7f);
                        for(int i=0;i<150;i++) {
                            Vector sp = new Vector((float)((r.nextDouble()-0.5)*9),(float)(r.nextDouble()*7),(float)((r.nextDouble()-0.5)*9));
                            w.spawnParticle(Particle.DUST,c.clone().add(sp),1,new Particle.DustOptions(i%3==0?ELITE_C:(i%3==1?ELITE_A:WHITE_C),2.3f));
                        }
                        for(LivingEntity le:tg) {
                            le.damage(18,p);
                            le.setVelocity(new Vector(0,-0.9f,0));
                        }
                        if(p.getHealth()<p.getAttribute(Attribute.MAX_HEALTH).getValue()) {
                            p.setHealth(Math.min(p.getAttribute(Attribute.MAX_HEALTH).getValue(),p.getHealth()+15));
                        }
                        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH,300,1,false,false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,400,2,false,false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,200,1,false,false));
                        cancel(); return;
                    }
                    if(bf<tg.size() && !tg.isEmpty()) {
                        final LivingEntity target = tg.get(bf % tg.size());
                        new BukkitRunnable() {
                            int beam=0;
                            public void run() {
                                if(beam>18) { target.damage(8,p); cancel(); return; }
                                Location bl = target.getLocation().clone().add(0,25-beam,0);
                                w.spawnParticle(Particle.DUST,bl,6,new Particle.DustOptions(ELITE_C,2.1f));
                                w.spawnParticle(Particle.FLAME,bl,2,0.15f,0.15f,0.15f,0.05f);
                                beam++;
                            }
                        }.runTaskTimer(plugin,0,1);
                    }
                    bf++;
                }
            }.runTaskTimer(plugin,31,2);
        }
    }
    
    private String getSkillName(int skill,int t) {
        if(skill==1) return t==0?"Shadow Step":(t==1?"Moonlight Dash":"Golden Thunder");
        if(skill==2) return t==0?"Void Spear":(t==1?"Emerald Scythe":"Dragon Orbs");
        if(skill==3) return t==0?"Moon Burst":(t==1?"Crescent Vortex":"Celestial Annihilation");
        return "Skill";
    }
    
    private void showCD(final Player p, final String skill, long ms) {
        final long[] s = {ms/1000};
        new BukkitRunnable() {
            public void run() {
                if(s[0]<=0 || !p.isOnline()) { cancel(); return; }
                sab(p,"§6"+skill+" §7§l✦ §f"+s[0]+"s");
                s[0]--;
            }
        }.runTaskTimer(plugin,0,20);
    }
    
    private void mark(final LivingEntity t) {
        marked.put(t.getUniqueId(),System.currentTimeMillis()+7000);
        new BukkitRunnable() {
            int tm=0;
            public void run() {
                if(tm>140 || !t.isValid() || !marked.containsKey(t.getUniqueId())) {
                    marked.remove(t.getUniqueId()); cancel(); return;
                }
                Location h = t.getLocation().add(0,2.8f,0);
                t.getWorld().spawnParticle(Particle.DUST,h,5,new Particle.DustOptions(Color.fromRGB(255,215,0),1.8f));
                tm+=2;
            }
        }.runTaskTimer(plugin,0,2);
    }
    
    private void spark(Location l,World w,Color c,int n) {
        for(int i=0;i<n;i++) {
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
        switch(sl) {
            case HEAD: it=p.getInventory().getHelmet(); break;
            case CHEST: it=p.getInventory().getChestplate(); break;
            case LEGS: it=p.getInventory().getLeggings(); break;
            case FEET: it=p.getInventory().getBoots(); break;
        }
        return it!=null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(key,PersistentDataType.BYTE);
    }
    
    private int tier(Player p) {
        if(plugin.getArmorManager().hasFullEliteSet(p)) return 2;
        if(plugin.getArmorManager().hasCrescentSet(p)) return 1;
        return 0;
    }
    
    private void sab(Player p,String m) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,TextComponent.fromLegacyText(m));
    }
    
    private PD get(Player p) {
        return data.computeIfAbsent(p.getUniqueId(),k->new PD());
    }
    
    private static class PD {
        long s1=0,s2=0,s3=0;
    }
}

