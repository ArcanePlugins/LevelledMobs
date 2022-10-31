package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class EggSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("egg")
            .withPermission("levelledmobs.command.levelledmobs.egg")
            .executes((sender, args) -> {
                sender.sendMessage("egg not implemented");
                //TODO impl
            });

}
