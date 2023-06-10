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
package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs.Companion.lmInstance
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule.RuleParseEvent
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule.RuleParsedEvent
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.DescriptiveException
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.condition.Condition
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.trigger.Trigger
import org.bukkit.Bukkit
import org.spongepowered.configurate.CommentedConfigurationNode

//todo doc
class RuleManager {

    val rules: MutableList<Rule> = mutableListOf()

    //todo use
    //todo doc
    fun load() {
        val ruleNodes: List<CommentedConfigurationNode> = lmInstance.configManager.rules.rootNode
            .node("rules")
            .childrenList()

        rules.addAll(ruleNodes.mapNotNull { parseRuleAtNode(it) })
    }

    //todo implementation incomplete
    //todo use
    //todo doc
    private fun parseRuleAtNode(
        node: CommentedConfigurationNode,
    ): Rule? {
        /* assemble initial rule object with configured ID value */

        val rule = Rule(id = node.node("rule").string!!.lowercase())

        if(rules.any { it.id == rule.id }) {
            throw DescriptiveException(
                "Rules must be given unique ID values, but found at least 2 rules with the ID '${rule.id}'"
            )
        }

        /* call the pre-parse event */

        val preParseEvent = RuleParseEvent(rule)
        Bukkit.getPluginManager().callEvent(preParseEvent)
        if (preParseEvent.isCancelled)
            return null

        /* parse triggers in rule config */

        val triggerIds: List<String> = node
            .node("triggers")
            .getList(String::class.java) ?: emptyList()

        triggerIds.forEach { rule.triggers.add(parseTrigger(it)) }

        /* parse 'if' conditions in rule config */

        val conditionNodes: List<CommentedConfigurationNode> = node
            .node("if")
            .childrenList()

        conditionNodes.forEach { rule.conditions.add(parseConditionAtNode(it)) }

        /* parse 'do' actions in rule config */

        val doActionNodes: List<CommentedConfigurationNode> = node
            .node("do")
            .childrenList()

        doActionNodes.forEach { rule.actions.add(parseActionAtNode(it)) }

        /* parse 'else' actions in rule config */

        val elseActionNodes: List<CommentedConfigurationNode> = node
            .node("else")
            .childrenList()

        elseActionNodes.forEach { rule.elseActions.add(parseActionAtNode(it)) }

        /* parse the delay in rule config */

        //todo parse delay
        // ...

        /* call 'rule parsed' event and return the parsed rule */

        Bukkit.getPluginManager().callEvent(RuleParsedEvent(rule))

        return rule
    }

    //todo use
    //todo doc
    private fun parseTrigger(
        id: String,
    ): Trigger {
        //todo use a map to map trigget IDs to their corresponding objects
        TODO("Not yet implemented; $id")
    }

    //todo use
    //todo doc
    private fun parseActionAtNode(
        node: CommentedConfigurationNode,
    ): Action {
        TODO("Not yet implemented; $node")
    }

    //todo use
    //todo doc
    private fun parseConditionAtNode(
        node: CommentedConfigurationNode,
    ): Condition {
        TODO("Not yet implemented; $node")
    }

    //todo use
    //todo doc
    fun callRulesWithTrigger(
        trigger: Trigger,
        context: Context,
    ) {
        rules.filter { trigger in it.triggers }.forEach { it.call(context) }
    }

}