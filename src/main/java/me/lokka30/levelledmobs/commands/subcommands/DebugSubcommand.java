package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.misc.DebugCreator;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class DebugSubcommand extends MessagesBase implements Subcommand {
    public DebugSubcommand(final LevelledMobs main) {
        super(main);
    }

    @Override
    public void parseSubcommand(final LevelledMobs main, final @NotNull CommandSender sender, final String label, final String @NotNull [] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.debug")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length <= 1){
            sender.sendMessage("Options: create / chunk_kill_count");
            return;
        }

        if ("create".equalsIgnoreCase(args[1])) {
            if (args.length >= 3 && "confirm".equalsIgnoreCase(args[2]))
                DebugCreator.createDebug(main, sender);
            else
                showMessage("other.create-debug");
        }
        else if ("chunk_kill_count".equalsIgnoreCase(args[1])) {
            chunkKillCount(sender, args);
        }
        else
            showMessage("other.create-debug");
    }

    private void chunkKillCount(final CommandSender sender, final String @NotNull [] args){
        if (args.length >= 3 && "reset".equalsIgnoreCase(args[2])){
            main.companion.clearChunkKillCache();
            sender.sendMessage("cache has been cleared");
            return;
        }

        showChunkKillCountSyntax(sender);
    }

    private void showChunkKillCountSyntax(final @NotNull CommandSender sender){
        sender.sendMessage("Options: reset");
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String @NotNull [] args) {
        Utils.logger.info(String.format("%s", args.length));
        if (args.length <= 2)
            return List.of("create", "chunk_kill_count");
        if ("chunk_kill_count".equalsIgnoreCase(args[1]))
            return List.of("reset");

        return Collections.emptyList();
    }
}
