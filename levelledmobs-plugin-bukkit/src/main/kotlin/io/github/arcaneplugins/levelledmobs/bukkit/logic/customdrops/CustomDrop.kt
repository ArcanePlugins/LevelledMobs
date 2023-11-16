package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops

import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient

abstract class CustomDrop(
    val type: String,
    val recipient: CustomDropRecipient
) {

    var chance = 100.0f
    var noSpawner = true
    var priority = 0
    var maxDropsInGroup: Int? = null
    var dropGroupId = "0"
    var chunkKillLimited = true
    var requiredPermissions: MutableList<String> = mutableListOf()
    var deathCauses: MutableList<String> = mutableListOf()
    var entityMinLevel: Int? = null
    var entityMaxLevel: Int? = null
    var overridesVanillaDrops = false
    var overridesNonVanillaDrops = false
    var formulaCondition: String? = null
    var shuffle = true

    fun withDeathCauses(
        deathCauses: MutableList<String>
    ) : CustomDrop{
        this.deathCauses.addAll(deathCauses)
        return this
    }

    fun withFormulaCondition(
        formulaCondition: String?
    ) : CustomDrop{
        this.formulaCondition = formulaCondition
        return this
    }

    fun withOverridesVanillaDrops(
        overridesVanillaDrops: Boolean
    ) : CustomDrop{
        this.overridesVanillaDrops = overridesVanillaDrops
        return this
    }

    fun withOverridesNonVanillaDrops(
        withOverridesNonVanillaDrops: Boolean
    ) : CustomDrop{
        this.overridesNonVanillaDrops = withOverridesNonVanillaDrops
        return this
    }

    fun withRequiredPermissions(
        requiredPermissions: MutableList<String>
    ) : CustomDrop{
        this.requiredPermissions = requiredPermissions
        return this
    }

    fun withChunkKillLimited(
        chunkKillLimited: Boolean
    ) : CustomDrop{
        this.chunkKillLimited = chunkKillLimited
        return this
    }

    fun withDropGroupId(
        dropGroupId: String
    ) : CustomDrop{
        this.dropGroupId = dropGroupId
        return this
    }

    fun withMaxDropsInGroup(
        maxDropsInGroup: Int?
    ) : CustomDrop{
        this.maxDropsInGroup = maxDropsInGroup
        return this
    }

    fun withPriority(
        priority: Int
    ) : CustomDrop{
        this.priority = priority
        return this
    }

    fun withNoSpawner(
        noSpawner: Boolean
    ) : CustomDrop{
        this.noSpawner = noSpawner
        return this
    }

    fun withChance(
        chance: Float
    ) : CustomDrop{
        this.chance = chance
        return this
    }

    fun withEntityMaxLevel(
        entityMaxLevel: Int
    ) : CustomDrop{
        this.entityMaxLevel = entityMaxLevel
        return this
    }

    fun withEntityMinLevel(
        entityMinLevel: Int
    ) : CustomDrop{
        this.entityMinLevel = entityMinLevel
        return this
    }

    fun withShuffling(
        shuffle: Boolean
    ) : CustomDrop{
        this.shuffle = shuffle
        return this
    }
}