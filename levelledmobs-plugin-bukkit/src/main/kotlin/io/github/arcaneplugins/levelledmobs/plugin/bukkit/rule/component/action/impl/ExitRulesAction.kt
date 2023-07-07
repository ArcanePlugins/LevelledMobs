package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.ExitRuleException
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context

class ExitRulesAction(
    rule: Rule
) : Action(
    id = "exit-rules",
    rule = rule
) {

    override fun call(context: Context) {
        throw ExitRuleException(recursive = true)
    }

}