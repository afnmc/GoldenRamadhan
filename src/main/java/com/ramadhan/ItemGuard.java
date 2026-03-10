package com.ramadhan;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemGuard implements Listener {
    private final GoldenMoon plugin;
    private final Map<UUID, List<ItemStack>> savedItems = new HashMap<>();

    public ItemGuard(GoldenMoon plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDurability(PlayerItemDamageEvent e) {
        if (isSpecial(e.getItem())) e.setCancelled(true);
    }

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
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.SHIELD_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.FRAGMENT_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.ARMOR_HELMET_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.ARMOR_CHEST_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.ARMOR_LEGS_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.ARMOR_BOOTS_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.ELITE_HELMET_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.ELITE_CHEST_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.ELITE_LEGS_KEY, PersistentDataType.BYTE) ||
               meta.getPersistentDataContainer().has(GoldenMoon.ELITE_BOOTS_KEY, PersistentDataType.BYTE);
    }
}
