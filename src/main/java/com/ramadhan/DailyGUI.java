package com.ramadhan;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class DailyGUI implements Listener, InventoryHolder {
    private final GoldenMoon plugin;
    private final Inventory inv;

    public DailyGUI(GoldenMoon plugin) { 
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, 54, "§0Daily Rewards Ramadhan");
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void prepareGui() {
        DailyManager dm = plugin.getDailyManager();
        int currentDay = dm.getRelativeDay();

        // Border Kaca Hitam biar rapi
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = bg.getItemMeta(); 
        if (bm != null) {
            bm.setDisplayName(" "); 
            bg.setItemMeta(bm);
        }
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        if (currentDay > 30) {
            ItemStack end = new ItemStack(Material.BARRIER);
            ItemMeta em = end.getItemMeta();
            if (em != null) {
                em.setDisplayName("§c§lEVENT TELAH BERAKHIR");
                end.setItemMeta(em);
            }
            inv.setItem(22, end);
        } else {
            // Slot layout untuk 30 hari
            int[] slots = {
                10,11,12,13,14,15,16, 
                19,20,21,22,23,24,25, 
                28,29,30,31,32,33,34, 
                37,38,39,40,41,42,43, 
                46,47
            };

            for (int d = 1; d <= 30; d++) {
                ItemStack item;
                ItemMeta m;

                if (d < currentDay) {
                    // HARI YANG SUDAH LEWAT
                    item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    m = item.getItemMeta();
                    if (m != null) m.setDisplayName("§aHari Ke-" + d + " §7(Selesai)");
                } else if (d == currentDay) {
                    // HARI INI (TOMBOL KLAIM)
                    item = new ItemStack(Material.PAPER);
                    m = item.getItemMeta();
                    if (m != null) {
                        m.setDisplayName("§e§lHARI KE-" + d + " §f(Klaim Sekarang)");
                        m.setLore(Arrays.asList("", "§7Klik untuk mengambil hadiah hari ini!", ""));
                    }
                } else if (d == 30) {
                    // PREVIEW HARI KE-30 (PEDANG LUNAR)
                    item = new ItemStack(Material.NETHERITE_SWORD);
                    m = item.getItemMeta();
                    if (m != null) {
                        m.setDisplayName("§f§lHARI KE-30: §e§lGRAND PRIZE");
                        m.setLore(Arrays.asList("§7Hadiah Utama: §eLunar Crescent Blade", "§8Terkunci sampai hari ke-30."));
                    }
                } else {
                    // HARI MENDATANG (TERKUNCI)
                    item = new ItemStack(Material.BARRIER);
                    m = item.getItemMeta();
                    if (m != null) m.setDisplayName("§8Hari Ke-" + d + " (Terkunci)");
                }

                if (m != null) item.setItemMeta(m);
                inv.setItem(slots[d-1], item);
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // 1. Cek apakah ini inventory kita
        if (!(e.getInventory().getHolder() instanceof DailyGUI)) return;
        
        e.setCancelled(true); // Gak boleh ambil item dari GUI

        // 2. Cek apakah yang diklik itu Paper (Tombol klaim hari ini)
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() != Material.PAPER) return;

        Player p = (Player) e.getWhoClicked();
        DailyManager dm = plugin.getDailyManager();
        
        // 3. PANGGIL MANAGER UNTUK PROSES HADIAH
        // Di sini kuncinya: Manager bakal cek hari, kalau hari 2 ya dapet random.
        // Gak bakal dapet pedang kecuali variable getRelativeDay() == 30.
        dm.giveDailyReward(p);
        
        p.closeInventory();
    }
}
