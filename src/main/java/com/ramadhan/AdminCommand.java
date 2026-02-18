package com.ramadhan;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final GoldenMoon plugin;
    public AdminCommand(GoldenMoon plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) return true;
        
        if (a.length > 0) {
            // FIX: Sekarang /gm daily akan membuka GUI
            if (a[0].equalsIgnoreCase("daily")) {
                new DailyGUI(plugin).openInventory(p);
                return true;
            }
            
            if (a[0].equalsIgnoreCase("getsword") && p.isOp()) {
                p.getInventory().addItem(plugin.getDailyManager().getSpecialBlade());
                p.sendMessage("§e§l[!] §fGolden Crescent Blade diberikan!");
                return true;
            }
        } else {
            p.sendMessage("§cGunakan: /gm daily atau /gm getsword");
        }
        return true;
    }
}
