package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
            this.message = Objects.requireNonNull(
                getActionNode().node("message").getList(String.class),
                "message"
            ).toArray(new String[0]);
        } catch(ConfigurateException | NullPointerException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void run(Context context) {
        final String[] lines = new String[getMessage().length];
        for(int i = 0; i < lines.length; i++) {
            lines[i] = LogicHandler.replacePapiAndContextPlaceholders(getMessage()[i], context);
        }
        final Component msg = Message.formatMd(lines);

        for(final Player player : Bukkit.getOnlinePlayers()) {
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
