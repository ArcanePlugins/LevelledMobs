/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Reloads all LevelledMobs configuration from disk
 *
 * @author lokka30
 * @since 2.0
 */
public class ReloadSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, final @NotNull CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.reload")){
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        main.reloadLM(sender);
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {
        return Collections.emptyList(); //No tab completions.
    }
}
