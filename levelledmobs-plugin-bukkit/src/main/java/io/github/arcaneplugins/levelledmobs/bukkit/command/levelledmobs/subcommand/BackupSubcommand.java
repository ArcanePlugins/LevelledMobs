package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public final class BackupSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("backup")
            .withPermission("levelledmobs.command.levelledmobs.backup")
            .withShortDescription("Backup your config files.")
            .withFullDescription("Copies LevelledMobs' configuration files into a compressed " +
                "zip file, placed in the `backups` directory within LM's directory.")
            .executes((sender, args) -> {
                sender.sendMessage("[LM] error: backup not implemented");
                //TODO impl
            });
    }

}
