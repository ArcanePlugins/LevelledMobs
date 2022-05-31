package me.lokka30.levelledmobs.bukkit.commands;

import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.GRAY;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.utils.TempConst;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class CommandWrapper {

    /* vars */

    private final Set<String> labels = new LinkedHashSet<>();

    /* constructors */

    public CommandWrapper(final @NotNull String... labels) {
        this.labels.addAll(Arrays.asList(labels));
    }

    /* methods */

    /*
    Make the CommandSender run the command with given args.

    Remember to factor in whether the sender has the required permission(s).
     */
    public abstract void run(
        final @NotNull CommandSender sender,
        final @NotNull String[] args
    );

    /*
    Generate a list of suggestions for the CommandSender's current context. For example, if they
    have typed `/lm`, suggest them a list of subcommands which they can run.

    Remember to factor in whether the sender has the required permission(s).
     */
    @NotNull
    public abstract List<String> suggest(
        final @NotNull CommandSender sender,
        final @NotNull String[] args
    );

    /*
    Check if the sender has the given permission. If warn is enabled, then the sender will be warned
    about their inability to access something if they lack the permission.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean hasPerm(
        final @NotNull CommandSender sender,
        final @NotNull String permission,
        final boolean warn
    ) {
        if(sender.hasPermission(permission))
            return true;

        if(warn)
            sender.sendMessage(TempConst.PREFIX_SEV + "You don't have access to that; it " +
                "requires the permission '" + AQUA + permission + GRAY + "'.");

        return false;
    }

    /*
    Check if the sender is a player. If warn is enabled, then the sender will be warned
    about their inability to access something as they are not a player.
     */
    @SuppressWarnings("SameParameterValue")
    protected boolean isSenderPlayer(
        final @NotNull CommandSender sender,
        final boolean warn
    ) {
        if(sender instanceof Player)
            return true;

        if(warn)
            sender.sendMessage(TempConst.PREFIX_SEV + "You don't have access to that; it is " +
                "only accessible to players.");

        return false;
    }

    /* getters and setters */

    @NotNull
    public Set<String> getLabels() {
        return labels;
    }

}
