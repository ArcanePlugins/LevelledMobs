package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.ConfirmSubcommand;
import org.bukkit.ChatColor;

public class TestRoutine {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("test")
            .withShortDescription("Test routine for maintainers, DO NOT RUN.")
            .withFullDescription("Test routine for maintainers, DO NOT RUN.")
            .withPermission("levelledmobs.command.levelledmobs.routine.test")
            .executes((sender, args) -> {
                sender.sendMessage(
                    ChatColor.YELLOW +
                        "This command is potentially dangerous; to confirm it, run /lm confirm."
                );

                //noinspection CodeBlock2Expr
                ConfirmSubcommand.CONFIRMATION_MAP.put(
                    sender,
                    () -> {
                        sender.sendMessage(
                            ChatColor.RED + "Error: Not Implemented"
                        );
                    }
                );
            });
    }

}
