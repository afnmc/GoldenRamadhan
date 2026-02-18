package com.ramadhan;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {
    public static NamespacedKey SWORD_KEY;

    @Override
    public void onEnable() {
        SWORD_KEY = new NamespacedKey(this, "golden_crescent_blade");
        saveDefaultConfig();

        // Registrasi Listener
        DailyGUI dailyGUI = new DailyGUI(this);
        getServer().getPluginManager().registerEvents(dailyGUI, this);
        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        
        // Registrasi Command Admin
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }
        
        getLogger().info("§a[GoldenRamadhan] v16.8 Berhasil Load! Semua fitur aktif.");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c[GoldenRamadhan] v16.8 Shutdown.");
    }
}
