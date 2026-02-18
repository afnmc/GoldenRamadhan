package com.ramadhan;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public class DailyManager {
    private final GoldenMoon plugin;

    public DailyManager(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    public int getAbidjanDate() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Abidjan"));
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public boolean canClaim(Player p) {
        int today = getAbidjanDate();
        if (today > 30) return false;
        int last = plugin.getConfig().getInt("players." + p.getUniqueId() + ".last-day", 0);
        return today > last;
    }

    public void setClaimed(Player p) {
        plugin.getConfig().set("players." + p.getUniqueId() + ".last-day", getAbidjanDate());
        plugin.saveConfig();
    }

    // Fix Error getSpecialBlade
    public ItemStack getSpecialBlade() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lGolden Crescent Blade");
            meta.setLore(Arrays.asList(
                "§7Pedang suci Ramadhan.",
                "",
                "§e§lSKILL:",
                "§f- Lunar Burst (Shift + Full Stack)",
                "§f- Moonlight Recall (Shift Heal)",
                "§f- Shadow Dash (On Kill Teleport)"
            ));
            meta.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte) 1);
            sword.setItemMeta(meta);
        }
        return sword;
    }
}
