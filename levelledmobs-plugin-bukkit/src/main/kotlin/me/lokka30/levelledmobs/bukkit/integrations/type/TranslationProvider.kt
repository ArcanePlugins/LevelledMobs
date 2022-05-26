package me.lokka30.levelledmobs.bukkit.integrations.type

import org.bukkit.entity.EntityType

/*
FIXME Comment
 */
interface TranslationProvider {

    /*
    FIXME Comment
     */
    fun getTranslatedEntityName(entityType: EntityType, locale: String): String

}