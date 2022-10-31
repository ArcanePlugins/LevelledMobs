package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntityTypeArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class SummonSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("summon")
            .withPermission("levelledmobs.command.levelledmobs.summon")
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
