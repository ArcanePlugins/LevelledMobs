package io.github.arcaneplugins.levelledmobs.rules.strategies

import io.github.arcaneplugins.levelledmobs.util.Log
import java.util.LinkedList
import java.util.TreeMap
import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.util.Utils.isInteger
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import kotlin.math.max

/**
 * Holds the configuration and logic for applying a levelling system that is based upon random
 * levelling
 *
 * @author stumper66
 * @since 3.1.0
 */
class RandomLevellingStrategy : LevellingStrategy, Cloneable {
    val weightedRandom: MutableMap<String, Int> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    var doMerge: Boolean = false
    private var randomArray: Array<Int>? = null
    private var minLevel = 0
    private var maxLevel = 0
    var autoGenerate = false
    var enabled = true

    fun generateLevel(minLevel: Int, maxLevel: Int): Int {
        return generateLevel(null, minLevel, maxLevel)
    }

    override fun generateLevel(
        lmEntity: LivingEntityWrapper?,
        minLevel: Int,
        maxLevel: Int
    ): Int {
        // this function only has lmEmtity to satify the interface requirement
        if (weightedRandom.isEmpty()) {
            return getRandomLevel(minLevel, maxLevel)
        }

        if (this.randomArray == null || (minLevel != this.minLevel) || (maxLevel != this.maxLevel)) {
            populateWeightedRandom(minLevel, maxLevel)
        }

        // populateWeightedRandom(..) should've populated randomArray but if weightedRandom
        // was empty then it won't do anything
        if (this.randomArray == null) {
            return getRandomLevel(minLevel, maxLevel)
        }

        val useArrayNum = ThreadLocalRandom.current().nextInt(0, randomArray!!.size)
        return randomArray!![useArrayNum]
    }

    fun populateWeightedRandom(minLevel: Int, maxLevel: Int) {
        if (weightedRandom.isEmpty()) {
            return
        }

        this.minLevel = minLevel
        this.maxLevel = maxLevel
        var count = 0
        val numbers: MutableList<IntArray> = LinkedList()
        val values: MutableList<Int> = LinkedList()
        val numbersUsed: MutableList<Int> = LinkedList()
        val origOverallNumberRange: MutableList<Int> = LinkedList()

        for (i in minLevel..maxLevel) {
            origOverallNumberRange.add(i)
        }

        val overallNumberRange: MutableList<Int> = LinkedList(origOverallNumberRange)

        // first loop parses the number range string and counts totals
        // so we know how big to size the array
        for ((range, value) in this.weightedRandom) {
            if (range.isEmpty()) {
                continue
            }

            val numRange: IntArray = parseNumberRange(range)
            if (numRange[0] == -1 && numRange[1] == -1) {
                Log.war("Invalid number range for weighted random: $range")
                continue
            }

            val start = if (numRange[0] < 0) numRange[1] else numRange[0]
            val end = if (numRange[1] < 0) numRange[0] else numRange[1]
            numbers.add(intArrayOf(start, end))
            values.add(value)

            for (i in start..end) {
                if (!origOverallNumberRange.contains(i)) {
                    continue
                }
                if (!numbersUsed.contains(i)) {
                    numbersUsed.add(i)
                }

                count += value
            }
        }

        count -= numbersUsed.size
        count += overallNumberRange.size

        this.randomArray = Array(count){0}
        var newCount = 0

        // now we actually populate the array
        for ((valuesCount, nums) in numbers.withIndex()) {
            for (i in nums[0]..nums[1]) {
                if (!origOverallNumberRange.contains(i)) {
                    continue
                }
                overallNumberRange.remove(i)
                for (t in 0 until values[valuesCount]) {
                    randomArray!![newCount] = i
                    newCount++
                }
            }
        }

        for (number in overallNumberRange) {
            randomArray!![newCount] = number
            newCount++
        }
    }

    private fun parseNumberRange(range: String): IntArray {
        val results = intArrayOf(-1, -1)

        if (!range.contains("-")) {
            if (!isInteger(range)) {
                return results
            }

            results[0] = range.toInt()
            results[1] = results[0]
            return results
        }

        val nums = range.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (nums.size < 2) {
            return results
        }

        nums[0] = nums[0].trim { it <= ' ' }
        nums[1] = nums[1].trim { it <= ' ' }

        if (nums[0].isNotEmpty() && isInteger(nums[0])) {
            results[0] = nums[0].toInt()
        }

        if (nums[1].isNotEmpty() && isInteger(nums[1])) {
            results[1] = nums[1].toInt()
        }

        return results
    }

    private fun getRandomLevel(minLevel: Int, maxLevel: Int): Int {
        var useMinLevel = minLevel
        var useMaxLevel = maxLevel
        useMinLevel = max(useMinLevel.toDouble(), 0.0).toInt()
        useMaxLevel = max(useMinLevel.toDouble(), useMaxLevel.toDouble()).toInt()
        return ThreadLocalRandom.current().nextInt(useMinLevel, useMaxLevel + 1)
    }

    override fun mergeRule(levellingStrategy: LevellingStrategy) {
        if (levellingStrategy !is RandomLevellingStrategy) {
            return
        }

        if (levellingStrategy.doMerge && levellingStrategy.enabled) {
            weightedRandom.putAll(levellingStrategy.weightedRandom)
        }
    }

    override fun cloneItem(): RandomLevellingStrategy? {
        var copy: RandomLevellingStrategy? = null
        try {
            copy = super.clone() as RandomLevellingStrategy
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy
    }

    override fun toString(): String {
        if (weightedRandom.isEmpty()) {
            return if (this.autoGenerate) "Random Levelling (auto generate)"
            else "Random Levelling"
        }

        if (minLevel == 0) {
            return weightedRandom.toString()
        }

        return "$minLevel-$maxLevel: $weightedRandom"
    }
}