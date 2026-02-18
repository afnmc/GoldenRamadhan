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
import java.util.*;

public class DailyGUI implements Listener {
    private final GoldenMoon plugin;

    public DailyGUI(GoldenMoon plugin) { this.plugin = plugin; }

    public void openInventory(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Daily Rewards Ramadhan");
        int currentDay = plugin.getDailyManager().getRelativeDay();

        // Border Kaca
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = bg.getItemMeta(); bm.setDisplayName(" "); bg.setItemMeta(bm);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        // Jika sudah Day 31 ke atas
        if (currentDay > 30) {
            ItemStack end = new ItemStack(Material.BARRIER);
            ItemMeta em = end.getItemMeta();
            em.setDisplayName("§c§lEVENT TELAH BERAKHIR");
            em.setLore(Arrays.asList("§7Sampai jumpa di Ramadhan tahun depan!"));
            end.setItemMeta(em);
            inv.setItem(22, end);
        } else {
            // Slot mapping 30 hari
            int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43, 46,47};
            for (int d = 1; d <= 30; d++) {
                ItemStack item;
                ItemMeta m;
                
                if (d < currentDay) {
                    item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    m = item.getItemMeta();
                    m.setDisplayName("§aHari Ke-" + d + " §7(Selesai)");
                } else if (d == currentDay) {
                    item = new ItemStack(Material.PAPER);
                    m = item.getItemMeta();
                    m.setDisplayName("§e§lHARI KE-" + d);
                    m.setLore(Arrays.asList("§f> §6Klik untuk klaim hadiah!", "§7Status: §aTerbuka"));
                } else {
                    item = new ItemStack(Material.BARRIER);
                    m = item.getItemMeta();
                    m.setDisplayName("§8Hari Ke-" + d);
                    m.setLore(Arrays.asList("§7Status: §cTerkunci"));
                }
                
                item.setItemMeta(m);
                inv.setItem(slots[d-1], item);
            }
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§0Daily Rewards Ramadhan")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() != Material.PAPER) return;

        Player p = (Player) e.getWhoClicked();
        DailyManager dm = plugin.getDailyManager();
        
        if (dm.canClaim(p)) {
            dm.setClaimed(p);
            p.getInventory().addItem(new ItemStack(Material.EMERALD, 10));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            p.sendMessage("§6§lRamadhan §7» §aHadiah harian berhasil diklaim!");
            p.closeInventory();
        } else {
            p.sendMessage("§cKamu sudah klaim hari ini!");
        }
    }
}
