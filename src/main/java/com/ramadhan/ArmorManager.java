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
    
    private void updateArmorBonus(Player p) {
        ArmorSetData data = playerArmorData.computeIfAbsent(p.getUniqueId(), k -> new ArmorSetData());
        
        try { p.removePotionEffect(PotionEffectType.NIGHT_VISION); } catch(Exception ignored) {}
        try { p.removePotionEffect(PotionEffectType.SPEED); } catch(Exception ignored) {}
        
        if(hasCrescentSet(p) && !hasFullEliteSet(p)) {
            data.setBonusActive = true;
            data.isElite = false;
            try {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            } catch(Exception ignored) {}
        } else if(hasFullEliteSet(p)) {
            data.setBonusActive = true;
            data.isElite = true;
            try {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
            } catch(Exception ignored) {}
        } else {
            data.setBonusActive = false;
        }
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
        }
    }    
    public boolean tryMoonStep(Player p) {
        ArmorSetData data = playerArmorData.get(p.getUniqueId());
        if(data == null || !data.setBonusActive || data.isElite) return false;
        if(!hasPiece(p, GoldenMoon.ARMOR_BOOTS_KEY)) return false;
        if(!data.moonStepReady) return false;
        
        org.bukkit.util.Vector dir = p.getLocation().getDirection().setY(0).normalize();
        p.teleport(p.getLocation().add(dir.clone().multiply(3)));
        
        data.moonStepReady = false;
        new BukkitRunnable() {
            @Override
            public void run() {
                data.moonStepReady = true;
            }
        }.runTaskLater(plugin, 60);
        
        return true;
    }
    
    // ✅ FIX: GIVE KIT LENGKAP (HELMET, CHEST, LEGS, BOOTS, SHIELD)
    public boolean giveKit(Player p, String kitType) {
        org.bukkit.configuration.ConfigurationSection kitConfig = 
            plugin.getConfig().getConfigurationSection("admin-kits." + kitType);
        if(kitConfig == null) return false;
        
        String kitName = kitConfig.getString("name", "Unknown Kit");
        p.sendMessage("§6✦ §fMenerima: " + kitName);
        
        // ✅ Loop semua item di kit
        org.bukkit.configuration.ConfigurationSection itemsSection = kitConfig.getConfigurationSection("items");
        if(itemsSection == null) return false;
        
        for(String itemKey : itemsSection.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection itemConfig = itemsSection.getConfigurationSection(itemKey);
            if(itemConfig == null) continue;
            
            ItemStack item = createKitItem(itemConfig);
            if(item != null) {
                String type = itemConfig.getString("type", "").toLowerCase();
                
                // ✅ Equip di slot yang benar
                switch(type) {
                    case "helmet" -> {
                        p.getInventory().setHelmet(item);
                        p.sendMessage("§a  + §f" + item.getItemMeta().getDisplayName());
                    }
                    case "chestplate" -> {
                        p.getInventory().setChestplate(item);                        p.sendMessage("§a  + §f" + item.getItemMeta().getDisplayName());
                    }
                    case "leggings" -> {
                        p.getInventory().setLeggings(item);
                        p.sendMessage("§a  + §f" + item.getItemMeta().getDisplayName());
                    }
                    case "boots" -> {
                        p.getInventory().setBoots(item);
                        p.sendMessage("§a  + §f" + item.getItemMeta().getDisplayName());
                    }
                }
            }
        }
        
        // ✅ Give shield di offhand
        ItemStack shield = createLunarAegis();
        p.getInventory().setItemInOffHand(shield);
        p.sendMessage("§a  + §f" + shield.getItemMeta().getDisplayName());
        
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
                switch(type.toLowerCase()) {
                    case "helmet" -> key = GoldenMoon.ARMOR_HELMET_KEY;
                    case "chestplate" -> key = GoldenMoon.ARMOR_CHEST_KEY;
                    case "leggings" -> key = GoldenMoon.ARMOR_LEGS_KEY;
                    case "boots" -> key = GoldenMoon.ARMOR_BOOTS_KEY;
                    case "elite_helmet" -> key = GoldenMoon.ELITE_HELMET_KEY;
                    case "elite_chestplate" -> key = GoldenMoon.ELITE_CHEST_KEY;
                    case "elite_leggings" -> key = GoldenMoon.ELITE_LEGS_KEY;
                    case "elite_boots" -> key = GoldenMoon.ELITE_BOOTS_KEY;
                }
                
                if(key != null) {
                    meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);
                }
                item.setItemMeta(meta);            }
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
                "§7Shield dengan parry mechanic",
                "",
                "§e§lABILITY:",
                "§f- §6Block: §780% damage reduction",
                "§f- §bPerfect Parry: §7Reflect damage",
                "",
                "§8§oUnbreakable"
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
        boolean moonStepReady = true;
    }
            }
