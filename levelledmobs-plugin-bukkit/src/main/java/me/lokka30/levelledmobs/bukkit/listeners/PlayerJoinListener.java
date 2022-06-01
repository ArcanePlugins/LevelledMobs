package me.lokka30.levelledmobs.bukkit.listeners;

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
    }
}
