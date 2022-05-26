package me.lokka30.levelledmobs.bukkit.integrations

import me.lokka30.levelledmobs.bukkit.integrations.internal.CitizensIntegration
import me.lokka30.levelledmobs.bukkit.integrations.internal.RtuLangApiIntegration

class IntegrationHandler {

    val internalIntegrations = setOf(
        CitizensIntegration(),
        RtuLangApiIntegration()
    )

    val integrations = mutableSetOf<Integration>()

    init {
        integrations.addAll(internalIntegrations)
    }
}