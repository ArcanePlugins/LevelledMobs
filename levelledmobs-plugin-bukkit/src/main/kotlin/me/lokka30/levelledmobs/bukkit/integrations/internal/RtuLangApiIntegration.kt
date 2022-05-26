package me.lokka30.levelledmobs.bukkit.integrations.internal

import me.lokka30.levelledmobs.bukkit.integrations.Integration
import me.lokka30.levelledmobs.bukkit.integrations.type.TranslationProvider
import org.bukkit.entity.EntityType

class RtuLangApiIntegration : Integration(
    "Allows usage of RTULangAPI for automatic translations",
    true,
    true
), TranslationProvider {

    override fun getTranslatedEntityName(entityType: EntityType, locale: String): String {
        TODO("Not yet implemented")
    }

}