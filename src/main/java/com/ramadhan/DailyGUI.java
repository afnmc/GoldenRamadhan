package com.ramadhan;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class DailyGUI implements Listener, InventoryHolder {
    private final GoldenMoon plugin;
    private final Inventory inv;

    public DailyGUI(GoldenMoon plugin) { 
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, 54, "§0Daily Rewards Ramadhan");
    }

    @Override
    public Inventory getInventory() { return inv; }

    public void prepareGui(Player p) {
        DailyManager dm = plugin.getDailyManager();
        int currentDay = dm.getRelativeDay();

        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = bg.getItemMeta(); 
        if (bm != null) { bm.setDisplayName(" "); bg.setItemMeta(bm); }
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        if (currentDay > 30) {
            ItemStack end = new ItemStack(Material.BARRIER);
            ItemMeta em = end.getItemMeta();
            if (em != null) em.setDisplayName("§c§lEVENT TELAH BERAKHIR");
            end.setItemMeta(em);
            inv.setItem(22, end);
        } else {
            int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43, 46,47};

            for (int d = 1; d <= 30; d++) {
                ItemStack item; ItemMeta m;
                if (d < currentDay) {
                    item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    m = item.getItemMeta();
                    if (m != null) m.setDisplayName("§aHari Ke-" + d + " §7(Selesai)");
                } else if (d == currentDay) {
                    item = new ItemStack(Material.PAPER);
                    m = item.getItemMeta();
                    if (m != null) {
                        m.setDisplayName("§e§lHARI KE-" + d + " §f(Klaim Sekarang)");
                        m.setLore(Arrays.asList("", "§7Klik untuk mengambil hadiah hari ini!", ""));
                    }
                } else if (d == 30) {
                    item = new ItemStack(Material.NETHERITE_SWORD);
                    m = item.getItemMeta();
                    if (m != null) {
                        m.setDisplayName("§f§lHARI KE-30: §e§lGRAND PRIZE");
                        m.setLore(Arrays.asList("§7Hadiah Utama: §eLunar Crescent Blade", "§8Terkunci sampai hari ke-30."));
                    }
                } else {
                    item = new ItemStack(Material.BARRIER);
                    m = item.getItemMeta();
                    if (m != null) m.setDisplayName("§8Hari Ke-" + d + " (Terkunci)");
                }
                if (m != null) item.setItemMeta(m);
                if(d-1 < slots.length) inv.setItem(slots[d-1], item);
            }
        }
        
        // Trader Button
        ItemStack traderBtn = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta tbMeta = traderBtn.getItemMeta();
        if(tbMeta != null) {
            tbMeta.setDisplayName("§a§l✦ Fragment Trader §a§l✦");
            tbMeta.setLore(Arrays.asList("", "§fTukar Lunar Fragment", "§fmenjadi Excellent Key!", "", "§eKlik untuk membuka trader"));
            traderBtn.setItemMeta(tbMeta);
        }
        inv.setItem(44, traderBtn);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DailyGUI)) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        Player p = (Player) e.getWhoClicked();
        
        // Claim daily
        if (e.getCurrentItem().getType() == Material.PAPER) {
            DailyManager dm = plugin.getDailyManager();
            dm.giveDailyReward(p);
            p.closeInventory();
            return;
        }
        
        // Open trader
        if(e.getSlot() == 44 && e.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
            p.closeInventory();
            new TraderGUI(plugin).open(p);
        }
    }
}
