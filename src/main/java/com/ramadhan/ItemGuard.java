package com.ramadhan;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public class ItemGuard implements Listener {

    // Simpan item sementara buat dikasih pas respawn
    private final Map<UUID, List<ItemStack>> savedItems = new HashMap<>();

    // 1. Anti Buang
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (isSpecial(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }

    // 2. Anti Rename di Anvil
    @EventHandler
    public void onAnvil(InventoryClickEvent e) {
        if (e.getInventory().getType() == InventoryType.ANVIL && isSpecial(e.getCurrentItem())) {
            e.setCancelled(true);
        }
    }

    // 3. Anti Hilang Pas Mati (Manual Backup)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        List<ItemStack> toSave = new ArrayList<>();
        
        // Cek item di drops, kalau ada yang spesial, hapus dari tanah dan simpan
        Iterator<ItemStack> iterator = e.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (isSpecial(item)) {
                toSave.add(item.clone());
                iterator.remove();
            }
        }
        
        if (!toSave.isEmpty()) {
            savedItems.put(p.getUniqueId(), toSave);
        }
    }

    // 4. Balikin item pas respawn
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (savedItems.containsKey(p.getUniqueId())) {
            for (ItemStack item : savedItems.get(p.getUniqueId())) {
                p.getInventory().addItem(item);
            }
            savedItems.remove(p.getUniqueId());
        }
    }

    private boolean isSpecial(ItemStack item) {
        return item != null && item.hasItemMeta() && 
               item.getItemMeta().getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE);
    }
}
