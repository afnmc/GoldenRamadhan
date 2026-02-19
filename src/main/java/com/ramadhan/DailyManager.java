package com.ramadhan;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
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
        // Lock Start: 18 Februari 2026, 00:00:00 (Waktu Abidjan)
        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Abidjan"));
        startCal.set(2026, Calendar.FEBRUARY, 18, 0, 0, 0);
        this.START_TIME_MILLIS = startCal.getTimeInMillis();
    }

    public void openDailyMenu(Player player) {
        DailyGUI gui = new DailyGUI(plugin);
        gui.prepareGui();
        player.openInventory(gui.getInventory());
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

    /**
     * Eksekusi pemberian hadiah
     */
    public void giveDailyReward(Player p) {
        int day = getRelativeDay();

        if (!canClaim(p)) {
            p.sendMessage("§c§l[!] §cKamu sudah mengambil hadiah hari ini!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (day == 30) {
            // --- HADIAH UTAMA ---
            p.getInventory().addItem(getSpecialBlade());
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            Bukkit.broadcastMessage("§6§l[DAILY] §e" + p.getName() + " §ftelah mengklaim §b§lLunar Crescent Blade §fdi hari ke-30!");
        } else {
            // --- HADIAH RANDOM ---
            giveRandomReward(p);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            p.sendMessage("§a§l[!] §fHadiah harian hari ke-§e" + day + " §fberhasil diklaim!");
        }

        setClaimed(p);
    }

    /**
     * Daftar hadiah random (Bisa lo tambah/ubah sesuka hati)
     */
    private void giveRandomReward(Player p) {
        Random r = new Random();
        int chance = r.nextInt(10); // 0-9 (10 Pilihan)

        switch (chance) {
            case 0:
                addItem(p, Material.DIAMOND, 5, "§b5x Diamond");
                break;
            case 1:
                addItem(p, Material.GOLD_INGOT, 12, "§612x Gold Ingot");
                break;
            case 2:
                addItem(p, Material.ENCHANTED_GOLDEN_APPLE, 1, "§d1x Notch Apple");
                break;
            case 3:
                addItem(p, Material.NETHERITE_SCRAP, 2, "§42x Netherite Scrap");
                break;
            case 4:
                addItem(p, Material.EXPERIENCE_BOTTLE, 64, "§a1 Stack XP Bottle");
                break;
            case 5:
                addItem(p, Material.IRON_BLOCK, 4, "§f4x Iron Block");
                break;
            case 6:
                addItem(p, Material.ENDER_PEARL, 16, "§316x Ender Pearl");
                break;
            case 7:
                addItem(p, Material.OBSIDIAN, 32, "§532x Obsidian");
                break;
            case 8:
                addItem(p, Material.TOTEM_OF_UNDYING, 1, "§e1x Totem of Undying");
                break;
            case 9:
                // Contoh hadiah Duit via Command (Eco)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + p.getName() + " 10000");
                p.sendMessage("§e§l+ §fDuit Jajan §a$10,000");
                break;
        }
    }

    private void addItem(Player p, Material mat, int qty, String name) {
        p.getInventory().addItem(new ItemStack(mat, qty));
        p.sendMessage("§e§l+ §f" + name);
    }

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
            m.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte)1);
            s.setItemMeta(m);
        }
        return s;
    }
}
