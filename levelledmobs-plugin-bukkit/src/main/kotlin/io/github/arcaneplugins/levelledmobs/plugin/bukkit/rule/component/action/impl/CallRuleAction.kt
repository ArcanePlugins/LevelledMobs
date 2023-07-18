/*
This program is/was a part of the LevelledMobs project's source code.
Copyright (C) 2023  Lachlan Adamson (aka lokka30)
Copyright (C) 2023  LevelledMobs Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

    lateinit var ruleIdToCall: String
        private set

    constructor(
        callerRule: Rule,
        node: CommentedConfigurationNode
    ) : this(callerRule) {
        this.ruleIdToCall = node.node("rule-to-call").string!!
    }

    constructor(
        callerRule: Rule,
        ruleToCall: Rule
    ) : this(callerRule) {
        this.ruleIdToCall = ruleToCall.id
    }

    override fun call(
        context: Context
    ) {
        rule.callOtherRule(
            ruleToCall = lmInstance.ruleManager.getRuleById(ruleIdToCall),
            context = context
        )
    }

}
