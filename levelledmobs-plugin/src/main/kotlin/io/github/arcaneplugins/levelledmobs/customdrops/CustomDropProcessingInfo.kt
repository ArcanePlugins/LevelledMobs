package io.github.arcaneplugins.levelledmobs.customdrops

import java.util.TreeMap
import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.DeathCause
import io.github.arcaneplugins.levelledmobs.enums.EquipmentClass
import io.github.arcaneplugins.levelledmobs.rules.CustomDropsRuleSet
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Used to store information when a custom drop item is being requested either during mob spawn in
 * for equipped items or after mob death to get the items the mob will potentially drop
 *
 * @author stumper66
 * @since 2.4.1
 */
class CustomDropProcessingInfo {
    var lmEntity: LivingEntityWrapper? = null
    var mobKiller: Player? = null
    val playerLevelVariableCache = mutableMapOf<String, Int>()
    var deathCause: DeathCause? = null
    var addition = 0f
    var isSpawner = false
    var equippedOnly = false
    var deathByFire = false
    var wasKilledByPlayer = false
    var doNotMultiplyDrops  = false
    var hasOverride = false
    var hasCustomDropId = false
    var hasEquippedItems = false
    var retryNumber = 0
    var equippedChanceRole = 0f
    var groupLimits: GroupLimits? = null
    var customDropId: String? = null
    var itemWasEquipped = false
    var equipmentClass: EquipmentClass? = null
    var newDrops: MutableList<ItemStack>? = null
    var dropInstance: CustomDropInstance? = null
    var equippedItemsInfo: EquippedItemsInfo? = null
    var overallChanceDebugMessage: String = ""
    private val groupIDsDroppedAlready: MutableMap<String, Int> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    private val itemsDroppedById = mutableMapOf<UUID, Int>()
    var prioritizedDrops: MutableMap<Int, MutableList<CustomDropBase>>? = null
    var dropRules: CustomDropsRuleSet? = null
    val allDropInstances=  mutableListOf<CustomDropInstance>()
    private var debugMessages: StringBuilder? = null
    val stackToItem = mutableListOf<Map.Entry<ItemStack, CustomDropItem>>()

    fun itemGotDropped(dropBase: CustomDropBase, amountDropped: Int) {
        if (dropBase.hasGroupId) {
            val count = groupIDsDroppedAlready.getOrDefault(
                dropBase.groupId, 0
            ) + amountDropped

            groupIDsDroppedAlready[dropBase.groupId!!] = count
        }

        val count = itemsDroppedById.getOrDefault(dropBase.uid, 0) + amountDropped
        itemsDroppedById[dropBase.uid] = count

        if (equipmentClass != null && dropBase is CustomDropItem && equippedItemsInfo != null){
            EquippedItemsInfo.droppedEquipmentByClass.add(equipmentClass!!)
        }
    }

    fun getDropItemsCountForGroup(dropBase: CustomDropBase): Int {
        val useGroupId = if (dropBase.hasGroupId) dropBase.groupId else "default"

        return groupIDsDroppedAlready.getOrDefault(useGroupId, 0)
    }

    fun getItemsDropsById(dropBase: CustomDropBase): Int {
        return itemsDroppedById.getOrDefault(dropBase.uid, 0)
    }

    fun getItemsDropsByGroup(dropBase: CustomDropBase): Int {
        val useGroupId = if (dropBase.hasGroupId) dropBase.groupId else "default"

        return groupIDsDroppedAlready.getOrDefault(useGroupId, 0)
    }

    fun addDebugMessage(debugType: DebugType, message: String?) {
        if (!LevelledMobs.instance.debugManager.isDebugTypeEnabled(debugType)) {
            return
        }

        addDebugMessage(message)
    }

    fun addDebugMessage(message: String?) {
        if (this.debugMessages == null) {
            this.debugMessages = StringBuilder()
        }

        if (debugMessages!!.isNotEmpty()) {
            debugMessages!!.append(System.lineSeparator())
        }

        debugMessages!!.append(message)
    }

    fun writeAnyDebugMessages() {
        if (this.debugMessages == null || debugMessages!!.isEmpty()) {
            return
        }

        DebugManager.log(DebugType.CUSTOM_DROPS, lmEntity) { debugMessages.toString() }
        debugMessages!!.setLength(0)
    }
}