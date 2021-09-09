/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.levelledmobs.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.CommandHandler;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lokka30
 * @since v4.0.0
 * This is a subcommand of the '/levelledmobs' command.
 * This subcommand provides various functions for LM developers or highly advanced LM users.
 * @see me.lokka30.levelledmobs.commands.levelledmobs.LevelledMobsCommand
 * @see CommandHandler
 */
public class AdvancedSubcommand implements CommandHandler.Subcommand {

    @Override
    public void parseCommand(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        /*
        TODO
            lokka30: Complete method body
         */
        sender.sendMessage("Advanced.work in progress");
    }

    @Override
    public @NotNull List<String> parseTabCompletions(@NotNull LevelledMobs main, @NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        /*
        TODO
            lokka30: Complete method body
         */

        return new ArrayList<>();
    }
}
