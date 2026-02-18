package com.ramadhan;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.entity.EntityDamageItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemGuard implements Listener {

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack result = e.getResult();
        if (result == null || !isSpecial(result)) return;

        // Cek apakah player mencoba mengganti nama
        if (e.getInventory().getRenameText() != null && !e.getInventory().getRenameText().isEmpty()) {
            e.setResult(null); // Matiin output kalau mau di-rename
        }
    }

    @EventHandler
    public void onItemDamage(EntityDamageItemEvent e) {
        if (isSpecial(e.getItem().getItemStack())) e.setCancelled(true);
    }

    private boolean isSpecial(ItemStack item) {
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
