package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;

public class ReloadSubcommand {

    public static final CommandAPICommand INSTANCE =
        new CommandAPICommand("reload")
            .withPermission("levelledmobs.command.levelledmobs.reload")
            .executes((sender, args) -> {
                //TODO translatable messages
                sender.sendMessage("LM: Reloading...");
                try {
                    LevelledMobs.getInstance().reload();
                    sender.sendMessage("LM: Reload complete.");
                } catch(Exception ex) {
                    sender.sendMessage("LM: Reload failed, check console for more information.");
                }
            });

}
