package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

/**
 * This holds all the attributes set for a custom drop item
 *
 * @author stumper66
 * @since 2.5.0
 */
class CustomDropItem() : CustomDropBase(
        LevelledMobs.instance.customDropsHandler.customDropsParser.defaults),
        Cloneable {
    var customModelDataId = 0
    var amountFormula: String? = null
    var equippedChance: SlidingChance? = null
    var noMultiplier = false
    var onlyDropIfEquipped = false
    var equipOnHelmet = false
    var customName: String? = null
    var lore: MutableList<String>? = null
    var itemFlags: MutableList<ItemFlag>? = null
    var itemFlagsStrings: MutableList<String>? = null
    var allowedList: MutableList<String>? = null
    var excludedList: MutableList<String>? = null
    var hasDamageRange = false
        private set
    var damageRangeMin = 0
    var damageRangeMax = 0
    var minItems = 0
    var maxItems = 0
    var equipOffhand = false
    private var _material: Material? = null
    var isExternalItem = false
    var externalPluginName: String? = null
    var externalType: String? = null
    var externalItemId: String? = null
    var nbtData: String? = null
    var externalAmount: Double? = null
    var externalExtras: MutableMap<String, Any>? = null
    var enchantmentChances: EnchantmentChances? = null

    constructor(defaults: CustomDropsDefaults) : this() {
        setDefaults(defaults)
    }

    private fun setDefaults(defaults: CustomDropsDefaults) {
        this.customModelDataId = defaults.customModelData
        if (this.chance != null)
            this.chance!!.setFromInstance(defaults.chance)
        else
            this.chance = defaults.chance
        this.maxLevel = defaults.maxLevel
        this.minLevel = defaults.minLevel
        this.groupId = defaults.groupId
        this.maxDropGroup = defaults.maxDropGroup
        if (this.equippedChance != null)
            this.equippedChance!!.setFromInstance(defaults.equippedChance)
        else
            this.equippedChance = defaults.equippedChance
        this.noMultiplier = defaults.noMultiplier
        this.onlyDropIfEquipped = defaults.onlyDropIfEquipped
        this.equipOffhand = defaults.equipOffhand
        this.equipOnHelmet = defaults.equipOnHelmet
    }

    fun cloneItem(): CustomDropItem? {
        var copy: CustomDropItem? = null
        try {
            copy = super.clone() as CustomDropItem
            copy.itemStack = itemStack!!.clone()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy
    }

    fun setDamageRangeFromString(numberOrNumberRange: String?): Boolean {
        if (numberOrNumberRange.isNullOrEmpty()) {
            return false
        }

        if (!numberOrNumberRange.contains("-")) {
            if (!Utils.isInteger(numberOrNumberRange)) {
                return false
            }

            this.damage = numberOrNumberRange.toInt()
            this.hasDamageRange = false
            this.damageRangeMax = 0
            this.damageRangeMin = 0
            return true
        }

        val nums = numberOrNumberRange.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (nums.size != 2) {
            return false
        }

        if (!Utils.isInteger(nums[0].trim { it <= ' ' }) || !Utils.isInteger(
                nums[1].trim { it <= ' ' })
        ) {
            return false
        }
        this.damageRangeMin = nums[0].trim { it <= ' ' }.toInt()
        this.damageRangeMax = nums[1].trim { it <= ' ' }.toInt()
        this.hasDamageRange = true

        return true
    }

    var damage = 0
        set(value) {
            field = value
            this.hasDamageRange = false
        }

    var material: Material
        get() = this._material!!
        set(value) {
            this._material = value
            this.itemStack = ItemStack(_material!!, 1)
        }

    fun getDamageAsString(): String {
        return if (this.hasDamageRange) {
            "$damageRangeMin-$damageRangeMax"
        } else {
            damage.toString()
        }
    }

    var itemStacks: MutableList<ItemStack>? = null
        set(value) {
            field = value
            if (itemStacks != null && itemStacks!!.isNotEmpty()) {
                this.itemStack = itemStacks!![0]
                this.material = itemStack!!.type
            }
        }
        get() {
            if (field != null && field!!.isNotEmpty())
                return field

            if (itemStack == null) return null
            return mutableListOf(itemStack!!)
        }

    var itemStack: ItemStack? = null
        set(value) {
            field = value
            _material = value!!.type
        }

    override fun toString(): String {
        return "${material.name}, amount: $amountAsString, chance: $chance, equipped: $equippedChance"
    }
}