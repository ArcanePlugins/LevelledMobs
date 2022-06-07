package me.lokka30.levelledmobs.bukkit.listener;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener extends ListenerWrapper {

    public PlayerJoinListener() {
        super(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handle(final PlayerJoinEvent event) {
        /* Trigger */
        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context().withPlayer(event.getPlayer()),
            "on-player-join"
        );
    }
}
