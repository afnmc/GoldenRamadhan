package com.ramadhan;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemGuard implements Listener {
    
    // Map buat nyimpen item pas mati biar gak ilang
    private final Map<UUID, List<ItemStack>> savedItems = new HashMap<>();

    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        ItemStack item = e.getInventory().getItem(0);
        if (item == null || !isSpecial(item)) return;

        // Blokir rename: kalau teks di box rename gak kosong, hasil output ilang
        String renameText = e.getInventory().getRenameText();
        if (renameText != null && !renameText.isEmpty()) {
            e.setResult(null);
        }
    }

    @EventHandler
    public void onDurability(PlayerItemDamageEvent e) {
        if (isSpecial(e.getItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        List<ItemStack> toSave = new ArrayList<>();
        
        // Cek semua drop, kalau ada pedang sakti, ambil dan hapus dari lantai
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

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        // Balikin item yang disimpen tadi pas player hidup lagi
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
