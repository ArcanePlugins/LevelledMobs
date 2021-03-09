package io.github.lokka30.levelledmobs.commands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.commands.subcommands.*;
import io.github.lokka30.levelledmobs.misc.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles the command execution of '/levelledmobs'.
 *
 * @author lokka30
 */
public class LevelledMobsCommand implements CommandExecutor, TabCompleter {

    private final LevelledMobs instance;

    public LevelledMobsCommand(final LevelledMobs instance) {
        this.instance = instance;
    }

    private final InfoSubcommand infoSubcommand = new InfoSubcommand();
    private final KillSubcommand killSubcommand = new KillSubcommand();
    private final ReloadSubcommand reloadSubcommand = new ReloadSubcommand();
    private final SummonSubcommand summonSubcommand = new SummonSubcommand();
    private final CompatibilitySubcommand compatibilitySubcommand = new CompatibilitySubcommand();
    private final GenerateMobDataSubcommand generateMobDataSubcommand = new GenerateMobDataSubcommand();

    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (sender.hasPermission("levelledmobs.command")) {
            if (args.length == 0) {
                sendMainUsage(sender, label);
            } else {
                switch (args[0].toLowerCase()) {
                    case "kill":
                        killSubcommand.parseSubcommand(instance, sender, label, args);
                        break;
                    case "reload":
                        reloadSubcommand.parseSubcommand(instance, sender, label, args);
                        break;
                    case "summon":
                        summonSubcommand.parseSubcommand(instance, sender, label, args);
                        break;
                    case "info":
                        infoSubcommand.parseSubcommand(instance, sender, label, args);
                        break;
                    case "compatibility":
                        compatibilitySubcommand.parseSubcommand(instance, sender, label, args);
                        break;
                    case "generatemobdata":
                        generateMobDataSubcommand.parseSubcommand(instance, sender, label, args);
                        break;
                    default:
                        sendMainUsage(sender, label);
                }
			}
		} else {
			instance.configUtils.sendNoPermissionMsg(sender);
		}
		return true;
	}

	private void sendMainUsage(CommandSender sender, String label) {
		List<String> mainUsage = instance.messagesCfg.getStringList("command.levelledmobs.main-usage");

		mainUsage = Utils.replaceAllInList(mainUsage, "%prefix%", instance.configUtils.getPrefix());
		mainUsage = Utils.replaceAllInList(mainUsage, "%label%", label);
		mainUsage = Utils.colorizeAllInList(mainUsage);

		mainUsage.forEach(sender::sendMessage);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (args.length == 1) {
			List<String> suggestions = new ArrayList<>();

			if (sender.hasPermission("levelledmobs.command.summon")) {
				suggestions.add("summon");
            }
            if (sender.hasPermission("levelledmobs.command.kill")) {
                suggestions.add("kill");
            }
            if (sender.hasPermission("levelledmobs.command.reload")) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("levelledmobs.command.info")) {
                suggestions.add("info");
            }
            if (sender.hasPermission("levelledmobs.command.compatibility")) {
                suggestions.add("compatibility");
            }

            return suggestions;
        } else {
			switch (args[0].toLowerCase()) {
                case "summon":
                    return summonSubcommand.parseTabCompletions(instance, sender, args);
                case "kill":
                    return killSubcommand.parseTabCompletions(instance, sender, args);
                case "generatemobdata":
                    return generateMobDataSubcommand.parseTabCompletions(instance, sender, args);
                // missing subcommands don't have tab completions.
                default:
                    return null;
            }
		}
	}
}
