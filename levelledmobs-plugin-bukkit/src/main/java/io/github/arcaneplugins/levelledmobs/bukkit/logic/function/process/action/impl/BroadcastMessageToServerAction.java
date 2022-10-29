package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import de.themoep.minedown.MineDown;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class BroadcastMessageToServerAction extends Action {

    private final String requiredPermission;
    private String[] message = null;

    public BroadcastMessageToServerAction(
        final @NotNull Process process,
        final @NotNull CommentedConfigurationNode node
    ) {
        super(process, node);

        this.requiredPermission = getActionNode()
            .node("required-permission")
            .getString("");

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
        for(var line : getMessage()) {
            final var lineComponents = MineDown.parse(line);
            for(var player : Bukkit.getOnlinePlayers()) {
                if (!hasRequiredPermission() || player.hasPermission(
                    getRequiredPermission())) {
                    player.spigot().sendMessage(lineComponents);
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
