package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();
        
        // Reset Logic (WIB Offset)
        ZonedDateTime nowUTC = ZonedDateTime.now(ZoneId.of("UTC"));
        int offset = plugin.getConfig().getInt("daily-reset-hour-wib", 6);
        LocalDate logicalDate = nowUTC.plusHours(7 - offset).toLocalDate();

        String last = dataConfig.getString(id + ".last_login");
        int unlocked = dataConfig.getInt(id + ".unlocked", 0);

        if (last == null || !last.equals(logicalDate.toString())) {
            if (unlocked < 30) {
                dataConfig.set(id + ".unlocked", unlocked + 1);
                dataConfig.set(id + ".last_login", logicalDate.toString());
                saveData();
                p.sendMessage("§6§l🌙 Day " + (unlocked + 1) + " terbuka! §fKetik §b/gm daily");
            }
        }
    }

    public int getUnlockedLevel(UUID id) { return dataConfig.getInt(id + ".unlocked", 0); }
    public int getClaimedLevel(UUID id) { return dataConfig.getInt(id + ".claimed", 0); }
    public void setClaimedLevel(UUID id, int lv) { dataConfig.set(id + ".claimed", lv); saveData(); }

    public void giveReward(Player p, int day) {
        ItemStack item = (day == 30) ? getSpecialBlade() : new ItemStack(Material.GOLD_INGOT, 3);
        p.getInventory().addItem(item);
    }

    public ItemStack getSpecialBlade() {
        ItemStack s = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta m = s.getItemMeta();
        m.setDisplayName("§6§lGolden Crescent Blade");
        m.setLore(Arrays.asList(
            "§7Ramadhan 2026 Special", 
            "§fStatus: §aLegendary",
            "§eSkill 1: §6Lunar Slash (Hit)",
            "§eSkill 2: §6Crescent Burst (5x Hit + Shift)",
            "§bPasif: §fKeep Inventory (Soulbound)"
        ));
        // Wajib pasang PDC supaya SkillListener ngenalin pedang ini
        m.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte) 1);
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        s.setItemMeta(m);
        return s;
    }

    private void saveData() { try { dataConfig.save(dataFile); } catch (IOException ignored) {} }
}
