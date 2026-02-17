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
            if (a[0].equalsIgnoreCase("daily")) {
                new DailyGUI(plugin).open(p);
            } else if (a[0].equalsIgnoreCase("getsword") && p.isOp()) {
                p.getInventory().addItem(plugin.getDailyManager().getSpecialBlade());
                p.sendMessage("§aPedang diberikan!");
            } else if (a[0].equalsIgnoreCase("reset") && p.isOp()) {
                plugin.getDailyManager().setClaimedLevel(p.getUniqueId(), 0);
                p.sendMessage("§aData klaim direset!");
            }
        }
        return true;
    }
}
