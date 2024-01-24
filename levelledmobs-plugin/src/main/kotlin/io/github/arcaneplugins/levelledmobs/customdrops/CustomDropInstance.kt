package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.arcaneplugins.levelledmobs.misc.CustomUniversalGroups
import org.bukkit.entity.EntityType

/**
 * Holds a mob or group instance and associates it with a list of custom drop items. This is where
 * the override for a mob / group is set
 *
 * @author stumper66
 * @since 2.4.0
 */
class CustomDropInstance {
    var associatedMob: EntityType? = null
        private set
    private var entityGroup: CustomUniversalGroups? = null
    var customItems = mutableListOf<CustomDropBase>()
    var overallChance: Float? = null
    var overallPermissions = mutableListOf<String>()
    var overrideStockDrops: Boolean? = null
    var utilizesGroupIds: Boolean = false
    var isBabyMob: Boolean = false

    constructor(associatedMob: EntityType?){
        this.associatedMob = associatedMob
    }

    constructor(associatedMob: EntityType?, isBabyMob: Boolean){
        this.associatedMob = associatedMob
        this.isBabyMob = isBabyMob
    }

    constructor(entityGroup: CustomUniversalGroups){
        this.entityGroup = entityGroup
    }

    fun combineDrop(dropInstance: CustomDropInstance?) {
        if (dropInstance == null) {
            throw NullPointerException("dropInstance")
        }

        this.overrideStockDrops = dropInstance.overrideStockDrops

        if (dropInstance.utilizesGroupIds) {
            this.utilizesGroupIds = true
        }

        customItems.addAll(dropInstance.customItems)
    }

    fun getMobOrGroupName(): String {
        return if (this.associatedMob != null) {
            associatedMob!!.name
        } else if (this.entityGroup != null) {
            entityGroup!!.name
        } else {
            "" // this return should never happen
        }
    }

    val getOverrideStockDrops: Boolean
        get() = this.overrideStockDrops != null && this.overrideStockDrops!!

    override fun toString(): String {
        return if (this.associatedMob != null) {
            if (getOverrideStockDrops) associatedMob!!.name + " - override" else associatedMob!!.name
        } else if (this.entityGroup != null) {
            if (getOverrideStockDrops) entityGroup.toString() + " - override" else entityGroup.toString()
        } else {
            "CustomDropInstance"
        }
    }
}