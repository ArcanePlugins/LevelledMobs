package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs.Companion.lmInstance
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import org.spongepowered.configurate.CommentedConfigurationNode

class CallRuleAction private constructor(
    rule: Rule
) : Action(
    id = "call-rule",
    rule = rule
) {

    lateinit var ruleToCall: Rule
        private set

    constructor(
        callerRule: Rule,
        node: CommentedConfigurationNode
    ) : this(callerRule) {
        this.ruleToCall = lmInstance.ruleManager.getRuleById(
            node.node("rule-to-call").string!!
        )
    }

    constructor(
        callerRule: Rule,
        ruleToCall: Rule
    ) : this(callerRule) {
        this.ruleToCall = ruleToCall
    }

    override fun call(
        context: Context
    ) {
        ruleToCall.callOtherRule(
            ruleToCall = ruleToCall,
            context = context
        )
    }

}