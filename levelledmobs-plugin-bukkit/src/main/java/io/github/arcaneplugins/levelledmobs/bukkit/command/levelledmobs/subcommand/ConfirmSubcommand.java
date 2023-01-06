package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.CommandSender;

public final class ConfirmSubcommand {

    public static final Map<CommandSender, Runnable> CONFIRMATION_MAP = new HashMap<>();

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("confirm")
            .withPermission("levelledmobs.command.levelledmobs.confirm")
            .withShortDescription("Confirms a potentially dangerous action from another command.")
            .withFullDescription("Allows users to confirm actions from potentially dangerous " +
                "LM commands, such as some routines in `/lm routine`.")
            .executes((sender, args) -> {
                if(CONFIRMATION_MAP.containsKey(sender)) {
                    Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_START.sendTo(sender);
                    CONFIRMATION_MAP.get(sender).run();
                    CONFIRMATION_MAP.remove(sender);
                    Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_COMPLETE.sendTo(sender);
                } else {
                    Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_NONE.sendTo(sender);
                }
            });
    }

}
