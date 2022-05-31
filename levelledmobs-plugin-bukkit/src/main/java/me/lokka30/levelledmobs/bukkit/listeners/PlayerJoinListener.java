package me.lokka30.levelledmobs.bukkit.listeners;

import static me.lokka30.levelledmobs.bukkit.utils.TempConst.PREFIX_INF;
import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.DARK_GRAY;
import static org.bukkit.ChatColor.UNDERLINE;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener extends ListenerWrapper {

    public PlayerJoinListener() {
        super("org.bukkit.event.player.PlayerJoinEvent", true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handle(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        player.sendMessage(PREFIX_INF + AQUA + "This server is running LevelledMobs 4 [Alpha]");
        player.sendMessage(PREFIX_INF + DARK_GRAY + UNDERLINE + "https://github.com/lokka30/LevelledMobs/");
    }
}
