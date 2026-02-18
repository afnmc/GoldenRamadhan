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

        // Command: /goldenmoon get (Untuk ambil pedang)
        if (args.length > 0 && args[0].equalsIgnoreCase("get")) {
            p.getInventory().addItem(plugin.getDailyManager().getSpecialBlade());
            p.sendMessage("§6§lGoldenMoon §7» §aPedang suci berhasil ditambahkan!");
            return true;
        }

        // Command: /goldenmoon (Untuk buka GUI Daily)
        // Kita panggil getRelativeDay() untuk mastiin sistem tanggal jalan
        int day = plugin.getDailyManager().getRelativeDay();
        p.sendMessage("§6§lGoldenMoon §7» §fHari saat ini: §eDay " + (day > 30 ? "Event End" : day));
        
        new DailyGUI(plugin).openInventory(p);
        return true;
    }
}
