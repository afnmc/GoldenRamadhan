package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {

    private static GoldenMoon instance;
    private DailyManager dailyManager;
    public static NamespacedKey SWORD_KEY;

    @Override
    public void onEnable() {
        instance = this;
        SWORD_KEY = new NamespacedKey(this, "golden_crescent_blade");
        saveDefaultConfig();
        
        // Inisialisasi Manager
        this.dailyManager = new DailyManager(this);

        // Register Command
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        // Register Events
        var pm = getServer().getPluginManager();
        pm.registerEvents(dailyManager, this);
        pm.registerEvents(new SkillListener(this), this);
        pm.registerEvents(new DailyGUI(this), this);

        getLogger().info("========================================");
        getLogger().info("   Golden Ramadhan v14 - STABLE        ");
        getLogger().info("   Sistem: Individual Progress         ");
        getLogger().info("========================================");
    }

    public DailyManager getDailyManager() { return dailyManager; }

    public String getMsg(String path) {
        String msg = getConfig().getString(path);
        if (msg == null) return ChatColor.RED + "Missing: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static GoldenMoon getInstance() { return instance; }
}
