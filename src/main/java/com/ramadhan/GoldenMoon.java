package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main Class for Golden Ramadhan Plugin
 * Supports Minecraft 1.21.8 - 1.21.10
 */
public class GoldenMoon extends JavaPlugin {

    private static GoldenMoon instance;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Initialize Configuration
        saveDefaultConfig();

        // 2. Register Events/Listeners
        registerListeners();

        // 3. Register Commands
        registerCommands();

        // 4. Start Visual Task (Golden Moon Particles)
        // Task runs every 20 ticks (1 second)
        new MoonTask(this).runTaskTimer(this, 0L, 20L);

        getLogger().info("========================================");
        getLogger().info("   Golden Ramadhan Plugin v1.0         ");
        getLogger().info("   Status: ENABLED                     ");
        getLogger().info("   Support: 1.21.8 - 1.21.10           ");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Golden Ramadhan Plugin Disabled. Goodbye!");
    }

    /**
     * Registers all event listeners for the plugin
     */
    private void registerListeners() {
        var pm = getServer().getPluginManager();
        
        // Visual Moon & Join Messages
        pm.registerEvents(new MoonTask(this), this);
        
        // 30-Day Daily Login System
        pm.registerEvents(new DailyManager(this), this);
        
        // Takjil Sharing Quest
        pm.registerEvents(new QuestManager(this), this);
        
        // Lunar Sweep Skill Logic
        pm.registerEvents(new SkillListener(), this);
    }

    /**
     * Registers all commands for the plugin
     */
    private void registerCommands() {
        // Make sure "ramadhan" is defined in your plugin.yml
        if (getCommand("ramadhan") != null) {
            getCommand("ramadhan").setExecutor(new AdminCommand());
        }
    }

    /**
     * Helper to get colored messages from config.yml
     * @param path The config path
     * @return Colored string
     */
    public String getMsg(String path) {
        String message = getConfig().getString(path, "&cMessage missing: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Singleton pattern to access plugin instance
     * @return GoldenMoon instance
     */
    public static GoldenMoon getInstance() {
        return instance;
    }
}
