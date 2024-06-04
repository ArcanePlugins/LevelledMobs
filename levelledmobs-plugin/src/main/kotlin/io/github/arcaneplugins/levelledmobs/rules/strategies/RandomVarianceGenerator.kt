package io.github.arcaneplugins.levelledmobs.rules.strategies

import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.misc.StringReplacer
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.concurrent.ThreadLocalRandom

object RandomVarianceGenerator {
    fun generateVariance(
        lmEntity: LivingEntityWrapper,
        input: StringReplacer
    ){
        // syntax:
        // %rand_-5_10% = get number between -5 and 10
        // %rand_20% = get number between 0 and 20
        var text = input.text
        var count = 0
        var foundRand = false

        while (true){
            count++
            val start = text.indexOf("%rand_")
            if (start < 0) break

            val end = text.indexOf("%", start + 1, true)
            if (end <= 0) {
                DebugManager.log(DebugType.RANDOM_NUMBER, lmEntity){
                    "Invalid input: $text"
                }
                return
            }

            val randText = text.substring(start, end + 1)
            val result = generateVariance2(lmEntity, randText)
            text = text.replace(randText, result)
            foundRand = true

            if (count > 100) break
        }

        if (foundRand) input.text = text
    }

    private fun generateVariance2(
        lmEntity: LivingEntityWrapper,
        numberText: String
    ): String{
        var useMin = 0
        var useMax = 1
        val part = numberText.substring(1, numberText.length - 1)
        val split = part.split("_")

        if (split.size == 3){
            if (split[2].isNotEmpty()){
                useMax = split[2].toInt()
                useMin = split[1].toInt()
            }
            else if (split[1].isNotEmpty())
                useMax = split[1].toInt()
        }
        else if (split.size == 2)
            useMax = split[1].toInt()

        useMin = useMin.coerceAtMost(useMax)
        useMax = useMax.coerceAtLeast(useMin + 1)

        val result = ThreadLocalRandom.current().nextInt(useMin, useMax)
        DebugManager.log(DebugType.RANDOM_NUMBER, lmEntity){
            "min $useMin, max: $useMax, result: $result"
        }

        return result.toString()
    }
}