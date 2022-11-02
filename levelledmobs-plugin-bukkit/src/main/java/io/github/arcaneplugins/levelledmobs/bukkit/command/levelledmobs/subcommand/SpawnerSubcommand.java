package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class SpawnerSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("spawner")
            .withPermission("levelledmobs.command.levelledmobs.spawner")
            .withShortDescription("Create and customise a spawner which spawns levelled mobs.")
            .withFullDescription("Create and customise a spawner which generates levelled mobs, " +
                "with customisations such as what level range the spawner can generate, and more.")
            .executes((sender, args) -> {
                sender.sendMessage("spawner not implemented");
                //TODO impl
            });

}
