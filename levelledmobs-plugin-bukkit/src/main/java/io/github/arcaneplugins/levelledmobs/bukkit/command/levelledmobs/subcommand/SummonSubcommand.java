package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntityTypeArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public final class SummonSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("summon")
            .withPermission("levelledmobs.command.levelledmobs.summon")
            .withShortDescription("Summons a levelled mob of your specifications.")
            .withFullDescription("Summons a levelled mob of your specifications, similar to how " +
                "Minecraft's summon command works.")
            .withSubcommands(
                new CommandAPICommand("entity")
                    .withArguments(
                        new EntityTypeArgument("entityType")
                        //TODO add more arguments
                    )
                    .executes((sender, args) -> {
                        //TODO impl
                        sender.sendMessage("Not implemented");
                    }),
                new CommandAPICommand("custom")
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
