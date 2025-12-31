package io.github.arcaneplugins.levelledmobs.result

/**
 * Holds values used for applying multiplier buffs
 *
 * @author stumper66
 * @since 4.0
 */
data class MultiplierResult(
    val multiplierAmount: Float,
    val baseModAmount: Float?,
    val isAddition: Boolean
){
    override fun toString(): String {
        return "multiplier: $multiplierAmount, base amount: $baseModAmount, is addition: $isAddition"
    }
}