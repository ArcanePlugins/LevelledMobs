package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;

public final class ReloadSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("reload")
            .withPermission("levelledmobs.command.levelledmobs.reload")
            .withShortDescription("Reloads LM config files.")
            .withFullDescription("Reloads LevelledMobs config files.")
            .executes((sender, args) -> {
                Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_START.sendTo(sender);

                try {
                    LevelledMobs.getInstance().reload();
                } catch(final Exception ex) {
                    ex.printStackTrace();
                    Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_COMPLETE_FAILURE.sendTo(sender);
                    return;
                }

                Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_COMPLETE_SUCCESS.sendTo(sender);
            });
    }

}
