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
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule.*
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.DescriptiveException
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.TimeUtil
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl.DebugAction
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.condition.Condition
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.trigger.LmTrigger
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.trigger.Trigger
import org.bukkit.Bukkit
import org.spongepowered.configurate.CommentedConfigurationNode
import java.util.function.BiFunction

//todo doc
class RuleManager {

    //todo doc
    val rules: MutableList<Rule> = mutableListOf()

    //todo doc
    val conditionHandlers: MutableMap<String, BiFunction<Rule, CommentedConfigurationNode, Condition>> =
        mutableMapOf(
            // TODO add conditions in here.
            // <condition-id> to BiFunction { rule, node -> ConditionImplementation(rule, node) },
        )

    //todo doc
    val actionHandlers: MutableMap<String, BiFunction<Rule, CommentedConfigurationNode, Action>> =
        mutableMapOf(
            "debug" to BiFunction { rule, _ -> DebugAction(rule) },
        )

    //todo doc
    val triggers: MutableSet<Trigger> = LmTrigger.values().toMutableSet()

    //todo doc
    fun load() {
        val ruleNodes: List<CommentedConfigurationNode> = lmInstance.configManager.rules.rootNode
            .node("rules")
            .childrenList()

        rules.addAll(ruleNodes.mapNotNull { parseRuleAtNode(it) })
    }

    //todo doc
    private fun parseRuleAtNode(
        node: CommentedConfigurationNode,
    ): Rule? {
        /* assemble initial rule object with configured ID value */
        val rule = Rule(id = node.node("rule").string!!.lowercase())

        /* make sure there are no duplicate rule IDs */
        if (rules.any { it.id == rule.id }) {
            throw DescriptiveException(
                "Rules must be given unique ID values, but found at least 2 rules with the ID '${rule.id}'"
            )
        }

        /* call the pre-parse event */
        val preParseEvent = RulePreParseEvent(rule)
        Bukkit.getPluginManager().callEvent(preParseEvent)
        if (preParseEvent.isCancelled)
            return null

        /* parse triggers in rule config */
        val triggerIds: List<String> = node
            .node("triggers")
            .getList(String::class.java) ?: emptyList()

        triggers.addAll(triggerIds.mapNotNull { parseTrigger(it, rule) })

        /* parse 'if' conditions in rule config */
        val conditionNodes: List<CommentedConfigurationNode> = node
            .node("if")
            .childrenList()

        rule.conditions.addAll(conditionNodes.mapNotNull { parseConditionAtNode(rule, it) })

        /* parse 'do' actions in rule config */
        val doActionNodes: List<CommentedConfigurationNode> = node
            .node("do")
            .childrenList()

        rule.actions.addAll(doActionNodes.mapNotNull { parseActionAtNode(rule, it) })

        /* parse 'else' actions in rule config */
        val elseActionNodes: List<CommentedConfigurationNode> = node
            .node("else")
            .childrenList()

        rule.elseActions.addAll(elseActionNodes.mapNotNull { parseActionAtNode(rule, it) })

        /* parse the delay in rule config */
        rule.delayTicks = TimeUtil.parseDelayAtConfigNode(node.node("delay"))

        /* call 'rule parsed' event and return the parsed rule */
        Bukkit.getPluginManager().callEvent(RulePostParseEvent(rule))

        /* done */
        return rule
    }

    //todo doc
    private fun parseTrigger(
        id: String,
        rule: Rule,
    ): Trigger? {
        /* locate trigger by id */

        val trigger: Trigger = triggers
            // find trigger with the given id
            .firstOrNull { it.id() == id }
            // if trigger is null, no trigger was found with the given id; throw exception
            ?: throw DescriptiveException("For rule '${rule.id}', there is no Trigger available with the ID '${id}'; please check for spelling mistakes")

        /* pre parse event */

        val preParse = TriggerPreParseEvent(trigger, rule)
        Bukkit.getPluginManager().callEvent(preParse)
        if (preParse.isCancelled) return null

        /* post parse event */

        val postParse = TriggerPostParseEvent(trigger, rule)
        Bukkit.getPluginManager().callEvent(postParse)

        return trigger
    }

    //todo doc
    private fun parseActionAtNode(
        rule: Rule,
        node: CommentedConfigurationNode,
    ): Action? {
        val id: String = node.node("action").string!!.lowercase()

        if (!actionHandlers.containsKey(id)) {
            throw DescriptiveException("Rule '${rule.id}' declares an action with the ID '${id}' which doesn't exist; please check for spelling mistakes")
        }

        /* parse action with id */

        val action: Action = actionHandlers[id]!!.apply(rule, node)

        /* call pre-parse event */

        val preParse = ActionPreParseEvent(action)
        Bukkit.getPluginManager().callEvent(preParse)
        if (preParse.isCancelled) return null

        /* call post-parse event */

        val postParse = ActionPostParseEvent(action)
        Bukkit.getPluginManager().callEvent(postParse)

        return action
    }

    //todo doc
    private fun parseConditionAtNode(
        rule: Rule,
        node: CommentedConfigurationNode,
    ): Condition? {
        val id: String = node.node("condition").string!!.lowercase()

        if (!conditionHandlers.containsKey(id)) {
            throw DescriptiveException("Rule '${rule.id}' declares a condition with the ID '${id}' which doesn't exist; please check for spelling mistakes")
        }

        /* parse action with id */

        val condition: Condition = conditionHandlers[id]!!.apply(rule, node)

        /* call pre-parse event */

        val preParse = ConditionPreParseEvent(condition)
        Bukkit.getPluginManager().callEvent(preParse)
        if (preParse.isCancelled) return null

        /* call post-parse event */

        val postParse = ConditionPostParseEvent(condition)
        Bukkit.getPluginManager().callEvent(postParse)

        return condition
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