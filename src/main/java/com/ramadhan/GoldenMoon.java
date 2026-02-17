package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GOLDEN RAMADHAN 2026 - Main Class
 * Supports Version 1.21.8 - 1.21.10
 */
public class GoldenMoon extends JavaPlugin {

    private static GoldenMoon instance;
    private DailyManager dailyManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Simpan config default (config.yml)
        saveDefaultConfig();

        // 2. Inisialisasi Manager
        this.dailyManager = new DailyManager(this);

        // 3. Registrasi Event / Listener
        registerListeners();

        // 4. Registrasi Perintah (Command)
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        // 5. Jalankan Task Visual Malam Hari (Partikel Emas)
        // Berjalan setiap 1 detik (20 ticks)
        new MoonTask(this).runTaskTimer(this, 0L, 20L);

        // Console Logging
        getLogger().info("========================================");
        getLogger().info("   Golden Ramadhan Plugin v1.0         ");
        getLogger().info("   GUI, Skills, & Time-Offset Ready    ");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Golden Ramadhan Plugin Disabled.");
    }

    /**
     * Mendaftarkan semua event ke server
     */
    private void registerListeners() {
        var pm = getServer().getPluginManager();
        
        // Visual Moon & Join Message
        pm.registerEvents(new MoonTask(this), this);
        
        // Login 30 Hari (Daily System)
        pm.registerEvents(dailyManager, this);
        
        // Misi Bagi Takjil (Quest)
        pm.registerEvents(new QuestManager(this), this);
        
        // Skill Pedang (Aura, Hit, Shield)
        // Melewatkan 'this' agar SkillListener bisa baca config
        pm.registerEvents(new SkillListener(this), this);
    }

    /**
     * Helper untuk mengambil pesan berwarna dari config.yml
     */
    public String getMsg(String path) {
        String msg = getConfig().getString(path, "&cMissing: " + path);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Mendapatkan instance DailyManager (untuk GUI & Command)
     */
    public DailyManager getDailyManager() {
        return dailyManager;
    }

    /**
     * Singleton instance
     */
    public static GoldenMoon getInstance() {
        return instance;
    }
}
