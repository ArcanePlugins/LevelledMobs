/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.command.levelledmobs.subcommand;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.command.CommandHandler;
import me.lokka30.levelledmobs.command.levelledmobs.LevelledMobsCommand;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author lokka30
 * @since v4.0.0
 * This is a subcommand of the '/levelledmobs' command.
 * This subcommand provides a list of available subcommands.
 * @see me.lokka30.levelledmobs.command.levelledmobs.LevelledMobsCommand
 * @see CommandHandler
 */
public class HelpSubcommand implements CommandHandler.Subcommand {

    /*
    TODO LIST:
        - Test if the run method works properly.
        - Add customisable messages to the run method.
        - Test if the customisable messages work properly.
     */

    final HashSet<String> labels = new HashSet<>(Set.of("HELP", "COMMANDS", "SUBCOMMANDS"));
    @Override
    public @NotNull HashSet<String> getLabels() {
        return labels;
    }

    @Override
    public @NotNull String getMainLabel() {
        return "help";
    }

    @Override
    public void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String baseCommandLabel, @NotNull String subCommandLabel, @NotNull String[] args) {
        sender.sendMessage("Available subcommands:");

        for(CommandHandler.Subcommand availableSubcommand : LevelledMobsCommand.subcommands) {
            sender.sendMessage(" -> /" + baseCommandLabel + " " + availableSubcommand.getMainLabel() + " " + availableSubcommand.getUsage());
        }
    }
}
