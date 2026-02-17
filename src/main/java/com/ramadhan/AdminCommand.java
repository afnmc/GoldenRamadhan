package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;

public class AdminCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        // Cek Permission (Opsional: agar hanya OP yang bisa)
        if (!player.isOp()) {
            player.sendMessage("§cYou don't have permission to do this!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("getsword")) {
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            ItemMeta m = sword.getItemMeta();
            if (m != null) {
                m.setDisplayName("§6§lGolden Crescent Blade");
                m.setLore(Arrays.asList(
                    "§7Limited Edition - 2026", 
                    "§eAbility: §6Lunar Sweep", 
                    "§7Admin Test Item"
                ));
                m.setUnbreakable(true);
                sword.setItemMeta(m);
            }
            player.getInventory().addItem(sword);
            player.sendMessage("§a[Admin] §fYou have received the §6Golden Crescent Blade §ffor testing!");
        }

        return true;
    }
}

