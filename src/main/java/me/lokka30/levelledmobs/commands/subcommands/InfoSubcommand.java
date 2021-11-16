/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Shows LevelledMobs information such as the version number
 *
 * @author lokka30
 * @since v2.0.0
 */
public class InfoSubcommand extends MessagesBase implements Subcommand {
    public InfoSubcommand(final LevelledMobs main){
        super(main);
    }

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender, final String label, final String[] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.info")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length == 1) {
            final String version = main.getDescription().getVersion();
            final String description = main.getDescription().getDescription();
            assert description != null;
            final List<String> supportedVersions = Utils.getSupportedServerVersions();
            final List<String> codeContributors = List.of("stumper66", "Eyrian", "iCodinqs", "deiphiz", "CoolBoy", "Esophose",
                    "7smile7", "UltimaOath", "konsolas", "Shevchik", "Hugo5551", "limzikiki", "bStats Project", "SpigotMC Project");
            final String listSeparator = Objects.requireNonNull(main.messagesCfg.getString("command.levelledmobs.info.listSeparator"), "messages.yml: command.levelledmobs.info.listSeparator is undefined");

            showMessage("command.levelledmobs.info.about",
                    new String[]{ "%version%", "%description%", "%supportedVersions%", "%contributors%"},
                    new String[]{ version, description, String.join(listSeparator, supportedVersions), String.join(listSeparator, codeContributors) }
            );
        }
        else
            showMessage("command.levelledmobs.info.usage");
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender, final String[] args) {
        // This subcommand has no tab completions.
        return Collections.emptyList();
    }
}
