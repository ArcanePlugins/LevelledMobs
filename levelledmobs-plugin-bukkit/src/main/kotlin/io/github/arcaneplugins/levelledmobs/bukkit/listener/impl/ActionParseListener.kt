package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.ActionParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.AddDropTablesAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.AddNbtTagAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.BroadcastMessageToNearbyPlayersAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.BroadcastMessageToServerAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.BroadcastMessageToWorldAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.ExitAllAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.ExitFunctionAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.ExitProcessAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.RemoveDropTablesAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.RunFunctionAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.SetDeathLabelAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.SetDropTablesAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.SetPermanentLabelAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.TestAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.UpdateLabelsAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs.SetBuffsAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setlevel.SetLevelAction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setpacketlabel.SetPacketLabelAction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class ActionParseListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onActionParse(
        event: ActionParseEvent
    ){
        val process = event.process
        val node = event.node

        when (event.identifier.lowercase()) {
            "add-drop-tables" -> addAction(event, AddDropTablesAction(process, node))
            "add-nbt-tag" -> addAction(event, AddNbtTagAction(process, node))
            "broadcast-message-to-nearby-players" -> addAction(event, BroadcastMessageToNearbyPlayersAction(process, node))
            "broadcast-message-to-server" -> addAction(event, BroadcastMessageToServerAction(process, node))
            "broadcast-message-to-world" -> addAction(event, BroadcastMessageToWorldAction(process, node))
            "exit-all" -> addAction(event, ExitAllAction(process, node))
            "exit-function" -> addAction(event, ExitFunctionAction(process, node))
            "exit-process" -> addAction(event, ExitProcessAction(process, node))
            "remove-drop-tables" -> addAction(event, RemoveDropTablesAction(process, node))
            "run-function" -> addAction(event, RunFunctionAction(process, node))
            "set-buffs" -> addAction(event, SetBuffsAction(process, node))
            "set-death-label" -> addAction(event, SetDeathLabelAction(process, node))
            "set-drop-tables" -> addAction(event, SetDropTablesAction(process, node))
            "set-level" -> addAction(event, SetLevelAction(process, node))
            "set-packet-label" -> addAction(event, SetPacketLabelAction(process, node))
            "set-permanent-label" -> addAction(event, SetPermanentLabelAction(process, node))
            "test" -> addAction(event, TestAction(process, node))
            "update-labels" -> addAction(event, UpdateLabelsAction(process, node))
        }
    }

    private fun addAction(
        event: ActionParseEvent,
        action: Action
    ) {
        event.process.actions.add(action)
        event.claimed = true
    }
}