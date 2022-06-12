/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.subcommands.DebugSubcommand;
import me.lokka30.levelledmobs.commands.subcommands.InfoSubcommand;
import me.lokka30.levelledmobs.commands.subcommands.KillSubcommand;
import me.lokka30.levelledmobs.commands.subcommands.ReloadSubcommand;
import me.lokka30.levelledmobs.commands.subcommands.RulesSubcommand;
import me.lokka30.levelledmobs.commands.subcommands.SpawnerEggCommand;
import me.lokka30.levelledmobs.commands.subcommands.SpawnerSubCommand;
import me.lokka30.levelledmobs.commands.subcommands.SummonSubcommand;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

/**
 * This class handles the command execution of '/levelledmobs'.
 *
 * @author lokka30
 * @since 2.4.0
 */
public class LevelledMobsCommand implements CommandExecutor, TabCompleter {

    private final LevelledMobs main;

    public LevelledMobsCommand(final LevelledMobs main) {
        this.main = main;
        debugSubcommand = new DebugSubcommand(main);
        infoSubcommand = new InfoSubcommand(main);
        killSubcommand = new KillSubcommand(main);
        spawnerSubCommand = new SpawnerSubCommand(main);
        reloadSubcommand = new ReloadSubcommand();
        rulesSubcommand = new RulesSubcommand(main);
        spawnerEggCommand = new SpawnerEggCommand(main);
        summonSubcommand = new SummonSubcommand(main);
    }

    // Retain alphabetical order please.
    private final DebugSubcommand debugSubcommand;
    private final InfoSubcommand infoSubcommand;
    private final KillSubcommand killSubcommand;
    private final ReloadSubcommand reloadSubcommand;
    public final RulesSubcommand rulesSubcommand;
    private final SpawnerEggCommand spawnerEggCommand;
    public final SpawnerSubCommand spawnerSubCommand;
    private final SummonSubcommand summonSubcommand;

    public boolean onCommand(
        @NotNull final CommandSender sender,
        final @NotNull Command command,
        final @NotNull String label,
        final String[] args
    ) {
        if (sender.hasPermission("levelledmobs.command")) {
            if (args.length == 0) {
                sendMainUsage(sender, label);
            } else {
                switch (args[0].toLowerCase()) {
                    // Retain alphabetical order please.
                    case "debug":
                        debugSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "egg":
                        spawnerEggCommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "info":
                        infoSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "kill":
                        killSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "reload":
                        reloadSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "rules":
                        rulesSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "spawner":
                        spawnerSubCommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "summon":
                        summonSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    default:
                        sendMainUsage(sender, label);
                        break;
                }
            }
        } else {
            main.configUtils.sendNoPermissionMsg(sender);
        }
        return true;
    }

    private void sendMainUsage(@NotNull final CommandSender sender, final String label) {
        List<String> mainUsage = main.messagesCfg.getStringList("command.levelledmobs.main-usage");
        mainUsage = Utils.replaceAllInList(mainUsage, "%prefix%", main.configUtils.getPrefix());
        mainUsage = Utils.replaceAllInList(mainUsage, "%label%", label);
        mainUsage = Utils.colorizeAllInList(mainUsage);
        mainUsage.forEach(sender::sendMessage);
    }

    // Retain alphabetical order please.
    private final List<String> commandsToCheck = Arrays.asList("debug", "egg", "summon", "kill",
        "reload", "info", "spawner", "rules");

    @Override
    public List<String> onTabComplete(final @NotNull CommandSender sender,
        final @NotNull Command cmd, final @NotNull String alias,
        @NotNull final String @NotNull [] args) {
        if (args.length == 1) {
            final List<String> suggestions = new LinkedList<>();

            commandsToCheck.forEach(command -> {
                if (sender.hasPermission("levelledmobs.command." + command)) {
                    suggestions.add(command);
                }
            });

            return suggestions;
        } else {
            switch (args[0].toLowerCase()) {
                // Retain alphabetical order please.
                case "kill":
                    return killSubcommand.parseTabCompletions(main, sender, args);
                case "rules":
                    return rulesSubcommand.parseTabCompletions(main, sender, args);
                case "spawner":
                    return spawnerSubCommand.parseTabCompletions(main, sender, args);
                case "summon":
                    return summonSubcommand.parseTabCompletions(main, sender, args);
                case "egg":
                    return spawnerEggCommand.parseTabCompletions(main, sender, args);
                case "debug":
                    return debugSubcommand.parseTabCompletions(main, sender, args);
                // the missing subcommands don't have tab completions, don't bother including them.
                default:
                    return Collections.emptyList();
            }
        }
    }
}
