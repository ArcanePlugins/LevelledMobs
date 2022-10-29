package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import de.themoep.minedown.MineDown;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class BroadcastMessageToNearbyPlayersAction extends Action {

    private final String requiredPermission;
    private String[] message = null;
    private final double range;

    public BroadcastMessageToNearbyPlayersAction(
        final @NotNull Process process,
        final @NotNull CommentedConfigurationNode node
    ) {
        super(process, node);

        this.requiredPermission = getActionNode()
            .node("required-permission")
            .getString("");

        this.range = getActionNode()
            .node("range")
            .getDouble(16d);

        try {
            this.message = Objects.requireNonNull(getActionNode().node("message")
                .getList(String.class)).toArray(new String[0]);
        } catch(ConfigurateException | NullPointerException ex) {
            Log.sev("Unable to parse action '" + getClass().getSimpleName() + "' in " +
                "process '" + process.getIdentifier() + "': invalid message value. This is " +
                "usually the result of a user-caused syntax error in settings.yml. A stack trace " +
                "will be printed below for debugging purposes.",
                true);
            ex.printStackTrace();
        }
    }

    @Override
    public void run(Context context) {
        final Entity entity;

        if(context.getEntity() != null) {
            entity = context.getEntity();
        } else if(context.getPlayer() != null) {
            entity = context.getPlayer();
        } else {
            Log.sev(String.format(
                "A 'broadcast-message-to-world' action has encountered an issue in process '%s' " +
                    "(in function '%s'), where a context is missing an entity or player.",
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier()
            ), true);
            return;
        }

        for(var line : getMessage()) {
            final var lineComponents = MineDown.parse(line);
            for(var nearbyEntity : entity.getNearbyEntities(range, range, range)) {
                if(nearbyEntity instanceof Player player) {
                    if (!hasRequiredPermission() || player.hasPermission(
                        getRequiredPermission())) {
                        player.spigot().sendMessage(lineComponents);
                    }
                }
            }
        }
    }

    public boolean hasRequiredPermission() { return !requiredPermission.isEmpty(); }

    @NotNull
    public String getRequiredPermission() { return requiredPermission; }

    @NotNull
    public String[] getMessage() { return message; }
}
