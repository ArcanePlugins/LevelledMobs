package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.stumper66.lm_items.ExternalItemRequest
import io.github.stumper66.lm_items.LM_Items
import java.util.Hashtable
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.inventory.ItemStack

/**
 * Interfaces with the plugin LM_Items so can custom items from 3rd party plugins can be used
 * directly in custom drops
 *
 * @author stumper66
 * @since 3.5.0
 */
class LMItemsParser {
    fun parseExternalItemAttributes(
        materialName: String,
        item: CustomDropItem
    ): Boolean {
        if (!LevelledMobs.instance.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
            if (ExternalCompatibilityManager.hasLMItemsInstalled()) {
                Utils.logger.warning(
                    String.format(
                        "customdrops.yml references external item '%s' but LM_Items is an old version",
                        materialName
                    )
                )
            } else {
                Utils.logger.warning(
                    String.format(
                        "customdrops.yml references external item '%s' but LM_Items is not installed",
                        materialName
                    )
                )
            }
            return false
        }

        val colon = materialName.indexOf(":")
        item.externalPluginName = materialName.substring(0, colon)
        item.externalItemId = materialName.substring(colon + 1)
        val lmitems = LM_Items.plugin

        if (!lmitems.doesSupportPlugin(item.externalPluginName!!)) {
            Utils.logger.warning(
                String.format(
                    "customdrops.yml references item from plugin '%s' but LM_Items does not support that plugin",
                    item.externalPluginName
                )
            )
            return false
        }

        item.isExternalItem = true
        return getExternalItem(item, null)
    }

    fun getExternalItem(item: CustomDropItem, info: CustomDropProcessingInfo?): Boolean {
        val itemsAPI = LM_Items.plugin.getItemAPIForPlugin(item.externalPluginName!!)

        if (itemsAPI == null) {
            Utils.logger.warning(
                "Unable to get ItemsAPI from LM_Items for plugin " + item.externalPluginName
            )
            return false
        }

        val main = LevelledMobs.instance
        val itemRequest = ExternalItemRequest(item.externalItemId!!)
        itemRequest.itemType = item.externalType
        itemRequest.amount = item.externalAmount

        if (main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement2()) {
            itemRequest.getMultipleItems = "-" == itemRequest.itemId
            itemRequest.minItems = item.minItems
            itemRequest.maxItems = item.maxItems
            itemRequest.allowedList = item.allowedList
            itemRequest.excludedList = item.excludedList
            itemRequest.isDebugEnabled = main.debugManager.isEnabled
        }

        if (item.externalExtras != null) {
            itemRequest.extras = Hashtable(item.externalExtras!!.size)

            for (key in item.externalExtras!!.keys) {
                var value = item.externalExtras!![key]
                if (value is String && value.contains("%")) {
                    if (info != null) {
                        value = main.levelManager.replaceStringPlaceholders(
                            value, info.lmEntity!!,true,info.mobKiller,false)
                    } else if (ExternalCompatibilityManager.hasPapiInstalled()) {
                        value = ExternalCompatibilityManager.getPapiPlaceholder(null, value)
                    }
                }

                (itemRequest.extras as Hashtable<String, Any>)[key] = value
            }
        }

        val result = itemsAPI.getItem(itemRequest)

        if (!result.pluginIsInstalled) {
            Utils.logger.warning(
                String.format(
                    "custom item references plugin '%s' but that plugin is not installed",
                    item.externalPluginName
                )
            )
            return false
        }

        val itemStack = result.itemStack
        if (itemStack == null) {
            if (result.typeIsNotSupported) {
                if (item.externalType == null) {
                    Utils.logger.warning(
                        String.format(
                            "custom item '%s:%s' doesn't support type (null)",
                            item.externalPluginName, item.externalItemId
                        )
                    )
                } else {
                    Utils.logger.warning(
                        String.format(
                            "custom item '%s:%s' doesn't support type %s",
                            item.externalPluginName, item.externalItemId, item.externalType
                        )
                    )
                }

                return false
            }

            val msg = if (item.externalType == null) String.format(
                "&4custom item '%s:%s' returned a null item&r",
                item.externalPluginName, item.externalItemId
            ) else String.format(
                "&4custom item '%s:%s' (%s) returned a null item&r",
                item.externalPluginName, item.externalItemId, item.externalType
            )

            // on server startup show as warning message
            // after reload show as debug
            if (main.companion.hasFinishedLoading) {
                DebugManager.log(DebugType.CUSTOM_DROPS) { msg }
            } else {
                Utils.logger.warning(msg)
            }

            main.customDropsHandler.customDropsParser.invalidExternalItems.add(msg)

            return false
        }

        if (main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement2()) {
            item.itemStacks = result.itemStacks as MutableList<ItemStack>
        } else {
            item.itemStack = itemStack
        }

        return true
    }
}