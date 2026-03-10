package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {
    
    // 🔑 PERSISTENT DATA KEYS
    public static NamespacedKey SWORD_KEY;
    public static NamespacedKey SHIELD_KEY;
    public static NamespacedKey FRAGMENT_KEY;
    
    // Armor Keys - Crescent Set
    public static NamespacedKey ARMOR_HELMET_KEY;
    public static NamespacedKey ARMOR_CHEST_KEY;
    public static NamespacedKey ARMOR_LEGS_KEY;
    public static NamespacedKey ARMOR_BOOTS_KEY;
    
    // Armor Keys - Elite Set
    public static NamespacedKey ELITE_HELMET_KEY;
    public static NamespacedKey ELITE_CHEST_KEY;
    public static NamespacedKey ELITE_LEGS_KEY;
    public static NamespacedKey ELITE_BOOTS_KEY;
    
    // Managers
    private DailyManager dailyManager;
    private ArmorManager armorManager;

    @Override
    public void onEnable() {
        // Initialize Keys
        String prefix = "goldenmoon";
        SWORD_KEY = new NamespacedKey(this, prefix + "_crescent_blade");
        SHIELD_KEY = new NamespacedKey(this, prefix + "_lunar_aegis");
        FRAGMENT_KEY = new NamespacedKey(this, prefix + "_lunar_fragment");
        
        ARMOR_HELMET_KEY = new NamespacedKey(this, prefix + "_crescent_helmet");
        ARMOR_CHEST_KEY = new NamespacedKey(this, prefix + "_crescent_chest");
        ARMOR_LEGS_KEY = new NamespacedKey(this, prefix + "_crescent_legs");
        ARMOR_BOOTS_KEY = new NamespacedKey(this, prefix + "_crescent_boots");
        
        ELITE_HELMET_KEY = new NamespacedKey(this, prefix + "_elite_helmet");
        ELITE_CHEST_KEY = new NamespacedKey(this, prefix + "_elite_chest");
        ELITE_LEGS_KEY = new NamespacedKey(this, prefix + "_elite_legs");
        ELITE_BOOTS_KEY = new NamespacedKey(this, prefix + "_elite_boots");
        
        // Save & load config
        saveDefaultConfig();
        
        // Initialize Managers
        this.dailyManager = new DailyManager(this);
        this.armorManager = new ArmorManager(this);
        
        // Register Events
        getServer().getPluginManager().registerEvents(new DailyGUI(this), this);
        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemGuard(this), this);
        getServer().getPluginManager().registerEvents(armorManager, this);
        getServer().getPluginManager().registerEvents(new TraderGUI(this), this);
        getServer().getPluginManager().registerEvents(new MoonTask(this), this);
        getServer().getPluginManager().registerEvents(new QuestManager(this), this);
        
        // Register Commands
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }
        
        // Start Moon Task
        new MoonTask(this).runTaskTimer(this, 0, 20);
        
        getLogger().info("§6[GoldenMoon] §eVersion 21.0-Ramadan §a- Loaded!");
        getLogger().info("§7  ├─ Combat System: §aActive");
        getLogger().info("§7  ├─ Daily Rewards: §aActive");
        getLogger().info("§7  ├─ Fragment Trader: §aActive");
        getLogger().info("§7  └─ Armor Set Bonus: §aActive");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("§6[GoldenMoon] §cDisabled.");
    }

    // === Getters ===
    public DailyManager getDailyManager() { return dailyManager; }
    public ArmorManager getArmorManager() { return armorManager; }

    // === Config Helpers ===
    public String getMsg(String path) {
        String msg = getConfig().getString("messages." + path);
        if (msg == null) return "§cMessage " + path + " not found!";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    public String getFragmentName() {
        return getConfig().getString("fragment-name", "§b§lLunar Fragment");
    }
            }
