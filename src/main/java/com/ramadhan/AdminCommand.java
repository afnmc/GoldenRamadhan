package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class AdminCommand implements CommandExecutor {
    private final GoldenMoon plugin;
    public AdminCommand(GoldenMoon plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) return true;
        
        if (a.length > 0 && a[0].equalsIgnoreCase("getsword") && p.isOp()) {
            ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
            ItemMeta meta = sword.getItemMeta();

            meta.setDisplayName("§6§lGolden Crescent Blade");
            List<String> lore = new ArrayList<>();
            lore.add("§7Senjata suci dari cahaya bulan.");
            lore.add("");
            lore.add("§f§lSKILL PASIF:");
            lore.add("§e- Moonlight Aura: §fSabit di punggung.");
            lore.add("§e- Soul Bound: §fTidak hilang saat mati.");
            lore.add("");
            lore.add("§f§lSKILL AKTIF:");
            lore.add("§e- Left Click: §fDiagonal Moon Slash.");
            lore.add("§e- 5x Hit + Sneak: §fCrescent Burst.");
            lore.add("§e- Sneak (No Combo): §fMoonlight Heal.");
            
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

            sword.setItemMeta(meta);
            p.getInventory().addItem(sword);
            p.sendMessage("§e§l[!] §fKamu menerima senjata legendaris.");
        }
        return true;
    }
}
