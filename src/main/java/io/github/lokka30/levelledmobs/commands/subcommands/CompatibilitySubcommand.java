package io.github.lokka30.levelledmobs.commands.subcommands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CompatibilitySubcommand implements Subcommand {

    @Override
    public void parseSubcommand(LevelledMobs instance, CommandSender sender, String label, String[] args) {
        if (sender.hasPermission("levelledmobs.command.compatibility")) {
            if (args.length == 1) {
                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.compatibility.notice");
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
                instance.checkCompatibility();
            } else {
                List<String> messages = instance.messagesCfg.getStringList("command.levelledmobs.compatibility.usage");
                messages = Utils.replaceAllInList(messages, "%prefix%", instance.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%label%", label);
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        } else {
            instance.configUtils.sendNoPermissionMsg(sender);
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs instance, CommandSender sender, String[] args) {
        // This subcommand has no tab completions.
        return null;
    }
}
