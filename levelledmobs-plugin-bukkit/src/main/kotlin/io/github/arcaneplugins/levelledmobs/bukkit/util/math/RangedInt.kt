package io.github.arcaneplugins.levelledmobs.bukkit.util.math

import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.max
import kotlin.math.min

class RangedInt : Comparable<RangedInt> {
    val min: Int
    val max: Int

    constructor(min: Int?, max: Int?){
        this.min = min?: 0
        this.max = max?: 0
    }

    constructor(num: Int?){
        this.min = num?: 0
        this.max = num?: 0
    }

    constructor(str: String){
        /*
        note: this parser has been tested with the following inputs:

        10  -10     10-20       -10-20     -10--20     10--20       10 -20

        10 --20     10 - -20    -10 -20    -10- -20
         */

        // remove whitespace to simplify parsing

        /*
        note: this parser has been tested with the following inputs:

        10  -10     10-20       -10-20     -10--20     10--20       10 -20

        10 --20     10 - -20    -10 -20    -10- -20
         */

        // remove whitespace to simplify parsing
        var str2 = str.replace(" ", "")

        // we convert it to a char array to efficiently iterate through it
        val chars = str.toCharArray()

        require(chars.isNotEmpty()) { "Input string can't be empty" }

        // states which side we are currently parsing. left=true; right=false
        // note that in ranged ints, the right side value is optional.
        // also note that the min/max value can be on either side.

        // states which side we are currently parsing. left=true; right=false
        // note that in ranged ints, the right side value is optional.
        // also note that the min/max value can be on either side.
        var isLeft = true

        // information for parsing the left side
        val leftStart = 0 // index which the left side starts. constant
        var leftEnd = chars.size - 1 // index which the left side ends

        var rightStart = -1 // index which the right side starts. init'd with an invalid val
        val rightEnd = leftEnd // index which the right side ends. constant

        // iterate through each character in the input. divide it between left and right side
        for (i in chars.indices) {
            val c = chars[i]

            // we're looking for the range splitter here. if it doesn't exist, the ranged int is
            // just parsed on the left side and ignoring the non-existent right side.
            // ranged ints look like this: -1 - -50, where -1 is the left side and -50 is the right.
            // notice how the hyphen character is used to separate the range and this character is
            // also used for the negative sign so we can't just split the string into 2, unless
            // the number is unsigned, which we don't care about in Java most of the time

            // we're looking for the range splitter here. if it doesn't exist, the ranged int is
            // just parsed on the left side and ignoring the non-existent right side.
            // ranged ints look like this: -1 - -50, where -1 is the left side and -50 is the right.
            // notice how the hyphen character is used to separate the range and this character is
            // also used for the negative sign so we can't just split the string into 2, unless
            // the number is unsigned, which we don't care about in Java most of the time
            if (isLeft && i != 0 && c == '-') {
                leftEnd = i - 1
                require(i != chars.size - 1) { "Missing right side of RangedInt declaration" }
                rightStart = i + 1
                isLeft = false
            }
        }

        // parse the left side
        // note: substring end is exclusive, so we're adding 1
        val left = str.substring(leftStart, leftEnd + 1).toInt()

        if (isLeft) {
            // looks like there is no right side, so we'll just set min and max to the left value
            min = left
            max = left
        } else {
            // parse the right side
            // note: substring end is exclusive, so we're adding 1
            val right = str.substring(rightStart, rightEnd + 1).toInt()

            // remember that the left side is not always min, and the right side is not always max,
            // so we'll have to feed both through Math.min and Math.max to find out which is which.
            min = min(left, right)
            max = max(left, right)
        }
    }

    fun choose(): Int{
        return ThreadLocalRandom.current().nextInt(min, max + 1)
    }

    operator fun contains(integer: Int): Boolean {
        return integer in min..max
    }

    override fun compareTo(o: RangedInt): Int {
        return min.compareTo(max)
    }
}