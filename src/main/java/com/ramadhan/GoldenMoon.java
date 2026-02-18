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
        
        this.dailyManager = new DailyManager(this);

        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        var pm = getServer().getPluginManager();
        pm.registerEvents(dailyManager, this);
        pm.registerEvents(new SkillListener(this), this);
        pm.registerEvents(new DailyGUI(this), this);
        pm.registerEvents(new QuestManager(this), this); // Pastikan ini ada

        getLogger().info("Golden Moon v16 - Methods Restored!");
    }

    // Method yang dicari QuestManager dan MoonTask
    public String getMsg(String path) {
        String msg = getConfig().getString(path);
        if (msg == null) return ChatColor.RED + "Missing: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public DailyManager getDailyManager() { return dailyManager; }
    public static GoldenMoon getInstance() { return instance; }
}
