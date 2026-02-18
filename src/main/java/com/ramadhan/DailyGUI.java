package com.ramadhan;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public class DailyGUI implements Listener {
    private final GoldenMoon plugin;
    private final Inventory inventory;

    public DailyGUI(GoldenMoon plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(null, 54, "§0Daily Login Ramadhan");
    }

    // FIX: Method ini yang dicari AdminCommand
    public void openInventory(Player p) {
        setupItems(p);
        p.openInventory(this.inventory);
    }

    private void setupItems(Player p) {
        int unlocked = plugin.getDailyManager().getUnlockedLevel(p.getUniqueId());
        int claimed = plugin.getDailyManager().getClaimedLevel(p.getUniqueId());

        for (int i = 1; i <= 30; i++) {
            ItemStack item;
            if (i <= claimed) {
                item = createItem(Material.MAP, "§7Day " + i, "§aSUDAH DIKLAIM");
            } else if (i <= unlocked) {
                item = createItem(Material.CHEST_MINECART, "§eDay " + i, "§fKlik untuk Klaim Hadiah!");
            } else {
                item = createItem(Material.BARRIER, "§cDay " + i, "§7Belum terbuka.");
            }
            inventory.setItem(i + 9, item); // Mulai dari baris kedua
        }
    }

    private ItemStack createItem(Material mat, String name, String loreStr) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(loreStr);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§0Daily Login Ramadhan")) return;
        e.setCancelled(true);
        
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot() - 9;
        
        if (slot >= 1 && slot <= 30) {
            int unlocked = plugin.getDailyManager().getUnlockedLevel(p.getUniqueId());
            int claimed = plugin.getDailyManager().getClaimedLevel(p.getUniqueId());

            if (slot == claimed + 1 && slot <= unlocked) {
                plugin.getDailyManager().giveReward(p, slot);
                plugin.getDailyManager().setClaimedLevel(p.getUniqueId(), slot);
                p.closeInventory();
                p.sendMessage("§a§l[!] §fHadiah Day " + slot + " berhasil diklaim!");
            }
        }
    }
}
