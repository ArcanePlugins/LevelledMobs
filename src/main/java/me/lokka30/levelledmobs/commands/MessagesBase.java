package me.lokka30.levelledmobs.commands;

import java.util.List;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Provides common functions for showing messages stored in messages.yml to the user
 *
 * @author stumper66
 * @since 3.3.0
 */
public class MessagesBase {

    public MessagesBase(final LevelledMobs main) {
        this.main = main;
    }

    protected final LevelledMobs main;
    protected String messageLabel;
    protected CommandSender commandSender;

    protected void showMessage(final @NotNull String path) {
        if (commandSender == null) {
            throw new NullPointerException("CommandSender must be set before calling showMessage");
        }

        showMessage(path, commandSender);
    }

    protected void showMessage(final @NotNull String path, final @NotNull CommandSender sender) {
        List<String> messages = main.messagesCfg.getStringList(path);
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%",
            messageLabel != null ? messageLabel : "");
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(sender::sendMessage);
    }

    protected void showMessage(
        final @NotNull String path,
        final @NotNull String replaceWhat,
        final @NotNull String replaceWith
    ) {
        showMessage(
            path,
            new String[]{replaceWhat},
            new String[]{replaceWith}
        );
    }

    protected void showMessage(final @NotNull String path, final String @NotNull [] replaceWhat,
        final String @NotNull [] replaceWith) {
        if (commandSender == null) {
            throw new NullPointerException("CommandSender must be set before calling showMessage");
        }

        final List<String> messages = getMessage(path, replaceWhat, replaceWith);
        messages.forEach(commandSender::sendMessage);
    }

    protected void showMessage(final @NotNull String path, final String @NotNull [] replaceWhat,
        final String @NotNull [] replaceWith, final @NotNull CommandSender sender) {
        final List<String> messages = getMessage(path, replaceWhat, replaceWith);
        messages.forEach(sender::sendMessage);
    }

    @NotNull
    protected String getMessage(final @NotNull String path) {
        List<String> messages = main.messagesCfg.getStringList(path);
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%",
            messageLabel != null ? messageLabel : "");
        messages = Utils.colorizeAllInList(messages);

        if (messages.isEmpty()) {
            return "";
        }
        if (messages.size() == 1) {
            return messages.get(0);
        } else {
            return String.join("\n", messages);
        }
    }

    @NotNull
    protected List<String> getMessage(final @NotNull String path, final @NotNull String replaceWhat,
        final @NotNull String replaceWith) {
        return getMessage(
            path,
            new String[]{replaceWhat},
            new String[]{replaceWith}
        );
    }

    @NotNull
    protected List<String> getMessage(final @NotNull String path,
        final String @NotNull [] replaceWhat, final String @NotNull [] replaceWith) {
        if (replaceWhat.length != replaceWith.length) {
            throw new ArrayIndexOutOfBoundsException(
                "replaceWhat must be the same size as replaceWith");
        }

        List<String> messages = main.messagesCfg.getStringList(path);
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%label%",
            messageLabel != null ? messageLabel : "");

        for (int i = 0; i < replaceWhat.length; i++) {
            messages = Utils.replaceAllInList(messages, replaceWhat[i], replaceWith[i]);
        }

        messages = Utils.colorizeAllInList(messages);
        return messages;
    }
}
