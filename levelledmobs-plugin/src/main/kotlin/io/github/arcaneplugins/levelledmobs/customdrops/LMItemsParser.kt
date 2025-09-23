package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.stumper66.lm_items.ExternalItemRequest
import io.github.stumper66.lm_items.LM_Items
import java.util.Hashtable
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.managers.MobDataManager
import io.github.arcaneplugins.levelledmobs.managers.NotifyManager
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * Interfaces with the plugin LM_Items so can custom items from 3rd party plugins can be used
 * directly in custom drops
 *
 * @author stumper66
 * @since 3.5.0
 */
class LMItemsParser {
    private val pendingItems = mutableMapOf<CustomDropItem, String>()

    fun processPendingItems(){
        for (item in pendingItems){
            parseExternalItemAttributes(item.value, item.key)
        }
        pendingItems.clear()
    }

    fun parseExternalItemAttributes(
        materialName: String,
        item: CustomDropItem
    ): Boolean {
        if (!ExternalCompatibilityManager.instance.doesLMIMeetVersionRequirement()) {
            if (ExternalCompatibilityManager.hasLMItemsInstalled) {
                Log.war("customdrops.yml references external item '$materialName' but LM_Items is an old version")
            } else {
                Log.war("customdrops.yml references external item '$materialName' but LM_Items is not installed")
            }
            return false
        }

        if (!MainCompanion.instance.hasFinishedLoading){
            pendingItems[item] = materialName

            // this is a placeholder item for now
            item.itemStack = ItemStack(Material.STICK)
            return true
        }

        val colon = materialName.indexOf(":")
        item.externalPluginName = materialName.take(colon)
        item.externalItemId = materialName.substring(colon + 1)
        val lmitems = LM_Items.plugin

        if (!lmitems.doesSupportPlugin(item.externalPluginName!!)) {
            Log.war("customdrops.yml references item from plugin '${item.externalPluginName}' " +
                    "but LM_Items does not support that plugin")
            return false
        }

        item.isExternalItem = true
        return getExternalItem(item, null)
    }

    fun getExternalItem(
        item: CustomDropItem,
        info: CustomDropProcessingInfo?
    ): Boolean {
        val itemsAPI = LM_Items.plugin.getItemAPIForPlugin(item.externalPluginName!!)

        if (itemsAPI == null) {
            Log.war("Unable to get ItemsAPI from LM_Items for plugin ${item.externalPluginName}")
            return false
        }

        val main = LevelledMobs.instance
        val itemRequest = ExternalItemRequest(item.externalItemId!!)
        itemRequest.itemType = item.externalType
        itemRequest.amount = item.externalAmount
        if (info != null && !item.amountFormula.isNullOrEmpty()){
            itemRequest.amount = CustomDropsHandler.evaluateNumberFormula(
                item.amountFormula,
                "amount-formula",
                info.lmEntity!!
            ).result
        }

        if (main.mainCompanion.externalCompatibilityManager.doesLMIMeetVersionRequirement2()) {
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
                var useKey = key
                var value = item.externalExtras!![useKey]

                if (useKey.endsWith("-formula", ignoreCase = true)) {
                    value = evaluateFormula(useKey, value, info?.lmEntity)
                    useKey = useKey.dropLast(8)
                }
                else if (value is String && value.contains("%")) {
                    if (info != null) {
                        value = main.levelManager.replaceStringPlaceholders(
                            value, info.lmEntity!!,true,info.mobKiller,false)
                    } else if (ExternalCompatibilityManager.hasPapiInstalled) {
                        value = ExternalCompatibilityManager.getPapiPlaceholder(null, value,null)
                    }
                }

                (itemRequest.extras as Hashtable<String, Any>)[useKey] = value
            }
        }

        val result = itemsAPI.getItem(itemRequest)

        if (!result.pluginIsInstalled) {
            Log.war("custom item references plugin '${item.externalPluginName}' but that plugin is not installed")
            return false
        }

        val itemStack = result.itemStack
        if (itemStack == null) {
            if (result.typeIsNotSupported) {
                if (item.externalType == null) {
                    Log.war(
                        "custom item '${item.externalPluginName}:${item.externalItemId}' doesn't support type (null)"
                    )
                } else {
                    Log.war(
                        "custom item '${item.externalPluginName}:${item.externalItemId}' doesn't support type ${item.externalType}"
                    )
                }

                return false
            }

            val msg = if (item.externalType == null && (result.itemStacks == null || result.itemStacks!!.isEmpty()))
                "&4custom item '${item.externalPluginName}:${item.externalItemId}' returned a null item&r"
            else
                "&4custom item '${item.externalPluginName}:${item.externalItemId}' (${item.externalType}) returned a null item&r"

            // on server startup show as warning message
            // after reload show as debug
            if (main.mainCompanion.hasFinishedLoading) {
                DebugManager.log(DebugType.CUSTOM_DROPS) { msg }
            } else {
                Log.war(msg)
            }

            main.customDropsHandler.customDropsParser.invalidExternalItems.add(msg)

            return false
        }

        if (main.mainCompanion.externalCompatibilityManager.doesLMIMeetVersionRequirement2()) {
            if (result.itemStacks != null && result.itemStacks!!.isNotEmpty())
                item.itemStacks = result.itemStacks as MutableList<ItemStack>
            else
                item.itemStack = result.itemStack
        } else {
            item.itemStack = itemStack
        }

        return true
    }

    private fun evaluateFormula(
        name: String,
        value: Any?,
        lmEntity: LivingEntityWrapper?
    ): Any?{
        if (value == null) return null
        if (lmEntity == null) return 1 // maybe an hack but do you have any better ideas?

        val formulaPre = value.toString()
        val formula = LevelledMobs.instance.levelManager.replaceStringPlaceholdersForFormulas(formulaPre, lmEntity)
        val evalResult = MobDataManager.evaluateExpression(formula)
        if (evalResult.hadError){
            NotifyManager.notifyOfError("Error evaluating formula for custom drop extras for '$name' on mob: ${lmEntity.nameIfBaby}, ${evalResult.error}")
            DebugManager.log(DebugType.CUSTOM_DROPS_FORMULA, lmEntity){
                val msg = if (formula == formulaPre)
                    "   formula: '$formula'"
                else
                    "   formulaPre: '$formulaPre'\n" +
                            "   formula: '$formula'"

                "result (error, ${evalResult.error})\n$msg" }
            return null
        }

        val result = evalResult.result

        DebugManager.log(DebugType.CUSTOM_DROPS_FORMULA, lmEntity){
            val msg = if (formula == formulaPre)
                "   formula: '$formula'"
            else
                "   formulaPre: '$formulaPre'\n" +
                        "   formula: '$formula'"

            "result $result\n$msg"}

        return result
    }
}