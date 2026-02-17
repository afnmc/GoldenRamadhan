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
        if (a.length > 0) {
            if (a[0].equalsIgnoreCase("daily")) {
                new DailyGUI(plugin).open(p);
            } 
            else if (a[0].equalsIgnoreCase("getsword") && p.isOp()) {
                // --- PEMBUATAN PEDANG KHUSUS ---
                ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
                ItemMeta meta = sword.getItemMeta();

                // 1. Pasang Nama & Lore
                meta.setDisplayName("§6§lGolden Crescent Blade");
                List<String> lore = new ArrayList<>();
                lore.add("§7Pedang Legendaris Ramadhan.");
                lore.add("§eSkill: §fSabit Aura & Ledakan");
                lore.add("§8(Tidak akan hilang saat mati)");
                meta.setLore(lore);

                // 2. Pasang SEGEL (Biar gak bisa dipalsuin)
                meta.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte) 1);

                // 3. Bikin Tidak Bisa Hancur
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE); // Sembunyikan tulisan Unbreakable biar rapi
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                sword.setItemMeta(meta);
                // -------------------------------

                p.getInventory().addItem(sword);
                p.sendMessage("§aPedang Legendaris diberikan!");
            } 
            else if (a[0].equalsIgnoreCase("reset") && p.isOp()) {
                plugin.getDailyManager().setClaimedLevel(p.getUniqueId(), 0);
                p.sendMessage("§aData klaim direset!");
            }
        }
        return true;
    }
}
