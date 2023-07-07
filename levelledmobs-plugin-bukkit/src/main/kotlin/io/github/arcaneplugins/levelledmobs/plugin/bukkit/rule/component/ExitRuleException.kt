package io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc.DescriptiveException

class ExitRuleException(
    val recursive: Boolean
) : DescriptiveException(
    message = "Exit rule called"
)
