package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;

public final class ReloadSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("reload")
            .withPermission("levelledmobs.command.levelledmobs.reload")
            .withShortDescription("Reload LevelledMobs' configuration files.")
            .withFullDescription("Reloads LevelledMobs' configuration files without having to " +
                "restart the server.")
            .executes((sender, args) -> {
                sender.sendMessage("LM: Reloading...");
                //TODO translatable messages
                try {
                    LevelledMobs.getInstance().reload();
                    sender.sendMessage("LM: Reload complete.");
                    //TODO translatable messages
                } catch(Exception ex) {
                    sender.sendMessage("LM: Reload failed, check console for more information.");
                    //TODO translatable messages
                }
            });
    }

}
