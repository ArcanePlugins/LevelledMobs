package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.DebugCreator;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class DebugSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, final @NotNull CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.debug")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length == 3 && "create".equalsIgnoreCase(args[1]) && "confirm".equalsIgnoreCase(args[2]))
            DebugCreator.createDebug(main, sender);
        else{
            sender.sendMessage(MessageUtils.colorizeAll("&b&nCreate a Debugging ZIP\n" +
                    "&7You should only run this command if a LevelledMobs developer has asked you to. It is used to assist users who are experiencing issues with the plugin.\n" +
                    "&r\n" +
                    "&7This command will generate a ZIP file containing the following required data:\n" +
                    "&8 &m->&b Plugins list\n" +
                    "&8 &m->&b Server version\n" +
                    "&8 &m->&b Current and maximum online player count\n" +
                    "&8 &m->&7 The &blatest.log file&7 &8(/logs/latest.log)\n" +
                    "&r\n" +
                    "&7LevelledMobs developers will not redistribute or retain the data beyond the purpose of resolving any issue you may be experiencing. You may also verify the contents prior to sending the file.\n" +
                    "&7To proceed in creating the ZIP file, please run:\n" +
                    "&b/lm debug create confirm&7."));
        }
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {
        // This subcommand has no tab completions.
        return Collections.emptyList();
    }
}
