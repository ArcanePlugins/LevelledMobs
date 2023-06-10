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
package io.github.arcaneplugins.levelledmobs.plugin.bukkit.listener.entity

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.LevelledMobs.Companion.lmInstance
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.trigger.LmTrigger
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntitySpawnEvent

//todo doc
class EntitySpawnListener : ListenerWrapper(imperative = true) {

    @EventHandler
    fun onEntitySpawn(event: EntitySpawnEvent) {
        /* handle context for this event */
        val context = Context(
            entity = event.entity,
            event = event,
            location = event.location,
            world = event.entity.world,
        )

        if (event.entity is LivingEntity)
            context.livingEntity = event.entity as LivingEntity

        /* handle triggers for this event */
        lmInstance.ruleManager.callRulesWithTrigger(
            trigger = LmTrigger.ON_ENTITY_SPAWN,
            context = context
        )
    }

}