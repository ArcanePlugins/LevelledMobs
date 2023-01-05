package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public final class EggSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("egg")
            .withPermission("levelledmobs.command.levelledmobs.egg")
            .withShortDescription("Create and customise levelled mob spawn eggs.")
            .withFullDescription("Create and customise spawn eggs, which summon levelled " +
                "mobs of your chosen specifications, from the level "
                + "to the entity type and much more.")
            .executes((sender, args) -> {
                sender.sendMessage("[LM] error: egg not implemented");
                //TODO impl
            });
    }

}
