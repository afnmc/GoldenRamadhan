package com.ramadhan;

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

        // Registrasi Event
        getServer().getPluginManager().registerEvents(new DailyGUI(this), this);
        getServer().getPluginManager().registerEvents(new SkillListener(this), this);
        
        // Registrasi Command Admin (Buat ambil item)
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }
        
        getLogger().info("§6[GoldenRamadhan] §aPlugin v17.5 Aktif! Timezone: Abidjan.");
    }

    public DailyManager getDailyManager() {
        return dailyManager;
    }
}
