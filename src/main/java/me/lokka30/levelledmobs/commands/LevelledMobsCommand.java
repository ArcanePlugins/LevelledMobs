package me.lokka30.levelledmobs.commands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.subcommands.*;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * This class handles the command execution of '/levelledmobs'.
 *
 * @author lokka30
 */
public class LevelledMobsCommand implements CommandExecutor, TabCompleter {

    private final LevelledMobs main;

    public LevelledMobsCommand(final LevelledMobs main) {
        this.main = main;
        spawnerSubCommand = new SpawnerSubCommand(main);
        rulesSubcommand = new RulesSubcommand(main);
    }

    private final InfoSubcommand infoSubcommand = new InfoSubcommand();
    private final KillSubcommand killSubcommand = new KillSubcommand();
    private final ReloadSubcommand reloadSubcommand = new ReloadSubcommand();
    private final SummonSubcommand summonSubcommand = new SummonSubcommand();
    private final CompatibilitySubcommand compatibilitySubcommand = new CompatibilitySubcommand();
    private final GenerateMobDataSubcommand generateMobDataSubcommand = new GenerateMobDataSubcommand();
    private final SpawnerSubCommand spawnerSubCommand;
    private final RulesSubcommand rulesSubcommand;

    public boolean onCommand(@NotNull final CommandSender sender, final Command command, final String label, final String[] args) {
        if (sender.hasPermission("levelledmobs.command")) {
            if (args.length == 0) {
                sendMainUsage(sender, label);
            } else {
                switch (args[0].toLowerCase()) {
                    case "kill":
                        killSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "reload":
                        reloadSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "summon":
                        summonSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "info":
                        infoSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "compatibility":
                        compatibilitySubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "generatemobdata":
                        generateMobDataSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "spawner":
                        spawnerSubCommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "rules":
                        rulesSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "test":
                        doTest(sender, command, label, args);
                        break;
                    default:
                        sendMainUsage(sender, label);
                }
			}
		} else {
            main.configUtils.sendNoPermissionMsg(sender);
        }
		return true;
	}

	private void doTest(@NotNull final CommandSender sender, final Command command, final String label, final String[] args){
        if (Bukkit.getPluginManager().getPlugin("MMOCore") == null){
            sender.sendMessage("MMOCore not installed");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null){
            sender.sendMessage("PAPI not installed");
            return;
        }

        final Player player = (Player) sender;
        final String test = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%mmocore_level%");
        sender.sendMessage("player " + player.getName() + " level: " + test);

        //sender.sendMessage(player.getDisplayName() + "'s level: " + pd.getLevel());
    }

	private void sendMainUsage(@NotNull final CommandSender sender, final String label) {
        List<String> mainUsage = main.messagesCfg.getStringList("command.levelledmobs.main-usage");

        mainUsage = Utils.replaceAllInList(mainUsage, "%prefix%", main.configUtils.getPrefix());
        mainUsage = Utils.replaceAllInList(mainUsage, "%label%", label);
        mainUsage = Utils.colorizeAllInList(mainUsage);

        mainUsage.forEach(sender::sendMessage);
    }

	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String alias, @NotNull final String[] args) {
		if (args.length == 1) {
			List<String> suggestions = new LinkedList<>();

			if (sender.hasPermission("levelledmobs.command.summon"))
				suggestions.add("summon");
            if (sender.hasPermission("levelledmobs.command.kill"))
                suggestions.add("kill");
            if (sender.hasPermission("levelledmobs.command.reload"))
                suggestions.add("reload");
            if (sender.hasPermission("levelledmobs.command.info"))
                suggestions.add("info");
            if (sender.hasPermission("levelledmobs.command.compatibility"))
                suggestions.add("compatibility");
            if (sender.hasPermission("levelledmobs.command.compatibility.spawner"))
                suggestions.add("spawner");
            if (sender.hasPermission("levelledmobs.command.rules"))
                suggestions.add("rules");

            return suggestions;
        } else {
			switch (args[0].toLowerCase()) {
                case "summon":
                    return summonSubcommand.parseTabCompletions(main, sender, args);
                case "kill":
                    return killSubcommand.parseTabCompletions(main, sender, args);
                case "generatemobdata":
                    return generateMobDataSubcommand.parseTabCompletions(main, sender, args);
                case "spawner":
                    return spawnerSubCommand.parseTabCompletions(main, sender, args);
                case "rules":
                    return rulesSubcommand.parseTabCompletions(main, sender, args);
                // missing subcommands don't have tab completions.
                default:
                    return null;
            }
		}
	}
}
