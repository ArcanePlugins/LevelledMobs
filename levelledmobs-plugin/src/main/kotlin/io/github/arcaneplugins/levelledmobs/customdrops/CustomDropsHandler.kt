package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.MainCompanion
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.DeathCause
import io.github.arcaneplugins.levelledmobs.enums.DropInstanceBuildResult
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.result.PlayerLevelSourceResult
import io.github.arcaneplugins.levelledmobs.enums.LevelledMobSpawnReason
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.PaperUtils
import io.github.arcaneplugins.levelledmobs.util.SpigotUtils
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.LinkedList
import java.util.Locale
import java.util.TreeMap
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.ThreadLocalRandom
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable

/**
 * The main CustomDropsclass that holds useful functions for parsing, instantizing and more of
 * custom drop items
 *
 * @author stumper66
 * @since 2.4.0
 */
class CustomDropsHandler {
    // regular custom drops defined for a mob type
    private val customDropsitems = mutableMapOf<EntityType, CustomDropInstance>()
    // regular custom drops defined for a mob type that is a baby
    val customDropsitemsBabies = mutableMapOf<EntityType, CustomDropInstance>()

    // only used for the built-in universal groups
    private val customDropsitemsGroups = mutableMapOf<String, CustomDropInstance>()

    // these are drops defined by a drop table
    val customDropIDs: MutableMap<String, CustomDropInstance> = TreeMap(String.CASE_INSENSITIVE_ORDER)

    // mappings of groupIds to drop instance
    private val groupIdToInstance: MutableMap<String, CustomDropInstance> = TreeMap(String.CASE_INSENSITIVE_ORDER)

    // get a drop instance from it's groupid
    var customItemGroups = mutableMapOf<String, CustomDropInstance>()

    // groupid to grouplimits map
    val groupLimitsMap: MutableMap<String, GroupLimits> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    val customDropsParser = CustomDropsParser(this)
    val externalCustomDrops: ExternalCustomDrops = ExternalCustomDropsImpl()
    var lmItemsParser: LMItemsParser? = null
        private set
    private val customEquippedItems = WeakHashMap<LivingEntity, EquippedItemsInfo>()

    fun load(){
        if (ExternalCompatibilityManager.instance.doesLMIMeetVersionRequirement()) {
            this.lmItemsParser = LMItemsParser()
        }
    }

    fun clearDrops(){
        customDropsitems.clear()
        customDropsitemsBabies.clear()
        customDropsitemsGroups.clear()
        customDropIDs.clear()
        groupIdToInstance.clear()
        customItemGroups.clear()
        groupLimitsMap.clear()
    }

    fun getCustomDropsitems(): MutableMap<EntityType, CustomDropInstance> {
        val drops = mutableMapOf<EntityType, CustomDropInstance>()
        drops.putAll(this.customDropsitems)

        for (entityType in externalCustomDrops.getCustomDrops().keys) {
            val dropInstance = externalCustomDrops.getCustomDrops()[entityType]!!
            if (!drops.containsKey(entityType)) {
                drops[entityType] = dropInstance
                continue
            }

            // merge the 3rd party drops into the defined drops for the entity
            // 3rd party drop settings will override any conflicting
            val currentDropInstance = drops[entityType]
            currentDropInstance!!.combineDrop(dropInstance)

            if (dropInstance.overallChance != null) currentDropInstance.overallChance = dropInstance.overallChance
            currentDropInstance.overallPermissions.addAll(dropInstance.overallPermissions)
        }

        return drops
    }

    fun getCustomDropsitemsGroups(): MutableMap<String, CustomDropInstance> {
        val drops = mutableMapOf<String, CustomDropInstance>()
        drops.putAll(this.customItemGroups)
        drops.putAll(this.customDropsitemsGroups)

        for (groupName in externalCustomDrops.getCustomDropTables().keys) {
            val dropInstance: CustomDropInstance = externalCustomDrops.getCustomDropTables()[groupName]!!
            if (!drops.containsKey(groupName)) {
                drops[groupName] = dropInstance
                continue
            }

            // merge the 3rd party drops into the defined drops for the entity
            // 3rd party drop settings will override any conflicting
            val currentDropInstance = drops[groupName]
            currentDropInstance!!.combineDrop(dropInstance)

            if (dropInstance.overallChance != null) currentDropInstance.overallChance = dropInstance.overallChance
            currentDropInstance.overallPermissions.addAll(dropInstance.overallPermissions)
        }

        return drops
    }

    fun addCustomDropItem(entityType: EntityType, customDropInstance: CustomDropInstance) {
        customDropsitems[entityType] = customDropInstance
    }

    fun addCustomDropGroup(groupName: String, customDropInstance: CustomDropInstance) {
        customDropsitemsGroups[groupName] = customDropInstance
    }

    fun getCustomItemDrops(
        lmEntity: LivingEntityWrapper,
        drops: MutableList<ItemStack>,
        equippedOnly: Boolean
    ): CustomDropResult {
        val processingInfo = CustomDropProcessingInfo()
        processingInfo.lmEntity = lmEntity
        processingInfo.equippedOnly = equippedOnly
        processingInfo.newDrops = drops

        val main = LevelledMobs.instance
        processingInfo.dropRules = main.rulesManager.getRuleUseCustomDropsForMob(lmEntity)
        synchronized(lmEntity.livingEntity.persistentDataContainer) {
            processingInfo.isSpawner = (lmEntity.pdc
                .has(NamespacedKeys.spawnReasonKey, PersistentDataType.STRING) &&
                    LevelledMobSpawnReason.SPAWNER.toString() == lmEntity.pdc
                        .get(NamespacedKeys.spawnReasonKey, PersistentDataType.STRING)
                    )
            if (lmEntity.pdc
                    .has(NamespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING)
            ) {
                processingInfo.customDropId = lmEntity.pdc
                    .get(NamespacedKeys.keySpawnerCustomDropId, PersistentDataType.STRING)
                processingInfo.hasCustomDropId = !processingInfo.customDropId.isNullOrEmpty()
            }
        }

        if (lmEntity.associatedPlayer != null) {
            processingInfo.wasKilledByPlayer = true
            processingInfo.mobKiller = lmEntity.associatedPlayer
        } else {
            processingInfo.wasKilledByPlayer = false
        }

        if (lmEntity.livingEntity.lastDamageCause != null) {
            processingInfo.deathCause = DeathCause.valueOf(
                lmEntity.livingEntity.lastDamageCause!!.cause.toString()
                    .uppercase(Locale.getDefault())
            )
        }

        processingInfo.addition = BigDecimal.valueOf(
            main.mobDataManager.getAdditionsForLevel(lmEntity, Addition.CUSTOM_ITEM_DROP, 2F).amount.toDouble()
        )
            .setScale(0, RoundingMode.HALF_DOWN).intValueExact().toDouble() // truncate double to int

        processingInfo.doNotMultiplyDrops = main.rulesManager.getRuleCheckIfNoDropMultiplierEntitiy(
            lmEntity
        )

        if (lmEntity.livingEntity.lastDamageCause != null) {
            val damageCause = lmEntity.livingEntity
                .lastDamageCause!!.cause
            processingInfo.deathByFire =
                (damageCause == EntityDamageEvent.DamageCause.FIRE ||
                        damageCause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                        damageCause == EntityDamageEvent.DamageCause.LAVA)
        }

        if (!equippedOnly) {
            val mobLevel =
                if (lmEntity.getMobLevel > 0) "&r (level " + lmEntity.getMobLevel + ")" else ""
            processingInfo.addDebugMessage(
                DebugType.CUSTOM_DROPS,
                "&7Custom drops for &b" + lmEntity.nameIfBaby + mobLevel
            )

            processingInfo.addDebugMessage(
                DebugType.MOB_GROUPS,
                "&8- &7Groups: &b" + java.lang.String.join("&7, &b", lmEntity.getApplicableGroups()) + "&7."
            )
        }

        val groupsList = mutableListOf<String>()
        for (group in lmEntity.getApplicableGroups()) {
            if (!getCustomDropsitemsGroups().containsKey(group)) {
                continue
            }

            groupsList.add(group)
        }

        val buildResult: DropInstanceBuildResult = buildDropsListFromGroupsAndEntity(
            groupsList, lmEntity.entityType, processingInfo
        )
        if (buildResult != DropInstanceBuildResult.SUCCESSFUL) {
            // didn't make overall chance
            if (buildResult == DropInstanceBuildResult.DID_NOT_MAKE_CHANCE) {
                processingInfo.addDebugMessage(
                    DebugType.CUSTOM_DROPS,
                    String.format(
                        "&7%s (%s) - didn't make overall chance",
                        lmEntity.typeName, lmEntity.getMobLevel
                    )
                )
            } else {
                processingInfo.addDebugMessage(
                    DebugType.CUSTOM_DROPS, String.format(
                        "&7%s (%s) - didn't make overall chance permission for player: &b%s &r",
                        lmEntity.typeName, lmEntity.getMobLevel,
                        if (processingInfo.mobKiller == null) "(null)"
                        else processingInfo.mobKiller!!.name
                    )
                )
            }
            processingInfo.writeAnyDebugMessages()

            return CustomDropResult(processingInfo.stackToItem, processingInfo.hasOverride, false)
        }

        getCustomItemsFromDropInstance(processingInfo) // payload

        val postCount = drops.size
        val showCustomEquips = main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_EQUIPS)
        val showCustomDrops = main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)

        if (showCustomDrops || showCustomEquips) {
            if (equippedOnly && drops.isNotEmpty() && showCustomEquips) {
                if (lmEntity.getMobLevel > -1) {
                    processingInfo.addDebugMessage(
                        String.format(
                            "&7Custom equipment for &b%s &r(%s)", lmEntity.typeName,
                            lmEntity.getMobLevel
                        )
                    )
                } else {
                    processingInfo.addDebugMessage(
                        "&7Custom equipment for &b" + lmEntity.typeName + "&r"
                    )
                }
                val sb = StringBuilder()
                for (drop in drops) {
                    if (sb.isNotEmpty()) {
                        sb.append(", ")
                    }
                    sb.append(drop.type.name)
                }
                processingInfo.addDebugMessage("   $sb")
            } else if (!equippedOnly && showCustomDrops) {
                processingInfo.addDebugMessage(
                    String.format("&8 --- &7Custom items added: &b%s&7.", postCount)
                )
            }

            processingInfo.writeAnyDebugMessages()
        }

        return CustomDropResult(processingInfo.stackToItem, processingInfo.hasOverride, postCount > 0)
    }

    private fun buildDropsListFromGroupsAndEntity(
        groups: MutableList<String>,
        entityType: EntityType,
        info: CustomDropProcessingInfo
    ): DropInstanceBuildResult {
        info.prioritizedDrops = mutableMapOf()
        info.hasOverride = false
        var usesGroupIds = false

        val overrideNonDropTableDrops =
            info.dropRules != null && info.dropRules!!.chunkKillOptions!!.getDisableVanillaDrops()

        for (id in getDropIds(info)) {
            if (!customItemGroups.containsKey(id.trim())) {
                Utils.logger.warning("rule specified an invalid value for use-droptable-id: $id")
                continue
            }

            val dropInstance = customItemGroups[id.trim { it <= ' ' }]
            info.allDropInstances.add(dropInstance!!)

            for (baseItem in dropInstance.customItems) {
                processDropPriorities(baseItem, info)
            }

            if (dropInstance.utilizesGroupIds) {
                usesGroupIds = true
            }
            if (dropInstance.getOverrideStockDrops) {
                info.hasOverride = true
            }
        }

        if (!overrideNonDropTableDrops) {
            for (group in groups) {
                val dropInstance = getCustomDropsitemsGroups()[group]
                info.allDropInstances.add(dropInstance!!)

                for (baseItem in dropInstance.customItems) {
                    processDropPriorities(baseItem, info)
                }

                if (dropInstance.utilizesGroupIds) {
                    usesGroupIds = true
                }
                if (dropInstance.getOverrideStockDrops) {
                    info.hasOverride = true
                }
            }

            val dropMap: Map<EntityType, CustomDropInstance> =
                if (info.lmEntity!!.isBabyMob && customDropsitemsBabies.containsKey(entityType)) customDropsitemsBabies else getCustomDropsitems()

            if (dropMap.containsKey(entityType)) {
                val dropInstance = dropMap[entityType]
                info.allDropInstances.add(dropInstance!!)

                for (baseItem in dropInstance.customItems) {
                    processDropPriorities(baseItem, info)
                }

                if (dropInstance.utilizesGroupIds) {
                    usesGroupIds = true
                }
                if (dropInstance.getOverrideStockDrops) {
                    info.hasOverride = true
                }
            }
        }

        if (usesGroupIds) {
            for (customDropBases in info.prioritizedDrops!!.values) {
                customDropBases.shuffled()
            }
        }

        if (!checkOverallPermissions(info)) {
            return DropInstanceBuildResult.PERMISSION_DENIED
        }

        if (info.equippedOnly && !info.hasEquippedItems) {
            return DropInstanceBuildResult.SUCCESSFUL
        }

        return if (checkOverallChance(info)) DropInstanceBuildResult.SUCCESSFUL else DropInstanceBuildResult.DID_NOT_MAKE_CHANCE
    }

    private fun checkOverallPermissions(info: CustomDropProcessingInfo): Boolean {
        var hadAnyPerms = false
        for (dropInstance in info.allDropInstances) {
            if (dropInstance.overallPermissions.isEmpty()) {
                continue
            }

            hadAnyPerms = true
            for (perm in dropInstance.overallPermissions) {
                if (info.mobKiller == null) {
                    continue
                }
                val checkPerm = "LevelledMobs.permission.$perm"
                if (info.mobKiller!!.hasPermission(checkPerm)) {
                    return true
                }
            }
        }

        return !hadAnyPerms
    }

    private fun getDropIds(
        processingInfo: CustomDropProcessingInfo
    ): MutableList<String> {
        val dropIds = mutableListOf<String>()
        if (processingInfo.dropRules != null) {
            for (id in processingInfo.dropRules!!.useDropTableIds) {
                dropIds.addAll(id.split(","))
            }
        }

        if (processingInfo.hasCustomDropId && !dropIds.contains(processingInfo.customDropId)) {
            dropIds.add(processingInfo.customDropId!!)
        }

        return dropIds
    }

    private fun processDropPriorities(
        baseItem: CustomDropBase,
        processingInfo: CustomDropProcessingInfo
    ) {
        val priority = -baseItem.priority
        if (processingInfo.prioritizedDrops!!.containsKey(priority)) {
            processingInfo.prioritizedDrops!![priority]!!.add(baseItem)
        } else {
            val items: MutableList<CustomDropBase> = LinkedList()
            items.add(baseItem)
            processingInfo.prioritizedDrops!![priority] = items
        }

        if (baseItem is CustomDropItem
            && baseItem.equippedChance != null && !baseItem.equippedChance!!.isDefault
        ) {
            processingInfo.hasEquippedItems = true
        }
    }

    private fun getCustomItemsFromDropInstance(
        info: CustomDropProcessingInfo
    ) {
        val dropLimitsReached = mutableListOf<UUID>()
        val defaultLimits = groupLimitsMap.getOrDefault("default", null)!!

        for (items in info.prioritizedDrops!!.values) {
            // loop thru each drop list associated with any groupids

            val retriesHardcodedMax = 10
            var maxRetries = 1

            var i = 0
            while (i < maxRetries) {
                info.retryNumber = i

                for (drop in items) {
                    // loop thru all drops in this groupid

                    if (drop.hasGroupId) {
                        info.dropInstance = groupIdToInstance[drop.groupId]
                        info.groupLimits = groupLimitsMap.getOrDefault(drop.groupId, defaultLimits)
                        maxRetries =
                            info.groupLimits!!.retries.coerceAtMost(retriesHardcodedMax)
                    } else {
                        info.dropInstance = null
                        info.groupLimits = null
                    }

                    if (info.groupLimits != null) {
                        val itemDroppedCount = info.getItemsDropsById(drop)
                        if (info.groupLimits!!.hasReachedCapPerItem(itemDroppedCount)) {
                            if (!dropLimitsReached.contains(drop.uid)) {
                                dropLimitsReached.add(drop.uid)
                                val itemDescription =
                                    if ((drop is CustomDropItem)) drop.material.name else "CustomCommand"
                                DebugManager.log(DebugType.GROUP_LIMITS, info.lmEntity!!) {
                                    java.lang.String.format(
                                        "Reached cap-per-item limit of %s for %s",
                                        info.groupLimits!!.capPerItem, itemDescription
                                    )
                                }
                            }
                            continue
                        }

                        val groupDroppedCount = info.getDropItemsCountForGroup(drop)
                        if (info.groupLimits!!.hasReachedCapTotal(groupDroppedCount)) {
                            DebugManager.log(DebugType.GROUP_LIMITS, info.lmEntity!!) {
                                java.lang.String.format(
                                    "Reached cap-total of %s for group: %s",
                                    info.groupLimits!!.capTotal, drop.groupId
                                )
                            }
                            return
                        }
                    }

                    // payload:
                    getDropsFromCustomDropItem(info, drop)
                }

                i++
            }
        } // next group
    }

    private fun getDropsFromCustomDropItem(
        info: CustomDropProcessingInfo,
        dropBase: CustomDropBase
    ) {
        if (dropBase is CustomCommand && info.lmEntity!!.livingEntity
                .hasMetadata("noCommands") ||
            info.lmEntity!!.deathCause == EntityDamageEvent.DamageCause.VOID
        ) {
            return
        }

        if (info.equippedOnly && dropBase is CustomCommand
            && !dropBase.runOnSpawn
        ) {
            return
        }
        if (!info.equippedOnly && dropBase.playerCausedOnly && (dropBase.causeOfDeathReqs == null
                    || dropBase.causeOfDeathReqs!!.isEmpty()) && !info.wasKilledByPlayer
        ) {
            return
        }
        if (dropBase.noSpawner && info.isSpawner) {
            return
        }

        if (shouldDenyDeathCause(dropBase, info)) {
            return
        }

        if (!madePlayerLevelRequirement(info, dropBase)) {
            return
        }

        if (dropBase.excludedMobs.contains(info.lmEntity!!.typeName)) {
            if (dropBase is CustomDropItem && !info.equippedOnly) {
                info.addDebugMessage(
                    DebugType.CUSTOM_DROPS, String.format(
                        "&8 - &7Mob: &b%s&7, item: %s, mob was excluded", info.lmEntity!!.typeName,
                        dropBase.material.name
                    )
                )
            }
            return
        }

        val main = LevelledMobs.instance
        var doDrop =
            dropBase.maxLevel <= -1 || info.lmEntity!!.getMobLevel <= dropBase.maxLevel
        if (dropBase.minLevel > -1 && info.lmEntity!!.getMobLevel < dropBase.minLevel) {
            doDrop = false
        }
        if (!doDrop) {
            if (dropBase is CustomDropItem) {
                if (!info.equippedOnly && main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                    val itemStack: ItemStack =
                        if (info.deathByFire) getCookedVariantOfMeat(dropBase.itemStack!!)
                        else dropBase.itemStack!!

                    info.addDebugMessage(
                        String.format(
                            "&8- &7level: &b%s&7, fromSpawner: &b%s&7, item: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, dropped: &bfalse",
                            info.lmEntity!!.getMobLevel, info.isSpawner, itemStack.type.name,
                            dropBase.minLevel, dropBase.maxLevel, dropBase.noSpawner
                        )
                    )
                }
            } else if (dropBase is CustomCommand) {
                info.addDebugMessage(
                    DebugType.CUSTOM_DROPS, String.format(
                        "&8- custom-cmd: &7level: &b%s&7, fromSpawner: &b%s&7, minL: &b%s&7, maxL: &b%s&7, nospawner: &b%s&7, executed: &bfalse",
                        info.lmEntity!!.getMobLevel, info.isSpawner, dropBase.minLevel,
                        dropBase.maxLevel, dropBase.noSpawner
                    )
                )
            }
            return
        }

        // equip-chance and equip-drop-chance:
        if (!info.equippedOnly && dropBase is CustomDropItem) {
            if (!checkIfMadeEquippedDropChance(info, dropBase)) {
                if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                    info.addDebugMessage(
                        "&8 - &7item: &b${dropBase.itemStack!!.type.name}&7, was not equipped on mob, dropped: &bfalse&7.",
                    )
                }
                return
            }
        }

        if (!info.equippedOnly && !checkDropPermissions(info, dropBase)) {
            return
        }

        val runOnSpawn = dropBase is CustomCommand && dropBase.runOnSpawn
        var didNotMakeChance = false
        var chanceRole = 0.0f

        if (!info.equippedOnly && dropBase.useChunkKillMax && info.wasKilledByPlayer
            && hasReachedChunkKillLimit(info.lmEntity!!)
        ) {
            if (dropBase is CustomDropItem) {
                info.addDebugMessage(
                    DebugType.CUSTOM_DROPS, String.format(
                        "&8- &7level: &b%s&7, item: &b%s&7, gId: &b%s&7, chunk kill count reached",
                        info.lmEntity!!.getMobLevel,
                        dropBase.material.name, dropBase.groupId
                    )
                )
            } else {
                info.addDebugMessage(
                    DebugType.CUSTOM_DROPS, String.format(
                        "&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, chunk kill count reached",
                        info.lmEntity!!.getMobLevel, dropBase.groupId
                    )
                )
            }

            return
        }

        val dropChance =
            if (dropBase.chance != null) dropBase.chance!!.getSlidingChance(info.lmEntity!!.getMobLevel) else 0.0f
        if ((!info.equippedOnly || runOnSpawn) && dropChance < 1.0f) {
            chanceRole =
                if (dropChance > 0.0f) ThreadLocalRandom.current().nextInt(0, 100001).toFloat() * 0.00001f else 0.0f
            if (1.0f - chanceRole >= dropChance) {
                didNotMakeChance = true
            }
        }

        if (didNotMakeChance && (!info.equippedOnly || runOnSpawn) && main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
            if (dropBase is CustomDropItem) {
                val itemStack: ItemStack =
                    if (info.deathByFire) getCookedVariantOfMeat(dropBase.itemStack!!)
                    else dropBase.itemStack!!

                info.addDebugMessage(
                    DebugType.CUSTOM_DROPS, java.lang.String.format(
                        "&8 - &7item: &b%s&7, amount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &bfalse&7.",
                        itemStack.type.name, dropBase.amountAsString, dropBase.chance?.showMatchedChance(),
                        Utils.round(chanceRole.toDouble(), 4)
                    )
                )
            } else {
                info.addDebugMessage(
                    DebugType.CUSTOM_DROPS, java.lang.String.format(
                        "&8 - &7Custom command&7, chance: &b%s&7, chanceRole: &b%s&7, executed: &bfalse&7.",
                        dropBase.chance?.showMatchedChance(), Utils.round(chanceRole.toDouble(), 4)
                    )
                )
            }
        }
        if ((!info.equippedOnly || runOnSpawn) && didNotMakeChance) {
            return
        }

        var maxDropGroup = 0
        if (info.groupLimits != null && info.groupLimits!!.hasCapSelect) {
            maxDropGroup = info.groupLimits!!.capSelect.coerceAtLeast(0)
        } else if (info.groupLimits == null) {
            maxDropGroup = dropBase.maxDropGroup
        }

        if (!info.equippedOnly && dropBase.hasGroupId) {
            // legacy section, only executed if the old 'maxdropgroup' was used
            // instead of 'group-limits.capSelect'
            val groupDroppedCount = info.getItemsDropsByGroup(dropBase)

            if (maxDropGroup in 1..groupDroppedCount
                || info.groupLimits == null && maxDropGroup == 0 && groupDroppedCount > 0
            ) {
                if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                    if (dropBase is CustomDropItem) {
                        info.addDebugMessage(
                            String.format(
                                "&8- &7level: &b%s&7, item: &b%s&7, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, dropped: &bfalse",
                                info.lmEntity!!.getMobLevel,
                                dropBase.material.name, dropBase.groupId,
                                info.getItemsDropsByGroup(dropBase), groupDroppedCount
                            )
                        )
                    } else {
                        info.addDebugMessage(
                            String.format(
                                "&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, executed: &bfalse",
                                info.lmEntity!!.getMobLevel,
                                info.getItemsDropsByGroup(dropBase),
                                dropBase.maxDropGroup,
                                groupDroppedCount
                            )
                        )
                    }
                }
                return
            }
        }

        if (dropBase is CustomCommand) {
            // ------------------------------------------ commands get executed here then function returns ---------------------------------------------------
            executeCommand(dropBase, info)

            if (dropBase.hasGroupId) {
                if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                    val count = info.getItemsDropsByGroup(dropBase)
                    var msg = String.format(
                        "&8- &7level: &b%s&7, item: command, gId: &b%s&7, maxDropGroup: &b%s&7, groupDropCount: &b%s&7, executed: &btrue",
                        info.lmEntity!!.getMobLevel, dropBase.groupId, dropBase.maxDropGroup, count
                    )
                    if (info.retryNumber > 0) {
                        msg += ", retry: ${info.retryNumber}"
                    }
                    info.addDebugMessage(msg)
                }
            } else if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                var msg = String.format(
                    "&8- &7level: &b%s&7, item: custom command, gId: &b%s&7, maxDropGroup: &b%s&7, executed: &btrue",
                    info.lmEntity!!.getMobLevel, dropBase.groupId, dropBase.maxDropGroup
                )

                if (info.retryNumber > 0) {
                    msg += ", retry: ${info.retryNumber}"
                }
                info.addDebugMessage(msg)
            }

            return
            // -----------------------------------------------------------------------------------------------------------------------------------------------
        }
        if (dropBase !is CustomDropItem) {
            Utils.logger.warning("Unsupported drop type: " + dropBase.javaClass.name)
            return
        }

        if (!checkEquippedChances(info, dropBase)) return

        var newDropAmount = dropBase.amount
        if (dropBase.hasAmountRange) {
            val change = ThreadLocalRandom.current()
                .nextInt(0, dropBase.amountRangeMax - dropBase.amountRangeMin + 1)
            newDropAmount = dropBase.amountRangeMin + change
        }

        if (dropBase.hasGroupId && info.groupLimits != null) {
            val gl = info.groupLimits!!

            if (gl.hasCapPerItem) {
                newDropAmount = newDropAmount.coerceAtMost(gl.capPerItem)
            }

            if (gl.hasCapTotal && dropBase.hasGroupId) {
                val hasDroppedSoFar = info.getDropItemsCountForGroup(dropBase)
                if (gl.capTotal - hasDroppedSoFar > gl.capTotal) {
                    newDropAmount = gl.capTotal
                }
            }
        }

        // if we made it this far then the item will be dropped
        if (dropBase.isExternalItem &&
            !main.mainCompanion.externalCompatibilityManager.doesLMIMeetVersionRequirement()
        ) {
            Utils.logger.warning("Could not get external custom item - LM_Items is not installed")
        }

        if (dropBase.isExternalItem && main.mainCompanion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
            lmItemsParser!!.getExternalItem(dropBase, info)
        }

        if (dropBase.itemStacks == null) return

        for (newItemPre in dropBase.itemStacks!!) {
            // will only be multiple items for supported LM Items items

            var newItem = newItemPre.clone()

            processEnchantmentChances(dropBase, newItem)

            if (info.deathByFire) {
                newItem = getCookedVariantOfMeat(dropBase.itemStack!!)
            }

            if (newDropAmount > 1) newItem.amount = newDropAmount

            if (!dropBase.noMultiplier && !info.doNotMultiplyDrops) {
                main.levelManager.multiplyDrop(info.lmEntity!!, newItem, info.addition, true)
                newDropAmount = newItem.amount
            } else if (newDropAmount > newItem.maxStackSize) {
                newDropAmount = newItem.maxStackSize
            }

            if (newItem.amount != newDropAmount) {
                newItem.amount = newDropAmount
            }

            if (info.equippedOnly && main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_EQUIPS)) {
                val equippedChance =
                    if (dropBase.equippedChance != null) dropBase.equippedChance!!.showMatchedChance() else "0.0"
                info.addDebugMessage(
                    java.lang.String.format(
                        "&8 - &7item: &b%s&7, equipChance: &b%s&7, chanceRole: &b%s&7, equipped: &btrue&7.",
                        newItem.type.name, equippedChance,
                        Utils.round(info.equippedChanceRole.toDouble(), 4)
                    )
                )
            } else if (!info.equippedOnly && main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                val retryMsg = if (info.retryNumber > 0) ", retry: " + info.retryNumber else ""

                info.addDebugMessage(
                    java.lang.String.format(
                        "&8 - &7item: &b%s&7, amount: &b%s&7, newAmount: &b%s&7, chance: &b%s&7, chanceRole: &b%s&7, dropped: &btrue&7%s.",
                        newItem.type.name, dropBase.amountAsString, newDropAmount,
                        dropBase.chance?.showMatchedChance(), Utils.round(chanceRole.toDouble(), 4), retryMsg
                    )
                )
            }

            var damage: Int = dropBase.damage
            if (dropBase.hasDamageRange) {
                damage = ThreadLocalRandom.current()
                    .nextInt(dropBase.damageRangeMin, dropBase.damageRangeMax + 1)
            }

            if (damage > 0 || dropBase.lore != null || dropBase.customName != null) {
                val meta = newItem.itemMeta

                if (damage > 0 && meta is Damageable) {
                    meta.damage = damage
                }

                if (meta != null && dropBase.lore != null && dropBase.lore!!.isNotEmpty()) {
                    val newLore: MutableList<String> = ArrayList(
                        dropBase.lore!!.size
                    )

                    for (lorePre in dropBase.lore!!) {
                        var lore = lorePre
                        if (lore.contains("%")) {
                            lore = lore.replace("%player%", if (info.mobKiller == null) "" else info.mobKiller!!.name)
                            lore = main.levelManager.replaceStringPlaceholders(
                                lore,
                                info.lmEntity!!,
                                true,
                                info.mobKiller,
                                false
                            )
                        }

                        newLore.add(lore)

                        if (main.ver.isRunningPaper && main.mainCompanion.useAdventure) {
                            PaperUtils.updateItemMetaLore(meta, newLore)
                        } else {
                            SpigotUtils.updateItemMetaLore(meta, newLore)
                        }
                    }
                }

                if (meta != null && dropBase.customName != null && dropBase.customName!!.isNotEmpty()) {
                    var customName = dropBase.customName!!.replace(
                        "%player%",
                        if (info.mobKiller == null) "" else info.mobKiller!!.name
                    )
                    customName = main.levelManager.replaceStringPlaceholders(
                        customName,
                        info.lmEntity!!,
                        true,
                        info.mobKiller,
                        false
                    )

                    if (main.ver.isRunningPaper && main.mainCompanion.useAdventure) {
                        PaperUtils.updateItemDisplayName(meta, customName)
                    } else {
                        SpigotUtils.updateItemDisplayName(meta, colorizeAll(customName))
                    }
                }

                newItem.setItemMeta(meta)
            }

            if (!info.equippedOnly) info.itemGotDropped(dropBase, newDropAmount)

            info.newDrops!!.add(newItem)
            info.stackToItem.add(Utils.getPair(newItem, dropBase))
        }
    }

    private fun checkEquippedChances(
        info: CustomDropProcessingInfo,
        dropItem: CustomDropItem
    ): Boolean {
        if (!info.equippedOnly) return true
        val equippedChance = if (dropItem.equippedChance != null) dropItem.equippedChance!!.getSlidingChance(
            info.lmEntity!!.getMobLevel
        ) else 0.0f
        if (equippedChance >= 1.0f) return true

        info.equippedChanceRole =
            if (equippedChance > 0.0f) ThreadLocalRandom.current().nextInt(0, 100001).toFloat() * 0.00001f else 0.0f

        if (equippedChance <= 0.0f || 1.0f - info.equippedChanceRole >= equippedChance) {
            if (LevelledMobs.instance.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_EQUIPS)) {
                info.addDebugMessage(
                    String.format(
                        "&8- Mob: &b%s&7, &7level: &b%s&7, item: &b%s&7, equipchance: &b%s&7, chancerole: &b%s&7, did not make equipped chance",
                        info.lmEntity!!.typeName, info.lmEntity!!.getMobLevel,
                        dropItem.material.name, dropItem.equippedChance!!.showMatchedChance(),
                        Utils.round(info.equippedChanceRole.toDouble(), 4)
                    )
                )
            }
            return false
        }

        return true
    }

    private fun checkOverallChance(info: CustomDropProcessingInfo): Boolean {
        for (dropInstance in info.allDropInstances) {
            if (dropInstance.overallChance == null || dropInstance.overallChance!!.isDefault ||
                dropInstance.overallChance!!.isAssuredChance
            ) {
                continue
            }

            synchronized(info.lmEntity!!.livingEntity.persistentDataContainer) {
                if (info.lmEntity!!.pdc
                        .has(NamespacedKeys.overallChanceKey, PersistentDataType.INTEGER)
                ) {
                    val value = info.lmEntity!!.pdc
                            .get(NamespacedKeys.overallChanceKey, PersistentDataType.INTEGER)

                    return value == 1
                }
            }

            // we'll roll the dice to see if we get any drops at all and store it in the PDC
            val chanceRole =
                ThreadLocalRandom.current().nextInt(0, 100001).toFloat() * 0.00001f
            val madeChance =
                1.0f - chanceRole < dropInstance.overallChance!!.getSlidingChance(info.lmEntity!!.getMobLevel)
            if (info.equippedOnly) {
                synchronized(info.lmEntity!!.livingEntity.persistentDataContainer) {
                    info.lmEntity!!.pdc
                        .set(
                            NamespacedKeys.overallChanceKey, PersistentDataType.INTEGER,
                            if (madeChance) 1 else 0
                        )
                }
            }

            return madeChance
        }

        return true
    }

    private fun processEnchantmentChances(
        dropItem: CustomDropItem,
        itemStack: ItemStack
    ) {
        if (dropItem.enchantmentChances == null || dropItem.enchantmentChances!!.isEmpty) return

        val chances = dropItem.enchantmentChances!!
        val main = LevelledMobs.instance
        val debug = StringBuilder()
        var isFirstEnchantment = true
        for (enchantment in chances.items.keys) {
            val opts = chances.options[enchantment]
            var madeAnyChance = false
            if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                if (!isFirstEnchantment) debug.append("; ")
                debug.append(enchantment.key.value()).append(": ")
            }

            if (isFirstEnchantment) isFirstEnchantment = false

            var enchantmentNumber = 0
            val levelsList = mutableListOf<Int>()
            levelsList.addAll(chances.items[enchantment]!!.keys)
            if (opts == null || opts.doShuffle) levelsList.shuffled()

            for (enchantLevel in levelsList) {
                val chanceValue = chances.items[enchantment]!![enchantLevel]!!
                if (chanceValue <= 0.0f) continue
                enchantmentNumber++

                val chanceRole =
                    ThreadLocalRandom.current().nextInt(0, 100001).toFloat() * 0.00001f
                val madeChance = 1.0f - chanceRole < chanceValue
                if (!madeChance) {
                    if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                        if (enchantmentNumber > 1) debug.append(", ")
                        debug.append(String.format("%s: &4%s&r &b(%s)&r", enchantLevel, chanceRole, chanceValue))
                    }
                    continue
                }

                if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                    if (enchantmentNumber > 1) debug.append(", ")
                    debug.append(String.format("%s: &2%s&r &b(%s)&r", enchantLevel, chanceRole, chanceValue))
                }

                if (itemStack.type == Material.ENCHANTED_BOOK) {
                    val meta = itemStack.itemMeta as EnchantmentStorageMeta
                    meta.addStoredEnchant(enchantment, enchantLevel, true)
                    itemStack.setItemMeta(meta)
                } else {
                    itemStack.addUnsafeEnchantment(enchantment, enchantLevel)
                }
                madeAnyChance = true
                break
            }

            if (!madeAnyChance && opts != null && opts.defaultLevel != null && opts.defaultLevel!! > 0) {
                if (itemStack.type == Material.ENCHANTED_BOOK) {
                    val meta = itemStack.itemMeta as EnchantmentStorageMeta
                    meta.addStoredEnchant(enchantment, opts.defaultLevel!!, true)
                    itemStack.setItemMeta(meta)
                } else {
                    itemStack.addUnsafeEnchantment(enchantment, opts.defaultLevel!!)
                }
                if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) debug.append(", used dflt: &2")
                    .append(opts.defaultLevel).append("&r")
            }
        }

        if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) Utils.logger.info(debug.toString())
    }

    private fun hasReachedChunkKillLimit(lmEntity: LivingEntityWrapper): Boolean {
        val maximumDeathInChunkThreshold: Int = LevelledMobs.instance.rulesManager.getMaximumDeathInChunkThreshold(
            lmEntity
        )
        if (maximumDeathInChunkThreshold <= 0) {
            return false
        }

        return lmEntity.chunkKillcount >= maximumDeathInChunkThreshold
    }

    private fun shouldDenyDeathCause(
        dropBase: CustomDropBase,
        info: CustomDropProcessingInfo
    ): Boolean {
        if (dropBase.causeOfDeathReqs == null || info.deathCause == null) {
            return false
        }

        if (info.wasKilledByPlayer && Utils.isDamageCauseInModalList(
                dropBase.causeOfDeathReqs!!,
                DeathCause.PLAYER_CAUSED
            )
        ) {
            return false
        }

        if (!Utils.isDamageCauseInModalList(dropBase.causeOfDeathReqs!!, info.deathCause!!)) {
            if (LevelledMobs.instance.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                val itemName = if (dropBase is CustomDropItem) dropBase.material.name else "(command)"
                info.addDebugMessage(
                    String.format(
                        "&8 - &7item: &b%s&7, death-cause: &b%s&7, death-cause-req: &b%s&7, dropped: &bfalse&7.",
                        itemName, info.deathCause, dropBase.causeOfDeathReqs
                    )
                )
            }

            return true
        }

        return false
    }

    private fun checkDropPermissions(
        info: CustomDropProcessingInfo,
        dropBase: CustomDropBase
    ): Boolean {
        if (info.equippedOnly || dropBase.permissions.isEmpty()) {
            return true
        }

        val main = LevelledMobs.instance
        if (info.mobKiller == null) {
            if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                info.addDebugMessage(
                    String.format(
                        "&8 - &7item: &b%s&7, not player was provided for item permissions",
                        if ((dropBase is CustomDropItem)) dropBase.itemStack!!.type.name
                        else "custom command"
                    )
                )
            }
            return false
        }

        var hadPermission = false
        for (perm in dropBase.permissions) {
            val permCheck = "levelledmobs.permission.$perm"
            if (info.mobKiller!!.hasPermission(permCheck)) {
                hadPermission = true
                break
            }
        }

        if (!hadPermission) {
            if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                info.addDebugMessage(
                    String.format(
                        "&8 - &7item: &b%s&7, player: &b%s&7 didn't have permission: &b%s&7",
                        if ((dropBase is CustomDropItem)) dropBase.itemStack!!.type.name
                        else "custom command",
                        info.mobKiller!!.name, dropBase.permissions
                    )
                )
            }
            return false
        }

        return true
    }

    private fun checkIfMadeEquippedDropChance(
        info: CustomDropProcessingInfo,
        item: CustomDropItem
    ): Boolean {
        if (item.equippedChance != null && item.equippedChance!!.isAssuredChance
            || !item.onlyDropIfEquipped
        ) {
            return true
        }

        return isMobWearingItem(item.itemStack!!, info.lmEntity!!.livingEntity, item)
    }

    private fun isMobWearingItem(
        item: ItemStack,
        mob: LivingEntity,
        customDropItem: CustomDropItem
    ): Boolean {
        val equipment = mob.equipment ?: return false
        val equippedItemsInfo = customEquippedItems[mob] ?: return false

        if (customDropItem.equipOnHelmet && item.isSimilar(equipment.helmet)) {
            return true
        }

        when (item.type) {
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET -> {
                if (equippedItemsInfo.helmet != null
                    && customDropItem == equippedItemsInfo.helmet
                ) {
                    return true
                }
                if (equippedItemsInfo.chestplate != null
                    && customDropItem == equippedItemsInfo.chestplate
                ) {
                    return true
                }
                if (equippedItemsInfo.leggings != null
                    && customDropItem == equippedItemsInfo.leggings
                ) {
                    return true
                }
                if (equippedItemsInfo.boots != null && customDropItem == equippedItemsInfo.boots) {
                    return true
                }
            }

            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE -> {
                if (equippedItemsInfo.chestplate != null
                    && customDropItem == equippedItemsInfo.chestplate
                ) {
                    return true
                }
                if (equippedItemsInfo.leggings != null
                    && customDropItem == equippedItemsInfo.leggings
                ) {
                    return true
                }
                if (equippedItemsInfo.boots != null && customDropItem == equippedItemsInfo.boots) {
                    return true
                }
            }

            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS -> {
                if (equippedItemsInfo.leggings != null
                    && customDropItem == equippedItemsInfo.leggings
                ) {
                    return true
                }
                if (equippedItemsInfo.boots != null && customDropItem == equippedItemsInfo.boots) {
                    return true
                }
            }

            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS -> if (equippedItemsInfo.boots != null && customDropItem == equippedItemsInfo.boots) {
                return true
            }

           else -> {}
        }
        if (equippedItemsInfo.mainHand != null && customDropItem == equippedItemsInfo.mainHand) {
            return true
        }

        return equippedItemsInfo.offhand != null && customDropItem == equippedItemsInfo.offhand
    }

    private fun madePlayerLevelRequirement(
        info: CustomDropProcessingInfo,
        dropBase: CustomDropBase
    ): Boolean {
        val main = LevelledMobs.instance
        if (dropBase.playerLevelVariable != null && !info.equippedOnly && dropBase.playeerVariableMatches.isNotEmpty()) {
            val papiResult: String = Utils.removeColorCodes(
                ExternalCompatibilityManager.getPapiPlaceholder(
                    info.mobKiller, dropBase.playerLevelVariable
                )
            )

            var foundMatch = false
            for (resultStr in dropBase.playeerVariableMatches) {
                if (Utils.matchWildcardString(papiResult, resultStr)) {
                    foundMatch = true
                    if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                        if (dropBase is CustomDropItem) {
                            info.addDebugMessage(
                                String.format(
                                    "&8 - &7Mob: &b%s&7, item: %s, PAPI val: %s, matched: %s",
                                    info.lmEntity!!.typeName, dropBase.material,
                                    papiResult, resultStr
                                )
                            )
                        } else {
                            info.addDebugMessage(
                                String.format(
                                    "&8 - &7Mob: &b%s&7, (customCommand), PAPI val: %s, matched: %s",
                                    info.lmEntity!!.typeName, papiResult, resultStr
                                )
                            )
                        }
                    }
                    break
                }
            }

            if (!foundMatch) {
                if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                    if (dropBase is CustomDropItem) {
                        info.addDebugMessage(
                            String.format(
                                "&8 - &7Mob: &b%s&7, item: %s, PAPI val: %s, no matches found",
                                info.lmEntity!!.typeName, dropBase.material,
                                papiResult
                            )
                        )
                    } else {
                        info.addDebugMessage(
                            String.format(
                                "&8 - &7Mob: &b%s&7, (customCommand), PAPI val: %s, no matches found",
                                info.lmEntity!!.typeName, papiResult
                            )
                        )
                    }
                }
                return false
            }
        }

        if (!info.equippedOnly && (dropBase.minPlayerLevel > -1 || dropBase.maxPlayerLevel > -1)) {
            // check if the variable result has been cached already and use it if so
            val variableToUse =
                if (dropBase.playerLevelVariable.isNullOrEmpty()) "%level%" else dropBase.playerLevelVariable!!
            val levelToUse: Int
            if (info.playerLevelVariableCache.containsKey(variableToUse)) {
                levelToUse = info.playerLevelVariableCache[variableToUse]!!
            } else {
                //levelToUse = main.levelManager.getPlayerLevelSourceNumber(info.mobKiller, variableToUse);
                val result: PlayerLevelSourceResult = main.levelManager.getPlayerLevelSourceNumber(
                    info.mobKiller, info.lmEntity!!, variableToUse
                )
                levelToUse = if (result.isNumericResult) result.numericResult else 1
                info.playerLevelVariableCache[variableToUse] = levelToUse
            }

            if (dropBase.minPlayerLevel > 0 && levelToUse < dropBase.minPlayerLevel ||
                dropBase.maxPlayerLevel in 1..<levelToUse
            ) {
                if (main.debugManager.isDebugTypeEnabled(DebugType.CUSTOM_DROPS)) {
                    if (dropBase is CustomDropItem) {
                        info.addDebugMessage(
                            String.format(
                                "&8 - &7Mob: &b%s&7, item: %s, lvl-src: %s, minlvl: %s, maxlvl: %s player level criteria not met",
                                info.lmEntity!!.typeName, dropBase.material,
                                levelToUse, dropBase.minPlayerLevel, dropBase.maxPlayerLevel
                            )
                        )
                    } else {
                        info.addDebugMessage(
                            String.format(
                                "&8 - &7Mob: &b%s&7, (customCommand), lvl-src: %s, minlvl: %s, maxlvl: %s player level criteria not met",
                                info.lmEntity!!.typeName, levelToUse, dropBase.minPlayerLevel,
                                dropBase.maxPlayerLevel
                            )
                        )
                    }
                }
                return false
            }
        }

        return true
    }

    private fun executeCommand(
        customCommand: CustomCommand,
        info: CustomDropProcessingInfo
    ) {
        if (info.equippedOnly && !customCommand.runOnSpawn) {
            return
        }
        if (!info.equippedOnly && !customCommand.runOnDeath) {
            return
        }

        val main = LevelledMobs.instance
        for (commandPre in customCommand.commands) {
            var command = processRangedCommand(commandPre, customCommand)
            command = main.levelManager.replaceStringPlaceholders(
                command, info.lmEntity!!, false,
                info.lmEntity!!.associatedPlayer, false
            )
            var mobScale = ""
            var mobScaleRounded = ""
            if (customCommand.mobScale != null) {
                val newScale = info.lmEntity!!.getMobLevel.toFloat() * customCommand.mobScale!!.toFloat()
                mobScale = newScale.toString()
                mobScaleRounded = (Utils.round(newScale.toDouble()).toInt()).toString()
            }
            command = command.replace("%mob-scale%", mobScale)
            command = command.replace("%mob-scale-rounded%", mobScaleRounded)

            if (command.contains("%") && ExternalCompatibilityManager.hasPapiInstalled) {
                command = ExternalCompatibilityManager.getPapiPlaceholder(info.mobKiller, command)
            }

            val maxAllowedTimesToRun: Int = LevelledMobs.instance.helperSettings.getInt(
                "customcommand-amount-limit", 10
            )
            var timesToRun = customCommand.amount

            if (customCommand.hasAmountRange) {
                timesToRun = (main.random.nextInt(
                    customCommand.amountRangeMax - customCommand.amountRangeMin + 1
                )
                        + customCommand.amountRangeMin)
            }

            if (timesToRun > maxAllowedTimesToRun) {
                timesToRun = maxAllowedTimesToRun
            }

            val debugCommand = if (timesToRun > 1) String.format("Command (%sx): ", timesToRun) else "Command: "

            val commandFinal = command
            DebugManager.log(DebugType.CUSTOM_COMMANDS, info.lmEntity!!) { debugCommand + commandFinal }

            if (customCommand.delay > 0) {
                val commandToRun = command
                val finalTimesToRun = timesToRun
                val runnable: BukkitRunnable = object : BukkitRunnable() {
                    override fun run() {
                        executeTheCommand(commandToRun, finalTimesToRun)
                    }
                }
                runnable.runTaskLater(main, customCommand.delay.toLong())
            } else {
                executeTheCommand(command, timesToRun)
            }
        }
    }

    private fun executeTheCommand(command: String, timesToRun: Int) {
        for (i in 0 until timesToRun) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        }
    }

    private fun processRangedCommand(
        command: String,
        cc: CustomCommand
    ): String {
        if (cc.rangedEntries.isEmpty()) {
            return command
        }

        var newCommand = command

        for ((rangedKey, rangedValue) in cc.rangedEntries) {
            if (!rangedValue.contains("-")) {
                newCommand = newCommand.replace("%$rangedKey%", rangedValue)
                continue
            }

            val nums = rangedValue.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (nums.size != 2) {
                continue
            }

            if (!Utils.isInteger(nums[0].trim { it <= ' ' }) || !Utils.isInteger(
                    nums[1].trim { it <= ' ' })
            ) {
                continue
            }
            var min = nums[0].trim { it <= ' ' }.toInt()
            val max = nums[1].trim { it <= ' ' }.toInt()
            if (max < min) {
                min = max
            }

            val rangedNum = LevelledMobs.instance.random.nextInt(max - min + 1) + min
            newCommand = newCommand.replace("%$rangedKey%", rangedNum.toString())
        }

        return newCommand
    }

    private fun getCookedVariantOfMeat(itemStack: ItemStack): ItemStack {
        return when (itemStack.type) {
            Material.BEEF -> ItemStack(Material.COOKED_BEEF)
            Material.CHICKEN -> ItemStack(Material.COOKED_CHICKEN)
            Material.COD -> ItemStack(Material.COOKED_COD)
            Material.MUTTON -> ItemStack(Material.COOKED_MUTTON)
            Material.PORKCHOP -> ItemStack(Material.COOKED_PORKCHOP)
            Material.RABBIT -> ItemStack(Material.COOKED_RABBIT)
            Material.SALMON -> ItemStack(Material.COOKED_SALMON)
            else -> itemStack
        }
    }

    fun addEntityEquippedItems(
        livingEntity: LivingEntity,
        equippedItemsInfo: EquippedItemsInfo
    ) {
        customEquippedItems[livingEntity] = equippedItemsInfo
    }

    fun setDropInstanceFromId(groupId: String, dropInstance: CustomDropInstance) {
        groupIdToInstance[groupId] = dropInstance
    }

    fun getGroupLimits(dropBase: CustomDropBase): GroupLimits? {
        val limitsDefault = groupLimitsMap["default"]

        if (!dropBase.hasGroupId || !groupLimitsMap.containsKey(dropBase.groupId)) {
            return limitsDefault
        }

        return groupLimitsMap[dropBase.groupId]
    }

    fun clearGroupIdMappings() {
        groupIdToInstance.clear()
    }
}