package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.misc.DebugCreator;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class DebugSubcommand extends MessagesBase implements Subcommand {
    public DebugSubcommand(final LevelledMobs main) {
        super(main);
    }

    @Override
    public void parseSubcommand(final LevelledMobs main, final @NotNull CommandSender sender, final String label, final String[] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.debug")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length == 3 && "create".equalsIgnoreCase(args[1]) && "confirm".equalsIgnoreCase(args[2]))
            DebugCreator.createDebug(main, sender);
        else
            showMessage("other.create-debug");
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {
        // This subcommand has no tab completions.
        return Collections.emptyList();
    }
}
