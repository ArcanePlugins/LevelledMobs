package io.github.arcaneplugins.levelledmobs.result

/**
 * Holds the results of a formula evaluation
 *
 * @author stumper66
 * @since 4.0
 */
data class EvaluationResult(
    val result: Double,
    val error: String?
){
    val hadError: Boolean
        get() = this.error != null
}