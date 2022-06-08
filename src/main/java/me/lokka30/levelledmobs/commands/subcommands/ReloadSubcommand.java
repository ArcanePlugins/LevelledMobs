/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import java.util.Collections;
import java.util.List;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.FileLoader;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Reloads all LevelledMobs configuration from disk
 *
 * @author lokka30
 * @since 2.0
 */
public class ReloadSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, final @NotNull CommandSender sender,
        final String label, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.reload")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        main.reloadLM(sender);

        if (main.companion.getHadRulesLoadError() && sender instanceof Player) {
            sender.sendMessage(FileLoader.getFileLoadErrorMessage());
        }
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender,
        final String[] args) {
        return Collections.emptyList(); //No tab completions.
    }
}
