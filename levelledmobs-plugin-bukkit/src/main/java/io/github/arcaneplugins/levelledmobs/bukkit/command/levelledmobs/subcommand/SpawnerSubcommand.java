package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public final class SpawnerSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("spawner")
            .withPermission("levelledmobs.command.levelledmobs.spawner")
            .withShortDescription("Create and customise spawners which spawn levelled mobs.")
            .withFullDescription("Create and customise spawners which spawn levelled mobs, " +
                "from the allowed level range to the period of spawning cycles and more.")
            .executes((sender, args) -> {
                sender.sendMessage("spawner not implemented");
                //TODO impl
            });

    }

}
