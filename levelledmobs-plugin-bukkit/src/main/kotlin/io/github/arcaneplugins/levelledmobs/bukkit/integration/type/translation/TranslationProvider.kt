package io.github.arcaneplugins.levelledmobs.bukkit.integration.type.translation

import org.bukkit.entity.EntityType

interface TranslationProvider {
    fun getTranslatedEntityName(
        entityType: EntityType,
        locale: String
    ): String?
}