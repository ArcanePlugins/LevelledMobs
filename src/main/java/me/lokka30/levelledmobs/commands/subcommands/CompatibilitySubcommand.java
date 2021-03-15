package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * @author lokka30
 */
public class CompatibilitySubcommand implements Subcommand {

    @Override
    public void parseSubcommand(LevelledMobs main, CommandSender sender, String label, String[] args) {
        if (sender.hasPermission("levelledmobs.command.compatibility")) {
            if (args.length == 1) {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.compatibility.notice");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
                main.companion.checkCompatibility();
            } else {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.compatibility.usage");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%label%", label);
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        } else {
            main.configUtils.sendNoPermissionMsg(sender);
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs main, CommandSender sender, String[] args) {
        // This subcommand has no tab completions.
        return null;
    }
}
