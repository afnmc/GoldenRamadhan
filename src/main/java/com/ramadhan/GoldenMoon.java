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
        
        getLogger().info("§aGolden Moon v16.2 - Sabit Runcing Aktif!");
    }

    public String getMsg(String path) {
        String msg = getConfig().getString(path);
        return (msg == null) ? "§cMissing: " + path : ChatColor.translateAlternateColorCodes('&', msg);
    }

    public DailyManager getDailyManager() { return dailyManager; }
    public static GoldenMoon getInstance() { return instance; }
}
