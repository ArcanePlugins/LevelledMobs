package io.github.arcaneplugins.levelledmobs.customdrops

import org.bukkit.entity.EntityType

/**
 * Provides an interface for 3rd party plugins to
 * add custom drops
 *
 * @author stumper66
 * @since 3.7.0
 */
interface ExternalCustomDrops {
    fun addCustomDrop(customDropInstance: CustomDropInstance)

    fun addCustomDropTable(dropName: String, customDropInstance: CustomDropInstance)

    fun getCustomDrops(): Map<EntityType, CustomDropInstance?>

    fun getCustomDropTables(): Map<String, CustomDropInstance?>

    fun clearAllExternalCustomDrops()
}