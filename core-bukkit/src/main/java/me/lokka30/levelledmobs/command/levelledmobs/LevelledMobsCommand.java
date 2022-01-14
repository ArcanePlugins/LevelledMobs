/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.command.levelledmobs;

import me.lokka30.levelledmobs.command.CommandHandler;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.compatibility.CompatibilitySubcommand;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.execute.ExecuteSubcommand;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.help.HelpSubcommand;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.info.InfoSubcommand;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.kill.KillSubcommand;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.reload.ReloadSubcommand;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.rules.RulesSubcommand;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.spawner.SpawnerSubcommand;
import me.lokka30.levelledmobs.command.levelledmobs.subcommand.summon.SummonSubcommand;
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

    public LevelledMobsCommand() {

        // create subcommands set
        subcommands.addAll(
                Arrays.asList(
                        new ExecuteSubcommand(),
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

    public static final HashSet<CommandHandler.Subcommand> subcommands = new HashSet<>();

    public static final List<String> subcommandsLabels = new LinkedList<>();

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
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String baseCommandLabel, @NotNull final String[] args) {
        if(CommandHandler.CommandUtils.senderDoesNotHaveRequiredPermission(sender, "levelledmobs.command.levelledmobs")) return true;

        if(args.length == 0) {
            sender.sendMessage("Invalid usage - please specify a subcommand. For a list of available subcommands, try '/" + baseCommandLabel + " help'.");
            return true;
        }

        final CommandHandler.Subcommand subcommand = getSubcommand(args[0]);
        if(subcommand == null) {
            sender.sendMessage("Invalid usage - the subcommand '" + args[0] + "' does not exist. For a list of available subcommands, try '/" + baseCommandLabel + " help'.");
        } else {
            subcommand.run(sender, baseCommandLabel, args[0].toLowerCase(Locale.ROOT), args);
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command cmd, @NotNull final String baseCommandLabel, @NotNull final String @NotNull [] args) {
        if(args.length == 0) {
            return Collections.emptyList();
        } else if(args.length == 1) {
            return subcommandsLabels;
        } else {
            final CommandHandler.Subcommand subcommand = getSubcommand(args[0]);
            if(subcommand != null) {
                return subcommand.getSuggestions(sender, baseCommandLabel, args[0].toLowerCase(Locale.ROOT), args);
            }
        }

        return Collections.emptyList();
    }
}
