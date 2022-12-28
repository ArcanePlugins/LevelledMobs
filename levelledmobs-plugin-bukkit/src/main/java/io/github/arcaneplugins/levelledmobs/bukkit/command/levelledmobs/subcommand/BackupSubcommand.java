package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public final class BackupSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("backup")
            .withPermission("levelledmobs.command.levelledmobs.backup")
            .withShortDescription("Backup your LevelledMobs configuration files.")
            .withFullDescription("Copies LevelledMobs' configuration files into a compressed " +
                "zip file, placed in the `LevelledMobs/backups` directory.")
            .executes((sender, args) -> {
                sender.sendMessage("backup not implemented");
                //TODO impl
            });
    }

}
