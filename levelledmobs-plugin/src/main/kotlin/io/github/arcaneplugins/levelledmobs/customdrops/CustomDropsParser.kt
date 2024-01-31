package io.github.arcaneplugins.levelledmobs.customdrops

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.compatibility.Compat117.all17Mobs
import io.github.arcaneplugins.levelledmobs.compatibility.Compat119.all19Mobs
import io.github.arcaneplugins.levelledmobs.compatibility.Compat120.all20Mobs
import io.github.arcaneplugins.levelledmobs.compatibility.Compat121.all21Mobs
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.managers.NBTManager
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.misc.CustomUniversalGroups
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.DeathCause
import io.github.arcaneplugins.levelledmobs.misc.YmlParsingHelper
import io.github.arcaneplugins.levelledmobs.result.NBTApplyResult
import io.github.arcaneplugins.levelledmobs.rules.MinAndMax
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.Utils
import java.util.LinkedList
import java.util.Locale
import java.util.SortedMap
import java.util.TreeMap
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.EnchantmentStorageMeta

/**
 * Parses all data from customdrops.yml and places into the corresponding java classes
 *
 * @author stumper66
 * @since 3.0.0
 */
class CustomDropsParser(
    private val handler: CustomDropsHandler
) {
    private val ymlHelper = YmlParsingHelper()
    var defaults = CustomDropsDefaults()
        private set
    private var hasMentionedNBTAPIMissing = false
    var dropsUtilizeNBTAPI: Boolean = false
    val invalidExternalItems = mutableListOf<String>()
    //private var dropBase: CustomDropBase? = null
    private var dropInstance: CustomDropInstance? = null
    private val defaultName = "default"
    private val invalidEntityTypesToIgnore = mutableListOf<String>()

    init {
        this.defaults.groupId = defaultName
        buildInvalidEntityTypesToIgnore()
    }

    private fun buildInvalidEntityTypesToIgnore() {
        invalidEntityTypesToIgnore.addAll(all17Mobs())
        invalidEntityTypesToIgnore.addAll(all19Mobs())
        invalidEntityTypesToIgnore.addAll(all20Mobs())
        invalidEntityTypesToIgnore.addAll(all21Mobs())
    }

    fun loadDrops(customDropsCfg: YamlConfiguration?) {
        this.dropsUtilizeNBTAPI = false
        if (customDropsCfg == null) {
            return
        }

        var isDropsEnabledForAnyRule = false

        for (rules in LevelledMobs.instance.rulesManager.rulesInEffect.values) {
            for (ruleInfo in rules) {
                if (ruleInfo.customDropsUseForMobs != null && ruleInfo.customDropsUseForMobs!!) {
                    isDropsEnabledForAnyRule = true
                    break
                }
            }
            if (isDropsEnabledForAnyRule) {
                break
            }
        }

        handler.clearDrops()

        if (isDropsEnabledForAnyRule) {
            handler.clearGroupIdMappings()
            parseCustomDrops(customDropsCfg)
        }

        DebugManager.log(DebugType.CUSTOM_DROPS) { "Group Limits: " + handler.groupLimitsMap }
    }

    private fun processDefaults(
        cs: ConfigurationSection?
    ) {
        if (cs == null) {
            Utils.logger.warning("Defaults section was null")
            return
        }

        // configure bogus items so we can utilize the existing attribute parse logic
        val drop = CustomDropItem(this.defaults)
        drop.material = Material.AIR
        drop.isDefaultDrop = true
        dropInstance = CustomDropInstance(EntityType.AREA_EFFECT_CLOUD)
        dropInstance!!.customItems.add(drop)

        // this sets the drop and dropinstance defaults
        parseCustomDropsAttributes(drop, cs)

        // now we'll use the attributes here for defaults
        defaults.setDefaultsFromDropItem(drop)
        defaults.override = dropInstance!!.getOverrideStockDrops
        defaults.overallChance = dropInstance!!.overallChance
        defaults.overallPermissions.addAll(dropInstance!!.overallPermissions)
        handler.customDropIDs[defaults.groupId!!] = dropInstance!!
    }

    private fun parseCustomDrops(config: ConfigurationSection) {
        handler.customItemGroups = mutableMapOf()

        processDefaults(objectToConfigurationSection2(config, "defaults"))

        val dropTableKey: String = ymlHelper.getKeyNameFromConfig(config, "drop-table")
        if (config[dropTableKey] != null) {
            val ms = config[dropTableKey] as MemorySection?
            if (ms != null) {
                val itemGroups = ms.getValues(true)

                for ((itemGroupName, value) in itemGroups) {
                    dropInstance = CustomDropInstance(
                        EntityType.AREA_EFFECT_CLOUD
                    ) // entity type doesn't matter
                    parseCustomDrops2(value as List<*>)
                    if (dropInstance!!.customItems.isNotEmpty() || dropInstance!!.getOverrideStockDrops) {
                        handler.customItemGroups[itemGroupName] = dropInstance!!
                        handler.customDropIDs[itemGroupName] = dropInstance!!
                    }
                }
            }
        }

        for (item in config.getKeys(false)) {
            var entityType: EntityType? = null
            val mobTypeOrGroups = item.split(";")

            for (mobTypeOrGroup in mobTypeOrGroups) {
                var mobTypeOrGroup = mobTypeOrGroup.trim { it <= ' ' }
                if (mobTypeOrGroup.isEmpty()) {
                    continue
                }
                if (mobTypeOrGroup.lowercase(Locale.getDefault()).startsWith("file-version")) {
                    continue
                }

                var universalGroup: CustomUniversalGroups? = null
                val isEntityTable = (mobTypeOrGroup.equals("drop-table", ignoreCase = true))
                val isUniversalGroup = mobTypeOrGroup.lowercase(Locale.getDefault()).startsWith("all_")

                if (isUniversalGroup) {
                    try {
                        universalGroup = CustomUniversalGroups.valueOf(
                            mobTypeOrGroup.uppercase(Locale.getDefault())
                        )
                    } catch (e: Exception) {
                        Utils.logger.warning(
                            "invalid universal group in customdrops.yml: $mobTypeOrGroup"
                        )
                        continue
                    }
                    dropInstance = CustomDropInstance(universalGroup)
                } else if (!isEntityTable) {
                    if (mobTypeOrGroup.equals("defaults", ignoreCase = true)) {
                        continue
                    }

                    var isBabyMob = false
                    if (mobTypeOrGroup.lowercase(Locale.getDefault()).startsWith("baby_")) {
                        isBabyMob = true
                        mobTypeOrGroup = mobTypeOrGroup.substring(5)
                    }

                    try {
                        entityType = EntityType.valueOf(mobTypeOrGroup.uppercase(Locale.getDefault()))
                    } catch (e: Exception) {
                        if (!invalidEntityTypesToIgnore.contains(mobTypeOrGroup.uppercase(Locale.getDefault()))) {
                            Utils.logger.warning(
                                "invalid mob type in customdrops.yml: $mobTypeOrGroup"
                            )
                        }
                        continue
                    }
                    dropInstance = CustomDropInstance(entityType, isBabyMob)
                } else {
                    // item groups, we processed them beforehand
                    continue
                }

                dropInstance!!.overrideStockDrops = defaults.override
                dropInstance!!.overallChance = defaults.overallChance

                if (!isEntityTable) {
                    if (config.getList(item) != null) {
                        // standard drop processing
                        parseCustomDrops2(config.getList(item))
                    } else if (config[item] is MemorySection) {
                        // drop is using a item group
                        val csItem = objectToConfigurationSection2(config,item) ?: continue

                        val useEntityDropId = ymlHelper.getString(csItem, "usedroptable")
                        if (useEntityDropId != null && !handler.customItemGroups.containsKey(
                                useEntityDropId
                            )
                        ) {
                            Utils.logger.warning("Did not find droptable id match for name: $useEntityDropId")
                        } else if (useEntityDropId == null) {
                            Utils.logger.warning("Found a drop-table reference with no id!")
                        } else {
                            val refDrop = handler.customItemGroups[useEntityDropId]
                            for (itemDrop in refDrop!!.customItems) {
                                dropInstance!!.customItems.add((if (itemDrop is CustomDropItem) itemDrop.cloneItem() else (itemDrop as CustomCommand).cloneItem())!!)
                            }
                            if (refDrop.utilizesGroupIds) {
                                dropInstance!!.utilizesGroupIds = true
                            }
                            if (refDrop.getOverrideStockDrops) {
                                dropInstance!!.overrideStockDrops = true
                            }
                        }
                    }
                } // end if not entity table


                if (dropInstance!!.customItems.isNotEmpty() || dropInstance!!.getOverrideStockDrops) {
                    if (isUniversalGroup) {
                        if (handler.getCustomDropsitemsGroups().containsKey(
                                universalGroup.toString()
                            )
                        ) {
                            handler.getCustomDropsitemsGroups()[universalGroup.toString()]
                                ?.combineDrop(dropInstance)
                        } else {
                            handler.addCustomDropGroup(universalGroup.toString(), dropInstance!!)
                        }
                    } else {
                        val dropMap: MutableMap<EntityType, CustomDropInstance> =
                            if (dropInstance!!.isBabyMob) handler.customDropsitemsBabies else handler.getCustomDropsitems()

                        if (dropMap.containsKey(entityType)) {
                            dropMap[entityType]!!.combineDrop(dropInstance)
                        } else {
                            dropMap[entityType!!] = dropInstance!!
                            handler.addCustomDropItem(entityType, dropInstance!!)
                        }
                    }
                }
            } // next mob or group
        } // next root item from file
    }

    private fun parseCustomDrops2(itemConfigurations: List<*>?) {
        if (itemConfigurations == null) {
            return
        }

        for (itemObject in itemConfigurations) {
            if (itemObject is String) {
                // just the string was given
                val item = CustomDropItem(this.defaults)

                if ("override".equals(itemObject, ignoreCase = true)) {
                    dropInstance!!.overrideStockDrops = true
                    continue
                }

                addMaterialToDrop(itemObject, item)
                continue
            }
            val itemConfiguration = objectToConfigurationSectionOld(
                itemObject
            )
            if (itemConfiguration == null) {
                continue
            }

            val itemsToCheck = itemConfiguration.getValues(false)
                .entries

            if (itemsToCheck.isEmpty() && itemObject!!.javaClass == LinkedHashMap::class.java) {
                // empty list means a material name was provided with no attributes
                val materials = itemObject as LinkedHashMap<String, Any>
                var needsContinue = false
                for (materialName in materials.keys) {
                    val item = CustomDropItem(this.defaults)

                    if (addMaterialToDrop(materialName, item)) {
                        needsContinue = true
                        break
                    }
                }
                if (needsContinue) {
                    continue
                }
            }

            for (itemEntry in itemsToCheck) {
                val materialName = itemEntry.key

                if (checkForMobOverride(itemEntry)) {
                    continue
                }

                if ("overall_chance".equals(materialName, ignoreCase = true)) {
                    dropInstance!!.overallChance = parseSlidingChance(
                        itemConfiguration,
                        "overall_chance", defaults.overallChance
                    )
                    continue
                } else if ("overall_permission".equals(materialName, ignoreCase = true)) {
                    if (itemEntry.value is String) {
                        dropInstance!!.overallPermissions.add((itemEntry.value as String?)!!)
                    } else if (itemEntry.value is ArrayList<*>) {
                        dropInstance!!.overallPermissions.addAll(
                            itemEntry.value as ArrayList<String>
                        )
                    }

                    continue
                }

                if ("usedroptable".equals(materialName, ignoreCase = true)) {
                    if (itemEntry.value == null) {
                        Utils.logger.warning("Found a drop-table reference with no id!")
                        continue
                    }

                    val useEntityDropId = itemEntry.value.toString()

                    if (!handler.customItemGroups.containsKey(
                            useEntityDropId
                        )
                    ) {
                        Utils.logger.warning(
                            "Did not find droptable id match for name: $useEntityDropId"
                        )
                    } else {
                        val refDrop = handler.customItemGroups[useEntityDropId]
                        for (itemDrop in refDrop!!.customItems) {
                            dropInstance!!.customItems.add((if (itemDrop is CustomDropItem) itemDrop.cloneItem() else (itemDrop as CustomCommand).cloneItem())!!)
                        }
                        if (refDrop.utilizesGroupIds) {
                            dropInstance!!.utilizesGroupIds = true
                        }
                        if (refDrop.getOverrideStockDrops) {
                            dropInstance!!.overrideStockDrops = true
                        }
                    }
                    continue
                }

                val itemInfoConfiguration = objectToConfigurationSectionOld(
                    itemEntry.value
                )
                if (itemInfoConfiguration == null) {
                    continue
                }

                var dropBase: CustomDropBase
                if ("customCommand".equals(materialName, ignoreCase = true)) {
                    dropBase = CustomCommand(defaults)
                } else {
                    val item = CustomDropItem(this.defaults)
                    item.externalType = ymlHelper.getString(
                        itemInfoConfiguration, "type",
                        defaults.externalType
                    )
                    item.externalAmount = ymlHelper.getDouble2(
                        itemInfoConfiguration,
                        "external-amount", defaults.externalAmount
                    )
                    item.externalExtras = parseExternalExtras(itemInfoConfiguration)

                    if (!addMaterialToDrop(materialName, item)) {
                        continue
                    }

                    if (item.isExternalItem && LevelledMobs.instance.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
                        if (!handler.lmItemsParser!!.getExternalItem(item, null)) {
                            continue
                        }
                    }

                    dropBase = item
                }

                parseCustomDropsAttributes(dropBase, itemInfoConfiguration)
            }
        } // next item
    }

    private fun parseExternalExtras(
        cs: ConfigurationSection
    ): MutableMap<String, Any>? {
        val cs2: ConfigurationSection = ymlHelper.objToCS(cs, "extras") ?: return null

        val results: MutableMap<String, Any> = TreeMap(java.lang.String.CASE_INSENSITIVE_ORDER)

        for (name in cs2.getKeys(false)) {
            val value = cs2[name]
            if (value != null) results[name] = value
        }

        return if (results.isEmpty()) null
        else results
    }

    private fun parseCustomDropsAttributes(
        dropBase: CustomDropBase,
        cs: ConfigurationSection
    ) {
        dropBase.chance = parseSlidingChance(cs, "chance", defaults.chance)
        dropBase.useChunkKillMax = ymlHelper.getBoolean(
            cs, "use-chunk-kill-max",
            defaults.useChunkKillMax
        )
        dropBase.permissions.addAll(defaults.permissions)
        dropBase.permissions.addAll(ymlHelper.getStringSet(cs, "permission"))
        dropBase.minLevel = ymlHelper.getInt(cs, "minlevel", defaults.minLevel)
        dropBase.maxLevel = ymlHelper.getInt(cs, "maxlevel", defaults.maxLevel)

        dropBase.minPlayerLevel = ymlHelper.getInt(
            cs, "min-player-level",
            defaults.minPlayerLevel
        )
        dropBase.maxPlayerLevel = ymlHelper.getInt(
            cs, "max-player-level",
            defaults.maxPlayerLevel
        )
        dropBase.playerLevelVariable = ymlHelper.getString(
            cs, "player-level-variable",
            defaults.playerLevelVariable
        )
        dropBase.playeerVariableMatches.addAll(ymlHelper.getStringOrList(cs, "player-variable-match-value"))

        dropBase.playerCausedOnly = ymlHelper.getBoolean(
            cs, "player-caused",
            defaults.playerCausedOnly
        )
        dropBase.maxDropGroup = ymlHelper.getInt(cs, "maxdropgroup", defaults.maxDropGroup)
        if (!dropBase.isDefaultDrop) {
            dropBase.groupId = ymlHelper.getString(cs, "groupid")
        }

        if (dropBase.hasGroupId) {
            handler.setDropInstanceFromId(dropBase.groupId!!, dropInstance!!)
        }

        dropInstance!!.utilizesGroupIds = dropBase.hasGroupId
        parseGroupLimits(dropBase, cs)

        if (!ymlHelper.getString(cs, "amount").isNullOrEmpty()) {
            if (!dropBase.setAmountRangeFromString(ymlHelper.getString(cs, "amount"))) {
                Utils.logger.warning(
                    java.lang.String.format(
                        "Invalid number or number range for amount on %s, %s",
                        dropInstance!!.getMobOrGroupName(), ymlHelper.getString(cs, "amount")
                    )
                )
            }
        }

        val overallChance = parseSlidingChance(cs, "overall_chance", null)
        if (!ymlHelper.getString(cs, "overall_chance").isNullOrEmpty()) {
            if (overallChance == null || !overallChance.isDefault) {
                dropInstance!!.overallChance = null
            }
        }

        if (cs[ymlHelper.getKeyNameFromConfig(cs, "overall_permission")] != null) {
            dropInstance!!.overallPermissions.addAll(
                ymlHelper.getStringSet(cs, "overall_permission")
            )
        }

        dropBase.causeOfDeathReqs = buildCachedModalListOfDamageCause(
            cs,
            defaults.causeOfDeathReqs
        )

        if (dropBase is CustomCommand) {
            parseCustomCommand(dropBase, cs)
            return
        }

        parseCustomItem(cs, dropBase as CustomDropItem)
    }

    private fun parseSlidingChance(
        cs: ConfigurationSection,
        keyName: String,
        defaultValue: SlidingChance?
    ): SlidingChance? {
        val chanceOptsMap = cs[keyName]
        var chanceOpts: ConfigurationSection? = null

        if (chanceOptsMap is LinkedHashMap<*, *> || chanceOptsMap is MemorySection) {
            chanceOpts = objToCS(cs, keyName)
        }

        if (chanceOpts == null) {
            val parsedValue = ymlHelper.getFloat2(cs, keyName, null)

            if (defaultValue == null && parsedValue == null) return null

            val result = SlidingChance()
            if (parsedValue != null)
                result.chance = parsedValue
            else
                result.setFromInstance(defaultValue)

            result.defaults = defaultValue
            return result
        }

        val chanceRange = mutableMapOf<MinAndMax, MinAndMax>()
        val values = chanceOpts.getValues(false)
        for (str in values.keys) {
            val value = values[str]
            if (str == null || value == null) continue

            val mobLvl = MinAndMax.setAmountRangeFromString(str)
            val assignment = MinAndMax.setAmountRangeFromString(value.toString())

            if (mobLvl != null && assignment != null) {
                mobLvl.showAsInt = true
                chanceRange[mobLvl] = assignment
            }
        }

        val result: SlidingChance
        if (chanceRange.isEmpty()) {
            if (defaultValue == null) return null
            result = SlidingChance()
            result.setFromInstance(defaultValue)
        } else {
            result = SlidingChance()
            result.setFromInstance(defaultValue)
            result.changeRange = chanceRange
        }

        result.defaults = defaultValue
        return result
    }

    private fun parseCustomItem(cs: ConfigurationSection, item: CustomDropItem) {
        checkEquippedChance(item, cs)
        parseItemFlags(item, cs)

        item.onlyDropIfEquipped = ymlHelper.getBoolean(
            cs, "only-drop-if-equipped",
            defaults.onlyDropIfEquipped
        )
        item.equipOnHelmet = ymlHelper.getBoolean(cs, "equip-on-helmet", defaults.equipOnHelmet)
        item.equipOffhand = ymlHelper.getBoolean(cs, "equip-offhand", defaults.equipOffhand)
        item.priority = ymlHelper.getInt(cs, "priority", defaults.priority)
        item.noMultiplier = ymlHelper.getBoolean(cs, "nomultiplier", defaults.noMultiplier)
        item.noSpawner = ymlHelper.getBoolean(cs, "nospawner", defaults.noSpawner)
        item.customModelDataId = ymlHelper.getInt(
            cs, "custommodeldata",
            defaults.customModelData
        )
        item.externalType = ymlHelper.getString(cs, "type", defaults.externalType)
        item.externalAmount = ymlHelper.getDouble2(
            cs, "external-amount",
            defaults.externalAmount
        )
        item.minItems = ymlHelper.getInt(cs, "min-items", 1)
        item.maxItems = ymlHelper.getInt(cs, "max-items", 1)
        item.allowedList = ymlHelper.getStringOrList(cs, "allowed-list")
        item.excludedList = ymlHelper.getStringOrList(cs, "excluded-list")

        dropInstance!!.overrideStockDrops = ymlHelper.getBoolean2(
            cs, "override",
            defaults.override
        )

        if (!ymlHelper.getString(cs, "damage").isNullOrEmpty()) {
            if (!item.setDamageRangeFromString(ymlHelper.getString(cs, "damage"))) {
                Utils.logger.warning(
                    java.lang.String.format(
                        "Invalid number range for damage on %s, %s",
                        dropInstance!!.getMobOrGroupName(), ymlHelper.getString(cs, "damage")
                    )
                )
            }
        }
        if (cs.getStringList(ymlHelper.getKeyNameFromConfig(cs, "lore")).isNotEmpty()) {
            item.lore = cs.getStringList(ymlHelper.getKeyNameFromConfig(cs, "lore"))
        }
        item.customName = ymlHelper.getString(cs, "name", item.customName)

        if (!ymlHelper.getString(cs, "excludemobs").isNullOrEmpty()) {
            val excludes = ymlHelper.getString(cs, "excludemobs")!!.split(";")
            item.excludedMobs.clear()
            for (exclude in excludes) {
                item.excludedMobs.add(exclude.trim { it <= ' ' })
            }
        }

        parseEnchantments(objectToConfigurationSection2(cs, "enchantments"), item)
        item.nbtData = ymlHelper.getString(cs, "nbt-data", defaults.nbtData)
        if (item.material != Material.AIR && !item.nbtData.isNullOrEmpty()) {
            if (ExternalCompatibilityManager.hasNbtApiInstalled()) {
                val result: NBTApplyResult = NBTManager.applyNBTDataItem(item, item.nbtData!!)
                if (result.hadException) {
                    Utils.logger.warning(
                        java.lang.String.format(
                            "custom drop %s for %s has invalid NBT data: %s",
                            item.material, dropInstance!!.getMobOrGroupName(),
                            result.exceptionMessage
                        )
                    )
                } else if (result.itemStack != null) {
                    item.itemStack = result.itemStack
                    this.dropsUtilizeNBTAPI = true
                }
            } else if (!hasMentionedNBTAPIMissing) {
                Utils.logger.warning(
                    "NBT Data has been specified in customdrops.yml but required plugin NBTAPI is not installed!"
                )
                hasMentionedNBTAPIMissing = true
            }
        }

        applyMetaAttributes(item)
    }

    private fun parseGroupLimits(base: CustomDropBase, csParent: ConfigurationSection) {
        val cs: ConfigurationSection = objToCS(csParent, "group-limits") ?: return

        if (!base.hasGroupId) return

        val limits = GroupLimits()
        limits.capPerItem = ymlHelper.getInt(cs, "cap-per-item")
        limits.capTotal = ymlHelper.getInt(cs, "cap-total")
        limits.capEquipped = ymlHelper.getInt(cs, "cap-equipped")
        limits.capSelect = ymlHelper.getInt(cs, "cap-select")
        limits.retries = ymlHelper.getInt(cs, "retries")

        if (!limits.isEmpty || base.isDefaultDrop) {
            handler.groupLimitsMap[base.groupId!!] = limits
        }
    }

    private fun parseCustomCommand(customCommand: CustomCommand, cs: ConfigurationSection) {
        customCommand.commands.addAll(ymlHelper.getStringOrList(cs, "command"))
        customCommand.commandName = ymlHelper.getString(cs, "name")
        customCommand.delay = ymlHelper.getInt(cs, "delay", 0)
        customCommand.runOnSpawn = ymlHelper.getBoolean(cs, "run-on-spawn", false)
        customCommand.runOnDeath = ymlHelper.getBoolean(cs, "run-on-death", true)
        customCommand.mobScale = ymlHelper.getDouble2(cs, "mob-scale", null)
        parseRangedVariables(customCommand, cs)

        if (customCommand.commands.isEmpty()) {
            Utils.logger.warning("no command was specified for custom command")
        } else {
            dropInstance!!.customItems.add(customCommand)
        }
    }

    private fun buildCachedModalListOfDamageCause(
        cs: ConfigurationSection?,
        defaultValue: CachedModalList<DeathCause>?
    ): CachedModalList<DeathCause>? {
        if (cs == null) {
            return defaultValue
        }

        val cachedModalList: CachedModalList<DeathCause> = CachedModalList()
        val simpleStringOrArray = cs[ymlHelper.getKeyNameFromConfig(cs, "cause-of-death")]
        var cs2: ConfigurationSection? = null
        var useList = mutableListOf<String>()

        if (simpleStringOrArray is java.util.ArrayList<*>) {
            useList = simpleStringOrArray as java.util.ArrayList<String>
        } else if (simpleStringOrArray is String) {
            useList = mutableListOf(simpleStringOrArray)
        }

        if (useList.isEmpty()) {
            val useKeyName: String = ymlHelper.getKeyNameFromConfig(cs, "cause-of-death")

            cs2 = objToCS(cs, useKeyName)
        }
        if (cs2 == null && useList == null) {
            return defaultValue
        }

        cachedModalList.doMerge = ymlHelper.getBoolean(cs2, "merge")
        if (cs2 != null) {
            val allowedList: String = ymlHelper.getKeyNameFromConfig(cs2, "allowed-list")
            useList = YmlParsingHelper.getListFromConfigItem(cs2, allowedList)
        }

        for (item in useList) {
            if (item.trim { it <= ' ' }.isEmpty()) {
                continue
            }
            if ("*" == item.trim { it <= ' ' }) {
                cachedModalList.allowAll = true
                continue
            }
            try {
                val cause = DeathCause.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                cachedModalList.allowedList.add(cause)
            } catch (ignored: IllegalArgumentException) {
                Utils.logger.warning("Invalid damage cause: $item")
            }
        }
        if (cs2 == null) {
            return cachedModalList
        }

        val excludedList: String = ymlHelper.getKeyNameFromConfig(cs2, "excluded-list")

        for (item in YmlParsingHelper.getListFromConfigItem(cs2, excludedList)) {
            if (item.trim { it <= ' ' }.isEmpty()) {
                continue
            }
            if ("*" == item.trim { it <= ' ' }) {
                cachedModalList.excludeAll = true
                continue
            }
            try {
                val cause = DeathCause.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                cachedModalList.excludedList.add(cause)
            } catch (ignored: IllegalArgumentException) {
                Utils.logger.warning("Invalid damage cause: $item")
            }
        }

        if (cachedModalList.isEmpty() && !cachedModalList.allowAll && !cachedModalList.excludeAll) {
            return defaultValue
        }

        return cachedModalList
    }

    private fun parseEnchantments(cs: ConfigurationSection?, item: CustomDropItem) {
        if (cs == null) {
            return
        }

        val enchantMap = cs.getValues(false)

        for ((enchantName, value) in enchantMap) {
            if (value is LinkedHashMap<*, *>) {
                // contains enchantment chances

                val en = Enchantment.getByKey(
                    NamespacedKey.minecraft(enchantName.lowercase(Locale.getDefault()))
                )

                if (en == null) {
                    Utils.logger.warning("Invalid enchantment: $enchantName")
                    continue
                }

                val enchantments = value as Map<Any, Any>
                parseEnchantmentChances(en, enchantments, item)
                continue
            }

            var enchantLevel = 1
            if (value != null && Utils.isInteger(value.toString())) {
                enchantLevel = value.toString().toInt()
            }

            val en = Enchantment.getByKey(
                NamespacedKey.minecraft(enchantName.lowercase(Locale.getDefault()))
            )
            if (en != null) {
                if (item.material == Material.ENCHANTED_BOOK) {
                    val meta = item.itemStack
                        ?.itemMeta as EnchantmentStorageMeta
                    meta.addStoredEnchant(en, enchantLevel, true)
                    item.itemStack!!.setItemMeta(meta)
                } else {
                    item.itemStack!!.addUnsafeEnchantment(en, enchantLevel)
                }
            } else {
                Utils.logger.warning("Invalid enchantment: $enchantName")
            }
        }
    }

    private fun parseEnchantmentChances(
        enchantment: Enchantment,
        enchantmentsMap: Map<Any, Any>,
        item: CustomDropItem
    ) {
        val items: MutableMap<Int, Float> = TreeMap()
        var defaultLevel: Int? = null
        var doShuttle: Boolean? = null

        /*
        * ENCHANTMENTS:
        *  sharpness:
        *    1: 0.4
        *    2: 0.5
        *    3: 0.6
        *    default: 1
        */
        for ((key, value) in enchantmentsMap) {
            if ("shuffle".equals(key.toString(), ignoreCase = true)) {
                if ("false".equals(value.toString(), ignoreCase = true)) doShuttle = false
                continue
            }

            val isDefault = defaultName.equals(key.toString(), ignoreCase = true)
            var enchantmentLevel = 0

            if (!isDefault) {
                if (!Utils.isInteger(key.toString())) {
                    Utils.logger.warning(
                        String.format(
                            "Enchantment: %s, invalid enchantment level %s",
                            enchantment, key
                        )
                    )
                    continue
                }
                enchantmentLevel = key.toString().toInt()
            }

            var chanceValue: Double
            try {
                chanceValue = value.toString().toDouble()
            } catch (ignored: java.lang.Exception) {
                Utils.logger.warning(
                    String.format(
                        "Enchantment: %s, invalid chance specified: %s",
                        enchantment, value
                    )
                )
                continue
            }

            if (isDefault) defaultLevel = chanceValue.toInt()
            else items[enchantmentLevel] = chanceValue.toFloat()
        }

        if (items.isEmpty()) return

        if (item.enchantmentChances == null) item.enchantmentChances = EnchantmentChances()

        if (doShuttle != null || defaultLevel != null) {
            val opts = item.enchantmentChances!!.options.computeIfAbsent(
                enchantment
            ) { _ -> EnchantmentChances.ChanceOptions() }

            if (defaultLevel != null) opts.defaultLevel = defaultLevel
            if (doShuttle != null) opts.doShuffle = false
        }

        item.enchantmentChances!!.items[enchantment] = items
    }

    private fun parseRangedVariables(
        cc: CustomCommand,
        cs: ConfigurationSection
    ) {
        for (key in cs.getKeys(false)) {
            if (!key.lowercase(Locale.getDefault()).startsWith("ranged")) {
                continue
            }

            val value = cs.getString(key)
            if (value.isNullOrEmpty()) {
                continue
            }

            cc.rangedEntries[key] = value
        }
    }

    private fun applyMetaAttributes(item: CustomDropItem) {
        val meta = item.itemStack!!.itemMeta ?: return

        var madeChanges = false

        if (item.customModelDataId != defaults.customModelData) {
            meta.setCustomModelData(item.customModelDataId)
            item.itemStack!!.setItemMeta(meta)
            madeChanges = true
        }

        if (item.itemFlags != null && item.itemFlags!!.isNotEmpty()) {
            for (flag in item.itemFlags!!) {
                meta.addItemFlags(flag)
            }

            madeChanges = true
        }

        if (madeChanges) {
            item.itemStack!!.setItemMeta(meta)
        }
    }

    private fun parseItemFlags(item: CustomDropItem, cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        item.itemFlagsStrings = cs.getStringList(
            ymlHelper.getKeyNameFromConfig(cs, "item_flags")
        )
        if (item.itemFlagsStrings!!.isEmpty()) {
            item.itemFlagsStrings = cs.getStringList(
                ymlHelper.getKeyNameFromConfig(cs, "item-flags")
            )
        }

        if (item.itemFlagsStrings!!.isEmpty() && defaults.itemFlagsStrings != null) {
            item.itemFlagsStrings = defaults.itemFlagsStrings
        }

        var itemFlags: String? = null

        if (item.itemFlagsStrings!!.isEmpty()) {
            itemFlags = ymlHelper.getString(cs, "itemflags")
            if (itemFlags.isNullOrEmpty()) {
                itemFlags = ymlHelper.getString(cs, "item_flags")
            }
            if (itemFlags.isNullOrEmpty()) {
                itemFlags = ymlHelper.getString(cs, "item-flags")
            }
        }

        if (item.itemFlagsStrings!!.isEmpty() && itemFlags.isNullOrEmpty()) {
            return
        }
        val results: MutableList<ItemFlag> = LinkedList()

        item.itemFlagsStrings = if (item.itemFlagsStrings!!.isEmpty())
            itemFlags!!.replace(",", ";").split(";") as MutableList
            else item.itemFlagsStrings

        for (flag in item.itemFlagsStrings!!) {
            try {
                val newFlag = ItemFlag.valueOf(flag.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                results.add(newFlag)
            } catch (e: java.lang.Exception) {
                Utils.logger.warning(
                    String.format(
                        "Invalid itemflag: %s, item: %s, mobOrGroup: %s",
                        flag, item.material.name, dropInstance!!.getMobOrGroupName()
                    )
                )
            }
        }

        if (results.isNotEmpty()) {
            item.itemFlags = results
        }
    }

    private fun checkEquippedChance(
        item: CustomDropItem,
        cs: ConfigurationSection
    ) {
        item.equippedChance = parseSlidingChance(cs, "equipped", defaults.equippedChance)

        if (defaults.equippedChance == null ||
            item.equippedChance != null && !item.equippedChance!!.isDefault
        ) return
        val temp = ymlHelper.getString(cs, "equipped")
        if (temp.isNullOrEmpty()) {
            return
        }

        if ("false".equals(temp, ignoreCase = true)) {
            item.equippedChance!!.chance = 0.0f
        } else if ("true".equals(temp, ignoreCase = true)) {
            item.equippedChance!!.chance = 1.0f
        }
    }

    private fun objectToConfigurationSection2(
        cs: ConfigurationSection?,
        path: String
    ): ConfigurationSection? {
        if (cs == null) {
            return null
        }
        val useKey: String = ymlHelper.getKeyNameFromConfig(cs, path)
        val obj = cs[useKey] ?: return null

        when (obj) {
            is ConfigurationSection -> {
                return obj
            }

            is Map<*, *> -> {
                val result = MemoryConfiguration()
                result.addDefaults((obj as Map<String, Any>))
                return result.defaultSection
            }

            else -> {
                val currentPath = if (cs.currentPath.isNullOrEmpty()) path else cs.currentPath + "." + path
                Utils.logger.warning(
                    String.format(
                        "%s: couldn't parse Config of type: %s, value: %s",
                        currentPath, obj.javaClass.simpleName, obj
                    )
                )
                return null
            }
        }
    }

    private fun objectToConfigurationSectionOld(
        obj: Any?
    ): ConfigurationSection? {
        if (obj == null) {
            return null
        }

        when (obj) {
            is ConfigurationSection -> {
                return obj
            }

            is Map<*, *> -> {
                val result = MemoryConfiguration()
                result.addDefaults((obj as Map<String?, Any?>?)!!)
                return result.defaultSection
            }

            else -> {
                Utils.logger.warning(
                    "couldn't parse Config of type: " + obj.javaClass.simpleName + ", value: "
                            + obj
                )
                return null
            }
        }
    }

    private fun addMaterialToDrop(
        materialName: String,
        item: CustomDropItem
    ): Boolean {
        var materialName = materialName
        materialName = Utils.replaceEx(materialName, "mob_head", "player_head")
        materialName = Utils.replaceEx(materialName, "mobhead", "player_head")

        if (materialName.contains(":")) {
            // this item is referencing a custom item from an external plugin, we will call LM_Items to get it
            if (LevelledMobs.instance.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
                if (!handler.lmItemsParser!!.parseExternalItemAttributes(materialName, item)) {
                    return false
                }
            } else {
                if (ExternalCompatibilityManager.hasLMItemsInstalled()) {
                    Utils.logger.warning(
                        String.format(
                            "Custom drop '%s' requires plugin LM_Items but it is an old version",
                            materialName
                        )
                    )
                } else {
                    Utils.logger.warning(
                        String.format(
                            "Custom drop '%s' requires plugin LM_Items but it is not installed",
                            materialName
                        )
                    )
                }
                return false
            }
        } else {
            val material: Material
            if ("override".equals(materialName, ignoreCase = true)) {
                dropInstance!!.overrideStockDrops = true
                return true
            }
            try {
                material = Material.valueOf(materialName.uppercase(Locale.getDefault()))
            } catch (e: java.lang.Exception) {
                Utils.logger.warning(
                    String.format(
                        "Invalid material type specified in customdrops.yml for: %s, %s",
                        dropInstance!!.getMobOrGroupName(), materialName
                    )
                )
                return false
            }

            item.material = material
        }

        dropInstance!!.customItems.add(item)

        return true
    }

    private fun checkForMobOverride(itemEntry: Map.Entry<String, Any>): Boolean {
        if (itemEntry.key.equals("override", ignoreCase = true)) {
            val value = itemEntry.value
            if (value.javaClass == Boolean::class.java) {
                dropInstance!!.overrideStockDrops = value as Boolean
                return true
            }
        }

        return false
    }

    fun showCustomDropsDebugInfo(
        sender: CommandSender?
    ) {
        val sbMain = StringBuilder()
        var dropsCount = 0
        var commandsCount = 0
        for (cdi in handler.getCustomDropsitems().values) {
            for (base in cdi.customItems) {
                if (base is CustomDropItem) {
                    dropsCount++
                } else if (base is CustomCommand) {
                    commandsCount++
                }
            }
        }

        val allGroups = handler.getCustomDropsitemsGroups()
        val itemsCount =
            allGroups.size + handler.customDropsitemsBabies.size
        val customItemGroupCount = handler.customItemGroups.size
        sbMain.append(
            "drop instances: ${handler.getCustomDropsitems().size}, " +
            "custom groups: $itemsCount, " +
            "item groups: $customItemGroupCount, " +
            "items: $dropsCount, " +
            "commands: $commandsCount"
        )

        for (msg in invalidExternalItems) {
            sbMain.append("\n&4").append(msg).append("&r")
        }

        // build string list to alphabeticalize the drops by entity type including babies
        val typeNames: SortedMap<String, EntityType> = TreeMap()

        for (ent in handler.getCustomDropsitems().keys) {
            typeNames[ent.toString()] = ent
        }

        for (ent in handler.customDropsitemsBabies.keys) {
            typeNames[ent.toString() + "_2"] = ent
        }

        for (entTypeStr in typeNames.keys) {
            val isBaby = entTypeStr.endsWith("_2")
            val ent = EntityType.valueOf(
                if (isBaby) entTypeStr.substring(0, entTypeStr.length - 2) else entTypeStr
            )
            val dropInstance = if (isBaby) handler.customDropsitemsBabies[ent]!!
            else handler.getCustomDropsitems()[ent]!!

            val override = if (dropInstance.getOverrideStockDrops) " (override)" else ""
            val overallChance = if (dropInstance.overallChance != null) (" (overall_chance: "
                    + dropInstance.overallChance + ")") else ""
            sbMain.append("\nmob: &b")
            if (isBaby) {
                sbMain.append("(baby) ")
            }
            sbMain.append(ent.name).append("&r")
            sbMain.append(override).append(overallChance)
            if (dropInstance.overallPermissions.isNotEmpty()) {
                sbMain.append(" (overall perms: ")
                sbMain.append(dropInstance.overallPermissions).append(")")
            }

            Utils.logger.info("customItems count: ${dropInstance.customItems.size}")
            for (baseItem in dropInstance.customItems) {
                Utils.logger.info("checking ${dropInstance.customItems}")
                val result = showCustomDropsDebugInfo2(baseItem)
                if (result.isNotEmpty()) {
                    sbMain.append("\n").append(result)
                }
            }
        }

        for ((key, value) in allGroups) {
            val override = if (value.getOverrideStockDrops) " (override)" else ""
            val overallChance = if (value.overallChance != null) (" (overall_chance: "
                    + value.overallChance + ")") else ""
            if (sbMain.isNotEmpty()) {
                sbMain.append("\n")
            }
            sbMain.append("group: ").append(key)
            sbMain.append(override).append(overallChance)
            for (baseItem in value.customItems) {
                val result = showCustomDropsDebugInfo2(baseItem)
                if (result.isNotEmpty()) {
                    sbMain.append("\n").append(result)
                }
            }
        }

        if (sender != null)
            sender.sendMessage(colorizeAll(sbMain.toString()))
        else
            Utils.logger.info(sbMain.toString())
    }

    private fun showCustomDropsDebugInfo2(
        baseItem: CustomDropBase
    ): String {
        val command = if (baseItem is CustomCommand) baseItem else null
        val item = if (baseItem is CustomDropItem) baseItem else null

        val sb = java.lang.StringBuilder()
        if (item != null) {
            val itemMaterial = item.material.toString()
            sb.append(
                String.format(
                    "  &b%s&r, amount: &b%s&r, chance: &b%s&r", itemMaterial,
                    item.amountAsString, baseItem.chance
                )
            )
        } else if (baseItem is CustomCommand) {
            sb.append(
                String.format(
                    "  COMMAND, chance: &b%s&r, run-on-spawn: %s, run-on-death: %s",
                    baseItem.chance, baseItem.runOnSpawn, baseItem.runOnDeath
                )
            )
        }

        if (baseItem.minLevel > -1) {
            sb.append(", minL: &b")
            sb.append(baseItem.minLevel).append("&r")
        }
        if (baseItem.maxLevel > -1) {
            sb.append(", maxL: &b")
            sb.append(baseItem.maxLevel).append("&r")
        }

        if (baseItem.minPlayerLevel > -1) {
            sb.append(", minPL: &b")
            sb.append(baseItem.minPlayerLevel).append("&r")
        }
        if (baseItem.maxPlayerLevel > -1) {
            sb.append(", maxPL: &b")
            sb.append(baseItem.maxPlayerLevel).append("&r")
        }

        if (baseItem.permissions.isNotEmpty()) {
            sb.append(", perms: &b")
            sb.append(baseItem.permissions).append("&r")
        }

        if (baseItem.noSpawner) {
            sb.append(", nospn")
        }

        if (baseItem.causeOfDeathReqs != null) {
            sb.append(", ").append(baseItem.causeOfDeathReqs)
        }

        if (baseItem.hasGroupId) {
            sb.append(", gId: &b")
            sb.append(baseItem.groupId).append("&r")

            if (baseItem.maxDropGroup > 0 && !handler.groupLimitsMap.containsKey(baseItem.groupId)) {
                sb.append(", maxDropGroup: &b")
                sb.append(baseItem.maxDropGroup).append("&r")
            }
        }
        if (baseItem.priority > 0) {
            sb.append(", pri: &b")
            sb.append(baseItem.priority).append("&r")
        }

        if (command != null) {
            if (!command.commandName.isNullOrEmpty()) {
                sb.append(", name: &b")
                sb.append(command.commandName).append("&r")
            }

            return sb.toString()
        }

        if (item == null) {
            return sb.toString() // this shuts up the IDE for possible null reference
        }

        if (item.noMultiplier) {
            sb.append(", nomultp")
        }
        if (item.lore != null && item.lore!!.isNotEmpty()) {
            sb.append(", hasLore")
        }
        if (item.customName != null && item.customName!!.isNotEmpty()) {
            sb.append(", hasName")
        }
        if (item.damage != 0 || item.hasDamageRange) {
            sb.append(", dmg: &b")
            sb.append(item.getDamageAsString()).append("&r")
        }
        if (item.excludedMobs.isNotEmpty()) {
            sb.append(", hasExcludes")
        }
        if (item.equippedChance != null && !item.equippedChance!!.isDefault) {
            sb.append(", equipChance: &b")
            sb.append(item.equippedChance).append("&r")
        }
        if (item.onlyDropIfEquipped) {
            sb.append(", &bonlyDropIfEquipped&r")
        }
        if (item.equipOnHelmet) {
            sb.append(", &bequipHelmet&r")
        }
        if (item.itemFlags != null && item.itemFlags!!.isNotEmpty()) {
            sb.append(", itemflags: &b")
            sb.append(item.itemFlags!!.size).append("&r")
        }

        if (item.isExternalItem) {
            sb.append(", ext: ")
            sb.append(item.externalPluginName)

            if (item.externalType != null) {
                sb.append(", ex-type: ")
                sb.append(item.externalType)
            }
            if (item.externalItemId != null) {
                sb.append(", ex-id: ")
                sb.append(item.externalItemId)
            }
            if (item.externalAmount != null) {
                sb.append(", ex-amt: ")
                sb.append(item.externalAmount)
            }
            if (item.externalExtras != null) {
                sb.append(", ex-xtras: ")
                sb.append(item.externalExtras!!.size)
            }
        }

        if (item.enchantmentChances != null && !item.enchantmentChances!!.isEmpty) {
            val enchantmentLevels = java.lang.StringBuilder()
            enchantmentLevels.append("encht-lvls: ")

            for (enchantment in item.enchantmentChances!!.items.keys) {
                if (enchantmentLevels.length > 12) enchantmentLevels.append("; ")
                enchantmentLevels.append("&b")
                enchantmentLevels.append(enchantment.key.value())
                enchantmentLevels.append("&r: ")
                var isFirst = true
                for ((key, value) in item.enchantmentChances!!.items[enchantment]?.entries!!) {
                    if (!isFirst) enchantmentLevels.append(", ")
                    enchantmentLevels.append("$key-&b$value&r")

                    isFirst = false
                }

                if (item.enchantmentChances!!.options.containsKey(enchantment)) {
                    val opts = item.enchantmentChances!!.options[enchantment]
                    if (opts!!.defaultLevel != null) enchantmentLevels.append(", dflt: ").append(opts.defaultLevel)
                    if (!opts.doShuffle) enchantmentLevels.append(", no shfl")
                }
            }

            sb.append(System.lineSeparator())
            sb.append("    ")
            sb.append(enchantmentLevels)
        }

        if (item.itemStack != null) {
            val meta = item.itemStack!!.itemMeta
            val sb2 = java.lang.StringBuilder()
            if (meta != null) {
                for (enchant in meta.enchants.keys) {
                    if (sb2.isNotEmpty()) sb2.append(", ")

                    sb2.append(
                        String.format(
                            "&b%s&r (%s)", enchant.key.key,
                            item.itemStack!!.itemMeta.enchants[enchant]
                        )
                    )
                }
            }

            if (sb2.isNotEmpty()) {
                sb.append("\n    ").append(sb2)
            }
        }

        return sb.toString()
    }

    private fun objToCS(
        cs: ConfigurationSection?,
        path: String
    ): ConfigurationSection? {
        if (cs == null) {
            return null
        }
        val useKey: String = ymlHelper.getKeyNameFromConfig(cs, path)
        val obj = cs[useKey] ?: return null

        when (obj) {
            is ConfigurationSection -> {
                return obj
            }

            is Map<*, *> -> {
                val result = MemoryConfiguration()
                result.addDefaults((obj as Map<String, Any>))
                return result.defaultSection
            }

            else -> {
                val currentPath = if (cs.currentPath.isNullOrEmpty()) path else cs.currentPath + "." + path
                Utils.logger.warning(
                    "$currentPath: couldn't parse Config of type: " + obj.javaClass
                        .simpleName + ", value: " + obj
                )
                return null
            }
        }
    }
}