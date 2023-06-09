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

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule.RuleParseEvent
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule.RuleParsedEvent
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
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
        TODO("Not yet implemented")
    }

    //todo use
    //todo doc
    private fun parseRuleAtNode(
        node: CommentedConfigurationNode,
    ): Rule? {
        val rule = Rule(id = node.node("rule").string!!)

        val preParseEvent = RuleParseEvent(rule)
        Bukkit.getPluginManager().callEvent(preParseEvent)
        if (preParseEvent.isCancelled)
            return null

        val triggerIds: List<String> = node
            .node("triggers")
            .getList(String::class.java) ?: emptyList()

        triggerIds.forEach { triggerId -> rule.triggers.add(parseTrigger(triggerId)) }

        //todo parse conditions

        //todo parse actions

        //todo parse delay

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
    ): Action {
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