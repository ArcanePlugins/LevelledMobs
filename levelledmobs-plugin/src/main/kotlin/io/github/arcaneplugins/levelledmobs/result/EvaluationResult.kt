package io.github.arcaneplugins.levelledmobs.result

data class EvaluationResult(
    val result: Double,
    val error: String?
){
    val hadError: Boolean
        get() = this.error != null
}