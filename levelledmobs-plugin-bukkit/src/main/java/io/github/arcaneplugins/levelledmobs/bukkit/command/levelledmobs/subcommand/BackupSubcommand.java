package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class BackupSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("backup")
            .withPermission("levelledmobs.command.levelledmobs.backup")
            .withShortDescription("Backup your LevelledMobs configuration files.")
            .withFullDescription("Copies LevelledMobs' configuration files into a compressed " +
                "zip file, placed in the `LevelledMobs/backups` directory.")
            .executes((sender, args) -> {
                sender.sendMessage("backup not implemented");
                //TODO impl
            });

}
