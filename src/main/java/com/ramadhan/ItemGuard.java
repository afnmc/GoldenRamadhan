package com.ramadhan;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemGuard implements Listener {

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack result = e.getResult();
        if (result == null || !isSpecial(result)) return;

        // Versi aman untuk cek rename tanpa trigger warning deprecation
        String renameText = e.getInventory().getRenameText();
        if (renameText != null && !renameText.isEmpty()) {
            e.setResult(null); 
        }
    }

    private boolean isSpecial(ItemStack item) {
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
