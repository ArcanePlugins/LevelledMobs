package me.lokka30.levelledmobs.bukkit.integrations

import me.lokka30.levelledmobs.bukkit.integrations.internal.CitizensIntegration

class IntegrationHandler {

    val internalIntegrations = setOf<Integration>(
        CitizensIntegration()
    )

    val integrations = mutableSetOf<Integration>()

    init {
        integrations.addAll(internalIntegrations)
    }
}