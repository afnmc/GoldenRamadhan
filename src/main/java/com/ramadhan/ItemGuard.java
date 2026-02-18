package com.ramadhan;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public class ItemGuard implements Listener {
    private final Map<UUID, List<ItemStack>> savedItems = new HashMap<>();

    // DURABILITY: Tetap abadi
    @EventHandler
    public void onDurability(PlayerItemDamageEvent e) {
        if (isSpecial(e.getItem())) e.setCancelled(true);
    }

    // KEEP ON DEATH: Gak bakal drop pas mati
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        List<ItemStack> toSave = new ArrayList<>();
        Iterator<ItemStack> it = e.getDrops().iterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (isSpecial(item)) {
                toSave.add(item.clone());
                it.remove();
            }
        }
        if (!toSave.isEmpty()) savedItems.put(p.getUniqueId(), toSave);
    }

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
