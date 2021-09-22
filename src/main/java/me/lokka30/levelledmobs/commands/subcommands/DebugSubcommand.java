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

        if (args.length < 3 || !"create".equalsIgnoreCase(args[1]) && !"confirm".equalsIgnoreCase(args[2])){
            sender.sendMessage(MessageUtils.colorizeAll("&fBuild Debug Files&r\n" +
                    "If you're running this command, that means a LM Developer asked you to.\n" +
                    "We need copies of your LevelledMobs files, as well as a list of your current plugins, your current server build and version, " +
                    "and your current and maximum player count. Confirming this command will create a .ZIP file containing the above mentioned information, " +
                    "to be shared with a LM Developer. &fAbsolutely NOTHING&r within the .ZIP would contain private or personal information, and you can verify " +
                    "the contents of the .ZIP file prior to sending.\n" +
                    "If you are satisfied, please confirm by typing &b/lm debug create confirm"));

            return;
        }

        DebugCreator.createDebug(main, sender);
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {
        // This subcommand has no tab completions.
        return Collections.emptyList();
    }
}
