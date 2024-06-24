package io.github.arcaneplugins.levelledmobs.annotations

import io.github.arcaneplugins.levelledmobs.enums.RuleType

@Target(AnnotationTarget.FIELD)
annotation class RuleFieldInfo(
    val value: String,
    val ruleType: RuleType
)