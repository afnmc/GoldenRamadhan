package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {

    private static GoldenMoon instance;
    private DailyManager dailyManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Config
        saveDefaultConfig();

        // 2. Managers
        this.dailyManager = new DailyManager(this);

        // 3. Register Listeners
        var pm = getServer().getPluginManager();
        pm.registerEvents(new MoonTask(this), this);
        pm.registerEvents(dailyManager, this);
        pm.registerEvents(new QuestManager(this), this);
        pm.registerEvents(new SkillListener(this), this);
        pm.registerEvents(new DailyGUI(this), this); // GUI Listener

        // 4. Register Commands
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        // 5. Start Tasks
        new MoonTask(this).runTaskTimer(this, 0L, 20L);
        
        getLogger().info("Golden Ramadhan Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Golden Ramadhan Disabled.");
    }

    public DailyManager getDailyManager() {
        return dailyManager;
    }

    public static GoldenMoon getInstance() {
        return instance;
    }

    // --- INI BAGIAN YANG TADI HILANG ---
    /**
     * Helper untuk mengambil pesan berwarna dari config.yml
     */
    public String getMsg(String path) {
        String msg = getConfig().getString(path);
        if (msg == null) return ChatColor.RED + "Config missing: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    // -----------------------------------
}
