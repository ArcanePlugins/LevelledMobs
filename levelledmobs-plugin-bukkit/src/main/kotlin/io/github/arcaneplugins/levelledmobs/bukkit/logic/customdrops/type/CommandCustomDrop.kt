package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.cdevent.CustomDropsEventType
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable

class CommandCustomDrop(
    val commands: MutableList<String>,
    recipient: CustomDropRecipient
) : CustomDrop(StandardCustomDropType.COMMAND.name, recipient) {
    val commandRunEvents = mutableSetOf<String>()
    var commandDelay = 0L

    private fun execute(
        eventType: CustomDropsEventType,
        context: Context
    ){
        if (!commandRunEvents.contains(eventType.name)) return

        if (commandDelay == 0L) {
            executeImmediately(context)
        } else {
            object : BukkitRunnable() {
                override fun run() {
                    executeImmediately(context)
                }
            }.runTaskLater(LevelledMobs.lmInstance, commandDelay)
        }
    }

    private fun executeImmediately(
        context: Context
    ){
        for (command in commands) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                LogicHandler.replacePapiAndContextPlaceholders(command, context)
            )
        }
    }

    fun withCommandDelay(
        commandDelay: Long
    ): CustomDrop{
        this.commandDelay = commandDelay
        return this
    }

    fun withCommandRunEvents(
        runEvents: MutableList<String>
    ): CustomDrop{
        this.commandRunEvents.addAll(runEvents)
        return this
    }

    fun withCommandRunEvent(
        runEvent: String
    ): CustomDrop{
        this.commandRunEvents.add(runEvent)
        return this
    }
}