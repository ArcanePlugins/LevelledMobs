package io.github.arcaneplugins.levelledmobs.customdrops

import java.util.TreeMap
import org.bukkit.entity.EntityType

/**
 * This class allows 3rd parties to add custom drops directly to LevelledMobs
 *
 * @author stumper66
 * @since 3.7.0
 */
class ExternalCustomDropsImpl : ExternalCustomDrops {
    val customDropsitems = mutableMapOf<EntityType, CustomDropInstance>()
    val customDropIDs: MutableMap<String, CustomDropInstance> = TreeMap(java.lang.String.CASE_INSENSITIVE_ORDER)

    override fun addCustomDrop(customDropInstance: CustomDropInstance) {
        customDropsitems[customDropInstance.associatedMob!!] = customDropInstance
    }

    override fun addCustomDropTable(dropName: String, customDropInstance: CustomDropInstance) {
        customDropIDs[dropName] = customDropInstance
    }

    override fun getCustomDrops(): Map<EntityType, CustomDropInstance?> {
        return this.customDropsitems
    }

    override fun getCustomDropTables(): Map<String, CustomDropInstance?> {
        return this.customDropIDs
    }

    override fun clearAllExternalCustomDrops() {
        customDropsitems.clear()
        customDropIDs.clear()
    }
}