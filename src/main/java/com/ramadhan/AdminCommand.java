package com.ramadhan;

import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final GoldenMoon plugin;
    public AdminCommand(GoldenMoon plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length > 0 && a[0].equalsIgnoreCase("daily")) {
            DailyGUI.open(p, plugin.getDailyManager().getPlayerProgress(p.getUniqueId()));
        } else if (a.length > 0 && a[0].equalsIgnoreCase("getsword") && p.isOp()) {
            p.getInventory().addItem(plugin.getDailyManager().getSpecialBlade());
        }
        return true;
    }
}
