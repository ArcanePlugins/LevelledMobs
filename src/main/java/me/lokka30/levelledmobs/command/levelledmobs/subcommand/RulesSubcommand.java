/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.command.levelledmobs.subcommand;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.command.CommandHandler;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author lokka30
 * @since v4.0.0
 * This is a subcommand of the '/levelledmobs' command.
 * This subcommand allows users to manage LevelledMobs' rules configuration.
 * @see me.lokka30.levelledmobs.command.levelledmobs.LevelledMobsCommand
 * @see CommandHandler
 */
public class RulesSubcommand implements CommandHandler.Subcommand {

    /*
    TODO LIST:
        - Update the getUsage method body.
        - Complete the run method body.
        - Complete the getSuggestions method body.
        - Test if the run method works properly.
        - Test if the getSuggestions method works properly.
        - Add customisable messages to the run method.
        - Test if the customisable messages work properly.
     */

    @Override
    public @NotNull String getMainLabel() {
        return "rules";
    }

    final HashSet<String> labels = new HashSet<>(Set.of("RULES", "RULE"));
    @Override
    public @NotNull HashSet<String> getLabels() {
        return labels;
    }

    @Override
    public @NotNull String getUsage() {
        return ""; //TODO
    }

    @Override
    public void run(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String baseCommandLabel, @NotNull String subCommandLabel, @NotNull String[] args) {
        //TODO
        sender.sendMessage("The rules subcommand is work-in-progress.");
    }

    @Override
    public @NotNull List<String> getSuggestions(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String baseCommandLabel, @NotNull String subCommandLabel, @NotNull String[] args) {
        //TODO
        return Collections.emptyList();
    }
}
