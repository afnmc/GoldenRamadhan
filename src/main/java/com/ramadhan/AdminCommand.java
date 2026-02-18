package com.ramadhan;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final GoldenMoon plugin;

    public AdminCommand(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length > 0) {
            // Jalur Member: /gm daily
            if (args[0].equalsIgnoreCase("daily")) {
                plugin.getDailyManager().openDailyMenu(player);
                return true;
            }

            // Jalur Admin: /gm getsword
            if (args[0].equalsIgnoreCase("getsword")) {
                if (!player.hasPermission("goldenmoon.admin")) {
                    player.sendMessage("§c§l[!] §cIzin ditolak! Khusus Admin.");
                    return true;
                }
                player.getInventory().addItem(plugin.getDailyManager().getSpecialBlade());
                player.sendMessage("§f§l[!] §ePedang Lunar ditambahkan ke inventory!");
                return true;
            }
        }

        player.sendMessage("§eGunakan: §f/gm daily §eatau §f/gm getsword");
        return true;
    }
}
