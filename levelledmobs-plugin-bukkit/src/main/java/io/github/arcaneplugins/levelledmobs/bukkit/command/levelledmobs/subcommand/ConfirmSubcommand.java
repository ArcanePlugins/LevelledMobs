package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;

public class ConfirmSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("confirm")
            .withPermission("levelledmobs.command.levelledmobs.confirm")
            .executes((sender, args) -> {
                sender.sendMessage("confirm not implemented");
                //TODO impl
            });

}
