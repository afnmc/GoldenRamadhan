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
        if (!(sender instanceof Player p)) return true;
        if (!p.hasPermission("goldenmoon.admin")) return true;

        // Command Khusus: /gm getswordnya
        if (args.length > 0 && args[0].equalsIgnoreCase("getswordnya")) {
            p.getInventory().addItem(plugin.getDailyManager().getSpecialBlade());
            p.sendMessage("§6§lGoldenMoon §7» §aPedang Suci berhasil diberikan!");
            return true;
        }

        // Buka GUI Daily jika hanya ketik /gm
        new DailyGUI(plugin).openInventory(p);
        return true;
    }
}
