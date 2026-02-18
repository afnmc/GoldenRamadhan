package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.List;

public class AdminCommand implements CommandExecutor {
    private final GoldenMoon plugin;

    public AdminCommand(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Cek apakah itu player atau console
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCommand ini cuma bisa dipake sama Player, bre!");
            return true;
        }

        // PERMISSION CHECK: Biar gak semua orang bisa spawn
        if (!player.hasPermission("goldenmoon.admin")) {
            player.sendMessage("§c§l[!] §cLo gak punya izin buat pegang pusaka ini!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("getsword")) {
            player.getInventory().addItem(createLunarSword());
            player.sendMessage("§f§l[!] §ePedang Lunar (Admin Edition) berhasil didapatkan!");
            return true;
        }

        player.sendMessage("§eGunakan: §f/goldenmoon getsword");
        return true;
    }

    private ItemStack createLunarSword() {
        // Pake Netherite Sword biar kelihatan gahar
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§f§lLunar §e§lCrescent Blade");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Pusaka admin dengan kekuatan rembulan.");
            lore.add("§fStack Hit untuk membangkitkan §eMode Lunar§f.");
            lore.add("");
            lore.add("§6[!] §eSkill Aktif: §fJongkok (Sneak)");
            meta.setLore(lore);

            // DATA PENTING: Biar pedang ini tetep sakti meski namanya di-rename
            meta.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte) 1);
            
            meta.setUnbreakable(true); // Gak perlu repair manual
            sword.setItemMeta(meta);
        }
        return sword;
    }
}
