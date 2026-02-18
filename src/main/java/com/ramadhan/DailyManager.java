package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DailyManager {
    private final GoldenMoon plugin;
    private final long START_TIME_MILLIS; 

    public DailyManager(GoldenMoon plugin) {
        this.plugin = plugin;
        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Abidjan"));
        startCal.set(2026, Calendar.FEBRUARY, 18, 0, 0, 0);
        this.START_TIME_MILLIS = startCal.getTimeInMillis();
    }

    // Membuka GUI Daily (Fix Compilation Error)
    public void openDailyMenu(Player player) {
        player.openInventory(new DailyGUI(plugin).getInventory());
    }

    public int getRelativeDay() {
        Calendar nowCal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Abidjan"));
        long now = nowCal.getTimeInMillis();
        long diff = now - START_TIME_MILLIS;
        if (diff < 0) return 0; 
        return (int) TimeUnit.MILLISECONDS.toDays(diff) + 1;
    }

    public boolean canClaim(Player p) {
        int currentDay = getRelativeDay();
        if (currentDay > 30 || currentDay < 1) return false;
        int lastClaimed = plugin.getConfig().getInt("players." + p.getUniqueId() + ".last-day", 0);
        return currentDay > lastClaimed;
    }

    public void setClaimed(Player p) {
        plugin.getConfig().set("players." + p.getUniqueId() + ".last-day", getRelativeDay());
        plugin.saveConfig();
    }

    // Item sakti dengan NBT Key (PersistentData)
    public ItemStack getSpecialBlade() {
        ItemStack s = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName("§f§lLunar §e§lCrescent Blade");
            m.setLore(Arrays.asList(
                "§7Senjata suci titisan rembulan.",
                "",
                "§e§lSPECIAL ABILITY:",
                "§f- §6Dash Strike: §7Maju saat menyerang",
                "§f- §bLunar Pierce: §7Sneak (Stack 5) meledak putih",
                "§f- §aRejuvenate: §7Sneak (CD 10s) untuk Heal",
                "",
                "§8§oItem tidak akan drop & bebas di-rename"
            ));
            m.setUnbreakable(true);
            // Kunci Rahasia Skill
            m.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte)1);
            s.setItemMeta(m);
        }
        return s;
    }
}
