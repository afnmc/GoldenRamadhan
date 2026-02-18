package com.ramadhan;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public class ItemGuard implements Listener {

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack item = e.getInventory().getItem(0);
        if (item == null || !isSpecial(item)) return;
        // Izinkan Enchant, tapi jika kolom rename diisi, hilangkan hasilnya
        if (e.getInventory().getRenameText() != null && !e.getInventory().getRenameText().isEmpty()) {
            e.setResult(null);
        }
    }

    @EventHandler
    public void onDurability(PlayerItemDamageEvent e) {
        if (isSpecial(e.getItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        List<ItemStack> drops = e.getDrops();
        // Cari item special dan hapus dari drops agar tidak jatuh ke tanah
        drops.removeIf(item -> {
            if (isSpecial(item)) {
                e.getItemsToKeep().add(item); // Simpan di inventory player saat respawn
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
