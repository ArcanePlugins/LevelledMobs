package io.github.arcaneplugins.levelledmobs.bukkit.listener;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.ConfirmSubcommand;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener extends ListenerWrapper {

    public PlayerQuitListener() {
        super(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void handle(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        ConfirmSubcommand.CONFIRMATION_MAP.remove(player);

        /* Trigger */
        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            new Context().withPlayer(player),
            "on-player-quit", "on-player-leave"
        );
    }
}
