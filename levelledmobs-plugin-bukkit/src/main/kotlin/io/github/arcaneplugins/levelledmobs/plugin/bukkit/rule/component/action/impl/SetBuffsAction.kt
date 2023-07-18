package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import org.spongepowered.configurate.CommentedConfigurationNode

class SetBuffsAction private constructor(
    rule: Rule
) : Action(
    id = "set-buffs",
    rule = rule
) {

    //todo lateinit var ruleIdToCall: String
    //todo     private set

    constructor(
        callerRule: Rule,
        node: CommentedConfigurationNode
    ) : this(callerRule) {
        //todo
    }

    constructor(
        callerRule: Rule,
        ruleToCall: Rule
    ) : this(callerRule) {
        //todo
    }

    override fun call(
        context: Context
    ) {
        //todo
    }

}