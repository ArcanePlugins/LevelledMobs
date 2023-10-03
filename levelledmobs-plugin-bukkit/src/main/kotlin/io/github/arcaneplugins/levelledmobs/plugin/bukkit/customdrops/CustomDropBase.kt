package io.github.arcaneplugins.levelledmobs.plugin.bukkit.customdrops

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.Utils
import java.util.LinkedList
import java.util.TreeSet

abstract class CustomDropBase : Cloneable {
    var amountRangeMin = 0
        private set
    var amountRangeMax = 0
        private set
    var hasAmountRange = false
        private set
    var minLevel = 0
    var maxLevel = 0
    var priority = 0
    var maxDropGroup = 0
    var minPlayerLevel = 0
    var maxPlayerLevel = 0
    var useChunkKillMax = false
    var chance = 0f
    var playerCausedOnly = false
    var noSpawner = false
    var groupId: String? = null
    var playerLevelVariable: String? = null
    val permissions: List<String>
    val playerVariableMatches: List<String>
    val excludedMobs: Set<String>
    var causeOfDeathReqs: CachedModalList<DeathCause>? = null

    init {
        excludedMobs = TreeSet(String.CASE_INSENSITIVE_ORDER)
        permissions = LinkedList()
        playerVariableMatches = LinkedList()
    }

    var amount: Int = 0
        set(value) {
            field = 1.coerceAtLeast(value)
            field = 64.coerceAtMost(value)
        }

    fun getAmountAsString() : String{
        return if (this.hasAmountRange){
            String.format("%s-%s", amountRangeMin, amountRangeMax)
        } else{
            this.amount.toString()
        }
    }

    fun setAmountRangeFromString(numberOrNumberRange: String?): Boolean{
        if (numberOrNumberRange.isNullOrEmpty()) {
            return false
        }

        if (!numberOrNumberRange.contains("-")) {
            if (!Utils.isInteger(numberOrNumberRange)) {
                return false
            }
            amount = numberOrNumberRange.toInt()
            hasAmountRange = false
            return true
        }

        val nums = numberOrNumberRange.split("-")
        if (nums.size != 2) return false

        if (!Utils.isInteger(nums[0].trim()) || !Utils.isInteger(nums[1].trim())){
            return false
        }

        this.amountRangeMin = nums[0].trim().toInt()
        this.amountRangeMax = nums[1].trim().toInt()
        this.hasAmountRange = true

        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun clone(): CustomDropBase{
        val copy: CustomDropBase = super.clone() as CustomDropBase
        if (this.causeOfDeathReqs != null){
            copy.causeOfDeathReqs = this.causeOfDeathReqs!!.clone() as CachedModalList<DeathCause>
        }

        return copy
    }
}