package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class BroadcastMessageToWorldAction extends Action {

    private final String requiredPermission;
    private String[] message = null;

    public BroadcastMessageToWorldAction(
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
        final World world;

        if(context.getLocation() != null) {
            world = context.getLocation().getWorld();
        } else if(context.getEntity() != null) {
            world = context.getEntity().getWorld();
        } else if(context.getPlayer() != null) {
            world = context.getPlayer().getWorld();
        } else {
            throw new RuntimeException(String.format(
                "A 'broadcast-message-to-world' action has encountered an issue in process '%s' " +
                    "(in function '%s'), where a context is missing an entity or player.",
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier()
            ));
        }

        final String[] lines = new String[getMessage().length];
        for(int i = 0; i < lines.length; i++) {
            lines[i] = LogicHandler.replacePapiAndContextPlaceholders(getMessage()[i], context);
        }
        final Component msg = Message.formatMd(lines);

        for(final Player player : world.getPlayers()) {
            if(hasRequiredPermission() && !player.hasPermission(getRequiredPermission())) continue;
            player.sendMessage(msg);
        }
    }

    public boolean hasRequiredPermission() { return !requiredPermission.isEmpty(); }

    @NotNull
    public String getRequiredPermission() { return requiredPermission; }

    @NotNull
    public String[] getMessage() { return message; }
}
