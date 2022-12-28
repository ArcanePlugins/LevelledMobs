package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import io.github.arcaneplugins.levelledmobs.bukkit.command.CommandHandler;
import org.bukkit.ChatColor;

public final class HelpSubcommand {

    public static CommandAPICommand createInstance() {
        return new CommandAPICommand("help")
            .withPermission("levelledmobs.command.levelledmobs.help")
            .withShortDescription("View a list of available commands and support links.")
            .withFullDescription("Sends the user a list of available LevelledMobs commands and " +
                "also a list of URLs providing documentation and assistance.")
            .executes((sender, args) -> {
                //TODO this is just a basic implementation, needs to be changed quite a lot

                sender.sendMessage("Available commands:");

                for(CommandAPICommand cmd : CommandHandler.COMMANDS) {
                    sender.sendMessage(ChatColor.AQUA + cmd.getName() +
                        ChatColor.GRAY + ": " + cmd.getShortDescription());

                    for(CommandAPICommand subcmd : cmd.getSubcommands()) {
                        sender.sendMessage(ChatColor.GRAY + " • " + subcmd.getName() + ": " +
                            subcmd.getShortDescription());
                    }
                }

                sender.sendMessage("Support and documentation links: N/A");
            });
    }

}
