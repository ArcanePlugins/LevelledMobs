package io.github.arcaneplugins.levelledmobs.misc

import java.util.Stack
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.rules.RuleInfo
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapperBase
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType

class LivingEntityPlaceholder : LivingEntityWrapperBase(), LivingEntityInterface {
    companion object{
        private val cache = Stack<LivingEntityPlaceholder>()
        private val cachedplaceholdersLock = Any()

        fun getInstance(
            entityType: EntityType,
            location: Location
        ): LivingEntityPlaceholder {
            val leph: LivingEntityPlaceholder

            if (location.world == null) {
                throw NullPointerException("World can't be null")
            }

            synchronized(cachedplaceholdersLock) {
                leph = if (cache.empty()) {
                    LivingEntityPlaceholder()
                } else {
                    cache.pop()
                }
            }

            leph.populateEntityData(entityType, location, location.world)
            leph.inUseCount.set(1)
            return leph
        }
    }

    private fun populateEntityData(
        entityType: EntityType,
        location: Location,
        world: World
    ) {
        this.entityType = entityType
        super.populateData(world, location)
    }

    override fun free() {
        if (inUseCount.decrementAndGet() > 0) {
            return
        }
        if (!isPopulated) {
            return
        }

        clearEntityData()
        synchronized(cachedplaceholdersLock) {
            cache.push(this)
        }
    }

    override fun clearEntityData() {
        this.entityType = null
        super.clearEntityData()
    }

    override var entityType: EntityType? = null
        get() = field!!

    override fun getApplicableRules(): MutableList<RuleInfo> {
        return main.rulesManager.getApplicableRules(this).allApplicableRules
    }

    override val typeName: String
        get() = this.entityType!!.name

    override var spawnedTimeOfDay: Int? = null
        get() {
            if (field == null)
                field = world.time.toInt()

            return field
        }

    override val wasSummoned: Boolean
        get() = summonedLevel != null
}