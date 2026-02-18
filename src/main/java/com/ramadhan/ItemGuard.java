package com.ramadhan;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemGuard implements Listener {

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack item = e.getInventory().getItem(0);
        if (item == null || !isSpecial(item)) return;

        // Izinkan enchant, tapi blokir rename
        if (e.getInventory().getRenameText() != null && !e.getInventory().getRenameText().isEmpty()) {
            e.setResult(null);
        }
    }

    private boolean isSpecial(ItemStack item) {
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
