package com.ramadhan;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class DailyGUI {
    public static void open(Player p, int prog) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8🌙 Ramadhan Daily");
        for (int i = 1; i <= 30; i++) {
            ItemStack item = new ItemStack(i <= prog ? Material.LIME_STAINED_GLASS_PANE : (i == 30 ? Material.NETHERITE_SWORD : Material.RED_STAINED_GLASS_PANE));
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§eDay " + i);
            m.setLore(Arrays.asList("§7Status: " + (i <= prog ? "§aClaimed" : "§cLocked")));
            item.setItemMeta(m);
            inv.addItem(item);
        }
        p.openInventory(inv);
    }
}

