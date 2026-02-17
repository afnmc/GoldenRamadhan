package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

public class DailyManager implements Listener {
    private final GoldenMoon plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public DailyManager(GoldenMoon plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "userdata.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();

        // Logika Waktu (Abidjan ke WIB)
        ZonedDateTime nowAbidjan = ZonedDateTime.now(ZoneId.of("UTC"));
        int resetHourWib = plugin.getConfig().getInt("daily-reset-hour-wib", 6);
        LocalDate logicalDate = nowAbidjan.plusHours(7 - resetHourWib).toLocalDate();

        String lastLoginDate = dataConfig.getString(id + ".last_login_date");
        int unlockedDay = dataConfig.getInt(id + ".unlocked", 0);

        // Jika hari baru, buka kunci level selanjutnya
        if (lastLoginDate == null || !lastLoginDate.equals(logicalDate.toString())) {
            int nextDay = unlockedDay + 1;
            if (nextDay <= 30) {
                dataConfig.set(id + ".unlocked", nextDay);
                dataConfig.set(id + ".last_login_date", logicalDate.toString());
                saveData();

                p.sendMessage("§6§l🌙 RAMADHAN EVENT");
                p.sendMessage("§eDay " + nextDay + " §ftelah terbuka!");
                p.sendMessage("§fKetik §b/gm daily §funtuk mengambil hadiahmu.");
            }
        }
    }

    // Ambil level yang sudah DIKLAIM
    public int getClaimedLevel(UUID uuid) {
        return dataConfig.getInt(uuid + ".claimed", 0);
    }

    // Set level klaim
    public void setClaimedLevel(UUID uuid, int level) {
        dataConfig.set(uuid + ".claimed", level);
        saveData();
    }

    // Ambil level yang TERBUKA (Unlocked)
    public int getUnlockedLevel(UUID uuid) {
        return dataConfig.getInt(uuid + ".unlocked", 0);
    }

    // Fungsi beri hadiah (Dipanggil dari GUI)
    public void giveReward(Player p, int day) {
        ItemStack reward;
        if (day == 30) {
            reward = getSpecialBlade();
            p.sendMessage("§b§l[!] AMAZING! §fKamu mendapatkan §6Golden Crescent Blade!");
        } else {
            // Hadiah biasa: 3 Gold Ingot
            reward = new ItemStack(Material.GOLD_INGOT, 3);
            p.sendMessage("§a[Daily] §fKamu menerima hadiah §eDay " + day);
        }

        // Cek inventory penuh
        if (p.getInventory().firstEmpty() != -1) {
            p.getInventory().addItem(reward);
        } else {
            p.getWorld().dropItem(p.getLocation(), reward);
            p.sendMessage("§cInventory penuh! Barang dijatuhkan di kaki.");
        }
    }

    public ItemStack getSpecialBlade() {
        ItemStack s = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName("§6§lGolden Crescent Blade");
            m.setLore(Arrays.asList(
                "§7Limited Edition - Ramadhan 2026",
                "§eSkill: §6Lunar Sweep (Hit)",
                "§eSkill: §6Lunar Shield (Shift)"
            ));
            m.setUnbreakable(true);
            s.setItemMeta(m);
        }
        return s;
    }

    private void saveData() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
