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
        if (!dataFile.exists()) { plugin.getDataFolder().mkdirs(); try { dataFile.createNewFile(); } catch (IOException ignored) {} }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        UUID id = p.getUniqueId();
        
        ZonedDateTime nowAbidjan = ZonedDateTime.now(ZoneId.of("UTC"));
        int resetHourWib = plugin.getConfig().getInt("daily-reset-hour-wib");
        LocalDate logicalDate = nowAbidjan.plusHours(7 - resetHourWib).toLocalDate();

        String last = dataConfig.getString(id + ".last");
        int prog = dataConfig.getInt(id + ".prog", 0);

        if (last == null || !last.equals(logicalDate.toString())) {
            int next = prog + 1;
            if (next <= 30) {
                dataConfig.set(id + ".prog", next);
                dataConfig.set(id + ".last", logicalDate.toString());
                saveData();
                giveReward(p, next);
            }
        }
    }

    public void giveReward(Player p, int day) {
        p.sendMessage(plugin.getMsg("daily-claimed").replace("%day%", String.valueOf(day)));
        if (day == 30) {
            p.getInventory().addItem(getSpecialBlade());
            p.sendMessage(plugin.getMsg("limited-item-received"));
        } else {
            p.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 3));
        }
    }

    public ItemStack getSpecialBlade() {
        ItemStack s = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta m = s.getItemMeta();
        m.setDisplayName("§6§lGolden Crescent Blade");
        m.setLore(Arrays.asList("§7Limited Edition - 2026", "§eSkill: §6Lunar Sweep (Hit)", "§eSkill: §6Lunar Shield (Shift)"));
        s.setItemMeta(m);
        return s;
    }

    public int getPlayerProgress(UUID uuid) { return dataConfig.getInt(uuid + ".prog", 0); }
    private void saveData() { try { dataConfig.save(dataFile); } catch (IOException ignored) {} }
}
