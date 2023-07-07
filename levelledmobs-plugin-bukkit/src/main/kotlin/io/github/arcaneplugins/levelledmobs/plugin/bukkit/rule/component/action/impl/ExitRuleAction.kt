package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.ExitRuleException
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context

class ExitRuleAction(
    rule: Rule
) : Action(
    id = "exit-rule",
    rule = rule
) {

    override fun call(context: Context) {
        throw ExitRuleException(recursive = false)
    }

}