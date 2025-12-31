package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes.Multiplier

/**
 * Holds results used in parsing rules for fine tuning
 *
 * @author stumper66
 * @since 4.4.0
 */
data class FineTuningParseResult(
    val doNotMerge: Boolean,
    val doNotMergeAny: Boolean,
    val result: MutableMap<Addition, Multiplier>?
)
