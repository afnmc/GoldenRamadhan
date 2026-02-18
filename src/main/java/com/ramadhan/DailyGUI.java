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
        int currentDay = plugin.getDailyManager().getRelativeDay();

        // Border Kaca Hitam
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
            int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43, 46,47};
            for (int d = 1; d <= 30; d++) {
                ItemStack item;
                ItemMeta m;
                if (d < currentDay) {
                    item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                    m = item.getItemMeta();
                    if (m != null) m.setDisplayName("§aHari Ke-" + d + " §7(Selesai)");
                } else if (d == currentDay) {
                    item = new ItemStack(Material.PAPER);
                    m = item.getItemMeta();
                    if (m != null) {
                        m.setDisplayName("§e§lHARI KE-" + d);
                        m.setLore(Arrays.asList("§f> §6Klik untuk klaim hadiah!"));
                    }
                } else {
                    item = new ItemStack(Material.BARRIER);
                    m = item.getItemMeta();
                    if (m != null) m.setDisplayName("§8Hari Ke-" + d);
                }
                if (m != null) item.setItemMeta(m);
                inv.setItem(slots[d-1], item);
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof DailyGUI)) return;
        
        e.setCancelled(true);
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() != Material.PAPER) return;

        Player p = (Player) e.getWhoClicked();
        DailyManager dm = plugin.getDailyManager();
        
        if (dm.canClaim(p)) {
            dm.setClaimed(p);
            
            // HADIAH: Pedang Lunar Sakti + Bonus Emerald
            p.getInventory().addItem(dm.getSpecialBlade());
            p.getInventory().addItem(new ItemStack(Material.EMERALD, 5));
            
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            p.sendMessage("§6§lRamadhan §7» §aBerhasil klaim hadiah! Pedang Lunar didapatkan.");
            p.closeInventory();
        } else {
            p.sendMessage("§cKamu sudah klaim hari ini!");
        }
    }
}
