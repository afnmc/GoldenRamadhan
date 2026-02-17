package com.ramadhan;

import org.bukkit.Sound;
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
            // Command: /gm daily
            if (args[0].equalsIgnoreCase("daily")) {
                new DailyGUI(plugin).open(player);
                return true;
            }

            // Command: /gm getsword (Khusus Admin)
            // PASTIKAN kamu ketik: /gm getsword (bukan getsworld)
            if (args[0].equalsIgnoreCase("getsword")) {
                if (!player.isOp()) {
                    player.sendMessage("§cYou don't have permission.");
                    return true;
                }
                player.getInventory().addItem(plugin.getDailyManager().getSpecialBlade());
                player.sendMessage("§a[Admin] §fGolden Crescent Blade given!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
                return true;
            }
            
            // Command Reset (Buat test ulang): /gm reset
            if (args[0].equalsIgnoreCase("reset") && player.isOp()) {
                plugin.getDailyManager().setClaimedLevel(player.getUniqueId(), 0);
                // Jangan reset unlocked biar gak repot nunggu besok, cuma reset status klaim
                player.sendMessage("§aData klaim direset! Sekarang bisa klaim ulang via GUI.");
                return true;
            }
        }
        return true;
    }
}
