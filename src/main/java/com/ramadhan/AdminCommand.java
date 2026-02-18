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
            // 1. Jalur buat Member buka Daily
            if (args[0].equalsIgnoreCase("daily")) {
                // Member gak butuh permission admin buat buka daily
                plugin.getDailyManager().openGUI(player); 
                return true;
            }

            // 2. Jalur buat Admin ambil pedang instan
            if (args[0].equalsIgnoreCase("getsword")) {
                if (!player.hasPermission("goldenmoon.admin")) {
                    player.sendMessage("§c§l[!] §cIzin ditolak! Cuma Admin yang bisa pake command ini.");
                    return true;
                }
                // Panggil fungsi kasih pedang yang udah pake SWORD_KEY
                giveLunarSword(player); 
                player.sendMessage("§f§l[!] §ePedang Lunar ditambahkan!");
                return true;
            }
        }

        player.sendMessage("§eGunakan: §f/gm daily §eatau §f/gm getsword");
        return true;
    }

    // Fungsi ini yang bikin pedangnya sakti (Sabit Putih & TP Kill)
    private void giveLunarSword(Player p) {
        // ... (isi kodenya sama kayak yang gue kasih sebelumnya bre, pake SWORD_KEY)
    }
}
