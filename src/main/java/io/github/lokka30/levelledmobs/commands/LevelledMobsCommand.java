package io.github.lokka30.levelledmobs.commands;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.github.lokka30.levelledmobs.commands.subcommands.InfoSubcommand;
import io.github.lokka30.levelledmobs.commands.subcommands.KillSubcommand;
import io.github.lokka30.levelledmobs.commands.subcommands.ReloadSubcommand;
import io.github.lokka30.levelledmobs.commands.subcommands.SummonSubcommand;
import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class handles the command execution of '/levelledmobs'.
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

	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (sender.hasPermission("levelledmobs.command")) {
			if (args.length == 0) {
				sendMainUsage(sender, label);
			} else {
				switch (args[0].toLowerCase()) {
					case "kill":
						killSubcommand.parse(instance, sender, label, args);
						break;
					case "reload":
						reloadSubcommand.parse(instance, sender, label, args);
						break;
					case "summon":
						summonSubcommand.parse(instance, sender, label, args);
						break;
					case "info":
						infoSubcommand.parse(instance, sender, label, args);
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

		if (cmd.getName().equalsIgnoreCase("levelledmobs")) {
			List<String> cmds = new ArrayList<>();

			if (args.length == 1) {
				cmds.add("info");

				if (sender.hasPermission("levelledmobs.kill.all") || sender.hasPermission("levelledmobs.kill.near")) {
					cmds.add("kill");
				}

				if (sender.hasPermission("levelledmobs.reload")) {
					cmds.add("reload");
				}

				if (sender.hasPermission("levelledmobs.summon")) {
					cmds.add("summon");
				}
			} else if (args.length >= 2) {
				// lvlmobs kill all <world>
				// lblmobs kill near <radius>

				if (args[0].equalsIgnoreCase("kill")) {
					if (args.length == 2) {
						cmds.add("all");
						cmds.add("near");
					} else if (args.length == 3) {
						if (args[1].equalsIgnoreCase("all") && sender.hasPermission("levelledmobs.kill.all")) {
							List<String> enabledWorlds = Collections.singletonList("WorldName"); //TODO replace with get Enabled Worlds List
							//noinspection ConstantConditions
							if (!enabledWorlds.isEmpty()) {
								cmds.addAll(enabledWorlds);
							}
						} else if (args[1].equalsIgnoreCase("near")) {
							cmds.addAll(Utils.oneToNine);
						}
					} else {
						return cmds;
					}
				} else if (args[0].equalsIgnoreCase("summon")) {
					// len:    1      2        3        4       5          6            7   8
					// arg:    0      1        2        3       4          5            6   9
					// lvlmobs summon <amount> <entity> <level> here
					// lvlmobs summon <amount> <entity> <level> atPlayer   <playername>
					// lvlmobs summon <amount> <entity> <level> atLocation <x>          <y> <z>


					boolean isAtLocation = false;
					boolean isAtPlayer = false;
					if (args.length > 4) {
						if (args[4].equalsIgnoreCase("atlocation")) isAtLocation = true;
						if (args[4].equalsIgnoreCase("atplayer")) isAtPlayer = true;
					}

					if (args.length == 3) {
						cmds.addAll(Utils.mobs);
					} else if (args.length == 2 || args.length == 4) {
						cmds.addAll(Utils.oneToNine);
					} else if (args.length == 5) {
						cmds.add("atLocation");
						cmds.add("atPlayer");
						cmds.add("here");
					} else if (args.length == 6) {
						if (isAtLocation) {
							cmds.add("~ ~ ~");
							cmds.add("~ ~");
							cmds.add("~");
						} else if (isAtPlayer) return null; // causes player list to show
					} else if (args.length == 7 && !isAtPlayer) {
						cmds.add("~ ~");
						cmds.add("~");
					} else if (args.length == 8 && !isAtPlayer) {
						cmds.add("~");
					}
				}
			} // end if args >= 2

			return cmds;
		} // end if cmd == leveledmobs

		return null;
	}
}
