package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class GoldenMoon extends JavaPlugin {

    private static GoldenMoon instance;
    private DailyManager dailyManager; // Tambahkan ini agar tidak error di DailyGUI
    public static NamespacedKey SWORD_KEY;

    @Override
    public void onEnable() {
        instance = this;
        SWORD_KEY = new NamespacedKey(this, "golden_crescent_blade");

        saveDefaultConfig();
        
        // WAJIB: Inisialisasi dailyManager biar gak NullPointerException
        this.dailyManager = new DailyManager(this);

        // Register Command
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        // Register Listeners
        registerListeners();

        // Jalankan Task Visual Bulan
        new MoonTask(this).runTaskTimer(this, 0L, 20L);

        getLogger().info("========================================");
        getLogger().info("   Golden Moon v13 - FINAL BUILD       ");
        getLogger().info("   Status: Moonlight Ready!            ");
        getLogger().info("========================================");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(dailyManager, this);
        pm.registerEvents(new DailyGUI(this), this);
        pm.registerEvents(new SkillListener(this), this);
        pm.registerEvents(new QuestManager(this), this);
        pm.registerEvents(new MoonTask(this), this);
    }

    /**
     * Method untuk mengambil DailyManager.
     * Dibutuhkan oleh DailyGUI agar build tidak error.
     */
    public DailyManager getDailyManager() {
        return dailyManager;
    }

    /**
     * Method untuk mengambil pesan dari config dengan warna.
     * Dibutuhkan oleh MoonTask dan QuestManager agar build tidak error.
     */
    public String getMsg(String path) {
        String msg = getConfig().getString(path);
        if (msg == null) return ChatColor.RED + "Config Missing: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public static GoldenMoon getInstance() {
        return instance;
    }
}
