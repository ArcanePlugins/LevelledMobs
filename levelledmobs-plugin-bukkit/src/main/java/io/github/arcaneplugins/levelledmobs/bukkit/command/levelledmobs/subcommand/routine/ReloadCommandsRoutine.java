package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand.routine;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.RegisteredCommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler.LoadingStage;
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message;

public final class ReloadCommandsRoutine {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("reload-commands")
            .withShortDescription("Reloads CommandAPI commands (not recommended).")
            .withPermission("levelledmobs.command.levelledmobs.routine.reload-commands")
            .executes((sender, args) -> {
                sender.sendMessage(Message.formatMd(new String[]{
                    "%prefix-info% Reloading CommandAPI commands."
                }));

                sender.sendMessage(Message.formatMd(new String[]{
                    "%prefix-info% &8[Unregister]&7 Unregistering commands."
                }));
                for(final RegisteredCommand cmd : CommandAPI.getRegisteredCommands()) {
                    CommandAPI.unregister(cmd.commandName());
                }
                sender.sendMessage(Message.formatMd(new String[]{
                    "%prefix-info% &8[Unregister]&7 Unregistered commands successfully."
                }));

                sender.sendMessage(Message.formatMd(new String[]{
                    "%prefix-info% &8[Register]&7 Registering commands."
                }));
                CommandHandler.load(LoadingStage.FORCED);
                sender.sendMessage(Message.formatMd(new String[]{
                    "%prefix-info% &8[Register]&7 Registered commands successfully."
                }));

                sender.sendMessage(Message.formatMd(new String[]{
                    "%prefix-info% CommandAPI commands reloaded successfully."
                }));
            });
    }

}
