package io.github.arcaneplugins.levelledmobs.listeners

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.managers.DebugManager
import io.github.arcaneplugins.levelledmobs.misc.AdditionalLevelInformation
import io.github.arcaneplugins.levelledmobs.misc.DebugType
import io.github.arcaneplugins.levelledmobs.misc.LevellableState
import io.github.arcaneplugins.levelledmobs.rules.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTameEvent

/**
 * Listens when an entity is tamed so various rules can be applied
 *
 * @author stumper66
 * @since 2.4.0
 */
class EntityTameListener : Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onEntityTameEvent(event: EntityTameEvent) {
        val main = LevelledMobs.instance
        val lmEntity = LivingEntityWrapper.getInstance(event.entity)
        val levellableState: LevellableState = main.levelInterface.getLevellableState(lmEntity)

        if (levellableState != LevellableState.ALLOWED) {
            DebugManager.log(DebugType.ENTITY_TAME, lmEntity) { "Levelable state was $levellableState" }
            lmEntity.free()
            return
        }

        if (main.rulesManager.getRuleMobTamedStatus(lmEntity) === MobTamedStatus.NOT_TAMED) {
            DebugManager.log(DebugType.ENTITY_TAME, lmEntity) { "no-level-conditions.tamed = &btrue" }

            // if mob was levelled then remove it
            main.levelInterface.removeLevel(lmEntity)

            DebugManager.log(DebugType.ENTITY_TAME, lmEntity) { "Removed level of tamed mob" }
            lmEntity.free()
            return
        }

        DebugManager.log(DebugType.ENTITY_TAME, lmEntity) { "Applying level to tamed mob" }
        var level = -1
        if (lmEntity.isLevelled) {
            level = lmEntity.getMobLevel()
        }

        if (level == -1) {
            level = main.levelInterface.generateLevel(lmEntity)
            lmEntity.invalidateCache()
        }

        main.levelInterface.applyLevelToMob(
            lmEntity,
            level,
            isSummoned = false,
            bypassLimits = false,
            additionalLevelInformation = mutableSetOf(AdditionalLevelInformation.FROM_TAME_LISTENER)
        )

        lmEntity.free()
    }
}