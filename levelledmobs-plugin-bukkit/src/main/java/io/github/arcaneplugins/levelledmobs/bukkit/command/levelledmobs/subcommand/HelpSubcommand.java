package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class HelpSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("help")
            .withPermission("levelledmobs.command.levelledmobs.help")
            .executes((sender, args) -> {
                sender.sendMessage("help not implemented");
                //TODO impl
            });

}
