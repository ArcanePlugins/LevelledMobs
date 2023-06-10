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
package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs.Companion.lmInstance
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.condition.Condition
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.trigger.Trigger
import org.bukkit.Bukkit

//todo doc
class Rule(
    val id: String,
    val triggers: MutableSet<Trigger> = mutableSetOf(),
    val actions: MutableList<Action> = mutableListOf(),
    val elseActions: MutableList<Action> = mutableListOf(),
    val conditions: MutableList<Condition> = mutableListOf(),
    val delayTicks: Long = 0L,
) {

    //todo doc
    fun call(context: Context) {
        fun run() {
            if (conditions.all { !it.evaluate(context) }) {
                actions.forEach { it.call(context) }
            } else {
                elseActions.forEach { it.call(context) }
            }
        }

        if (delayTicks <= 0L) {
            run()
        } else {
            Bukkit.getScheduler().runTaskLater(lmInstance, ::run, delayTicks)
        }
    }

}