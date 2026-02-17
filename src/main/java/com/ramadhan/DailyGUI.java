package com.ramadhan;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class DailyGUI implements Listener {
    private final GoldenMoon plugin;
    public DailyGUI(GoldenMoon plugin) { this.plugin = plugin; }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8🌙 Rewards Claim");
        int unlocked = plugin.getDailyManager().getUnlockedLevel(p.getUniqueId());
        int claimed = plugin.getDailyManager().getClaimedLevel(p.getUniqueId());

        for (int i = 1; i <= 30; i++) {
            ItemStack item;
            String status;
            if (i <= claimed) {
                item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                status = "§a✔ Sudah Diklaim";
            } else if (i <= unlocked) {
                item = new ItemStack(i == 30 ? Material.NETHERITE_SWORD : Material.GOLD_BLOCK);
                status = "§e§lSIAP KLAIM!";
            } else {
                item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                status = "§c🔒 Belum Terbuka";
            }
            ItemMeta m = item.getItemMeta();
            m.setDisplayName("§6Day " + i);
            m.setLore(Arrays.asList("", status));
            item.setItemMeta(m);
            inv.setItem(i - 1, item);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§8🌙 Rewards Claim")) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        Player p = (Player) e.getWhoClicked();
        int day = e.getSlot() + 1;
        if (day < 1 || day > 30) return;

        int unlocked = plugin.getDailyManager().getUnlockedLevel(p.getUniqueId());
        int claimed = plugin.getDailyManager().getClaimedLevel(p.getUniqueId());

        if (day <= unlocked) {
            if (day > claimed) {
                plugin.getDailyManager().setClaimedLevel(p.getUniqueId(), day);
                plugin.getDailyManager().giveReward(p, day);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                p.closeInventory();
                p.sendMessage("§aBerhasil klaim Day " + day);
            } else {
                p.sendMessage("§cSudah diklaim!");
            }
        } else {
            p.sendMessage("§cDay " + day + " belum terbuka! Login besok.");
        }
    }
}
