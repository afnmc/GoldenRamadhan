package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {

    private static GoldenMoon instance;
    public static NamespacedKey SWORD_KEY;

    @Override
    public void onEnable() {
        instance = this;
        SWORD_KEY = new NamespacedKey(this, "golden_crescent_blade");

        saveDefaultConfig();
        
        // Register Command
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        // Register Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new SkillListener(this), this);

        getLogger().info("========================================");
        getLogger().info("   Golden Moon v13 - FINAL BUILD       ");
        getLogger().info("   Status: Moonlight Ready!            ");
        getLogger().info("========================================");
    }

    public static GoldenMoon getInstance() { return instance; }
}
