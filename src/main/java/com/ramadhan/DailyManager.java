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
        // Lock Start: 18 Februari 2026, 00:00:00 Waktu Abidjan
        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Abidjan"));
        startCal.set(2026, Calendar.FEBRUARY, 18, 0, 0, 0);
        this.START_TIME_MILLIS = startCal.getTimeInMillis();
    }

    public int getRelativeDay() {
        // Ambil waktu sekarang di Abidjan
        Calendar nowCal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Abidjan"));
        long now = nowCal.getTimeInMillis();
        
        long diff = now - START_TIME_MILLIS;
        if (diff < 0) return 0; // Belum mulai
        
        // Hitung selisih hari
        return (int) TimeUnit.MILLISECONDS.toDays(diff) + 1;
    }

    public boolean canClaim(Player p) {
        int currentDay = getRelativeDay();
        // End di Day 31
        if (currentDay > 30 || currentDay < 1) return false;
        
        int lastClaimed = plugin.getConfig().getInt("players." + p.getUniqueId() + ".last-day", 0);
        return currentDay > lastClaimed;
    }

    public void setClaimed(Player p) {
        plugin.getConfig().set("players." + p.getUniqueId() + ".last-day", getRelativeDay());
        plugin.saveConfig();
    }

    public ItemStack getSpecialBlade() {
        ItemStack s = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName("§6§lGolden Crescent Blade");
            m.setLore(Arrays.asList(
                "§7Senjata suci titisan rembulan.",
                "",
                "§e§lSKILL:",
                "§f- Lunar Burst (Shift + Atom Mode)",
                "§f- Spiral Recall (Heal & Resist)",
                "§f- Thunder Slash (TP Kill)"
            ));
            m.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte)1);
            s.setItemMeta(m);
        }
        return s;
    }
}
