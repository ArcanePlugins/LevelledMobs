package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class BackupSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("backup")
            .withPermission("levelledmobs.command.levelledmobs.backup")
            .executes((sender, args) -> {
                sender.sendMessage("backup not implemented");
                //TODO impl
            });

}
