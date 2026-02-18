package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {
    public static NamespacedKey SWORD_KEY;
    private DailyManager dailyManager;

    @Override
    public void onEnable() {
        // Initialize Key
        SWORD_KEY = new NamespacedKey(this, "golden_crescent_blade");
        saveDefaultConfig();
        
        this.dailyManager = new DailyManager(this);

        // Register Listeners (Termasuk ItemGuard baru)
        getServer().getPluginManager().registerEvents(new DailyGUI(this), this);
        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemGuard(), this);
        
        // Register Command
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }
        
        getLogger().info("§6[GoldenMoon] §eVersion 20.0 §a- Visual God Tier & Security Active!");
    }

    public DailyManager getDailyManager() { return dailyManager; }

    // Helper untuk ambil pesan dari config
    public String getMsg(String path) {
        String msg = getConfig().getString("messages." + path);
        if (msg == null) return "§cMessage " + path + " not found!";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
