package com.ramadhan;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class MoonTask extends BukkitRunnable implements Listener {
    private final GoldenMoon plugin;
    public MoonTask(GoldenMoon plugin) { this.plugin = plugin; }
    @EventHandler
    public void onJoin(PlayerJoinEvent e) { e.getPlayer().sendMessage(plugin.getMsg("welcome-message")); }
    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            long time = p.getWorld().getTime();
            if (time > 13000 && time < 23000) {
                p.spawnParticle(Particle.DUST, p.getLocation().add(0, 3, 0), 5, new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f));
            }
        }
    }
}

