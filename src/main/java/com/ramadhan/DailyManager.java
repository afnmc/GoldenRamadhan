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
        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("Africa/Abidjan"));
        startCal.set(2026, Calendar.FEBRUARY, 18, 0, 0, 0);
        this.START_TIME_MILLIS = startCal.getTimeInMillis();
    }

    public void openDailyMenu(Player player) {
        DailyGUI gui = new DailyGUI(plugin);
        gui.prepareGui(player);
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

    public void giveDailyReward(Player p) {
        int day = getRelativeDay();

        if (!canClaim(p)) {
            p.sendMessage(plugin.getMsg("already-claimed"));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (day == 30 && plugin.getConfig().getBoolean("daily-rewards.day-30.enabled", true)) {
            p.getInventory().addItem(getSpecialBlade());
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            if(plugin.getConfig().getBoolean("daily-rewards.day-30.broadcast", true)) {
                Bukkit.broadcastMessage(plugin.getConfig().getString("daily-rewards.day-30.message")
                    .replace("%player%", p.getName()));
            }
        } else if (day >= 21 && day <= 29) {
            int fragmentAmount = plugin.getConfig().getInt("daily-rewards.fragment-days." + day, 10);
            giveFragment(p, fragmentAmount);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            p.sendMessage(plugin.getMsg("claim-success").replace("%day%", String.valueOf(day)));
        } else if (day >= 1 && day <= 20) {
            giveRandomReward(p);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            p.sendMessage(plugin.getMsg("claim-success").replace("%day%", String.valueOf(day)));
        } else {
            p.sendMessage("§c✦ §fEvent belum atau sudah berakhir!");
            return;
        }
        setClaimed(p);
    }

    private void giveRandomReward(Player p) {
        Random r = new Random();
        int chance = r.nextInt(10);
        switch (chance) {
            case 0: addItem(p, Material.DIAMOND, 5, "§b5x Diamond"); break;
            case 1: addItem(p, Material.GOLD_INGOT, 12, "§612x Gold Ingot"); break;
            case 2: addItem(p, Material.ENCHANTED_GOLDEN_APPLE, 1, "§d1x Notch Apple"); break;
            case 3: addItem(p, Material.NETHERITE_SCRAP, 2, "§42x Netherite Scrap"); break;
            case 4: addItem(p, Material.EXPERIENCE_BOTTLE, 64, "§a1 Stack XP Bottle"); break;
            case 5: addItem(p, Material.IRON_BLOCK, 4, "§f4x Iron Block"); break;
            case 6: addItem(p, Material.ENDER_PEARL, 16, "§316x Ender Pearl"); break;
            case 7: addItem(p, Material.OBSIDIAN, 32, "§532x Obsidian"); break;
            case 8: addItem(p, Material.TOTEM_OF_UNDYING, 1, "§e1x Totem of Undying"); break;
            case 9:
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eco give " + p.getName() + " 10000");
                    p.sendMessage("§e§l+ §fDuit Jajan §a$10,000");
                } catch(Exception ignored) {
                    addItem(p, Material.GOLD_INGOT, 64, "§664x Gold Ingot (fallback)");
                }
                break;
        }
    }

    private void addItem(Player p, Material mat, int qty, String name) {
        p.getInventory().addItem(new ItemStack(mat, qty));
        p.sendMessage("§e§l+ §f" + name);
    }

    public void giveFragment(Player p, int amount) {
        ItemStack fragment = createFragment(amount);
        p.getInventory().addItem(fragment);
        p.sendMessage(plugin.getMsg("fragment-earned").replace("%amount%", String.valueOf(amount)));
    }

    public ItemStack createFragment(int amount) {
        Material mat = Material.valueOf(plugin.getConfig().getString("fragment-material", "AMETHYST_SHARD"));
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            meta.setDisplayName(plugin.getFragmentName());
            meta.setLore(plugin.getConfig().getStringList("fragment-lore"));
            meta.getPersistentDataContainer().set(GoldenMoon.FRAGMENT_KEY, PersistentDataType.BYTE, (byte)1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getSpecialBlade() {
        ItemStack s = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName("§f§lLunar §e§lCrescent Blade");
            m.setLore(Arrays.asList(
                "§7A sacred weapon blessed by the moon.",
                "",
                "§e§l⚔ COMBO SYSTEM:",
                "§f  • §6Hit 1 - Basic Strike §7(2.5 dmg + knockback)",
                "§f  • §bHit 2 - Moonfall Strike §7(4 dmg + launch + slow)",
                "§f  • §eHit 3 - Starfall Dance §7(dash + AoE slash)",
                "",
                "§e§l🛡 SHIELD SYNERGY:",
                "§f  • §6Perfect Parry §7(block on hit = reflect + counter)",
                "§f  • §bShield Bash §7(stun enemies while blocking)",
                "",
                "§e§l🌕 ULTIMATE - Lunar Eclipse:",
                "§f  • Charge Lunar Gauge to §e100%",
                "§f  • Hold §eRight-Click §7to charge",
                "§f  • Release for §bGolden Moon Blessing",
                "§f  • Heals allies + damages enemies",
                "",
                "§7§o✦ Unbreakable • Won't drop on death",
                "§7§o✦ Requires Lunar Armor for full potential"
            ));
            m.setUnbreakable(true);
            m.getPersistentDataContainer().set(GoldenMoon.SWORD_KEY, PersistentDataType.BYTE, (byte)1);
            s.setItemMeta(m);
        }
        return s;
    }
}    }

    private void addItem(Player p, Material mat, int qty, String name) {
        p.getInventory().addItem(new ItemStack(mat, qty));
        p.sendMessage("§e§l+ §f" + name);
    }

    public void giveFragment(Player p, int amount) {
        ItemStack fragment = createFragment(amount);
        p.getInventory().addItem(fragment);
        p.sendMessage(plugin.getMsg("fragment-earned").replace("%amount%", String.valueOf(amount)));
    }

    public ItemStack createFragment(int amount) {
        Material mat = Material.valueOf(plugin.getConfig().getString("fragment-material", "AMETHYST_SHARD"));
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if(meta != null) {
            meta.setDisplayName(plugin.getFragmentName());
            meta.setLore(plugin.getConfig().getStringList("fragment-lore"));
            meta.getPersistentDataContainer().set(GoldenMoon.FRAGMENT_KEY, PersistentDataType.BYTE, (byte)1);
            item.setItemMeta(meta);
        }
        return item;
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
