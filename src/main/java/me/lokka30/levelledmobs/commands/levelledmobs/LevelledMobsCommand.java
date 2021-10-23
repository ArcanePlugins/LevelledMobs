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

import java.util.*;

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

        // create subcommands set
        subcommands.addAll(
                Arrays.asList(
                        new AdvancedSubcommand(),
                        new CompatibilitySubcommand(),
                        new HelpSubcommand(),
                        new InfoSubcommand(),
                        new KillSubcommand(),
                        new ReloadSubcommand(),
                        new RulesSubcommand(),
                        new SpawnerSubcommand(),
                        new SummonSubcommand()
                )
        );

        // create subcommands labels list
        subcommands.forEach(subcommand -> subcommandsLabels.addAll(subcommand.getLabels()));
    }

    private final HashSet<CommandHandler.Subcommand> subcommands = new HashSet<>();
    private final List<String> subcommandsLabels = new ArrayList<>();
    //TODO make help subcommand.

    @Nullable
    public CommandHandler.Subcommand getSubcommand(@NotNull final String label) {
        for(CommandHandler.Subcommand subcommand : subcommands) {
            if(subcommand.getLabels().contains(label.toUpperCase(Locale.ROOT))) {
                return subcommand;
            }
        }
        return null;
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        if(CommandHandler.CommandUtils.senderDoesNotHaveRequiredPermission(main, sender, "levelledmobs.command.levelledmobs")) return true;

        if(args.length == 0) {
            sender.sendMessage("Invalid usage - please specify a subcommand. For a list of available subcommands, try '/" + label + " help'.");
            return true;
        }

        final CommandHandler.Subcommand subcommand = getSubcommand(args[0]);
        if(subcommand == null) {
            sender.sendMessage("Invalud usage - the subcommand '" + args[0] + "' does not exist. For a list of available subcommands, try '/" + label + " help'.");
        } else {
            subcommand.run(main, sender, label, args);
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String label, @NotNull final String[] args) {
        // TODO Test

        if(args.length == 0) {
            return Collections.emptyList();
        } else if(args.length == 1) {
            return subcommandsLabels;
        } else {
            final CommandHandler.Subcommand subcommand = getSubcommand(args[0]);
            if(subcommand != null) {
                return subcommand.getSuggestions(main, sender, label, args);
            }
        }

        return Collections.emptyList();
    }
}
