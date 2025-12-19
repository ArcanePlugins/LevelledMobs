package io.github.arcaneplugins.levelledmobs.rules.strategies

import io.github.arcaneplugins.levelledmobs.rules.MinAndMax
import io.github.arcaneplugins.levelledmobs.util.Log
import java.util.TreeMap
import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper

/**
 * Holds the configuration and logic for applying a levelling system that is based upon random
 * levelling
 *
 * @author stumper66
 * @since 3.1.0
 */
class RandomLevellingStrategy : LevellingStrategy, Cloneable {
    val weightedRandomMap: MutableMap<String, Int> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    private var randomArray = IntArray(0)
    private var minLevel = 0
    private var maxLevel = 0
    var autoGenerate = false
    var enabled = true

    override var strategyType = StrategyType.RANDOM
    override var shouldMerge: Boolean = false

    companion object{
        private val cachedWeightedRandom = mutableMapOf<String, RandomLevellingStrategy>()
        private val lockObj = Object()

        private fun getCachedWR(checkStr: String): RandomLevellingStrategy? {
            synchronized(lockObj){
                return cachedWeightedRandom[checkStr]
            }
        }

        private fun putCachedWR(checkStr: String, weightedRandom: RandomLevellingStrategy) {
            synchronized(lockObj){
                cachedWeightedRandom[checkStr] = weightedRandom
            }
        }

        fun clearCache(){
            synchronized(lockObj){
                cachedWeightedRandom.clear()
            }
        }
    }

    override fun generateNumber(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Float {
        // this function only has lmEmtity to satify the interface requirement
        if (weightedRandomMap.isEmpty())
            return getRandomLevel(minLevel, maxLevel).toFloat()

        if (this.randomArray.isEmpty() || (minLevel != this.minLevel) || (maxLevel != this.maxLevel)) {
            val checkStr = "$minLevel-$maxLevel $this"
            val cachedWR = getCachedWR(checkStr)

            if (cachedWR != null)
                setValuesFromCache(cachedWR)
            else{
                populateWeightedRandom(minLevel, maxLevel)
                putCachedWR(checkStr, this)
            }
        }

        // populateWeightedRandom(..) should've populated randomArray but if weightedRandom
        // was empty then it won't do anything
        if (this.randomArray.isEmpty())
            return getRandomLevel(minLevel, maxLevel).toFloat()

        val useArrayNum = ThreadLocalRandom.current().nextInt(0, randomArray.size)
        return randomArray[useArrayNum].toFloat()
    }

    private fun setValuesFromCache(cachedWeightedRandom: RandomLevellingStrategy){
        this.randomArray = cachedWeightedRandom.randomArray
        this.minLevel = cachedWeightedRandom.minLevel
        this.maxLevel = cachedWeightedRandom.maxLevel
        this.strategyType = StrategyType.WEIGHTED_RANDOM
    }

    fun populateWeightedRandom(minLevel: Int, maxLevel: Int) {
        if (weightedRandomMap.isEmpty()) {
            autoGenerate = true
            for (i in minLevel..maxLevel){
                val value = maxLevel - i + 1
                weightedRandomMap["$i"] = value
            }
        }
        else
            this.strategyType = StrategyType.WEIGHTED_RANDOM

        this.minLevel = minLevel
        this.maxLevel = maxLevel
        var count = 0
        val numbers = mutableListOf<MinAndMax>()
        val values = mutableListOf<Int>()

        // first loop parses the number range string and counts totals
        // so we know how big to size the array
        for ((range, value) in this.weightedRandomMap) {
            if (range.isEmpty()) continue

            val numRange = MinAndMax.setAmountRangeFromString(range)
            if (numRange == null) {
                Log.war("Invalid number range for weighted random: $range")
                continue
            }

            val start = if (numRange.min < 0f) numRange.max else numRange.min
            val end = if (numRange.max < 0) numRange.min else numRange.max

            numbers.add(MinAndMax(start, end))
            values.add(value)
            count += (end.toInt() - start.toInt() + 1) * value
        }

        this.randomArray = IntArray(count)
        var newCount = 0

        // now we actually populate the array
        for ((valuesCount, nums) in numbers.withIndex()) {
            for (i in nums.minAsInt..nums.maxAsInt) {
                repeat(values[valuesCount]) {
                    randomArray[newCount] = i
                    newCount++
                }
            }
        }
    }

    private fun getRandomLevel(minLevel: Int, maxLevel: Int): Int {
        val useMinLevel = minLevel.coerceAtLeast(0)
        val useMaxLevel = maxLevel.coerceAtLeast(useMinLevel)
        return ThreadLocalRandom.current().nextInt(useMinLevel, useMaxLevel + 1)
    }

    override fun mergeRule(levellingStrategy: LevellingStrategy?) {
        if (levellingStrategy !is RandomLevellingStrategy) return

        if (levellingStrategy.shouldMerge && levellingStrategy.enabled)
            weightedRandomMap.putAll(levellingStrategy.weightedRandomMap)

        this.strategyType =
            if (weightedRandomMap.isEmpty()) StrategyType.RANDOM
            else StrategyType.WEIGHTED_RANDOM
    }

    override fun cloneItem(): RandomLevellingStrategy {
        var copy: RandomLevellingStrategy? = null
        try {
            copy = super.clone() as RandomLevellingStrategy
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy!!
    }

    override fun toString(): String {
        if (weightedRandomMap.isEmpty()) {
            return if (this.autoGenerate) "Random Levelling (auto generate)"
            else "Random Levelling"
        }

        val mergeMessage = if (shouldMerge) " (merge)" else ""

        return if (minLevel == 0)
            "$weightedRandomMap$mergeMessage"
        else
            "$minLevel-$maxLevel: $weightedRandomMap$mergeMessage"
    }
}