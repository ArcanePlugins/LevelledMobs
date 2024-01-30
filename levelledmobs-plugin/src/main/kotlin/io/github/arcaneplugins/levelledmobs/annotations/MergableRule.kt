package io.github.arcaneplugins.levelledmobs.annotations

/**
 * Provides a common interface for various rules
 * that can be merged together
 *
 * @author stumper66
 * @since 3.12.0
 */
interface MergableRule {
    fun merge(mergableRule: MergableRule?)

    val doMerge: Boolean

    fun cloneItem(): Any
}