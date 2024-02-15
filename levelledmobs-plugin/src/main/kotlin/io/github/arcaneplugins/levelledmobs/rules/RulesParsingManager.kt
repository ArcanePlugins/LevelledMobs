@file:Suppress("UNCHECKED_CAST")

package io.github.arcaneplugins.levelledmobs.rules

import java.util.LinkedList
import java.util.Locale
import java.util.TreeMap
import java.util.TreeSet
import java.util.regex.Pattern
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.enums.LevelledMobSpawnReason
import io.github.arcaneplugins.levelledmobs.enums.MobCustomNameStatus
import io.github.arcaneplugins.levelledmobs.enums.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.enums.ModalListParsingTypes
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.misc.CustomUniversalGroups
import io.github.arcaneplugins.levelledmobs.misc.YmlParsingHelper
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes.Multiplier
import io.github.arcaneplugins.levelledmobs.rules.strategies.CustomStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.RandomLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.SpawnDistanceStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.YDistanceStrategy
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.Utils.isDouble
import io.github.arcaneplugins.levelledmobs.util.Utils.isInteger
import org.bukkit.Particle
import org.bukkit.block.Biome
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType

/**
 * Contains the logic that parses rules.yml and reads them into the corresponding java classes
 *
 * @author stumper66
 * @since 3.0.0
 */
class RulesParsingManager {
    //private val ymlHelper = YmlParsingHelper(YamlConfiguration())
    private var parsingInfo = RuleInfo()
    val rulePresets: MutableMap<String, RuleInfo> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    var customRules = mutableListOf<RuleInfo>()
    var defaultRule: RuleInfo? = null
    private var customBiomeGroups: MutableMap<String, MutableSet<String>>? = null
    private val emptyArrayPattern = Pattern.compile("\\[\\s+?]|\\[]")

    companion object{
        private const val MLALLOWEDLIST = "allowed-list"
        private const val MLALLOWEDGROUPS = "allowed-groups"
        private const val MLEXCLUDEDITEMS = "excluded-list"
        private const val MLEXCLUDEDGROUPS = "excluded-groups"
    }

    fun parseRulesMain(config: YamlConfiguration?) {
        if (config == null) {
            Log.war("rules config was null")
            return
        }

        val main = LevelledMobs.instance
        //ymlHelper.cs = config
        rulePresets.clear()
        main.rulesManager.rulesInEffect.clear()
        main.customMobGroups.clear()

        parseCustomMobGroups(YmlParsingHelper.objToCS(config, "mob-groups"))
        parseCustomBiomeGroups(YmlParsingHelper.objToCS(config, "biome-groups"))

        val presets = parsePresets(YmlParsingHelper.objToCS(config, "presets"))
        for (ri in presets) {
            rulePresets[ri.presetName!!] = ri
        }

        this.defaultRule = parseDefaults(YmlParsingHelper.objToCS(config, "default-rule"))
        main.rulesManager.rulesInEffect.add(defaultRule!!)
        main.rulesManager.anyRuleHasChance = defaultRule!!.conditionsChance != null
        main.rulesManager.hasAnyWGCondition = (defaultRule!!.conditionsWGregions != null
                || defaultRule!!.conditionsWGregionOwners != null)

        main.rulesManager.buildBiomeGroupMappings(customBiomeGroups)
        this.customRules = parseCustomRules(
            config[YmlParsingHelper.getKeyNameFromConfig(config, "custom-rules")]
        )

        checkCustomRules()
        autoGenerateWeightedRandom()
    }

    fun checkCustomRules() {
        val ruleMappings: MutableMap<String, RuleInfo> = TreeMap(String.CASE_INSENSITIVE_ORDER)
        val main = LevelledMobs.instance

        for (ruleInfo in customRules) {
            LevelledMobs.instance.rulesManager.rulesInEffect.add(ruleInfo)

            ruleMappings[ruleInfo.ruleName] = ruleInfo
            if (ruleInfo.conditionsChance != null) {
                main.rulesManager.anyRuleHasChance = true
            }
            if (ruleInfo.conditionsWGregions != null || ruleInfo.conditionsWGregionOwners != null) {
                main.rulesManager.hasAnyWGCondition = true
            }
        }

        synchronized(RulesManager.ruleLocker) {
            main.rulesManager.ruleNameMappings.clear()
            main.rulesManager.ruleNameMappings.putAll(ruleMappings)
            main.rulesManager.rulesCooldown.clear()
        }

        main.rulesManager.updateRulesHash()
        Log.inf("Current rules hash: " + main.rulesManager.currentRulesHash)
    }

    fun getAllRules(): MutableList<RuleInfo> {
        return getAllRules(true)
    }

    fun getAllRules(includePresets: Boolean): MutableList<RuleInfo> {
        val results = mutableListOf<RuleInfo>()
        if (this.defaultRule != null) {
            results.add(this.defaultRule!!)
        }
        if (includePresets) results.addAll(rulePresets.values)
        results.addAll(this.customRules)

        return results
    }

    private fun parseCustomMobGroups(cs: ConfigurationSection?) {
        if (cs == null) return

        for (groupName in cs.getKeys(false)) {
            val names = cs.getStringList(groupName)
            val groupMembers: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
            groupMembers.addAll(names)
            LevelledMobs.instance.customMobGroups[groupName] = groupMembers
        }
    }

    private fun parseCustomBiomeGroups(cs: ConfigurationSection?) {
        if (cs == null) return

        this.customBiomeGroups = mutableMapOf()

        for (groupName in cs.getKeys(false)) {
            val names = cs.getStringList(groupName)
            val groupMembers: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
            groupMembers.addAll(names)
            customBiomeGroups!![groupName] = groupMembers
        }
    }

    private fun parseDefaults(cs: ConfigurationSection?): RuleInfo {
        this.parsingInfo = RuleInfo("defaults")
        parsingInfo.restrictionsMinLevel = 0
        parsingInfo.restrictionsMaxLevel = 0
        parsingInfo.conditionsMobCustomnameStatus = MobCustomNameStatus.EITHER
        parsingInfo.conditionsMobTamedStatus = MobTamedStatus.EITHER
        parsingInfo.babyMobsInheritAdultSetting = true
        parsingInfo.mobLevelInheritance = true
        parsingInfo.creeperMaxDamageRadius = 5
        parsingInfo.nametagVisibleTime = 1000L

        if (cs == null) {
            Log.war("default-rule section was null")
            return this.parsingInfo
        }

        parseValues(YmlParsingHelper(cs))
        return this.parsingInfo
    }

    private fun parsePresets(
        cs: ConfigurationSection?
    ): MutableList<RuleInfo> {
        val results = mutableListOf<RuleInfo>()
        if (cs == null) return results

        var count = -1
        for (key in cs.getKeys(false)) {
            count++
            val csKey = YmlParsingHelper.objToCS(cs, key)
            if (csKey == null) {
                Log.war("nothing was specified for preset: $key")
                continue
            }

            this.parsingInfo = RuleInfo("preset $count")
            parsingInfo.presetName = key
            parseValues(YmlParsingHelper(csKey))
            results.add(this.parsingInfo)
        }

        return results
    }

    private fun buildCachedModalOfType(
        cs: ConfigurationSection?,
        defaultValue: CachedModalList<*>?,
        type: ModalListParsingTypes
    ): CachedModalList<*>? {
        if (cs == null) {
            return defaultValue
        }

        val mlpi = ModalListParsingInfo(type)

        when (type) {
            ModalListParsingTypes.BIOME -> {
                mlpi.configurationKey = "biomes"
                mlpi.itemName = "Biome"
                mlpi.supportsGroups = true
                mlpi.groupMapping =  LevelledMobs.instance.rulesManager.biomeGroupMappings
                mlpi.cachedModalList = CachedModalList<Biome?>()
            }

            ModalListParsingTypes.SPAWN_REASON -> {
                mlpi.configurationKey = "allowed-spawn-reasons"
                mlpi.itemName = "spawn reason"
                mlpi.cachedModalList = CachedModalList<LevelledMobSpawnReason?>()
            }

            ModalListParsingTypes.VANILLA_BONUSES -> {
                mlpi.configurationKey = "vanilla-bonus"
                mlpi.itemName = "vanilla bonus"
                mlpi.cachedModalList = CachedModalList<VanillaBonusEnum?>()
            }
        }
        return buildCachedModal(cs, defaultValue, mlpi)
    }

    private fun buildCachedModal(
        cs: ConfigurationSection?,
        defaultValue: CachedModalList<*>?,
        mlpi: ModalListParsingInfo
    ): CachedModalList<*>? {
        if (cs == null) return defaultValue

        //TODO: test changes here
        val useKeyName = YmlParsingHelper.getKeyNameFromConfig(cs, mlpi.configurationKey!!)

        val cachedModalList = mlpi.cachedModalList
        val simpleStringOrArray = cs[useKeyName]
        var cs2: ConfigurationSection? = null
        var useList: MutableList<String>? = null

        if (simpleStringOrArray is ArrayList<*>) {
            useList = (simpleStringOrArray as ArrayList<String>).toMutableList()
        } else if (simpleStringOrArray is String) {
            useList = mutableListOf(simpleStringOrArray)
        }

        if (useList == null) {
            cs2 = YmlParsingHelper.objToCS(cs, useKeyName)
        }
        if (cs2 == null && useList == null) {
            return defaultValue
        }

        cachedModalList!!.doMerge = YmlParsingHelper.getBoolean(cs2, "merge")

        if (cs2 != null && mlpi.supportsGroups) {
            cachedModalList.allowedGroups = TreeSet(String.CASE_INSENSITIVE_ORDER)
            cachedModalList.excludedGroups = TreeSet(String.CASE_INSENSITIVE_ORDER)

            for (group in YmlParsingHelper.getListFromConfigItem(
                cs2, MLALLOWEDGROUPS)
            ) {
                if (group.trim { it <= ' ' }.isEmpty()) {
                    continue
                }
                if (mlpi.groupMapping == null || !mlpi.groupMapping!!.containsKey(group)) {
                    Log.war("invalid ${mlpi.itemName} group: $group")
                } else {
                    cachedModalList.allowedGroups.add(group)
                }
            }

            for (group in YmlParsingHelper.getListFromConfigItem(
                cs2, MLEXCLUDEDGROUPS)
            ){
                if (group.trim { it <= ' ' }.isEmpty()) {
                    continue
                }
                if (!LevelledMobs.instance.rulesManager.biomeGroupMappings.containsKey(group)) {
                    Log.war("invalid ${mlpi.itemName} group: $group")
                } else {
                    cachedModalList.excludedGroups.add(group)
                }
            }
        }

        for (i in 0..1) {
            // 0 is allowed list, 1 is excluded list
            val invalidWord = if (i == 0) "allowed" else "excluded"
            val configKeyname: String =
                if (i == 0) MLALLOWEDLIST else MLEXCLUDEDITEMS
            if (i == 1 && cs2 == null) break

            if (cs2 != null) {
                useList = YmlParsingHelper.getListFromConfigItem(cs2, configKeyname)
            }

            for (item in useList!!) {
                if (item.trim { it <= ' ' }.isEmpty()) {
                    continue
                }
                if ("*" == item.trim { it <= ' ' }) {
                    if (i == 0) cachedModalList.allowAll = true
                    else cachedModalList.excludeAll = true

                    continue
                }
                if (emptyArrayPattern.matcher(item).matches()) {
                    continue
                }
                try {
                    when (mlpi.type) {
                        ModalListParsingTypes.BIOME -> {
                            val biomeModalList = cachedModalList as CachedModalList<Biome>
                            val modalList = if (i == 0) biomeModalList.allowedList else biomeModalList.excludedList

                            val biome = Biome.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                            modalList.add(biome)
                        }
                        ModalListParsingTypes.SPAWN_REASON -> {
                            val spawnReasonModalList = cachedModalList as CachedModalList<LevelledMobSpawnReason>
                            val modalList =
                                if (i == 0) spawnReasonModalList.allowedList else spawnReasonModalList.excludedList

                            val spawnReason =
                                LevelledMobSpawnReason.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                            modalList.add(spawnReason)
                        }
                        ModalListParsingTypes.VANILLA_BONUSES -> {
                            val vanillaBonusModalList = cachedModalList as CachedModalList<VanillaBonusEnum>
                            val modalList =
                                if (i == 0) vanillaBonusModalList.allowedList else vanillaBonusModalList.excludedList

                            val vanillaBonus =
                                VanillaBonusEnum.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                            modalList.add(vanillaBonus)
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    Log.war("Invalid $invalidWord ${mlpi.itemName}: $item")
                }
            }
        }

        return cachedModalList
    }

    private fun buildCachedModalListOfString(
        cs: ConfigurationSection?,
        name: String,
        defaultValue: CachedModalList<String>?
    ): CachedModalList<String>? {
        if (cs == null) return defaultValue

        val cachedModalList = CachedModalList(
            TreeSet(String.CASE_INSENSITIVE_ORDER),
            TreeSet(String.CASE_INSENSITIVE_ORDER)
        )
        val useKeyName = YmlParsingHelper.getKeyNameFromConfig(cs, name)
        val simpleStringOrArray = cs[useKeyName]
        var cs2: ConfigurationSection? = null
        var useList: MutableList<String>? = null

        if (simpleStringOrArray is java.util.ArrayList<*>) {
            useList = (simpleStringOrArray as ArrayList<String>).toMutableList()
        } else if (simpleStringOrArray is String) {
            useList = mutableListOf(simpleStringOrArray)
        }

        if (useList == null) {
            cs2 = YmlParsingHelper.objToCS(cs, useKeyName)
        }
        if (cs2 == null && useList == null) {
            return defaultValue
        }

        cachedModalList.doMerge = YmlParsingHelper.getBoolean(cs2, "merge")

        if (cs2 != null) {
            val allowedList = YmlParsingHelper.getKeyNameFromConfig(cs2, MLALLOWEDLIST)
            useList = YmlParsingHelper.getListFromConfigItem(cs2, allowedList)
        }

        for (item in useList!!) {
            if (item.trim { it <= ' ' }.isEmpty()) {
                continue
            }
            if ("*" == item.trim { it <= ' ' }) {
                cachedModalList.allowAll = true
                continue
            }
            if (emptyArrayPattern.matcher(item).matches()) {
                continue
            }
            cachedModalList.allowedList.add(item)
        }
        if (cs2 == null) {
            return cachedModalList
        }

        val allowedGroups = YmlParsingHelper.getKeyNameFromConfig(cs2, MLALLOWEDGROUPS)
        val excludedList = YmlParsingHelper.getKeyNameFromConfig(cs2, MLEXCLUDEDITEMS)
        val excludedGroups = YmlParsingHelper.getKeyNameFromConfig(cs2, MLEXCLUDEDGROUPS)
        cachedModalList.allowedGroups = getSetOfGroups(cs2, allowedGroups)

        for (item in YmlParsingHelper.getListFromConfigItem(cs2, excludedList)) {
            if (item.trim { it <= ' ' }.isEmpty()) {
                continue
            }
            if ("*" == item.trim { it <= ' ' }) {
                cachedModalList.excludeAll = true
                continue
            }
            cachedModalList.excludedList.add(item)
        }
        cachedModalList.excludedGroups = getSetOfGroups(cs2, excludedGroups)

        if (cachedModalList.isEmpty() && !cachedModalList.allowAll && !cachedModalList.excludeAll) {
            return defaultValue
        }

        return cachedModalList
    }

    private fun getSetOfGroups(
        cs: ConfigurationSection,
        key: String
    ): MutableSet<String> {
        var foundKeyName: String? = null
        for (enumeratedKey in cs.getKeys(false)) {
            if (key.equals(enumeratedKey, ignoreCase = true)) {
                foundKeyName = enumeratedKey
                break
            }
        }

        val results: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
        if (foundKeyName == null) {
            return results
        }

        val groups = cs.getStringList(foundKeyName)
        if (groups.isEmpty() && cs.getString(foundKeyName) != null) {
            groups.add(cs.getString(foundKeyName))
        }

        for (group in groups) {
            if (group!!.trim { it <= ' ' }.isEmpty()) {
                continue
            }
            var invalidGroup = false
            if (group.lowercase(Locale.getDefault()).startsWith("all_")) {
                try {
                    val customGroup = CustomUniversalGroups.valueOf(
                        group.uppercase(Locale.getDefault())
                    )
                    results.add(customGroup.toString())
                    continue
                } catch (e: IllegalArgumentException) {
                    invalidGroup = true
                }
            }
            if (LevelledMobs.instance.customMobGroups.containsKey(group)) {
                results.add(group)
            } else {
                invalidGroup = true
            }

            if (invalidGroup) {
                Log.war("Invalid group: $group")
            }
        }

        return results
    }

    private fun parseCustomRules(rulesSection: Any?): MutableList<RuleInfo> {
        val results = mutableListOf<RuleInfo>()
        if (rulesSection == null) {
            return results
        }

        for (hashMap in rulesSection as MutableList<MutableMap<String, Any>>) {
            val cs = objToCS2(hashMap)
            if (cs == null) {
                Log.war("cs was null (parsing custom-rules)")
                continue
            }

            this.parsingInfo = RuleInfo("rule " + results.size)
            parseValues(YmlParsingHelper(cs))
            results.add(this.parsingInfo)
        }

        return results
    }

    private fun parseValues(ymlHelper: YmlParsingHelper) {
        val ruleName = ymlHelper.getString( "name")
        if (ruleName != null) {
            parsingInfo.ruleName = ruleName
        }

        mergePreset(ymlHelper)

        parsingInfo.ruleIsEnabled = ymlHelper.getBoolean( "enabled", true)

        parseStrategies(YmlParsingHelper.objToCS(ymlHelper.cs, "strategies"))
        parseConditions(YmlParsingHelper.objToCS(ymlHelper.cs, "conditions"))
        parseApplySettings(YmlParsingHelper.objToCS(ymlHelper.cs, "apply-settings"))
    }

    private fun mergePreset(ymlHelper: YmlParsingHelper) {
        val presets = ymlHelper.getStringOrList("use-preset")
        if (presets.isEmpty()) return

        for (checkName: String in presets) {
            val checkNameTrimmed = checkName.trim { it <= ' ' }
            if (!rulePresets.containsKey(checkNameTrimmed)) {
                val ruleName = parsingInfo.presetName?: parsingInfo.ruleName
                Log.war(
                    "Rule: '$ruleName' specified preset name '$checkNameTrimmed' but none was found"
                )
                continue
            }

            parsingInfo.mergePresetRules(rulePresets[checkNameTrimmed])
        }
    }

    private fun parseTieredColoring(cs: ConfigurationSection?) {
        if (cs == null) return

        for (name in cs.getKeys(false)) {
            val value = cs.getString(name)

            if (!name.isNullOrEmpty() && value != null) {

                val coloringInfo: TieredColoringInfo? =
                    if ("default".equals(name, ignoreCase = true)) {
                    TieredColoringInfo.createDefault(value)
                } else {
                    TieredColoringInfo.createFromString(name, value)
                }

                if (coloringInfo != null) {
                    if (parsingInfo.tieredColoringInfos == null) {
                        parsingInfo.tieredColoringInfos = LinkedList()
                    }
                    parsingInfo.tieredColoringInfos!!.add(coloringInfo)
                }
            }
        }
    }

    private fun parseEntityNameOverride(cs: ConfigurationSection?) {
        if (cs == null) return

        val levelTiers: MutableMap<String, MutableList<LevelTierMatching>> = TreeMap(
            String.CASE_INSENSITIVE_ORDER
        )
        val entityNames: MutableMap<String, LevelTierMatching> = TreeMap(
            String.CASE_INSENSITIVE_ORDER
        )

        for (name in cs.getKeys(false)) {
            if ("merge".equals(name, ignoreCase = true) && cs.getBoolean(name)) {
                parsingInfo.mergeEntityNameOverrides = cs.getBoolean(name)
                continue
            }

            val names = cs.getStringList(name)
            if (names.isNotEmpty()) {
                val mobNames = LevelTierMatching()
                mobNames.mobName = name
                mobNames.names = names
                val names2 = mutableListOf<String>()

                for (nameFromList in names) {
                    if (nameFromList.isNotEmpty()) {
                        names2.add(nameFromList)
                    }
                }

                if (names2.isNotEmpty()) {
                    entityNames[name] = mobNames
                }
            } else if (cs.getString(name) != null) {
                if ("merge".equals(name, ignoreCase = true)) {
                    parsingInfo.mergeEntityNameOverrides = cs.getBoolean(name)
                    continue
                }
                if (cs[name] is String) {
                    val mobNames = LevelTierMatching()
                    val names2 = mutableListOf(cs.getString(name)!!)
                    mobNames.mobName = name
                    mobNames.names = names2
                    entityNames[name] = mobNames
                } else if (cs[name] is MemorySection || cs[name] is LinkedHashMap<*, *>) {
                    val tiers = parseNumberRange(
                        YmlParsingHelper.objToCS(cs, name),
                        name
                    )
                    if (!tiers.isNullOrEmpty()) {
                        levelTiers[name] = tiers
                    }
                }
            }
        }

        if (entityNames.isNotEmpty()) {
            parsingInfo.entityNameOverrides = entityNames
        }
        if (levelTiers.isNotEmpty()) {
            parsingInfo.entityNameOverridesLevel = levelTiers
        }
    }

    private fun parseNumberRange(
        cs: ConfigurationSection?,
        keyName: String
    ): MutableList<LevelTierMatching>? {
        if (cs == null) return null

        val levelTiers = mutableListOf<LevelTierMatching>()

        for (name in cs.getKeys(false)) {
            val names = cs.getStringList(name)
            val tier = LevelTierMatching()

            if ("merge".equals(name, ignoreCase = true)) {
                continue
            }

            tier.mobName = name

            if (names.isNotEmpty()) {
                // an array of names was provided
                tier.names = names
            } else if (cs.getString(name) != null) {
                // a string was provided
                tier.names = mutableListOf(cs.getString(name)!!)
            }

            if (!tier.setRangeFromString(keyName)) {
                Log.war("Invalid number range: $keyName")
            } else if (tier.names!!.isNotEmpty()) {
                levelTiers.add(tier)
            }
        }

        return levelTiers
    }

    private fun parseApplySettings(cs: ConfigurationSection?) {
        if (cs == null) return

        val ymlHelper = YmlParsingHelper(cs)
        parseFineTuning(YmlParsingHelper.objToCS(cs, "multipliers"))
        parseEntityNameOverride(YmlParsingHelper.objToCS(cs, "entity-name-override"))
        parseTieredColoring(YmlParsingHelper.objToCS(cs, "tiered-coloring"))
        parseHealthIndicator(YmlParsingHelper.objToCS(cs, "health-indicator"))

        parsingInfo.restrictionsMinLevel = ymlHelper.getInt2(
            "minlevel", parsingInfo.restrictionsMinLevel
        )
        parsingInfo.restrictionsMaxLevel = ymlHelper.getInt2(
            "maxlevel", parsingInfo.restrictionsMaxLevel
        )

        parsingInfo.conditionsNoDropEntities = buildCachedModalListOfString(
                  cs, "no-drop-multipler-entities", parsingInfo.conditionsNoDropEntities
        )
        parsingInfo.babyMobsInheritAdultSetting = ymlHelper.getBoolean2(
            "baby-mobs-inherit-adult-setting", parsingInfo.babyMobsInheritAdultSetting
        )
        parsingInfo.mobLevelInheritance = ymlHelper.getBoolean2(
            "level-inheritance", parsingInfo.mobLevelInheritance
        )
        parsingInfo.creeperMaxDamageRadius = ymlHelper.getInt2(
             "creeper-max-damage-radius",
            parsingInfo.creeperMaxDamageRadius
        )
        parsingInfo.customDropsUseForMobs = ymlHelper.getBoolean2(
            "use-custom-item-drops-for-mobs", parsingInfo.customDropsUseForMobs
        )
        parseChunkKillOptions(ymlHelper)
        parsingInfo.customDropDropTableIds.addAll(
            ymlHelper.getListFromConfigItem( "use-droptable-id")
        )
        parsingInfo.nametag = ymlHelper.getString("nametag", parsingInfo.nametag)
        parsingInfo.nametagCreatureDeath = ymlHelper.getString(
            "creature-death-nametag", parsingInfo.nametagCreatureDeath
        )
        parsingInfo.nametagPlaceholderLevelled = ymlHelper.getString(
            "nametag-placeholder-levelled", parsingInfo.nametagPlaceholderLevelled
        )
        parsingInfo.nametagPlaceholderUnlevelled = ymlHelper.getString(
            "nametag-placeholder-unlevelled", parsingInfo.nametagPlaceholderUnlevelled
        )
        parsingInfo.sunlightBurnAmount = ymlHelper.getDouble2(
            "sunlight-intensity", parsingInfo.sunlightBurnAmount
        )
        parsingInfo.lockEntity = ymlHelper.getBoolean2(
            "lock-entity", parsingInfo.lockEntity
        )
        parseNBTData(ymlHelper)
        parsingInfo.passengerMatchLevel = ymlHelper.getBoolean2(
             "passenger-match-level", parsingInfo.passengerMatchLevel
        )
        parsingInfo.nametagVisibleTime = ymlHelper.getIntTimeUnitMS(
             "nametag-visible-time", parsingInfo.nametagVisibleTime
        )
        parsingInfo.maximumDeathInChunkThreshold = ymlHelper.getInt2(
            "maximum-death-in-chunk-threshold", parsingInfo.maximumDeathInChunkThreshold
        )
        parsingInfo.chunkMaxCoolDownTime = ymlHelper.getIntTimeUnit(
            "chunk-max-cooldown-seconds", parsingInfo.chunkMaxCoolDownTime
        )
        parsingInfo.spawnerParticlesCount = ymlHelper.getInt2(
             "spawner-particles-count", parsingInfo.spawnerParticlesCount
        )
        parsingInfo.maxAdjacentChunks = ymlHelper.getInt2(
            "max-adjacent-chunks", parsingInfo.maxAdjacentChunks
        )
        if (parsingInfo.maxAdjacentChunks != null && parsingInfo.maxAdjacentChunks!! > 10) {
            parsingInfo.maxAdjacentChunks = 10
        }
        parseSpawnerParticle(ymlHelper.getString("spawner-particles"))
        parseDeathMessages(cs)

        val nametagVisibility = ymlHelper.getStringSet(
            "nametag-visibility-method"
        )
        val nametagVisibilityEnums = mutableListOf<NametagVisibilityEnum>()
        for (nametagVisEnum: String in nametagVisibility) {
            try {
                val nametagVisibilityEnum = NametagVisibilityEnum.valueOf(
                    nametagVisEnum.uppercase(Locale.getDefault())
                )
                nametagVisibilityEnums.add(nametagVisibilityEnum)
            } catch (ignored: Exception) {
                Log.war(
                    "Invalid value in nametag-visibility-method: $nametagVisibility" +
                            ", in rule: ${parsingInfo.ruleName}"
                )
            }
        }

        if (nametagVisibilityEnums.isNotEmpty())
            parsingInfo.nametagVisibilityEnum = nametagVisibilityEnums
    }

    private fun parseChunkKillOptions(ymlHelper: YmlParsingHelper) {
        val opts = ChunkKillOptions()

        opts.disableVanillaDrops = ymlHelper.getBoolean2( "disable-vanilla-drops-on-chunk-max", null)
        opts.disableItemBoost = ymlHelper.getBoolean2( "disable-item-boost-on-chunk-max", null)
        opts.disableXpDrops = ymlHelper.getBoolean2( "disable-xp-boost-on-chunk-max", null)

        if (!opts.isDefault) {
            parsingInfo.chunkKillOptions = opts
        }
    }

    private fun parseDeathMessages(csParent: ConfigurationSection) {
        val cs: ConfigurationSection = YmlParsingHelper.objToCS(
            csParent, "death-messages") ?: return

        val deathMessages = DeathMessages()

        for (weightStr in cs.getKeys(false)) {
            val messages = cs.getStringList(weightStr)
            if (messages.isEmpty()) {
                val temp = cs.getString(weightStr)
                if (!temp.isNullOrEmpty()) messages.add(temp)
            }

            for (message in messages) {
                if (!isInteger(weightStr)) {
                    Log.war("Invalid number in DeathMessages section: $weightStr")
                    continue
                }

                var weight = weightStr.toInt()
                if (weight > 100) {
                    Log.war("value of $weight is over the limit of 100 for death message weight")
                    weight = 100
                }

                deathMessages.addEntry(weight, message)
            }
        }

        if (!deathMessages.isEmpty) {
            parsingInfo.deathMessages = deathMessages
        }
    }

    private fun parseSpawnerParticle(particle: String?) {
        if (particle == null) return

        if ("none".equals(particle, ignoreCase = true)) {
            parsingInfo.spawnerParticle = null
            parsingInfo.useNoSpawnerParticles = true
            return
        }

        try {
            parsingInfo.spawnerParticle = Particle.valueOf(particle.uppercase(Locale.getDefault()))
        } catch (ignored: Exception) {
            Log.war(
                "Invalid value in spawner-particles: $particle, in rule: "
                        + parsingInfo.ruleName
            )
        }
    }

    private fun parseNBTData(ymlHelper: YmlParsingHelper) {
        val keyName = ymlHelper.getKeyNameFromConfig( "nbt-data")
        val temp = ymlHelper.cs[keyName] ?: return

        if (temp is MemorySection || temp is java.util.LinkedHashMap<*, *>) {
            val cs2 = ymlHelper.objToCS(keyName) ?: return

            val nbt = YmlParsingHelper.getString(cs2, "data", null)
            val nbtList = YmlParsingHelper.getStringSet(cs2, "data")
            if (nbt == null && nbtList.isEmpty()) {
                return
            }
            val doMerge = YmlParsingHelper.getBoolean(cs2, "merge", false)

            if (nbtList.isNotEmpty()) {
                parsingInfo.mobNBTData = MergeableStringList()
                parsingInfo.mobNBTData!!.setItemFromList(nbtList)
                parsingInfo.mobNBTData!!.doMerge = doMerge
            } else {
                parsingInfo.mobNBTData = MergeableStringList(nbt, doMerge)
            }
        } else if (temp is Collection<*>) {
            parsingInfo.mobNBTData = MergeableStringList()
            parsingInfo.mobNBTData!!.setItemFromList(temp as Collection<String>)
        } else if (temp is String) {
            parsingInfo.mobNBTData = MergeableStringList(temp)
        }
    }

    private fun parseConditions(cs: ConfigurationSection?) {
        if (cs == null) return

        val ymlHelper = YmlParsingHelper(cs)
        parsingInfo.conditionsWorlds = buildCachedModalListOfString(
            cs, "worlds",
            parsingInfo.conditionsWorlds
        )

        parsingInfo.conditionsMinLevel = ymlHelper.getInt2(
             "minlevel", parsingInfo.conditionsMinLevel
        )
        parsingInfo.conditionsMaxLevel = ymlHelper.getInt2(
             "maxlevel", parsingInfo.conditionsMaxLevel
        )

        parsingInfo.stopProcessingRules = ymlHelper.getBoolean2(
             "stop-processing", parsingInfo.stopProcessingRules
        )
        parsingInfo.conditionsChance = ymlHelper.getFloat2(
             "chance", parsingInfo.conditionsChance
        )

        val mobCustomNameStatus = ymlHelper.getString( "mob-customname-status")
        if (mobCustomNameStatus != null) {
            try {
                parsingInfo.conditionsMobCustomnameStatus = MobCustomNameStatus.valueOf(
                    mobCustomNameStatus.uppercase(Locale.getDefault())
                )
            } catch (e: Exception) {
                Log.war("Invalid value for $mobCustomNameStatus")
            }
        }

        val mobTamedStatus = ymlHelper.getString( "mob-tamed-status")
        if (mobTamedStatus != null) {
            try {
                parsingInfo.conditionsMobTamedStatus = MobTamedStatus.valueOf(
                    mobTamedStatus.uppercase(Locale.getDefault())
                )
            } catch (e: Exception) {
                Log.war("Invalid value for $mobTamedStatus")
            }
        }

        parsingInfo.conditionsApplyAboveY = ymlHelper.getInt2(
             "apply-above-y", parsingInfo.conditionsApplyAboveY
        )
        parsingInfo.conditionsApplyBelowY = ymlHelper.getInt2(
             "apply-below-y", parsingInfo.conditionsApplyBelowY
        )
        parsingInfo.conditionsMinDistanceFromSpawn = ymlHelper.getInt2(
            "min-distance-from-spawn", parsingInfo.conditionsMinDistanceFromSpawn
        )
        parsingInfo.conditionsMaxDistanceFromSpawn = ymlHelper.getInt2(
            "max-distance-from-spawn", parsingInfo.conditionsMaxDistanceFromSpawn
        )
        parsingInfo.conditionsCooldownTime = ymlHelper.getIntTimeUnitMS(
             "cooldown-duration", parsingInfo.conditionsCooldownTime
        )
        parsingInfo.conditionsTimesToCooldownActivation = ymlHelper.getInt2(
            "cooldown-limit", parsingInfo.conditionsTimesToCooldownActivation
        )
        parseWithinCoordinates(YmlParsingHelper.objToCS(ymlHelper.cs, "within-coordinates"))

        parsingInfo.conditionsWGregions = buildCachedModalListOfString(
            cs,
            "allowed-worldguard-regions", parsingInfo.conditionsWGregions
        )
        parsingInfo.conditionsWGregionOwners = buildCachedModalListOfString(
            cs,
            "allowed-worldguard-region-owners", parsingInfo.conditionsWGregionOwners
        )
        parsingInfo.conditionsSpawnReasons = buildCachedModalOfType(
            cs,
            parsingInfo.conditionsSpawnReasons, ModalListParsingTypes.SPAWN_REASON
        ) as CachedModalList<LevelledMobSpawnReason>?
        parsingInfo.conditionsCustomNames = buildCachedModalListOfString(
            cs, "custom-names",
            parsingInfo.conditionsCustomNames
        )
        parsingInfo.conditionsEntities = buildCachedModalListOfString(
            cs, "entities",
            parsingInfo.conditionsEntities
        )
        parsingInfo.conditionsBiomes = buildCachedModalOfType(
            cs,
            parsingInfo.conditionsBiomes, ModalListParsingTypes.BIOME
        ) as CachedModalList<Biome>?
        parsingInfo.conditionsExternalPlugins = buildCachedModalListOfString(
            cs,
            "external-plugins", parsingInfo.conditionsExternalPlugins
        )
        parsingInfo.conditionsMMnames = buildCachedModalListOfString(
            cs,
            "mythicmobs-internal-names", parsingInfo.conditionsMMnames
        )
        parsingInfo.conditionsSpawnerNames = buildCachedModalListOfString(
            cs, "spawner-names",
            parsingInfo.conditionsSpawnerNames
        )
        parsingInfo.conditionsSpawnegEggNames = buildCachedModalListOfString(
            cs,
            "spawner-egg-names", parsingInfo.conditionsSpawnegEggNames
        )
        parsingInfo.conditionsWorldTickTime = parseWorldTimeTicks(
            cs,
            parsingInfo.conditionsWorldTickTime
        )
        parsingInfo.conditionsPermission = buildCachedModalListOfString(
            cs, "permission",
            parsingInfo.conditionsPermission
        )
        parsingInfo.conditionsScoreboardTags = buildCachedModalListOfString(
            cs, "scoreboard-tags",
            parsingInfo.conditionsScoreboardTags
        )
        parsingInfo.conditionsSkyLightLevel = parseMinMaxValue(
            ymlHelper.getString( "skylight-level")
        )

        checkExternalPlugins()
    }

    private fun checkExternalPlugins(){
        val compats = parsingInfo.conditionsExternalPlugins

        if (compats == null || compats.isEmpty())
            return

        for (pluginName in compats.allowedList){
            val checkName = pluginName.replace("_", "-")
            if (!ExternalCompatibilityManager.instance.externalPluginDefinitions.containsKey(checkName)){
                Log.war("no external plugin definition found for: '$checkName'")
            }
        }
        for (pluginName in compats.excludedList){
            val checkName = pluginName.replace("_", "-")
            if (!ExternalCompatibilityManager.instance.externalPluginDefinitions.containsKey(checkName)){
                Log.war("no external plugin definition found for: '$checkName'")
            }
        }
    }

    private fun parseWithinCoordinates(cs: ConfigurationSection?) {
        if (cs == null) return

        val mdr = WithinCoordinates()

        for (axis in mutableListOf(
            WithinCoordinates.Axis.X,
            WithinCoordinates.Axis.Y,
            WithinCoordinates.Axis.Z
        )) {
            for (keyStart in mutableListOf("start-", "end-")) {
                val key = keyStart + axis.name.lowercase(Locale.getDefault())
                val isStart = "start-" == keyStart
                val value = YmlParsingHelper.getString(cs, key)

                if (!mdr.parseAxis(value, axis, isStart)) {
                    Log.war(
                        String.format(
                            "rule: %s, invalid value for %s: %s",
                            parsingInfo.ruleName, key, value
                        )
                    )
                }
            }
        }

        if (mdr.isEmpty) return

        parsingInfo.conditionsWithinCoords = mdr
    }

    private fun parseStrategies(cs: ConfigurationSection?) {
        if (cs == null) return

        val ymlHelper = YmlParsingHelper(cs)
        parsingInfo.maxRandomVariance = ymlHelper.getInt2(
             "max-random-variance", parsingInfo.maxRandomVariance
        )
        if (ymlHelper.getBoolean( "random"))
            parsingInfo.levellingStrategy = RandomLevellingStrategy()

        val csCustom = YmlParsingHelper.objToCS(cs, "custom")
        if (csCustom != null){
            val customStrategy = CustomStrategy()
            customStrategy.formula = csCustom.getString("formula")

            if (!customStrategy.formula.isNullOrEmpty()) {
                parsingInfo.levellingStrategy = customStrategy
            }
        }

        val csYDistance = YmlParsingHelper.objToCS(cs, "y-coordinate")
        if (csYDistance != null) {
            val ymlHelper2 = YmlParsingHelper(csYDistance)
            val yDistanceStrategy =
                if (parsingInfo.levellingStrategy is YDistanceStrategy) parsingInfo.levellingStrategy as YDistanceStrategy? else YDistanceStrategy()

            yDistanceStrategy!!.startingYLevel = ymlHelper2.getInt2(
                 "start", yDistanceStrategy.startingYLevel
            )
            yDistanceStrategy.endingYLevel = ymlHelper2.getInt2(
                 "end", yDistanceStrategy.endingYLevel
            )
            yDistanceStrategy.yPeriod = ymlHelper2.getInt2(
                 "period", yDistanceStrategy.yPeriod
            )

            if (parsingInfo.levellingStrategy != null
                && parsingInfo.levellingStrategy is YDistanceStrategy
            ) {
                parsingInfo.levellingStrategy!!.mergeRule(yDistanceStrategy)
            } else {
                parsingInfo.levellingStrategy = yDistanceStrategy
            }
        }

        val csSpawnDistance = YmlParsingHelper.objToCS(cs, "distance-from-spawn")
        if (csSpawnDistance != null) {
            val ymlHelper2 = YmlParsingHelper(csSpawnDistance)
            val spawnDistanceStrategy =
                if (parsingInfo.levellingStrategy is SpawnDistanceStrategy) parsingInfo.levellingStrategy as SpawnDistanceStrategy?
                else SpawnDistanceStrategy()

            spawnDistanceStrategy!!.increaseLevelDistance = ymlHelper2.getInt2(
                "increase-level-distance", spawnDistanceStrategy.increaseLevelDistance
            )
            spawnDistanceStrategy.startDistance = ymlHelper2.getInt2(
                "start-distance", spawnDistanceStrategy.startDistance
            )

            parseOptionalSpawnCoordinate(csSpawnDistance, spawnDistanceStrategy)

            if (ymlHelper2.getString( "blended-levelling") != null) {
                parseBlendedLevelling(
                    YmlParsingHelper.objToCS(csSpawnDistance, "blended-levelling"),
                    spawnDistanceStrategy
                )
            }

            if (parsingInfo.levellingStrategy != null
                && parsingInfo.levellingStrategy is SpawnDistanceStrategy
            ) {
                parsingInfo.levellingStrategy!!.mergeRule(spawnDistanceStrategy)
            } else {
                parsingInfo.levellingStrategy = spawnDistanceStrategy
            }
        }

        parseWeightedRandom(cs)
        parsePlayerLevellingOptions(YmlParsingHelper.objToCS(cs, "player-levelling"))
    }

    private fun parseWeightedRandom(cs: ConfigurationSection) {
        val useWeightedRandom = cs.getString("weighted-random")

        // weighted-random: true
        val isDisabled = "false".equals(useWeightedRandom, ignoreCase = true)
        if (isDisabled || "true".equals(useWeightedRandom, ignoreCase = true)) {
            val randomLevelling = RandomLevellingStrategy()
            if (isDisabled) {
                randomLevelling.enabled = false
            } else {
                randomLevelling.autoGenerate = true
            }
            parsingInfo.levellingStrategy = randomLevelling

            return
        }

        val csRandom = YmlParsingHelper.objToCS(cs, "weighted-random") ?: return
        val randomMap = mutableMapOf<String, Int>()
        val randomLevelling = RandomLevellingStrategy()
        randomLevelling.doMerge = YmlParsingHelper.getBoolean(csRandom, "merge")

        for (range in csRandom.getKeys(false)) {
            if ("merge".equals(range, ignoreCase = true)) {
                continue
            }
            val value = csRandom.getInt(range)
            randomMap[range] = value
        }

        if (randomMap.isNotEmpty()) {
            randomLevelling.weightedRandom.putAll(randomMap)
        }

        if (parsingInfo.levellingStrategy != null
            && parsingInfo.levellingStrategy is RandomLevellingStrategy
        ) {
            parsingInfo.levellingStrategy!!.mergeRule(randomLevelling)
        } else {
            parsingInfo.levellingStrategy = randomLevelling
        }
    }

    private fun parseWorldTimeTicks(
        cs: ConfigurationSection?,
        existingList: CachedModalList<MinAndMax>?
    ): CachedModalList<MinAndMax>? {
        if (cs == null) return existingList

        val configName = "world-time-tick"
        val temp = buildCachedModalListOfString(cs, configName, null) ?: return existingList
        val result = CachedModalList<MinAndMax>()
        result.allowAll = temp.allowAll
        result.excludeAll = temp.excludeAll
        result.excludedList.addAll(parseMinMaxValue(temp.excludedList, configName))
        result.allowedList.addAll(parseMinMaxValue(temp.allowedList, configName))

        return result
    }

    private fun parseMinMaxValue(
        numberPair: String?,
    ): MinAndMax? {
        if (numberPair == null) return null

        val result = parseMinMaxValue(mutableSetOf(numberPair), "skylight-level")

        return if (result.isEmpty()) {
            null
        } else {
            result.iterator().next()
        }
    }

    private fun parseMinMaxValue(
        numberPairs: MutableSet<String>,
        configName: String
    ): Set<MinAndMax> {
        val result = mutableSetOf<MinAndMax>()

        for (numberPair in numberPairs) {
            val split = numberPair.split("-")
            val minAndMax = MinAndMax()
            var hadInvalidValue = false
            for (i in 0..1) {
                if (!isInteger(split[i])) {
                    Log.war(
                        String.format(
                            "Invalid value for %s: '%s' in rule %s", configName, split[i],
                            parsingInfo.ruleName
                        )
                    )
                    hadInvalidValue = true
                    break
                }
                val parsedNum = split[i].toInt()

                if (i == 0) {
                    minAndMax.min = parsedNum.toFloat()
                    if (split.size == 1) {
                        minAndMax.max = parsedNum.toFloat()
                    }
                } else {
                    minAndMax.max = parsedNum.toFloat()
                }
            }

            if (hadInvalidValue) {
                continue
            }
            result.add(minAndMax)
        }

        return result
    }

    private fun parseHealthIndicator(cs: ConfigurationSection?) {
        if (cs == null)return

        val ymlHelper = YmlParsingHelper(cs)
        val indicator = HealthIndicator()
        indicator.indicator = ymlHelper.getString("indicator", indicator.indicator)
        indicator.indicatorHalf = ymlHelper.getString(
            "indicator-half", indicator.indicatorHalf
        )
        indicator.maxIndicators = ymlHelper.getInt2( "max", indicator.maxIndicators)
        indicator.scale = ymlHelper.getDouble2( "scale", indicator.scale)
        indicator.merge = ymlHelper.getBoolean2( "merge", indicator.merge)

        val csTiers = YmlParsingHelper.objToCS(cs, "colored-tiers")
        if (csTiers != null) {
            val tiers = mutableMapOf<Int, String>()

            for (name in csTiers.getKeys(false)) {
                val name2 = name.lowercase(Locale.getDefault()).replace("tier-", "")

                if ("default".equals(name, ignoreCase = true)) {
                    if (csTiers.getString(name).isNullOrEmpty()) {
                        Log.war("No value entered for colored tier: $name")
                    } else {
                        tiers[0] = csTiers.getString(name)!!
                    }

                    continue
                }

                if (!isInteger(name2)) {
                    Log.war("Not a valid colored tier, missing number: $name")
                    continue
                }

                val tierValue = csTiers.getString(name)
                if (tierValue.isNullOrEmpty()) {
                    Log.war("No value entered for colored tier: $name")
                    continue
                }

                val tierNumber = name2.toInt()
                if (tiers.containsKey(tierNumber)) {
                    Log.war("Duplicate tier: $name")
                } else {
                    tiers[tierNumber] = tierValue
                }
            }
            if (tiers.isNotEmpty()) {
                indicator.tiers = tiers
            }
        }

        if (parsingInfo.healthIndicator != null && parsingInfo.healthIndicator!!.doMerge) {
            parsingInfo.healthIndicator!!.merge(indicator)
        } else {
            parsingInfo.healthIndicator = indicator
        }
    }

    private fun parsePlayerLevellingOptions(cs: ConfigurationSection?) {
        if (cs == null) return

        val ymlHelper = YmlParsingHelper(cs)
        val options = PlayerLevellingOptions()
        options.matchPlayerLevel = ymlHelper.getBoolean2(
             "match-level", options.matchPlayerLevel
        )
        options.usePlayerMaxLevel = ymlHelper.getBoolean2(
             "use-player-max-level", options.usePlayerMaxLevel
        )
        options.playerLevelScale = ymlHelper.getDouble2(
             "player-level-scale", options.playerLevelScale
        )
        options.levelCap = ymlHelper.getInt2( "level-cap", options.levelCap)
        options.enabled = ymlHelper.getBoolean2( "enabled", options.enabled)
        options.doMerge = ymlHelper.getBoolean( "merge", options.doMerge)
        options.variable = ymlHelper.getString( "variable", options.variable)
        options.decreaseLevel = ymlHelper.getBoolean( "decrease-level", true)
        options.recheckPlayers = ymlHelper.getBoolean2( "recheck-players", options.recheckPlayers)
        options.preserveEntityTime = ymlHelper.getIntTimeUnitMS( "preserve-entity", options.preserveEntityTime)
        parsingInfo.playerLevellingOptions = options

        val csTiers = YmlParsingHelper.objToCS(cs, "tiers") ?: return
        val levelTiers = mutableListOf<LevelTierMatching>()

        for (name in csTiers.getKeys(false)) {
            val info = LevelTierMatching()

            val value = csTiers.getString(name)
            if (value == null) {
                Log.war("No value was specified for: $name")
                continue
            }

            if (!name.contains("-") && !isInteger(name)) {
                // found a source tier name rather than number
                info.sourceTierName = name
            } else if (!info.setRangeFromString(name)) {
                Log.war("Invalid number range: $name")
                continue
            }

            val levelRange = LevelTierMatching.getRangeFromString(value)
            if (levelRange.size < 2) {
                Log.war("Invalid number range (len): $value")
                continue
            }
            if (levelRange[0] == -1 && levelRange[1] == -1) {
                Log.war("Invalid number range: $value")
                continue
            }

            info.valueRanges = levelRange
            levelTiers.add(info)
        }

        if (levelTiers.isNotEmpty()) {
            options.levelTiers.addAll(levelTiers)
        }
    }

    private fun parseBlendedLevelling(
        cs: ConfigurationSection?,
        spawnDistanceStrategy: SpawnDistanceStrategy
    ) {
        if (cs == null) return

        val ymlHelper = YmlParsingHelper(cs)
        spawnDistanceStrategy.blendedLevellingEnabled = ymlHelper.getBoolean2(
             "enabled", spawnDistanceStrategy.blendedLevellingEnabled
        )
        spawnDistanceStrategy.transitionYheight = ymlHelper.getInt2(
             "transition-y-height", spawnDistanceStrategy.transitionYheight
        )
        spawnDistanceStrategy.lvlMultiplier = ymlHelper.getDouble2(
             "lvl-multiplier", spawnDistanceStrategy.lvlMultiplier
        )
        spawnDistanceStrategy.multiplierPeriod = ymlHelper.getInt2(
             "multiplier-period", spawnDistanceStrategy.multiplierPeriod
        )
        spawnDistanceStrategy.scaleDownward = ymlHelper.getBoolean2(
             "scale-downward", spawnDistanceStrategy.scaleDownward
        )
    }

    private fun parseOptionalSpawnCoordinate(
        cs: ConfigurationSection,
        sds: SpawnDistanceStrategy
    ) {
        val spawnLocation = YmlParsingHelper.objToCS(cs, "spawn-location") ?: return

        if (!"default".equals(spawnLocation.getString("x"), ignoreCase = true)) {
            sds.spawnLocationX = spawnLocation.getInt("x")
        }

        if (!"default".equals(spawnLocation.getString("z"), ignoreCase = true)) {
            sds.spawnLocationZ = spawnLocation.getInt("z")
        }
    }

    private fun parseFineTuning(cs: ConfigurationSection?) {
        if (cs == null) return

        parsingInfo.vanillaBonuses = buildCachedModalOfType(
            cs,
            parsingInfo.vanillaBonuses, ModalListParsingTypes.VANILLA_BONUSES
        ) as CachedModalList<VanillaBonusEnum>?
        parsingInfo.allMobMultipliers = parseFineTuningValues(cs, parsingInfo.allMobMultipliers)

        val csCustom = YmlParsingHelper.objToCS(cs, "custom-mob-level") ?: return
        val fineTuning: MutableMap<String, FineTuningAttributes> = TreeMap(
            String.CASE_INSENSITIVE_ORDER
        )

        for (mobName in csCustom.getKeys(false)) {
            var checkName = mobName
            if (checkName.lowercase(Locale.getDefault()).startsWith("baby_")) {
                checkName = checkName.substring(5)
            }

            try {
                EntityType.valueOf(checkName.uppercase(Locale.getDefault()))
            } catch (e: IllegalArgumentException) {
                Log.war(
                    "Invalid entity type: $mobName for fine-tuning in rule: "
                            + parsingInfo.ruleName
                )
                continue
            }

            val attribs = parseFineTuningValues(
                YmlParsingHelper.objToCS(csCustom, mobName), null
            )
            if (attribs == null) continue

            fineTuning[mobName] = attribs
        }

        if (fineTuning.isNotEmpty()) {
            if (parsingInfo.specificMobMultipliers != null) {
                parsingInfo.specificMobMultipliers!!.putAll(fineTuning)
            } else {
                parsingInfo.specificMobMultipliers = fineTuning
            }
        }
    }

    private fun parseFineTuningValues(
        cs: ConfigurationSection?,
        defaults: FineTuningAttributes?
    ): FineTuningAttributes? {
        if (cs == null) return defaults

        val ymlHelper = YmlParsingHelper(cs)
        val doMerge = ymlHelper.getBoolean( "merge", true)
        val attribs =
            if (parsingInfo.allMobMultipliers != null && doMerge) parsingInfo.allMobMultipliers
            else FineTuningAttributes()

        for (item in cs.getKeys(false)) {
            when (item.lowercase(Locale.getDefault())) {
                "use-stacked" -> attribs!!.useStacked = ymlHelper.getBoolean2(item, attribs.useStacked)
                "do-not-merge" -> attribs!!.doNotMerge = ymlHelper.getBoolean(item, false)
                "merge" -> attribs!!.doNotMerge = !ymlHelper.getBoolean(item, true)
                "vanilla-bonus", "custom-mob-level" -> {}
                else -> {
                    var lmMultiplier: LMMultiplier
                    try {
                        lmMultiplier = LMMultiplier
                            .valueOf(
                                item.replace("-", "_")
                                    .uppercase(Locale.getDefault())
                            )
                    } catch (ignored: Exception) {
                        Log.war("Invalid multiplier: $item")
                        continue
                    }

                    val addition = attribs!!.getAdditionFromLMMultiplier(lmMultiplier)
                    val multiplier = parseFineTuningValues2(cs, addition, item)
                    if (multiplier != null) {
                        attribs.addItem(addition, multiplier)
                    }
                }
            }
        }

        if (attribs!!.isEmpty) return defaults

        return attribs
    }

    private fun parseFineTuningValues2(
        cs: ConfigurationSection,
        addition: Addition,
        item: String
    ): Multiplier? {
        val values = cs.getList(item)
        if (values == null) {
            val value = YmlParsingHelper.getFloat2(cs, item, null)
            return if (value != null)
                Multiplier(addition, false, value, null, false)
            else
                null
        }

        var customFormula: String? = null
        var useStacked = false
        var value = 0f
        var count = 0
        var valueStr: String? = null
        var isAddition = false
        for (obj in values) {
            if (count > 2) break

            when (obj) {
                is Float -> { value = obj }
                is Double -> { value = obj.toFloat() }
                is Int -> { value = obj.toFloat() }
                is String -> {
                    if ("formula".equals(obj, ignoreCase = true) ||
                        "formula_add".equals(obj, ignoreCase = true)){
                        customFormula = valueStr
                        isAddition = true
                        useStacked = false
                    } else if ("formula_mult".equals(obj, ignoreCase = true)) {
                        customFormula = valueStr
                        isAddition = false
                        useStacked = false
                    }
                    else if ("stacked".equals(obj, ignoreCase = true)) {
                        useStacked = true
                    } else if (isDouble(obj)) {
                        value = obj.toFloat()
                    }

                    valueStr = obj
                }
            }

            count++
        }

        if (value > Float.MIN_VALUE || !customFormula.isNullOrEmpty()) {
            return Multiplier(
                addition, useStacked, value, customFormula, isAddition
            )
        }

        return null
    }

    private fun autoGenerateWeightedRandom() {
        var rls: RandomLevellingStrategy? = null
        var minLevel = 1
        var maxLevel = 1

        for (ruleInfo in LevelledMobs.instance.rulesManager.rulesInEffect) {
            if ("defaults" != ruleInfo.ruleName) continue

            if (ruleInfo.levellingStrategy is RandomLevellingStrategy) {
                if (rls == null) rls = ruleInfo.levellingStrategy as RandomLevellingStrategy
                else rls.mergeRule(rls)
            }

            if (ruleInfo.restrictionsMinLevel != null) minLevel = ruleInfo.restrictionsMinLevel!!
            if (ruleInfo.restrictionsMaxLevel != null) maxLevel = ruleInfo.restrictionsMaxLevel!!
        }

        if (rls == null || !rls.autoGenerate || rls.weightedRandom.isNotEmpty()) return
        for (i in minLevel..maxLevel) rls.weightedRandom[String.format("%s-%s", i, i)] = maxLevel - i + 1

        rls.populateWeightedRandom(minLevel, maxLevel)
    }

    private fun objToCS2(
        obj: Any?
    ): ConfigurationSection? {
        if (obj == null) return null

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
                Log.war(
                    "couldn't parse config of type: " + obj.javaClass.simpleName +
                            ", value: $obj"
                )
                return null
            }
        }
    }
}