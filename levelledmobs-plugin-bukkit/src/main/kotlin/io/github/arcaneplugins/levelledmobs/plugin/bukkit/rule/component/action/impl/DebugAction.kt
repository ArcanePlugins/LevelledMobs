package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context

class DebugAction(
    val rule: Rule
) : Action(id = "debug", rule = rule) {

    override fun call(
        context: Context
    ) {
        requireNotNull(context.player)

        context.player!!.sendMessage(
            "This is a debug message from LevelledMobs! This was called from rule ID '${rule.id}'. :)"
        )
    }

}