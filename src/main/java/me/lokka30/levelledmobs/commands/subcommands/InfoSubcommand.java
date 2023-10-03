/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import java.util.Collections;
import java.util.List;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Shows LevelledMobs information such as the version number
 *
 * @author lokka30
 * @since v2.0.0
 */
public class InfoSubcommand extends MessagesBase implements Subcommand {

    public InfoSubcommand(final LevelledMobs main) {
        super(main);
    }

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender,
        final String label, final String[] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.info")) {
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length == 1) {
            final String listSeparator = main.messagesCfg.getString("command.levelledmobs.info.listSeparator", "&7, &f");

            showMessage("command.levelledmobs.info.about",
                new String[]{
                    "%version%",
                    "%description%",
                    "%supportedVersions%",
                    "%maintainers%",
                    "%contributors%"},
                new String[]{
                    main.getDescription().getVersion(),
                    main.getDescription().getDescription(),
                    "1.16 - 1.20",
                    String.join(listSeparator, main.getDescription().getAuthors()),
                    "See &8&nhttps://github.com/lokka30/LevelledMobs/wiki/Credits"
                }
            );
        } else {
            showMessage("command.levelledmobs.info.usage");
        }
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final CommandSender sender,
        final String[] args) {
        // This subcommand has no tab completions.
        return Collections.emptyList();
    }
}
