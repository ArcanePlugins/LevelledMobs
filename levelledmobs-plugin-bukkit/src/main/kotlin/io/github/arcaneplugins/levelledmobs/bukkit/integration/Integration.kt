package io.github.arcaneplugins.levelledmobs.bukkit.integration

abstract class Integration(
    val identifier: String,
    val description: String,
    val enabledByDefault: Boolean,
    val internal: Boolean,
    val priority: IntegrationPriority
) {
    var enabled: Boolean = false
}