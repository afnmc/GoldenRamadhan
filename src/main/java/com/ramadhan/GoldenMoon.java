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
        
        getServer().getPluginManager().registerEvents(new MoonTask(this), this);
        getServer().getPluginManager().registerEvents(dailyManager, this);
        getServer().getPluginManager().registerEvents(new QuestManager(this), this);
        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        
        getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        
        new MoonTask(this).runTaskTimer(this, 0L, 20L);
    }

    public DailyManager getDailyManager() { return dailyManager; }
    public String getMsg(String path) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(path, ""));
    }
    public static GoldenMoon getInstance() { return instance; }
}
