package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import javax.annotation.Nonnull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener extends ListenerWrapper {

    public PlayerDeathListener() {
        super(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void handle(final @Nonnull PlayerDeathEvent event) {
        final Player player = event.getPlayer();

        final Context context = new Context().withPlayer(player);

        LevelledMobs.getInstance().getLogicHandler().runFunctionsWithTriggers(
            context, "on-player-death"
        );
    }
}
