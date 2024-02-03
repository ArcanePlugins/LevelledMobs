package io.github.arcaneplugins.levelledmobs.commands

import java.util.function.Consumer
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.Utils.colorizeAllInList
import io.github.arcaneplugins.levelledmobs.util.Utils.replaceAllInList
import org.bukkit.command.CommandSender

/**
 * Provides common functions for showing messages stored in messages.yml to the user
 *
 * @author stumper66
 * @since 3.3.0
 */
open class MessagesBase{
    protected var messageLabel: String? = null
    protected var commandSender: CommandSender? = null

    protected fun showMessage(path: String) {
        if (commandSender == null) {
            throw NullPointerException("CommandSender must be set before calling showMessage")
        }

        showMessage(path, commandSender!!)
    }

    protected fun showMessage(
        path: String,
        sender: CommandSender
    ) {
        var messages = LevelledMobs.instance.messagesCfg.getStringList(path)
        messages = replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        messages = replaceAllInList(messages, "%label%", messageLabel)
        messages = colorizeAllInList(messages)
        messages.forEach(Consumer { s: String -> sender.sendMessage(s) })
    }

    protected fun showMessage(
        path: String,
        replaceWhat: String,
        replaceWith: String
    ) {
        showMessage(
            path,
            arrayOf(replaceWhat),
            arrayOf(replaceWith)
        )
    }

    protected fun showMessage(
        path: String,
        replaceWhat: Array<String>,
        replaceWith: Array<String>
    ) {
        if (commandSender == null) {
            throw java.lang.NullPointerException("CommandSender must be set before calling showMessage")
        }

        val messages = getMessage(path, replaceWhat, replaceWith)
        messages.forEach(Consumer { s: String -> commandSender!!.sendMessage(s) })
    }

    protected fun showMessage(
        path: String,
        replaceWhat: Array<String>,
        replaceWith: Array<String>,
        sender: CommandSender
    ) {
        val messages = getMessage(path, replaceWhat, replaceWith)
        messages.forEach(Consumer { s: String? -> sender.sendMessage(s!!) })
    }

    protected fun getMessage(
        path: String
    ): String {
        var messages = LevelledMobs.instance.messagesCfg.getStringList(path)
        messages = replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        messages = replaceAllInList(messages, "%label%", messageLabel)
        messages = colorizeAllInList(messages)

        if (messages.isEmpty()) {
            return ""
        }
        return if (messages.size == 1) {
            messages[0]
        } else {
            messages.joinToString("\n")
        }
    }

    protected fun getMessage(
        path: String,
        replaceWhat: String,
        replaceWith: String
    ): MutableList<String> {
        return getMessage(
            path,
            arrayOf(replaceWhat),
            arrayOf(replaceWith)
        )
    }

    protected fun getMessage(
        path: String,
        replaceWhat: Array<String>,
        replaceWith: Array<String>
    ): MutableList<String> {
        if (replaceWhat.size != replaceWith.size) {
            throw ArrayIndexOutOfBoundsException(
                "replaceWhat must be the same size as replaceWith"
            )
        }

        var messages = LevelledMobs.instance.messagesCfg.getStringList(path)
        messages = replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        messages = replaceAllInList(
            messages, "%label%",
            if (messageLabel != null) messageLabel else ""
        )

        for (i in replaceWhat.indices) {
            messages = replaceAllInList(
                messages, replaceWhat[i],
                replaceWith[i]
            )
        }

        messages = colorizeAllInList(messages)
        return messages
    }
}