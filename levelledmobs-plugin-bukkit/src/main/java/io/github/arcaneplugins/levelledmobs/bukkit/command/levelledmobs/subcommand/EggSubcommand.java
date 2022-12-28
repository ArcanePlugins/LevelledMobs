package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public final class EggSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("egg")
            .withPermission("levelledmobs.command.levelledmobs.egg")
            .withShortDescription("Create and customise a spawn egg item which summons a " +
                "levelled mob.")
            .withFullDescription("Create and customise a spawn egg item which summons a levelled " +
                "mob of your chosen specifications, such as the level, entity type, and more.")
            .executes((sender, args) -> {
                sender.sendMessage("egg not implemented");
                //TODO impl
            });
    }

}
