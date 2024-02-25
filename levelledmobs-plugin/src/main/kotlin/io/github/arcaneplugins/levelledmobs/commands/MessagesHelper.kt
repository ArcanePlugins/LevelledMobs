package io.github.arcaneplugins.levelledmobs.commands

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.Utils
import java.util.function.Consumer
import org.bukkit.command.CommandSender

object MessagesHelper {
    fun showMessage(
        sender: CommandSender,
        path: String
    ) {
        var messages = LevelledMobs.instance.messagesCfg.getStringList(path)
        messages = Utils.replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        messages = Utils.replaceAllInList(messages, "%label%", "")
        messages = Utils.colorizeAllInList(messages)
        messages.forEach(Consumer { s: String -> sender.sendMessage(s) })
    }

    fun showMessage(
        sender: CommandSender,
        path: String,
        replaceWhat: String,
        replaceWith: String
    ) {
        return showMessage(
            sender,
            path,
            arrayOf(replaceWhat),
            arrayOf(replaceWith)
        )
    }

    fun showMessage(
        sender: CommandSender,
        path: String,
        replaceWhat: Array<String>,
        replaceWith: Array<String>
    ) {
        val messages = getMessage(path, replaceWhat, replaceWith)
        messages.forEach(Consumer { s: String -> sender.sendMessage(s) })
    }

    fun getMessage(
        path: String
    ): String {
        var messages = LevelledMobs.instance.messagesCfg.getStringList(path)
        messages = Utils.replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        messages = Utils.replaceAllInList(messages, "%label%", "")
        messages = Utils.colorizeAllInList(messages)

        if (messages.isEmpty()) {
            return ""
        }
        return if (messages.size == 1) {
            messages[0]
        } else {
            messages.joinToString("\n")
        }
    }

    fun getMessage(
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

    fun getMessage(
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
        messages = Utils.replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        messages = Utils.replaceAllInList(
            messages, "%label%", ""
        )

        for (i in replaceWhat.indices) {
            messages = Utils.replaceAllInList(
                messages, replaceWhat[i],
                replaceWith[i]
            )
        }

        messages = Utils.colorizeAllInList(messages)
        return messages
    }
}