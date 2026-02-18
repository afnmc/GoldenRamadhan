package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

public class AdminCommand implements CommandExecutor {
    private final GoldenMoon plugin;

    public AdminCommand(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length > 0) {
            // JALUR DAILY: Bisa dibuka semua player (Member & Admin)
            if (args[0].equalsIgnoreCase("daily")) {
                // Pastikan method di DailyManager namanya sesuai, biasanya open atau openGUI
                plugin.getDailyManager().open(player); 
                return true;
            }

            // JALUR GETSWORD: Khusus Admin/OP
            if (args[0].equalsIgnoreCase("getsword")) {
                if (!player.hasPermission("goldenmoon.admin")) {
                    player.sendMessage("§c§l[!] §cIzin ditolak! Cuma Admin yang bisa pake command ini.");
                    return true;
                }
                player.getInventory().addItem(createLunarSword());
                player.sendMessage("§f§l[!] §ePedang Lunar ditambahkan!");
                return true;
            }
        }

        player.sendMessage("§eGunakan: §f/gm daily §eatau §f/gm getsword");
        return true;
    }

    private ItemStack createLunarSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f§lLunar §e§lCrescent Blade");
            // Menggunakan PersistentData agar fitur Lunar Putih tetap jalan meski rename
            meta.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.setUnbreakable(true);
            sword.setItemMeta(meta);
        }
        return sword;
    }
}
