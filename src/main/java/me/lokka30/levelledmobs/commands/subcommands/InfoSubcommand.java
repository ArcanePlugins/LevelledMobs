/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Shows LevelledMobs information such as the version number
 *
 * @author lokka30
 * @since v2.0.0
 */
public class InfoSubcommand implements Subcommand {

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender, final String label, final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.info")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length == 1) {
            final String version = main.getDescription().getVersion();
            final String description = main.getDescription().getDescription();
            assert description != null;
            final List<String> supportedVersions = Utils.getSupportedServerVersions();
            final List<String> codeContributors = Arrays.asList("stumper66", "Eyrian", "iCodinqs", "deiphiz", "CoolBoy", "Esophose",
                    "7smile7", "UltimaOath", "konsolas", "Shevchik", "Hugo5551", "limzikiki", "bStats Project", "SpigotMC Project");
            final String listSeparator = Objects.requireNonNull(main.messagesCfg.getString("command.levelledmobs.info.listSeparator"), "messages.yml: command.levelledmobs.info.listSeparator is undefined");

            List<String> aboutMsg = main.messagesCfg.getStringList("command.levelledmobs.info.about");
            aboutMsg = Utils.replaceAllInList(aboutMsg, "%version%", version);
            aboutMsg = Utils.replaceAllInList(aboutMsg, "%description%", description);
            aboutMsg = Utils.replaceAllInList(aboutMsg, "%supportedVersions%", String.join(listSeparator, supportedVersions));
            aboutMsg = Utils.replaceAllInList(aboutMsg, "%contributors%", String.join(listSeparator, codeContributors));
            aboutMsg = Utils.colorizeAllInList(aboutMsg);
            aboutMsg.forEach(sender::sendMessage);
        } else {
            List<String> usageMsg = main.messagesCfg.getStringList("command.levelledmobs.info.usage");
            usageMsg = Utils.replaceAllInList(usageMsg, "%prefix%", main.configUtils.getPrefix());
            usageMsg = Utils.replaceAllInList(usageMsg, "%label%", label);
            usageMsg = Utils.colorizeAllInList(usageMsg);
            usageMsg.forEach(sender::sendMessage);
        }
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {
        // This subcommand has no tab completions.
        return Collections.emptyList();
    }
}
