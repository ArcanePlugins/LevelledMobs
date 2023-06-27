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

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.Rule
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.action.Action
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import org.bukkit.entity.Player

//todo document
class DebugAction(
    rule: Rule,
) : Action(
    id = "debug",
    rule = rule
) {

    override fun call(
        context: Context
    ) {
        context
            .entity!!
            .getNearbyEntities(15.0, 15.0, 15.0)
            .filterIsInstance<Player>()
            .filter { it.isOp }
            .forEach {
                it.sendMessage(
                    "LM Debug Message; ruleId=${rule.id}; entityType=${context.entity!!.type}"
                )
            }
    }

}