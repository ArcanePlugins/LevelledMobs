package io.github.arcaneplugins.levelledmobs.annotations

import io.github.arcaneplugins.levelledmobs.enums.RuleType

@Retention(AnnotationRetention.RUNTIME)
annotation class RuleInfoType(
    val ruleType: RuleType
)
