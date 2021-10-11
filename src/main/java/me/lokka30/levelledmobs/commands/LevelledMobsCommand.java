/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.subcommands.*;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
        spawnerSubCommand = new SpawnerSubCommand(main);
        rulesSubcommand = new RulesSubcommand(main);
    }

    // Retain alphabetical order please.
    private final CompatibilitySubcommand compatibilitySubcommand = new CompatibilitySubcommand();
    private final GenerateMobDataSubcommand generateMobDataSubcommand = new GenerateMobDataSubcommand();
    private final InfoSubcommand infoSubcommand = new InfoSubcommand();
    private final KillSubcommand killSubcommand = new KillSubcommand();
    private final ReloadSubcommand reloadSubcommand = new ReloadSubcommand();
    private final RulesSubcommand rulesSubcommand;
    private final SpawnerSubCommand spawnerSubCommand;
    private final SummonSubcommand summonSubcommand = new SummonSubcommand();
    private final DebugSubcommand debugSubcommand = new DebugSubcommand();

    public boolean onCommand(@NotNull final CommandSender sender, final Command command, final String label, final String[] args) {
        if (sender.hasPermission("levelledmobs.command")) {
            if (args.length == 0) {
                sendMainUsage(sender, label);
            } else {
                switch (args[0].toLowerCase()) {
                    // Retain alphabetical order please.
                    case "compatibility":
                        compatibilitySubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "debug":
                        debugSubcommand.parseSubcommand(main, sender, label, args);
                        break;
                    case "generatemobdata":
                        generateMobDataSubcommand.parseSubcommand(main, sender, label, args);
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
                    case "test":
                        test(sender, args);
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

    private void test(@NotNull final CommandSender sender, final String @NotNull [] args) {

    }

    private void sendMainUsage(@NotNull final CommandSender sender, final String label) {
        List<String> mainUsage = main.messagesCfg.getStringList("command.levelledmobs.main-usage");
        mainUsage = Utils.replaceAllInList(mainUsage, "%prefix%", main.configUtils.getPrefix());
        mainUsage = Utils.replaceAllInList(mainUsage, "%label%", label);
        mainUsage = Utils.colorizeAllInList(mainUsage);
        mainUsage.forEach(sender::sendMessage);
    }

    // Retain alphabetical order please.
    private final List<String> commandsToCheck = Arrays.asList("debug", "summon", "kill", "reload", "info", "compatibility", "spawner", "rules");

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command cmd, final String alias, @NotNull final String @NotNull [] args) {
        if (args.length == 1) {
            List<String> suggestions = new LinkedList<>();

            commandsToCheck.forEach(command -> {
                if (sender.hasPermission("levelledmobs.command." + command)) {
                    suggestions.add(command);
                }
            });

            return suggestions;
        } else {
            switch (args[0].toLowerCase()) {
                // Retain alphabetical order please.
                case "generatemobdata":
                    return generateMobDataSubcommand.parseTabCompletions(main, sender, args);
                case "kill":
                    return killSubcommand.parseTabCompletions(main, sender, args);
                case "rules":
                    return rulesSubcommand.parseTabCompletions(main, sender, args);
                case "spawner":
                    return spawnerSubCommand.parseTabCompletions(main, sender, args);
                case "summon":
                    return summonSubcommand.parseTabCompletions(main, sender, args);
                // the missing subcommands don't have tab completions, don't bother including them.
                default:
                    return Collections.emptyList();
            }
        }
    }
}
