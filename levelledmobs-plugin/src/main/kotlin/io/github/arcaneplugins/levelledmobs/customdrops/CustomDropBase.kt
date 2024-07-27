package io.github.arcaneplugins.levelledmobs.customdrops

import java.util.TreeSet
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.util.Utils

/**
 * Base class for all custom drops including custom commands
 *
 * @author stumper66
 * @since 3.0.0
 */
abstract class CustomDropBase(
    val defaults: CustomDropsDefaults
) {
    val uid: UUID = UUID.randomUUID()
    var amountRangeMin = 0
    var amountRangeMax = 0
    var hasAmountRange = false
        private set
    var minLevel = 0
    var maxLevel = 0
    var priority = 0
    var maxDropGroup = 0
    var minPlayerLevel = 0
    var maxPlayerLevel = 0
    var useChunkKillMax = false
    var chance: SlidingChance? = null
    var playerCausedOnly = false
    var noSpawner = false
    var isDefaultDrop = false
    var groupId: String? = null
    var playerLevelVariable: String? = null
    val permissions = mutableListOf<String>()
    val playeerVariableMatches = mutableListOf<String>()
    val excludedMobs: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    var causeOfDeathReqs: CachedModalList<String>? = null

    var amount: Int = 1
        set(value) {
            field = value.coerceAtMost(64)
            field = field.coerceAtLeast(1)
            this.hasAmountRange = false
        }

    val hasGroupId: Boolean
        get() = !this.groupId.isNullOrEmpty()


    open val amountAsString: String
        get() {
            return if (this.hasAmountRange) {
                "$amountRangeMin-$amountRangeMax"
            } else {
                amount.toString()
            }
        }

    fun setAmountRangeFromString(
        numberOrNumberRange: String?
    ): Boolean {
        if (numberOrNumberRange.isNullOrEmpty()) {
            return false
        }

        if (!numberOrNumberRange.contains("-")) {
            if (!Utils.isDouble(numberOrNumberRange)) {
                return false
            }


            this.amount = numberOrNumberRange.toDouble().toInt()
            this.hasAmountRange = false
            return true
        }

        val nums = numberOrNumberRange.split("-")
        if (nums.size != 2) {
            return false
        }

        if (!Utils.isDouble(nums[0].trim()) || !Utils.isDouble(nums[1].trim())
        ) {
            return false
        }
        this.amountRangeMin = nums[0].trim().toDouble().toInt()
        this.amountRangeMax = nums[1].trim().toDouble().toInt()
        this.hasAmountRange = true

        return true
    }
}