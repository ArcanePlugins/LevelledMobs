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
        val text = input.text
        val start = text.indexOf("%rand_")
        val end = text.indexOf("%", start + 1, true)
        if (end <= 0) {
            DebugManager.log(DebugType.RANDOM_NUMBER, lmEntity){
                "Invalid input: $text"
            }
            return
        }

        var useMin = 0
        var useMax = 1
        val fullpart = text.substring(start, end + 1)
        val part = fullpart.substring(1, fullpart.length - 1)
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

        input.replace(fullpart, result)
    }
}