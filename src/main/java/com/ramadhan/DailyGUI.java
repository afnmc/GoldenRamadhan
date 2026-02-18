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
    private final Random random = new Random();

    public DailyGUI(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    public void openInventory(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§0Daily Rewards Ramadhan");
        int today = plugin.getDailyManager().getAbidjanDate();

        // Border Kaca Hitam
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta(); gMeta.setDisplayName(" "); glass.setItemMeta(gMeta);
        for (int i = 0; i < 54; i++) inv.setItem(i, glass);

        // Mapping 30 Hari (Slot 10-43, skip pinggiran)
        int day = 1;
        for (int slot = 10; slot <= 43; slot++) {
            if (slot % 9 == 0 || slot % 9 == 8) continue;
            if (day > 30) break;

            ItemStack item;
            ItemMeta meta;

            if (today > 30) {
                item = new ItemStack(Material.BARRIER);
                meta = item.getItemMeta();
                meta.setDisplayName("§c§lEVENT SELESAI");
                meta.setLore(Collections.singletonList("§7Sampai jumpa tahun depan!"));
            } else {
                item = new ItemStack(Material.PAPER);
                meta = item.getItemMeta();
                meta.setDisplayName("§e§lHARI KE-" + day);
                List<String> lore = new ArrayList<>();
                if (day < today) lore.add("§c§oSudah Lewat");
                else if (day == today) {
                    lore.add("§a§lBISA DIKLAIM");
                    lore.add("§7Reset: 06:00 WIB");
                } else lore.add("§8Belum Terbuka");
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
            inv.setItem(slot, item);
            day++;
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§0Daily Rewards Ramadhan")) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() != Material.PAPER) return;

        DailyManager dm = plugin.getDailyManager();
        if (dm.canClaim(p)) {
            dm.setClaimed(p);
            // Gacha Hadiah
            Material[] rewards = {Material.DIAMOND, Material.GOLD_INGOT, Material.NETHERITE_SCRAP, Material.EMERALD};
            p.getInventory().addItem(new ItemStack(rewards[random.nextInt(rewards.length)], 2));
            
            p.sendMessage("§6§lDaily §7» §aBerhasil klaim hadiah!");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            p.closeInventory();
        } else {
            p.sendMessage("§c[!] Kamu sudah klaim hari ini atau event berakhir.");
        }
    }
}
