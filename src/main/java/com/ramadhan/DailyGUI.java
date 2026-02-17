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

    public DailyGUI(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8🌙 Rewards Claim");
        int unlocked = plugin.getDailyManager().getUnlockedLevel(p.getUniqueId());
        int claimed = plugin.getDailyManager().getClaimedLevel(p.getUniqueId());

        for (int i = 1; i <= 30; i++) {
            ItemStack item;
            String status;

            if (i <= claimed) {
                // SUDAH DIKLAIM (Kaca Hijau)
                item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                status = "§a✔ Sudah Diklaim";
            } else if (i <= unlocked) {
                // SIAP DIKLAIM (Gold Block / Chest)
                item = new ItemStack(i == 30 ? Material.NETHERITE_SWORD : Material.GOLD_BLOCK);
                status = "§e§lKLIK UNTUK KLAIM!";
            } else {
                // BELUM TERBUKA (Kaca Merah)
                item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                status = "§c🔒 Terkunci (Login besok)";
            }

            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§fDay §6" + i);
            meta.setLore(Arrays.asList("", status));
            item.setItemMeta(meta);
            inv.setItem(i - 1, item);
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§8🌙 Rewards Claim")) return;
        e.setCancelled(true); // Kunci barang biar gak dicolong

        if (e.getCurrentItem() == null || !e.getCurrentItem().hasItemMeta()) return;
        Player p = (Player) e.getWhoClicked();

        String name = e.getCurrentItem().getItemMeta().getDisplayName();
        // Ambil angka dari nama "Day 5" -> 5
        int day = Integer.parseInt(name.replaceAll("[^0-9]", ""));

        int unlocked = plugin.getDailyManager().getUnlockedLevel(p.getUniqueId());
        int claimed = plugin.getDailyManager().getClaimedLevel(p.getUniqueId());

        // Logika Validasi Klaim
        if (day > claimed && day <= unlocked) {
            // Proses Klaim
            // Update data dulu biar gak bisa dupe
            plugin.getDailyManager().setClaimedLevel(p.getUniqueId(), day); 
            
            // Kasih barang
            plugin.getDailyManager().giveReward(p, day);
            
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
            p.closeInventory();
        } else if (day <= claimed) {
            p.sendMessage("§cKamu sudah mengambil hadiah hari ini!");
        } else {
            p.sendMessage("§cHari ini belum terbuka! Login lagi besok.");
        }
    }
}
