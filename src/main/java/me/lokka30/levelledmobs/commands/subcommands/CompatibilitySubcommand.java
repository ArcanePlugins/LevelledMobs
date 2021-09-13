/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Shows if your server has known compatibility issues with LevelledMobs
 *
 * @author lokka30
 * @since 2.4.0
 */
public class CompatibilitySubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender, final String label, final String[] args) {
        if (sender.hasPermission("levelledmobs.command.compatibility")) {
            if (args.length == 1) {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.compatibility.notice");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
                main.companion.checkCompatibility();
            } else {
                List<String> messages = main.messagesCfg.getStringList("command.levelledmobs.compatibility.usage");
                messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
                messages = Utils.replaceAllInList(messages, "%label%", label);
                messages = Utils.colorizeAllInList(messages);
                messages.forEach(sender::sendMessage);
            }
        } else {
            main.configUtils.sendNoPermissionMsg(sender);
        }
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {
        // This subcommand has no tab completions.
        return Collections.emptyList();
    }
}
