package me.lokka30.levelledmobs.bukkit.listener.action;

import de.themoep.minedown.MineDown;
import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class BroadcastMessageToServerAction extends Action {

    private final String requiredPermission;
    private String[] message = null;

    public BroadcastMessageToServerAction(Process process, final CommentedConfigurationNode node) {
        super(process, node);

        this.requiredPermission = getNode()
            .node("required-permission")
            .getString("");

        try {
            this.message = Objects.requireNonNull(getNode().node("message")
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
        new BukkitRunnable() {
            @Override
            public void run() {
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
        }.runTaskAsynchronously(LevelledMobs.getInstance());
    }

    public boolean hasRequiredPermission() { return !requiredPermission.isEmpty(); }

    @NotNull
    public String getRequiredPermission() { return requiredPermission; }

    @NotNull
    public String[] getMessage() { return message; }
}
