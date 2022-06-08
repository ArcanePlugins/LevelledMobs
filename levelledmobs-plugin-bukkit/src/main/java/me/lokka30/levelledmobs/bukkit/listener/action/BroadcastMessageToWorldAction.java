package me.lokka30.levelledmobs.bukkit.listener.action;

import de.themoep.minedown.MineDown;
import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class BroadcastMessageToWorldAction extends Action {

    private final String requiredPermission;
    private String[] message = null;

    public BroadcastMessageToWorldAction(Process process, final CommentedConfigurationNode node) {
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
        final World world;

        if(context.getEntity() != null) {
            world = context.getEntity().getWorld();
        } else if(context.getPlayer() != null) {
            world = context.getPlayer().getWorld();
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
            for(var player : world.getPlayers()) {
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
