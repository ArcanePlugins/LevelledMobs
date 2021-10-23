/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.levelledmobs;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.CommandHandler;
import me.lokka30.levelledmobs.commands.levelledmobs.subcommands.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * @author lokka30
 * @since v4.0.0
 * This class handles execution of the `/levelledmobs` command.
 * @see TabExecutor
 * @see CommandHandler
 */
public class LevelledMobsCommand implements TabExecutor {

    private final LevelledMobs main;
    public LevelledMobsCommand(@NotNull final LevelledMobs main) {
        this.main = main;
    }

    final AdvancedSubcommand advancedSubcommand = new AdvancedSubcommand();
    final CompatibilitySubcommand compatibilitySubcommand = new CompatibilitySubcommand();
    final InfoSubcommand infoSubcommand = new InfoSubcommand();
    final KillSubcommand killSubcommand = new KillSubcommand();
    final ReloadSubcommand reloadSubcommand = new ReloadSubcommand();
    final RulesSubcommand rulesSubcommand = new RulesSubcommand();
    final SpawnerSubcommand spawnerSubcommand = new SpawnerSubcommand();
    final SummonSubcommand summonSubcommand = new SummonSubcommand();

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        if(CommandHandler.CommandUtils.senderDoesNotHaveRequiredPermission(main, sender, "levelledmobs.command.levelledmobs")) return true;

        if(args.length == 0) {
            sendInvalidUsage(sender, label);
        } else {
            switch(args[0].toLowerCase(Locale.ROOT)) {
                case "advanced":
                    advancedSubcommand.parseCommand(main, sender, label, args);
                    break;
                case "compatibility":
                    compatibilitySubcommand.parseCommand(main, sender, label, args);
                    break;
                case "info":
                    infoSubcommand.parseCommand(main, sender, label, args);
                    break;
                case "kill":
                    killSubcommand.parseCommand(main, sender, label, args);
                    break;
                case "reload":
                    reloadSubcommand.parseCommand(main, sender, label, args);
                    break;
                case "rules":
                    rulesSubcommand.parseCommand(main, sender, label, args);
                    break;
                case "spawner":
                    spawnerSubcommand.parseCommand(main, sender, label, args);
                    break;
                case "summon":
                    summonSubcommand.parseCommand(main, sender, label, args);
                    break;
                default:
                    sendInvalidUsage(sender, label);
                    break;
            }
        }

        return true;
    }

    /**
     * If a CommandSender specifies an invalid argument or
     * invalid amount of arguments then this will be sent
     * (only for the base command, not subcommands, which
     * handle their own usage)
     * @param sender who sent the command
     * @param label alias used to run the command
     */
    private void sendInvalidUsage(final CommandSender sender, final String label) {
        //TODO Complete method body.
        sender.sendMessage("Invalid usage, label: " + label);
    }

    private final List<String> subcommandsList = Arrays.asList(
            "advanced",
            "compatibility",
            "info",
            "kill",
            "reload",
            "rules",
            "spawner",
            "summon"
    );

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        // TODO Test

        if(args.length == 0) {
            return new ArrayList<>();
        } else if(args.length == 1) {
            return subcommandsList;
        } else {
            switch(args[0].toLowerCase(Locale.ROOT)) {
                case "advanced":
                    return advancedSubcommand.parseTabCompletions(main, sender, label, args);
                case "compatibility":
                    return compatibilitySubcommand.parseTabCompletions(main, sender, label, args);
                case "info":
                    return infoSubcommand.parseTabCompletions(main, sender, label, args);
                case "kill":
                    return killSubcommand.parseTabCompletions(main, sender, label, args);
                case "reload":
                    return reloadSubcommand.parseTabCompletions(main, sender, label, args);
                case "rules":
                    return rulesSubcommand.parseTabCompletions(main, sender, label, args);
                case "spawner":
                    return spawnerSubcommand.parseTabCompletions(main, sender, label, args);
                case "summon":
                    return summonSubcommand.parseTabCompletions(main, sender, label, args);
                default:
                    return new ArrayList<>();
            }
        }
    }
}
