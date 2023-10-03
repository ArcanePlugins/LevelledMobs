package io.github.arcaneplugins.levelledmobs.plugin.bukkit.customdrops

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.enums.CustomUniversalGroups
import java.util.LinkedList
import org.bukkit.entity.EntityType

class CustomDropInstance {
    private val associatedMob: EntityType?
    private val entityGroup: CustomUniversalGroups?
    val customItems: List<CustomDropBase>
    var overallChance: Float? = null
    val overallPermissions: List<String>
    var overrideStockDrops = false
    var utilizesGroupIds = false
    val isBabyMob: Boolean

    constructor(associatedMob: EntityType?){
        this.associatedMob = associatedMob
        this.entityGroup = null
        this.customItems = LinkedList()
        this.overallPermissions = LinkedList()
        this.isBabyMob = false
    }

    constructor(associatedMob: EntityType?, isBabyMob: Boolean){
        this.associatedMob = associatedMob
        this.entityGroup = null
        this.customItems = LinkedList()
        this.overallPermissions = LinkedList()
        this.isBabyMob = isBabyMob
    }

    constructor(entityGroup: CustomUniversalGroups){
        this.associatedMob = null
        this.entityGroup = entityGroup
        this.customItems = LinkedList()
        this.overallPermissions = LinkedList()
        this.isBabyMob = false
    }

    fun combineDrop(dropInstance: CustomDropInstance?){
        if (dropInstance == null) return

        if (dropInstance.overrideStockDrops){
            this.overrideStockDrops = true
        }

        if (dropInstance.utilizesGroupIds){
            this.utilizesGroupIds = true
        }

        (this.customItems as LinkedList).addAll(dropInstance.customItems)
    }

    fun getMobOrGroupName() : String{
        return associatedMob?.name ?: (entityGroup?.name ?: "")
    }

    val associatedMobType: EntityType?
        get() = this.associatedMob

    override fun toString(): String {
        return if (associatedMob != null) {
            if (overrideStockDrops) associatedMob.name + " - override" else associatedMob.name
        } else if (entityGroup != null) {
            if (overrideStockDrops) "$entityGroup - override" else entityGroup.toString()
        } else {
            "CustomDropInstance"
        }
    }
}