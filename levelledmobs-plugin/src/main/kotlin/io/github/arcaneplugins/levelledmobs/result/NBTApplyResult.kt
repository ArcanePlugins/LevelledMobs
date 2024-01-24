package io.github.arcaneplugins.levelledmobs.result

import org.bukkit.inventory.ItemStack

/**
 * @author stumper66
 * @since 3.1.0
 */
class NBTApplyResult {
    var itemStack: ItemStack? = null
    var exceptionMessage: String? = null
    var objectsAdded: MutableList<String>? = null
    var objectsUpdated: MutableList<String>? = null
    var objectsRemoved: MutableList<String>? = null

    val hadException: Boolean
        get() = this.exceptionMessage != null
}