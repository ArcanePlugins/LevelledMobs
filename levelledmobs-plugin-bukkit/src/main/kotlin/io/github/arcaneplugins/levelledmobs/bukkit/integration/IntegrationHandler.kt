package io.github.arcaneplugins.levelledmobs.bukkit.integration

import io.github.arcaneplugins.levelledmobs.bukkit.integration.impl.CitizensIntegration
import io.github.arcaneplugins.levelledmobs.bukkit.integration.impl.NbtApiIntegration
import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt.NbtProvider
import org.bukkit.Bukkit

object IntegrationHandler {
    val integrations = mutableSetOf<Integration>()

    fun load(): Boolean{
        // TODO add check if these integrations are disabled.
        // TODO check why this returns a boolean


        // TODO add check if these integrations are disabled.
        // TODO check why this returns a boolean
        if (hasRequiredPlugins("Citizens")) integrations
            .add(CitizensIntegration())

        if (hasRequiredPlugins("NBTAPI")) integrations
            .add(NbtApiIntegration())

        return true
    }

    fun hasRequiredPlugins(plugin: String): Boolean{
        return hasRequiredPlugins(mutableListOf(plugin))
    }

    fun hasRequiredPlugins(plugins: MutableList<String>): Boolean{
        for (plugin in plugins){
            if (!Bukkit.getPluginManager().isPluginEnabled(plugin)){
                return false
            }
        }

        return true
    }

    fun getPrimaryNbtProvider(): NbtProvider?{
        var selected: Integration? = null

        for (integration in integrations){
            if (!integration.enabled) continue

            if (integration is NbtProvider){
                if (selected == null ||
                    integration.priority.ordinal > selected.priority.ordinal){
                    selected = integration
                }
            }
        }

        return if (selected == null){
            null
        } else{
            selected as NbtProvider
        }
    }
}