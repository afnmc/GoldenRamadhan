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

        // Bikin Border/Background Kaca Hitam
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fMeta = filler.getItemMeta();
        fMeta.setDisplayName(" ");
        filler.setItemMeta(fMeta);
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Map 30 Hari ke Slot Tengah agar rapi (10-43)
        int dayCounter = 1;
        for (int slot = 10; slot <= 43; slot++) {
            // Melewatkan kolom paling kiri dan kanan (slot 17, 18, 26, 27, dst)
            if (slot % 9 == 0 || slot % 9 == 8) continue;
            if (dayCounter > 30) break;

            ItemStack dayItem = new ItemStack(Material.PAPER);
            ItemMeta dMeta = dayItem.getItemMeta();
            dMeta.setDisplayName("§e§lHARI KE-" + dayCounter);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Klik untuk mengklaim hadiah harian.");
            lore.add("");
            lore.add("§a▶ Klik untuk Ambil!");
            dMeta.setLore(lore);
            dayItem.setItemMeta(dMeta);

            inv.setItem(slot, dayItem);
            dayCounter++;
        }

        p.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§0Daily Rewards Ramadhan")) return;
        e.setCancelled(true);

        if (e.getCurrentItem() == null || e.getCurrentItem().getType() != Material.PAPER) return;

        Player p = (Player) e.getWhoClicked();
        
        // Logic Hadiah Gacha Random
        giveRandomReward(p);
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    private void giveRandomReward(Player p) {
        List<ItemStack> pool = new ArrayList<>();
        pool.add(new ItemStack(Material.DIAMOND, 2));
        pool.add(new ItemStack(Material.GOLD_INGOT, 8));
        pool.add(new ItemStack(Material.EMERALD, 4));
        pool.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
        pool.add(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
        pool.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 16));
        pool.add(new ItemStack(Material.TOTEM_OF_UNDYING, 1));

        ItemStack win = pool.get(random.nextInt(pool.size()));
        p.getInventory().addItem(win);
        p.sendMessage("§6§lDaily §7» §fSelamat! Kamu mendapatkan §e" + win.getAmount() + "x " + win.getType().name());
    }
}
