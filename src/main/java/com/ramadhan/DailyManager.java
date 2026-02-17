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
import java.util.Arrays;
import java.util.UUID;

public class DailyManager implements Listener {
    private final GoldenMoon plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final LocalDate startDate = LocalDate.of(2026, 2, 18);
    private final int eventDuration = 30;

    public DailyManager(GoldenMoon plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "userdata.yml");
        if (!dataFile.exists()) { try { dataFile.getParentFile().mkdirs(); dataFile.createNewFile(); } catch (IOException ignored) {} }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        LocalDate today = LocalDate.now();
        if (today.isBefore(startDate)) return;
        if (today.isAfter(startDate.plusDays(eventDuration - 1))) { p.sendMessage(plugin.getMsg("event-ended")); return; }

        UUID id = p.getUniqueId();
        String last = dataConfig.getString(id + ".last");
        int progress = dataConfig.getInt(id + ".prog", 0);

        if (last == null || !last.equals(today.toString())) {
            int next = progress + 1;
            if (next <= 30) {
                dataConfig.set(id + ".prog", next);
                dataConfig.set(id + ".last", today.toString());
                try { dataConfig.save(dataFile); } catch (IOException ignored) {}
                giveReward(p, next);
            }
        }
    }

    private void giveReward(Player p, int day) {
        p.sendMessage(plugin.getMsg("daily-claimed").replace("%day%", String.valueOf(day)));
        if (day == 30) {
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            ItemMeta m = sword.getItemMeta();
            m.setDisplayName("§6§lGolden Crescent Blade");
            m.setLore(Arrays.asList("§7Limited Edition - 2026", "§eAbility: §6Lunar Sweep", "§7Unlocked on Day 30."));
            sword.setItemMeta(m);
            p.getInventory().addItem(sword);
            p.sendMessage(plugin.getMsg("limited-item-received"));
        } else {
            p.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, 3));
        }
    }
}

