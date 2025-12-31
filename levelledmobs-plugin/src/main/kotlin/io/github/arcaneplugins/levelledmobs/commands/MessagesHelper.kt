package io.github.arcaneplugins.levelledmobs.commands

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.Utils
import java.util.function.Consumer
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender

object MessagesHelper {
    fun showMessage(
        sender: CommandSender,
        path: String
    ) {
        var messages = LevelledMobs.instance.messagesCfg.getStringList(path)
        messages = Utils.replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        val label = if (sender is ConsoleCommandSender) "lm" else "/lm"
        messages = Utils.replaceAllInList(messages, "%label%", label)
        messages = Utils.colorizeAllInList(messages)

        sendAllMessages(sender, messages)
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
            mutableListOf(replaceWhat),
            mutableListOf(replaceWith)
        )
    }

    fun showMessage(
        sender: CommandSender,
        path: String,
        replaceWhat: MutableList<String>,
        replaceWith: MutableList<String>
    ) {
        val messages = getMessage(path, replaceWhat, replaceWith)
        sendAllMessages(sender, messages)
    }

    private fun sendAllMessages(sender: CommandSender, messages: MutableList<String>){
        if (messages.size == 1)
            sender.sendMessage(messages.first())
        else{
            val sb = StringBuilder()
            messages.forEach(Consumer { s: String -> sb.append(s).append('\n') })
            sender.sendMessage(sb.toString())
        }
    }

    fun getMessage(
        path: String
    ): String {
        var messages = LevelledMobs.instance.messagesCfg.getStringList(path)
        messages = Utils.replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        messages = Utils.replaceAllInList(messages, "%label%", "")
        messages = Utils.colorizeAllInList(messages)

        if (messages.isEmpty()) return ""

        return if (messages.size == 1)
            messages.first()
        else
            messages.joinToString("\n")
    }

    fun getMessage(
        path: String,
        replaceWhat: String,
        replaceWith: String
    ): MutableList<String> {
        return getMessage(
            path,
            mutableListOf(replaceWhat),
            mutableListOf(replaceWith)
        )
    }

    fun getMessage(
        path: String,
        replaceWhat: MutableList<String>,
        replaceWith: MutableList<String>
    ): MutableList<String> {
        if (replaceWhat.size != replaceWith.size) {
            throw ArrayIndexOutOfBoundsException(
                "replaceWhat must be the same size as replaceWith"
            )
        }

        var messages = LevelledMobs.instance.messagesCfg.getStringList(path)
        messages = Utils.replaceAllInList(messages, "%prefix%", LevelledMobs.instance.configUtils.prefix)
        messages = Utils.replaceAllInList(
            messages, "%label%", "/lm"
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