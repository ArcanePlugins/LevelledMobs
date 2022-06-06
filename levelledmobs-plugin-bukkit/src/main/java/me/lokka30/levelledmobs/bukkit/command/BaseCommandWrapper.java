package me.lokka30.levelledmobs.bukkit.command;

import java.util.List;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public abstract class BaseCommandWrapper extends CommandWrapper implements TabExecutor {

    /* vars */

    private final String primaryLabel;

    /* constructors */

    public BaseCommandWrapper(final String primaryLabel) {
        super(primaryLabel);
        this.primaryLabel = primaryLabel;
    }

    /* methods */

    @Override
    public boolean onCommand(
        final @NotNull CommandSender sender,
        final @NotNull Command cmd,
        final @NotNull String label,
        final @NotNull String[] args
    ) {
        run(sender, bukkitArgsToWrapperArgs(label, args));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(
        final @NotNull CommandSender sender,
        final @NotNull Command cmd,
        final @NotNull String label,
        final @NotNull String[] args
    ) {
        return suggest(sender, bukkitArgsToWrapperArgs(label, args));
    }

    @NotNull
    protected String[] bukkitArgsToWrapperArgs(
        final @NotNull String bukkitLabel,
        final @NotNull String[] bukkitArgs
    ) {
        final String[] wrapperArgs = new String[bukkitArgs.length + 1];
        wrapperArgs[0] = bukkitLabel;
        System.arraycopy(bukkitArgs, 0, wrapperArgs, 1, bukkitArgs.length);
        return wrapperArgs;
    }

    public boolean register() {
        final PluginCommand pluginCmd = LevelledMobs.getInstance().getCommand(primaryLabel);
        if(pluginCmd == null) {
            Log.sev("Unable to register command '/" + getLabels() + "': Bukkit was " +
                "unable to retrieve this command from the plugin. Have you tampered with "
                + "the inbuilt resource 'plugin.yml'?", true);
            return false;
        }

        pluginCmd.setExecutor(this);
        return true;
    }

    /* getters and setters */

    @NotNull
    public String getPrimaryLabel() { return primaryLabel; }
}
