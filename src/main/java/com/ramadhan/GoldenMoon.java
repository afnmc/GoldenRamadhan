package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {
    private static GoldenMoon instance;
    private DailyManager dailyManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.dailyManager = new DailyManager(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new MoonTask(this), this);
        pm.registerEvents(dailyManager, this);
        pm.registerEvents(new QuestManager(this), this);
        pm.registerEvents(new SkillListener(this), this);
        pm.registerEvents(new DailyGUI(this), this);

        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        new MoonTask(this).runTaskTimer(this, 0L, 20L);
    }

    public DailyManager getDailyManager() { return dailyManager; }
    public static GoldenMoon getInstance() { return instance; }

    public String getMsg(String path) {
        String msg = getConfig().getString(path);
        if (msg == null) return ChatColor.RED + "Missing config: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
