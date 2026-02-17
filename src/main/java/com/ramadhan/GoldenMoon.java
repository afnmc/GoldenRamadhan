package com.ramadhan;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GOLDEN RAMADHAN 2026 - Main Class
 * Memastikan semua Manager dan Listener terdaftar dengan benar.
 */
public class GoldenMoon extends JavaPlugin {

    private static GoldenMoon instance;
    private DailyManager dailyManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Load atau buat config.yml default
        saveDefaultConfig();

        // 2. Inisialisasi DailyManager (Urusan data & hadiah)
        this.dailyManager = new DailyManager(this);

        // 3. Registrasi SEMUA Listener
        registerListeners();

        // 4. Registrasi Command /gm
        if (getCommand("goldenmoon") != null) {
            getCommand("goldenmoon").setExecutor(new AdminCommand(this));
        }

        // 5. Jalankan Task Visual Bulan Malam Hari
        new MoonTask(this).runTaskTimer(this, 0L, 20L);

        getLogger().info("========================================");
        getLogger().info("   Golden Ramadhan v1.0 - ENABLED      ");
        getLogger().info("   Semua Skill & Aura Telah Aktif!     ");
        getLogger().info("========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Golden Ramadhan v1.0 - DISABLED.");
    }

    /**
     * Mendaftarkan semua class event agar fungsi plugin jalan.
     */
    private void registerListeners() {
        var pm = getServer().getPluginManager();
        
        // Listener untuk sistem login harian
        pm.registerEvents(dailyManager, this);
        
        // Listener untuk GUI (Klaim manual & anti-curang)
        pm.registerEvents(new DailyGUI(this), this);
        
        // Listener untuk Skill Pedang (Aura, Hit, Dash, Shield)
        pm.registerEvents(new SkillListener(this), this);
        
        // Listener untuk Quest Bagi Takjil
        pm.registerEvents(new QuestManager(this), this);
        
        // Listener untuk visual partikel malam
        pm.registerEvents(new MoonTask(this), this);
    }

    /**
     * Helper Method: Mengambil pesan dari config.yml dengan warna.
     * Sangat penting agar MoonTask dan QuestManager tidak error saat compile.
     */
    public String getMsg(String path) {
        String msg = getConfig().getString(path);
        if (msg == null) return ChatColor.RED + "Config Missing: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public DailyManager getDailyManager() {
        return dailyManager;
    }

    public static GoldenMoon getInstance() {
        return instance;
    }
}
