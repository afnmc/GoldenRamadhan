package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        MoonTask moonTask = new MoonTask(this);
        getServer().getPluginManager().registerEvents(moonTask, this);
        getServer().getPluginManager().registerEvents(new QuestManager(this), this);
        getServer().getPluginManager().registerEvents(new DailyManager(this), this);
        getServer().getPluginManager().registerEvents(new SkillListener(), this);
        moonTask.runTaskTimer(this, 0L, 20L);
        getLogger().info("Golden Ramadhan Enabled Successfully!");
    }
    public String getMsg(String path) {
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString(path, "Message missing"));
    }
}
