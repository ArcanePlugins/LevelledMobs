package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context

//todo make the rulemanager parse this action
//todo document
class DebugAction(
    rule: Rule
) : Action(id = "debug", rule = rule) {

    override fun call(
        context: Context
    ) {
        context.player!!.sendMessage(
            "This is a debug message from LevelledMobs! This was called from rule ID '${rule.id}'. :)"
        )
    }

}