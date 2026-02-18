package com.ramadhan;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public class ItemGuard implements Listener {

    // Anti Buang
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (isSpecial(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    // Anti Rename di Anvil
    @EventHandler
    public void onAnvil(InventoryClickEvent e) {
        if (e.getInventory().getType() == InventoryType.ANVIL && isSpecial(e.getCurrentItem())) {
            e.setCancelled(true);
        }
    }

    // Anti Drop pas mati (Keep on Death)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        List<ItemStack> drops = e.getDrops();
        drops.removeIf(item -> {
            if (isSpecial(item)) {
                e.getItemsToKeep().add(item); // Masukin ke daftar barang yang tetep dibawa
                return true;
            }
            return false;
        });
    }

    private boolean isSpecial(ItemStack item) {
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}

