package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {
    public static NamespacedKey SWORD_KEY;
    private DailyManager dailyManager;

    @Override
    public void onEnable() {
        SWORD_KEY = new NamespacedKey(this, "golden_crescent_blade");
        saveDefaultConfig();
        
        this.dailyManager = new DailyManager(this);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new DailyGUI(this), this);
        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        
        // Register Command
        getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        
        getLogger().info("§6[GoldenRamadhan] §aPlugin v17.6 Loaded! Jam Abidjan Aktif.");
    }

    public DailyManager getDailyManager() { return dailyManager; }

    // Fix Error getMsg
    public String getMsg(String path) {
        String msg = getConfig().getString("messages." + path);
        if (msg == null) return "§cMessage " + path + " not found!";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
