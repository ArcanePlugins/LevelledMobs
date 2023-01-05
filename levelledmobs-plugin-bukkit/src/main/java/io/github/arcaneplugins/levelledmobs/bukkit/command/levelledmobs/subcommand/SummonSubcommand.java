package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntityTypeArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public final class SummonSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("summon")
            .withPermission("levelledmobs.command.levelledmobs.summon")
            .withShortDescription("Summons a levelled mob of chosen specifications.")
            .withFullDescription("Summons a levelled mob of chosen specifications, similar to " +
                "Minecraft's `/summon` command.")
            .withSubcommands(
                new CommandAPICommand("entity")
                    .withShortDescription("Summons a levelled entity of the specified type.")
                    .withFullDescription("Summons a levelled entity of the specified type.")
                    .withArguments(
                        new EntityTypeArgument("entityType")
                        //TODO add more arguments
                    )
                    .executes((sender, args) -> {
                        //TODO impl
                        sender.sendMessage("Not implemented");
                    }),
                new CommandAPICommand("custom")
                    .withShortDescription("Summons a levelled entity fro the specified custom " +
                        "entity template.")
                    .withFullDescription("Summons a levelled entity fro the specified custom " +
                        "entity template.")
                    .withArguments(
                        // TODO may want to use a MultiLiteralArgument for the custom-entity id
                        new StringArgument("customEntityId")
                        //TODO add more args
                    )
                    .executes((sender, args) -> {
                        //TODO impl
                        sender.sendMessage("Not implemented");
                    })
            );
    }

}
