package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class QuestManager implements Listener {
    private final GoldenMoon plugin;
    public QuestManager(GoldenMoon plugin) { this.plugin = plugin; }
    @EventHandler
    public void onGiveTakjil(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player giver = event.getPlayer();
        Player receiver = (Player) event.getRightClicked();
        ItemStack item = giver.getInventory().getItemInMainHand();
        if (item.getType() == Material.BREAD) {
            item.setAmount(item.getAmount() - 1);
            giver.sendMessage(plugin.getMsg("quest-success"));
            giver.getInventory().addItem(new ItemStack(Material.GOLD_NUGGET, 5));
            receiver.sendMessage(plugin.getMsg("receive-takjil").replace("%player%", giver.getName()));
        }
    }
}

