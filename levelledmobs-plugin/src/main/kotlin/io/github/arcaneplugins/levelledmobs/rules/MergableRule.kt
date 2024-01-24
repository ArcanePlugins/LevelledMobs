package io.github.arcaneplugins.levelledmobs.rules

interface MergableRule {
    fun merge(mergableRule: MergableRule?)

    val doMerge: Boolean

    fun cloneItem(): Any
}