package com.ramadhan;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

public class DailyGUI implements Listener {
    private final GoldenMoon plugin;
    private final Inventory inv;

    public DailyGUI(GoldenMoon plugin) {
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(null, 27, "§0Daily Rewards");
    }

    public void openInventory(Player p) {
        p.openInventory(this.inv);
    }
}
