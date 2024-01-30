package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.arcaneplugins.levelledmobs.misc.CachedModalList

/**
 * Holds all default values for either all custom drop items
 *
 * @author stumper66
 * @since 2.4.0
 */
class CustomDropsDefaults {
    var noMultiplier = false
    var noSpawner = false
    var override = false
    var playerCausedOnly = false
    var onlyDropIfEquipped = false
    var amount = 1
    var priority = 0
    var minLevel = -1
    var maxLevel = -1
    var equipOffhand = false
    var equipOnHelmet = false
    var customModelData = -1
    var maxDropGroup = 0
    var minPlayerLevel = -1
    var maxPlayerLevel = -1
    var chance: SlidingChance? = null
    var useChunkKillMax = false
    var equippedChance: SlidingChance? = null
    var overallChance: SlidingChance? = null
    var groupId: String? = null
    var playerLevelVariable: String? = null
    var nbtData: String? = null
    var itemFlagsStrings: MutableList<String>? = null
    var permissions = mutableListOf<String>()
    var overallPermissions = mutableListOf<String>()
    var causeOfDeathReqs: CachedModalList<DeathCause>? = null
    var externalType: String? = null
    var externalItemId: String? = null
    var externalAmount: Double? = null
    var runOnSpawn = false
    var runOnDeath = true

    fun setDefaultsFromDropItem(
    dropBase: CustomDropBase
    ) {
        if (this.chance != null)
            this.chance!!.setFromInstance(dropBase.chance)
        else
            this.chance = dropBase.chance

        this.useChunkKillMax = dropBase.useChunkKillMax
        this.amount = dropBase.amount
        this.minLevel = dropBase.minLevel
        this.maxLevel = dropBase.maxLevel
        this.priority = dropBase.priority
        this.maxDropGroup = dropBase.maxDropGroup
        this.noSpawner = dropBase.noSpawner
        this.playerCausedOnly = dropBase.playerCausedOnly
        if ("default" != dropBase.groupId) {
            this.groupId = dropBase.groupId
        }
        this.minPlayerLevel = dropBase.minPlayerLevel
        this.maxPlayerLevel = dropBase.maxPlayerLevel
        this.playerLevelVariable = dropBase.playerLevelVariable
        permissions.addAll(dropBase.permissions)
        this.causeOfDeathReqs = dropBase.causeOfDeathReqs

        if (dropBase is CustomDropItem) {
            this.customModelData = dropBase.customModelDataId
            if (this.equippedChance != null)
                this.equippedChance!!.setFromInstance(dropBase.equippedChance)
            else
                this.equippedChance = dropBase.equippedChance
            this.noMultiplier = dropBase.noMultiplier
            this.onlyDropIfEquipped = dropBase.onlyDropIfEquipped
            this.externalType = dropBase.externalType
            this.externalItemId = dropBase.externalItemId
            this.externalAmount = dropBase.externalAmount
            this.equipOffhand = dropBase.equipOffhand
            this.nbtData = dropBase.nbtData
            this.itemFlagsStrings = dropBase.itemFlagsStrings
            this.equipOnHelmet = dropBase.equipOnHelmet
        } else if (dropBase is CustomCommand) {
            this.runOnSpawn = dropBase.runOnSpawn
            this.runOnDeath = dropBase.runOnDeath
        }
    }
}