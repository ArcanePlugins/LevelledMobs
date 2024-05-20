package io.github.arcaneplugins.levelledmobs.annotations

import io.github.arcaneplugins.levelledmobs.enums.RuleType

@Target(AnnotationTarget.PROPERTY)
annotation class RuleFieldInfo(
    val value: String,
    val ruleType: RuleType
)