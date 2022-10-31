package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class SpawnerSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("spawner")
            .withPermission("levelledmobs.command.levelledmobs.spawner")
            .executes((sender, args) -> {
                sender.sendMessage("spawner not implemented");
                //TODO impl
            });

}
