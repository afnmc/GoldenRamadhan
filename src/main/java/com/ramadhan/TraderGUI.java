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
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class TraderGUI implements Listener, InventoryHolder {
    private final GoldenMoon plugin;
    private final Inventory inv;
    private static final int FRAGMENT_SLOT = 13;
    private static final int EXCHANGE_SLOT = 31;
    
    public TraderGUI(GoldenMoon plugin) {
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, 45, "§0§l✦ Lunar Fragment Trader");
    }
    
    @Override
    public Inventory getInventory() { 
        return inv; 
    }
    
    public void open(Player p) {
        prepareGui(p);
        p.openInventory(inv);
    }
    
    private void prepareGui(Player p) {
        inv.clear();
        ItemStack bg = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if(bgMeta != null) { 
            bgMeta.setDisplayName(" "); 
            bg.setItemMeta(bgMeta); 
        }
        for(int i = 0; i < 45; i++) {
            if(i != FRAGMENT_SLOT && i != EXCHANGE_SLOT) {
                inv.setItem(i, bg);
            }        }
        
        int fragmentCount = countFragments(p);
        ItemStack fragmentDisplay = plugin.getDailyManager().createFragment(fragmentCount);
        ItemMeta fdMeta = fragmentDisplay.getItemMeta();
        if(fdMeta != null) {
            fdMeta.setDisplayName("§b§lLunar Fragment §7✦");
            fdMeta.setLore(Arrays.asList(
                "", 
                "§fJumlah: §e" + fragmentCount, 
                "",
                "§7Tukar §e" + plugin.getConfig().getInt("trader.fragment-cost", 10) + " §7fragment",
                "§7untuk §e1 Excellent Key",
                "",
                fragmentCount >= plugin.getConfig().getInt("trader.fragment-cost", 10) ? 
                    "§a§l[✓] Cukup!" : "§c§l[✗] Kurang!"
            ));
            fragmentDisplay.setItemMeta(fdMeta);
        }
        inv.setItem(FRAGMENT_SLOT, fragmentDisplay);
        
        ItemStack exchangeBtn = new ItemStack(Material.EMERALD);
        ItemMeta ebMeta = exchangeBtn.getItemMeta();
        if(ebMeta != null) {
            ebMeta.setDisplayName("§a§l✦ TUKAR SEKARANG");
            int cost = plugin.getConfig().getInt("trader.fragment-cost", 10);
            ebMeta.setLore(Arrays.asList(
                "", 
                "§fKlik untuk menukar:", 
                "§e- §b" + cost + " Lunar Fragment", 
                "§e+ §f1 Excellent Key", 
                "",
                fragmentCount >= cost ? "§a§l[✓] Cukup!" : "§c§l[✗] Fragment kurang!", 
                ""
            ));
            exchangeBtn.setItemMeta(ebMeta);
        }
        inv.setItem(EXCHANGE_SLOT, exchangeBtn);
    }
    
    private int countFragments(Player p) {
        int count = 0;
        for(ItemStack item : p.getInventory().getContents()) {
            if(isFragment(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }
        private boolean isFragment(ItemStack item) {
        return item != null && item.hasItemMeta() &&
               item.getItemMeta().getPersistentDataContainer()
                   .has(GoldenMoon.FRAGMENT_KEY, PersistentDataType.BYTE);
    }
    
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if(!(e.getInventory().getHolder() instanceof TraderGUI)) return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p)) return;
        if(e.getClickedInventory() == null) return;
        
        int slot = e.getSlot();
        if(slot == EXCHANGE_SLOT) {
            executeExchange(p);
            return;
        }
        if(slot == FRAGMENT_SLOT) {
            prepareGui(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        }
    }
    
    private void executeExchange(Player p) {
        int cost = plugin.getConfig().getInt("trader.fragment-cost", 10);
        int owned = countFragments(p);
        
        if(owned < cost) {
            p.sendMessage("§c✦ §fFragment tidak cukup! Butuh §e" + cost);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            prepareGui(p);
            return;
        }
        
        removeFragments(p, cost);
        
        // Execute Excellence Crate command
        String keyCommand = plugin.getConfig().getString("trader.key-output.command", "excellence givekey %player% excellent 1");
        String finalCommand = keyCommand.replace("%player%", p.getName());
        
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
            p.sendMessage("§e✦ §fTukar berhasil! §e1 Excellent Key §fditambahkan.");
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        } catch(Exception ex) {
            p.sendMessage("§c✦ §fError: Command excellence tidak tersedia!");
            p.sendMessage("§7Pastikan plugin Excellence Crate terinstall.");
        }
                prepareGui(p);
        Bukkit.getScheduler().runTaskLater(plugin, () -> p.closeInventory(), 10);
    }
    
    private void removeFragments(Player p, int amountToRemove) {
        int remaining = amountToRemove;
        for(int i = 0; i < p.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = p.getInventory().getItem(i);
            if(isFragment(item)) {
                int stackSize = item.getAmount();
                if(stackSize <= remaining) {
                    p.getInventory().setItem(i, null);
                    remaining -= stackSize;
                } else {
                    item.setAmount(stackSize - remaining);
                    p.getInventory().setItem(i, item);
                    remaining = 0;
                }
            }
        }
    }
}
