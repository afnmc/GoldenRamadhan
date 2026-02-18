package com.ramadhan;

import org.bukkit.entity.Player;
import java.util.*;

public class DailyManager {
    private final GoldenMoon plugin;

    public DailyManager(GoldenMoon plugin) {
        this.plugin = plugin;
    }

    public int getAbidjanDate() {
        // Force jam Abidjan (UTC+0)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Abidjan"));
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public boolean canClaim(Player p) {
        int today = getAbidjanDate();
        
        // Lock total jika sudah lewat tanggal 30 di Abidjan
        if (today > 30) return false;

        // Cek hari terakhir klaim di config.yml
        int lastClaim = plugin.getConfig().getInt("players." + p.getUniqueId() + ".last-day", 0);
        return today > lastClaim;
    }

    public void setClaimed(Player p) {
        plugin.getConfig().set("players." + p.getUniqueId() + ".last-day", getAbidjanDate());
        plugin.saveConfig();
    }
}
