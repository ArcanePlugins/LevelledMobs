package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Reloads all LevelledMobs configuration from disk
 *
 * @author lokka30
 */
public class ReloadSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, final CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.reload")){
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        main.reloadLM(sender);
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {
        return null; //No tab completions.
    }
}
