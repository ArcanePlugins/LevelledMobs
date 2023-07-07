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
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings.debug.DebugCategory.RULE_MANAGER
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.event.rule.*
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.DescriptiveException
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.Log.debug
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.TimeUtil
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl.DebugAction
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl.ExitRuleAction
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.impl.ExitRulesAction
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.condition.Condition
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.condition.impl.DebugCondition
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.trigger.Trigger
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.trigger.impl.LmTrigger
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
            "debug" to BiFunction { rule, _ -> DebugCondition(rule) },
        )

    //todo doc
    val actionHandlers: MutableMap<String, BiFunction<Rule, CommentedConfigurationNode, Action>> =
        mutableMapOf(
            "debug" to BiFunction { rule, _ -> DebugAction(rule) },
            "exit-rule" to BiFunction { rule, _ -> ExitRuleAction(rule) },
            "exit-rules" to BiFunction { rule, _ -> ExitRulesAction(rule) },
        )

    //todo doc
    val triggers: MutableSet<Trigger> = LmTrigger.values().toMutableSet()

    //todo doc
    fun load() {
        debug(RULE_MANAGER) { "RuleManager#load START" }
        debug(RULE_MANAGER) { "Fetching rule nodes from rules.yml" }
        val ruleNodes: List<CommentedConfigurationNode> = lmInstance.configManager.rules.rootNode
            .node("rules")
            .childrenList()

        debug(RULE_MANAGER) { "Parsing each node" }
        rules.addAll(ruleNodes.mapNotNull { parseRuleAtNode(it) })

        debug(RULE_MANAGER) {
            "Parsed the following rules: ${rules.joinToString { it.id }}"
        }

        debug(RULE_MANAGER) { "RuleManager#load DONE" }
    }

    //todo doc
    private fun parseRuleAtNode(
        node: CommentedConfigurationNode,
    ): Rule? {
        debug(RULE_MANAGER) { "RuleManager#parseRuleAtNode START" }
        debug(RULE_MANAGER) { "Node being parsed: ${node.path()}" }

        /* assemble initial rule object with configured ID value */
        val rule = Rule(id = node.node("rule").string!!.lowercase())
        debug(RULE_MANAGER) { "Instantiated rule object with id=${rule.id}" }

        /* make sure there are no duplicate rule IDs */
        debug(RULE_MANAGER) { "Checking for duplicate rule ID" }
        if (rules.any { it.id == rule.id }) {
            throw DescriptiveException(
                "Rules must be given unique ID values, but found at least 2 rules with the ID '${rule.id}'"
            )
        }
        debug(RULE_MANAGER) { "No duplicate rule ID found" }

        /* call the pre-parse event */
        debug(RULE_MANAGER) { "Calling RulePreParseEvent" }
        val preParseEvent = RulePreParseEvent(rule)
        Bukkit.getPluginManager().callEvent(preParseEvent)
        debug(RULE_MANAGER) { "Called RulePreParseEvent" }
        if (preParseEvent.isCancelled) {
            debug(RULE_MANAGER) { "RulePreParseEvent was cancelled, returning null rule" }
            return null
        }
        debug(RULE_MANAGER) { "RulePreParseEvent was not cancelled; continuing" }

        /* parse triggers in rule config */
        debug(RULE_MANAGER) { "Fetching rule's triggers from rules.yml" }
        val triggerIds: List<String> = node
            .node("triggers")
            .getList(String::class.java) ?: emptyList()
        debug(RULE_MANAGER) { "Fetched the following triggers: $triggerIds" }
        debug(RULE_MANAGER) { "Feeding those triggers into parseTrigger" }
        rule.triggers.addAll(triggerIds.mapNotNull { parseTrigger(it, rule) })
        debug(RULE_MANAGER) {
            "The following triggers were parsed: ${rule.triggers.joinToString {it.id()}}"
        }

        /* parse 'if' conditions in rule config */
        debug(RULE_MANAGER) { "Fetching condition nodes" }
        val conditionNodes: List<CommentedConfigurationNode> = node
            .node("if")
            .childrenList()
        debug(RULE_MANAGER) { "Fetched ${conditionNodes.size} condition nodes" }
        debug(RULE_MANAGER) { "Parsing condition nodes" }
        rule.conditions.addAll(conditionNodes.mapNotNull { parseConditionAtNode(rule, it) })
        debug(RULE_MANAGER) {
            "Parsed the following conditions: ${rule.conditions.joinToString { it.id }}"
        }

        /* parse 'do' actions in rule config */
        debug(RULE_MANAGER) { "Fetching actions from config" }
        val doActionNodes: List<CommentedConfigurationNode> = node
            .node("do")
            .childrenList()
        debug(RULE_MANAGER) { "Fetched ${doActionNodes.size} action nodes" }
        debug(RULE_MANAGER) { "Parsing action nodes" }
        rule.actions.addAll(doActionNodes.mapNotNull { parseActionAtNode(rule, it) })
        debug(RULE_MANAGER) {
            "Parsed the following action ids: ${rule.actions.joinToString { it.id }}"
        }

        /* parse 'else' actions in rule config */
        debug(RULE_MANAGER) { "Fetching else-actions from config" }
        val elseActionNodes: List<CommentedConfigurationNode> = node
            .node("else")
            .childrenList()
        debug(RULE_MANAGER) { "Fetched ${elseActionNodes.size} else-actions from config" }
        debug(RULE_MANAGER) { "Parsing else-actions" }
        rule.elseActions.addAll(elseActionNodes.mapNotNull { parseActionAtNode(rule, it) })
        debug(RULE_MANAGER) {
            "Parsed the following else-action ids: ${rule.elseActions.joinToString { it.id }}"
        }

        /* parse the delay in rule config */
        debug(RULE_MANAGER) { "Parsing time delay (ticks)" }
        rule.delayTicks = TimeUtil.parseDelayAtConfigNode(node.node("delay"))
        debug(RULE_MANAGER) { "Parsed delay of ${rule.delayTicks}" }

        /* call 'rule parsed' event and return the parsed rule */
        debug(RULE_MANAGER) { "Calling RulePostParseEvent" }
        Bukkit.getPluginManager().callEvent(RulePostParseEvent(rule))
        debug(RULE_MANAGER) { "RuleManager#parseRuleAtNode DONE" }

        /* done */
        return rule
    }

    //todo doc
    private fun parseTrigger(
        id: String,
        rule: Rule,
    ): Trigger? {
        debug(RULE_MANAGER) { "RuleManager#parseTrigger START" }
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

        debug(RULE_MANAGER) { "RuleManager#parseTrigger DONE" }
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

    //todo doc
    fun callRulesWithTrigger(
        trigger: Trigger,
        context: Context,
    ) {
        debug(RULE_MANAGER) {
            "callRulesWithTrigger START; trigger=${trigger.id()}; rulesToFilter=${rules.size}"
        }
        debug(RULE_MANAGER) {
            "Rules-Triggers overview:\n" +
                    rules.joinToString { rule ->
                        rule.id + ": " + rule.triggers.joinToString { it.id() }
                    }
        }
        rules
            .filter {
                trigger.id() in it.triggers.map { otherTrigger -> otherTrigger.id() }
            }
            .forEach {
                debug(RULE_MANAGER) { "callRulesWithTrigger: calling rule: ${it.id}" }
                it.call(context)
            }
    }

    //todo doc
    fun getRuleById(
        id: String
    ): Rule {
        return rules.first { it.id.equals(id, ignoreCase = true) }
    }

}