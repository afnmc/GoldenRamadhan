package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey; // IMPORT INI
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {

    private static GoldenMoon instance;
    private DailyManager dailyManager;
    
    // KUNCI RAHASIA IDENTITAS PEDANG
    public static NamespacedKey SWORD_KEY;

    @Override
    public void onEnable() {
        instance = this;
        
        // Inisialisasi Key
        SWORD_KEY = new NamespacedKey(this, "golden_crescent_blade");

        saveDefaultConfig();
        this.dailyManager = new DailyManager(this);
        registerListeners();

        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        new MoonTask(this).runTaskTimer(this, 0L, 20L);

        getLogger().info("Golden Ramadhan v1.0 - ENABLED");
    }

    @Override
    public void onDisable() {
        getLogger().info("Golden Ramadhan v1.0 - DISABLED.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(dailyManager, this);
        pm.registerEvents(new DailyGUI(this), this);
        pm.registerEvents(new SkillListener(this), this); // Skill & Death Handler
        pm.registerEvents(new QuestManager(this), this);
        pm.registerEvents(new MoonTask(this), this);
    }

    public String getMsg(String path) {
        String msg = getConfig().getString(path);
        if (msg == null) return ChatColor.RED + "Config Missing: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public DailyManager getDailyManager() {
        return dailyManager;
    }

    public static GoldenMoon getInstance() {
        return instance;
    }
}
