package com.ramadhan;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class ArmorManager implements Listener {
    
    private final GoldenMoon plugin;
    private final Map<UUID, ArmorSetData> playerArmorData = new HashMap<>();
    
    public ArmorManager(GoldenMoon plugin) {
        this.plugin = plugin;
    }
    
    private boolean hasPiece(Player p, NamespacedKey key) {
        for(ItemStack item : p.getInventory().getArmorContents()) {
            if(item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasCrescentSet(Player p) {
        return hasPiece(p, GoldenMoon.ARMOR_HELMET_KEY) &&
               hasPiece(p, GoldenMoon.ARMOR_CHEST_KEY) &&
               hasPiece(p, GoldenMoon.ARMOR_LEGS_KEY) &&
               hasPiece(p, GoldenMoon.ARMOR_BOOTS_KEY);
    }
    
    public boolean hasFullEliteSet(Player p) {
        return hasPiece(p, GoldenMoon.ELITE_HELMET_KEY) &&
               hasPiece(p, GoldenMoon.ELITE_CHEST_KEY) &&
               hasPiece(p, GoldenMoon.ELITE_LEGS_KEY) &&
               hasPiece(p, GoldenMoon.ELITE_BOOTS_KEY);
    }
    
    public boolean hasCrescentChestplate(Player p) {
        return hasPiece(p, GoldenMoon.ARMOR_CHEST_KEY);
    }
    
    public boolean hasCrescentLeggings(Player p) {
        return hasPiece(p, GoldenMoon.ARMOR_LEGS_KEY);
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        updateArmorBonus(e.getPlayer());
    }
    
    @EventHandler
    public void onArmorChange(org.bukkit.event.player.PlayerItemChangeEvent e) {
        if(e.getEntity() instanceof Player p) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateArmorBonus(p);
                }
            }.runTaskLater(plugin, 5);
        }
    }
    
    private void updateArmorBonus(Player p) {
        ArmorSetData data = playerArmorData.computeIfAbsent(p.getUniqueId(), k -> new ArmorSetData());
        
        // Remove old effects safely
        try { p.removePotionEffect(PotionEffectType.NIGHT_VISION); } catch(Exception ignored) {}
        try { p.removePotionEffect(PotionEffectType.SPEED); } catch(Exception ignored) {}
        
        if(hasCrescentSet(p) && !hasFullEliteSet(p)) {
            data.setBonusActive = true;
            data.isElite = false;
            try {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            } catch(Exception ignored) {}
            if(!data.auraTask) {
                data.auraTask = true;
                startAuraEffect(p, Color.fromRGB(255, 215, 0), 1.0f);
            }
        } else if(hasFullEliteSet(p)) {
            data.setBonusActive = true;
            data.isElite = true;
            try {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
            } catch(Exception ignored) {}
            if(!data.auraTask) {
                data.auraTask = true;
                startAuraEffect(p, Color.fromRGB(200, 200, 220), 1.3f);
            }
        } else {
            data.setBonusActive = false;
            if(data.auraTask) data.auraTask = false;
        }
    }
    
    private void startAuraEffect(Player p, Color color, float size) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ArmorSetData data = playerArmorData.get(p.getUniqueId());
                if(data == null || !data.setBonusActive || !p.isOnline()) {
                    this.cancel();
                    return;
                }
                double pulse = Math.sin(System.currentTimeMillis() / 200.0) * 0.3;
                for(double angle = 0; angle < 360; angle += 30) {
                    double rad = Math.toRadians(angle);
                    Location auraLoc = p.getLocation().add(
                        Math.cos(rad) * (1.2 + pulse),
                        0.5 + Math.random() * 1.5,
                        Math.sin(rad) * (1.2 + pulse)
                    );
                    p.getWorld().spawnParticle(Particle.DUST, auraLoc, 1,
                        new Particle.DustOptions(color, size));
                }
            }
        }.runTaskTimer(plugin, 0, 4);
    }
    
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player p)) return;
        ArmorSetData data = playerArmorData.get(p.getUniqueId());
        if(data == null || !data.setBonusActive) return;
        
        if(!data.isElite) {
            e.setDamage(e.getDamage() * 0.95);
        } else if(new Random().nextInt(100) < 10) {
            e.setCancelled(true);
            p.getWorld().spawnParticle(Particle.FLASH, p.getLocation().add(0, 1, 0), 1);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.5f, 2.0f);
        }
    }
    
    public boolean tryMoonStep(Player p) {
        ArmorSetData data = playerArmorData.get(p.getUniqueId());
        if(data == null || !data.setBonusActive || data.isElite) return false;
        if(!hasPiece(p, GoldenMoon.ARMOR_BOOTS_KEY)) return false;
        if(!data.moonStepReady) return false;
        
        Vector dir = p.getLocation().getDirection().setY(0).normalize();
        Location target = p.getLocation().add(dir.clone().multiply(4));
        
        for(double i = 0; i < 4; i += 0.4) {
            Location trail = p.getLocation().add(dir.clone().multiply(i).setY(0.1));
            p.getWorld().spawnParticle(Particle.DUST, trail, 2,
                new Particle.DustOptions(Color.fromRGB(255, 223, 100), 1.5f));
        }
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.8f);
        p.teleport(target);
        
        data.moonStepReady = false;
        new BukkitRunnable() {
            @Override
            public void run() {
                data.moonStepReady = true;
            }
        }.runTaskLater(plugin, 60);
        
        return true;
    }
    
    public boolean giveKit(Player p, String kitType) {
        org.bukkit.configuration.ConfigurationSection kitConfig = 
            plugin.getConfig().getConfigurationSection("admin-kits." + kitType);
        if(kitConfig == null) return false;
        
        String kitName = kitConfig.getString("name", "Unknown Kit");
        p.sendMessage("§6✦ §fMenerima: " + kitName);
        
        for(String itemKey : kitConfig.getKeys(false)) {
            if(itemKey.equalsIgnoreCase("name")) continue;
            org.bukkit.configuration.ConfigurationSection itemConfig = kitConfig.getConfigurationSection(itemKey);
            if(itemConfig == null) continue;
            ItemStack item = createKitItem(itemConfig);
            if(item != null) p.getInventory().addItem(item);
        }
        
        ItemStack shield = createLunarAegis();
        p.getInventory().setItemInOffHand(shield);
        updateArmorBonus(p);
        return true;
    }
    
    private ItemStack createKitItem(org.bukkit.configuration.ConfigurationSection config) {
        try {
            Material mat = Material.valueOf(config.getString("material", "NETHERITE_CHESTPLATE"));
            String type = config.getString("type", "");
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if(meta != null) {
                meta.setDisplayName(config.getString("name", "Custom Armor"));
                meta.setLore(config.getStringList("lore"));
                meta.setUnbreakable(true);
                NamespacedKey key = null;
                switch(type) {
                    case "helmet": key = GoldenMoon.ARMOR_HELMET_KEY; break;
                    case "chestplate": key = GoldenMoon.ARMOR_CHEST_KEY; break;
                    case "leggings": key = GoldenMoon.ARMOR_LEGS_KEY; break;
                    case "boots": key = GoldenMoon.ARMOR_BOOTS_KEY; break;
                    case "elite_helmet": key = GoldenMoon.ELITE_HELMET_KEY; break;
                    case "elite_chestplate": key = GoldenMoon.ELITE_CHEST_KEY; break;
                    case "elite_leggings": key = GoldenMoon.ELITE_LEGS_KEY; break;
                    case "elite_boots": key = GoldenMoon.ELITE_BOOTS_KEY; break;
                }
                if(key != null) {
                    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);
                }
                item.setItemMeta(meta);
            }
            return item;
        } catch(Exception e) {
            plugin.getLogger().warning("Failed to create kit item: " + e.getMessage());
            return null;
        }
    }
    
    private ItemStack createLunarAegis() {
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta meta = shield.getItemMeta();
        if(meta != null) {
            meta.setDisplayName("§b§lLunar §fAegis");
            meta.setLore(Arrays.asList(
                "§7Shield suci dengan parry mechanic.",
                "",
                "§e§lABILITY:",
                "§f- §6Block: §7Tahan kanan untuk mengurangi 80% damage",
                "§f- §bPerfect Parry: §7Release saat hit = reflect + counter",
                "§f- §aShield Bash: §7Klik kiri saat block = stun + knockback",
                "",
                "§8§oItem tidak akan drop & bebas di-rename"
            ));
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE, (byte)1);
            shield.setItemMeta(meta);
        }
        return shield;
    }
    
    public ArmorSetData getArmorData(Player p) {
        return playerArmorData.computeIfAbsent(p.getUniqueId(), k -> new ArmorSetData());
    }
    
    public static class ArmorSetData {
        boolean setBonusActive = false;
        boolean isElite = false;
        boolean auraTask = false;
        boolean moonStepReady = true;
    }
                                 }
