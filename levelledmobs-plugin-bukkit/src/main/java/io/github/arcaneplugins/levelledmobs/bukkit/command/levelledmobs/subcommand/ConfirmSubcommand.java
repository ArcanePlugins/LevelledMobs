package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.CommandSender;

public class ConfirmSubcommand {

    public static final Map<CommandSender, Runnable> CONFIRMATION_MAP = new HashMap<>();

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("confirm")
            .withPermission("levelledmobs.command.levelledmobs.confirm")
            .withShortDescription("Confirms a potentially dangerous LevelledMobs command.")
            .withFullDescription("Allows users to confirm actions from potentially dangerous " +
                "LevelledMobs commands, such as one from `/lm routine`.")
            .executes((sender, args) -> {
                if(CONFIRMATION_MAP.containsKey(sender)) {
                    //TODO Translatable Message
                    sender.sendMessage("LM: Confirming action.");
                    CONFIRMATION_MAP.get(sender).run();
                    CONFIRMATION_MAP.remove(sender);
                } else {
                    //TODO Translatable Message
                    sender.sendMessage("LM: Nothing to confirm.");
                }
            });

}
