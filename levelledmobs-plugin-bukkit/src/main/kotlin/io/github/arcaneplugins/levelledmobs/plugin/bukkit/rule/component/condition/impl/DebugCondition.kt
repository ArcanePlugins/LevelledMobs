package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.condition.impl

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.condition.Condition
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import org.bukkit.GameMode

class DebugCondition(id: String, rule: Rule) : Condition(id, rule) {
    override fun evaluate(context: Context): Boolean {
        return context.player!!.gameMode == GameMode.CREATIVE
    }
}