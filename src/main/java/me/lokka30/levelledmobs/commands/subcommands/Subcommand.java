/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Provides the interface for parsing commands sent to LevelledMobs
 *
 * @author lokka30
 */
public interface Subcommand {

    void parseSubcommand(final LevelledMobs main, final CommandSender sender, final String label, final String[] args);

    List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args);
}
