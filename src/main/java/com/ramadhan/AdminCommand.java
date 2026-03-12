package com.ramadhan;

import org.bukkit.Bukkit;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c✦ §fCommand ini hanya untuk player!");
            return true;
        }

        if (!player.hasPermission("goldenmoon.admin")) {
            player.sendMessage(plugin.getMsg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e=== §6GoldenMoon Admin §e===");
            player.sendMessage("§f/gm daily §7- Buka menu daily");
            player.sendMessage("§f/gm trader §7- Buka fragment trader");
            player.sendMessage("§f/gm getsword §7- Dapatkan Lunar Crescent Blade");
            player.sendMessage("§f/gm getkit <crescent|elite> §7- Dapatkan full armor kit");
            player.sendMessage("§f/gm givefragment <player> <amount> §7- Beri fragment");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "daily" -> plugin.getDailyManager().openDailyMenu(player);
            
            case "trader" -> {
                new TraderGUI(plugin).open(player);
                player.sendMessage("§a✦ §fTrader GUI dibuka!");
            }
            
            case "getsword" -> {
                player.getInventory().addItem(plugin.getDailyManager().getSpecialBlade());
                player.sendMessage("§a✦ §fLunar Crescent Blade ditambahkan!");
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
            
            case "getkit" -> {
                if (args.length < 2) { 
                    player.sendMessage("§eGunakan: §f/gm getkit <crescent|elite>"); 
                    player.sendMessage("§7- §bcrescent§7: Crescent Guardian Set (4 armor + shield)");
                    player.sendMessage("§7- §6elite§7: Golden Moon Elite Set (4 armor + shield)");
                    return true; 
                }
                String kitType = args[1].toLowerCase();
                if (!kitType.equals("crescent") && !kitType.equals("elite")) {
                    player.sendMessage("§c✦ §fTipe kit tidak valid! Gunakan: §bcrescent §fatau §6elite");
                    return true;
                }
                if (plugin.getArmorManager().giveKit(player, kitType)) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                }
            }
            
            case "givefragment" -> {
                if (args.length < 3) { 
                    player.sendMessage("§eGunakan: §f/gm givefragment <player> <amount>"); 
                    return true; 
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { 
                    player.sendMessage("§c✦ §fPlayer tidak online!"); 
                    return true; 
                }
                int amount;
                try { 
                    amount = Integer.parseInt(args[2]); 
                } catch (NumberFormatException e) { 
                    player.sendMessage("§c✦ §fAmount harus angka!"); 
                    return true; 
                }
                plugin.getDailyManager().giveFragment(target, amount);
                player.sendMessage("§a✦ §fBerikan §b" + amount + " Lunar Fragment §fke §e" + target.getName());
            }
            
            default -> player.sendMessage("§c✦ §fCommand tidak dikenal! Gunakan /gm untuk help.");
        }
        return true;
    }
}
