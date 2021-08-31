/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.handlers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author lokka30
 * @see PluginCommand
 * @see TabExecutor
 * @see CommandUtils
 * @since v4.0.0
 * This class handles common methods
 * and command registration for
 * all LevelledMobs commands.
 */
public class CommandHandler {

    private final LevelledMobs main;

    public CommandHandler(final LevelledMobs main) {
        this.main = main;
    }

    /**
     * @param baseCommandLabel the main base label of the command, the one specified in plugin.yml
     * @param clazz            the TabExecutor class handling execution of the command
     * @author lokka30
     * @since v4.0.0
     * This method attempts to register a command with
     * the specifies base-label and TabExecutor class.
     */
    public void register(String baseCommandLabel, TabExecutor clazz) {
        final PluginCommand pluginCmd = main.getCommand(baseCommandLabel);

        if (pluginCmd == null) {
            Utils.LOGGER.error("&3CommandHandler: &7Unable to register command '&b/" + baseCommandLabel + "&7', as &bPluginCommand=null&7. Did you modify &bplugin.yml&7 incorrectly?");
        } else {
            pluginCmd.setExecutor(clazz);
            Utils.LOGGER.info("&3CommandHandler: &7Registered command '&b/" + baseCommandLabel + "&7'.");
        }
    }

    /**
     * @author lokka30
     * @see CommandHandler
     * @since v4.0.0
     * This is a static class that contains common methods
     * used across LevelledMobs' commands that make it easier
     * to accomplish repeated things, such as ensuring users have
     * the base permission, if not send them a no-permission message.
     */
    public static class CommandUtils {

        /**
         * If a CommandSender does not have the permission specified,
         * then this method will send them the 'no permission' message
         * and inform the Command that called this method whether the
         * CommandSender has the required permission or not.
         *
         * @param main       LevelledMobs main class
         * @param sender     CommandSender that executed the command
         * @param permission Permission the CommandSender requires to run the command
         * @return whether the CommandSender has the required permission or not
         * @see CommandSender#hasPermission(String)
         */
        public static boolean senderDoesNotHaveRequiredPermission(final LevelledMobs main, final CommandSender sender, final String permission) {
            if (sender.hasPermission(permission)) return false;

            //TODO send no permission message.
            sender.sendMessage("You lack the permission " + permission);

            return true;
        }
    }

    /**
     * @author lokka30
     * @since v4.0.0
     * This interface is implemented by all
     * subcommands used in the plugin.
     */
    public interface Subcommand {

        /**
         * @param main   main class of LevelledMobs
         * @param sender who executed the command
         * @param label  what alias the sender used to run the command
         * @param args   what arguments the sender specified with the command
         * @author lokka30
         * @since v4.0.0
         * Make the Subcommand run with the specified information.
         */
        void parseCommand(@NotNull final LevelledMobs main, @NotNull final CommandSender sender, @NotNull final String label, @NotNull final String[] args);

        /**
         * @param main   main class of LevelledMobs
         * @param sender who executed the command
         * @param label  what alias the sender used to run the command
         * @param args   what arguments the sender specified with the command
         * @author lokka30
         * @since v4.0.0
         * Make the Subcommand return a list of tab
         * completions with the specified information.
         * If no tab completions should be sent, do not
         * return `null`, just return `new ArrayList<>()`,
         * hence the @NonNull annotation.
         */
        @NotNull
        List<String> parseTabCompletions(@NotNull final LevelledMobs main, @NotNull final CommandSender sender, @NotNull final String label, @NotNull final String[] args);
    }
}