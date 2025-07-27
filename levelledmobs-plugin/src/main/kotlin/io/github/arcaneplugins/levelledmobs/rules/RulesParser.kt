@file:Suppress("UNCHECKED_CAST")

package io.github.arcaneplugins.levelledmobs.rules

import java.util.Locale
import java.util.TreeMap
import java.util.TreeSet
import java.util.regex.Pattern
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.enums.MobCustomNameStatus
import io.github.arcaneplugins.levelledmobs.enums.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.enums.ModalListParsingTypes
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.misc.CustomUniversalGroups
import io.github.arcaneplugins.levelledmobs.misc.LMSpawnReason
import io.github.arcaneplugins.levelledmobs.misc.YmlParsingHelper
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes.Multiplier
import io.github.arcaneplugins.levelledmobs.rules.strategies.CustomStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.PlayerLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.RandomLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.SpawnDistanceStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.StrategyType
import io.github.arcaneplugins.levelledmobs.rules.strategies.YDistanceStrategy
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.util.Utils.isDouble
import io.github.arcaneplugins.levelledmobs.util.Utils.isInteger
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.block.Biome
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.EntityType
import org.bukkit.generator.structure.Structure

/**
 * Contains the logic that parses rules.yml and reads them into the corresponding java classes
 *
 * @author stumper66
 * @since 3.0.0
 */
class RulesParser {
    private var parsingInfo = RuleInfo()
    val rulePresets: MutableMap<String, RuleInfo> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    var customRules = mutableListOf<RuleInfo>()
    var defaultRule: RuleInfo? = null
    private var customBiomeGroups: MutableMap<String, MutableSet<String>>? = null

    companion object{
        private const val MLINCLUDEDLIST = "included-list"
        private const val MLINCLUDEDGROUPS = "included-groups"
        private const val MLEXCLUDEDITEMS = "excluded-list"
        private const val MLEXCLUDEDGROUPS = "excluded-groups"
        private val validModalListOptions = mutableListOf<String>()
        private val emptyArrayPattern = Pattern.compile("\\[\\s+?]|\\[]")

        init {
            validModalListOptions.addAll(mutableListOf(
                MLINCLUDEDGROUPS, MLINCLUDEDLIST, MLEXCLUDEDITEMS, MLEXCLUDEDGROUPS, "merge"
            ))
        }

        fun buildCachedModalListOfString(
            cs: ConfigurationSection?,
            name: String,
            defaultValue: CachedModalList<String>?,
            ruleName: String?
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

            if (cs2 != null) {
                for (key in cs2.getKeys(false)) {
                    if (!validModalListOptions.contains(key)){
                        if (ruleName == null)
                            Log.war("Invalid modal list option: '$key'")
                        else
                            Log.war("Invalid modal list option: '$key' in rule: $ruleName")
                    }
                }
            }

            cachedModalList.doMerge = YmlParsingHelper.getBoolean(cs2, "merge")

            if (cs2 != null) {
                val allowedList = YmlParsingHelper.getKeyNameFromConfig(cs2, MLINCLUDEDLIST)
                useList = YmlParsingHelper.getListFromConfigItem(cs2, allowedList)
            }

            for (item in useList!!) {
                if (item.trim { it <= ' ' }.isEmpty()) {
                    continue
                }
                if ("*" == item.trim { it <= ' ' }) {
                    cachedModalList.includeAll = true
                    continue
                }
                if (emptyArrayPattern.matcher(item).matches()) {
                    continue
                }
                cachedModalList.includedList.add(item)
            }
            if (cs2 == null) {
                return cachedModalList
            }

            val allowedGroups = YmlParsingHelper.getKeyNameFromConfig(cs2, MLINCLUDEDGROUPS)
            val excludedList = YmlParsingHelper.getKeyNameFromConfig(cs2, MLEXCLUDEDITEMS)
            val excludedGroups = YmlParsingHelper.getKeyNameFromConfig(cs2, MLEXCLUDEDGROUPS)
            cachedModalList.includedGroups = getSetOfGroups(cs2, allowedGroups)

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

            if (cachedModalList.isEmpty() && !cachedModalList.includeAll && !cachedModalList.excludeAll) {
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
                    } catch (_: IllegalArgumentException) {
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

        private fun buildCachedModalOfType(
            cs: ConfigurationSection?,
            defaultValue: CachedModalList<*>?,
            type: ModalListParsingTypes,
            ruleInfo: RuleInfo
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
                    mlpi.groupMapping = LevelledMobs.instance.rulesManager.biomeGroupMappings
                    mlpi.cachedModalList = CachedModalList<Biome>()
                }

                ModalListParsingTypes.SPAWN_REASON -> {
                    mlpi.configurationKey = "spawn-reasons"
                    mlpi.itemName = "spawn reason"
                    mlpi.cachedModalList = CachedModalList<String>()
                }

                ModalListParsingTypes.VANILLA_BONUSES -> {
                    mlpi.configurationKey = "vanilla-bonus"
                    mlpi.itemName = "vanilla bonus"
                    mlpi.cachedModalList = CachedModalList<VanillaBonusEnum>()
                }

                ModalListParsingTypes.STRUCTURE -> {
                    mlpi.configurationKey = "structures"
                    mlpi.itemName = "Structures"
                    mlpi.cachedModalList = CachedModalList<Structure>()
                }
            }
            return buildCachedModal(cs, defaultValue, mlpi, ruleInfo)
        }

        private fun buildCachedModal(
            cs: ConfigurationSection?,
            defaultValue: CachedModalList<*>?,
            mlpi: ModalListParsingInfo,
            ruleInfo: RuleInfo
        ): CachedModalList<*>? {
            if (cs == null) return defaultValue

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

            if (cs2 != null) {
                for (key in cs2.getKeys(false)) {
                    if (!validModalListOptions.contains(key))
                        Log.war("Invalid modal list option: '$key' in rule: ${ruleInfo.ruleName}")
                }
            }

            cachedModalList!!.doMerge = YmlParsingHelper.getBoolean(cs2, "merge")

            if (cs2 != null && mlpi.supportsGroups) {
                cachedModalList.includedGroups = TreeSet(String.CASE_INSENSITIVE_ORDER)
                cachedModalList.excludedGroups = TreeSet(String.CASE_INSENSITIVE_ORDER)

                for (group in YmlParsingHelper.getListFromConfigItem(
                    cs2, MLINCLUDEDGROUPS)
                ) {
                    if (group.trim { it <= ' ' }.isEmpty()) {
                        continue
                    }
                    if (mlpi.groupMapping == null || !mlpi.groupMapping!!.containsKey(group)) {
                        Log.war("invalid ${mlpi.itemName} group: $group")
                    } else {
                        cachedModalList.includedGroups.add(group)
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
                // 0 is included list, 1 is excluded list
                val invalidWord = if (i == 0) "included" else "excluded"
                val configKeyname: String =
                    if (i == 0) MLINCLUDEDLIST else MLEXCLUDEDITEMS
                if (i == 1 && cs2 == null) break

                if (cs2 != null) {
                    useList = YmlParsingHelper.getListFromConfigItem(cs2, configKeyname)
                }

                for (item in useList!!) {
                    if (item.trim { it <= ' ' }.isEmpty()) {
                        continue
                    }
                    if ("*" == item.trim { it <= ' ' }) {
                        if (i == 0) cachedModalList.includeAll = true
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
                                val modalList = if (i == 0) biomeModalList.includedList else biomeModalList.excludedList

                                val biome = Utils.getBiome(item.trim())
                                if (biome != null)
                                    modalList.add(biome)
                                else
                                    Log.war("Invalid biome name: $item")
                            }
                            ModalListParsingTypes.SPAWN_REASON -> {
                                val spawnReasonModalList = cachedModalList as CachedModalList<String>
                                val modalList =
                                    if (i == 0) spawnReasonModalList.includedList else spawnReasonModalList.excludedList

                                if (LMSpawnReason.validateSpawnReason(item.trim().uppercase()))
                                    modalList.add(item.trim().uppercase())
                                else
                                    Log.war("rule: ${ruleInfo.ruleName}, invalid spawn reason: $item")
                            }
                            ModalListParsingTypes.VANILLA_BONUSES -> {
                                val vanillaBonusModalList = cachedModalList as CachedModalList<VanillaBonusEnum>
                                val modalList =
                                    if (i == 0) vanillaBonusModalList.includedList else vanillaBonusModalList.excludedList

                                val vanillaBonus =
                                    VanillaBonusEnum.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                                modalList.add(vanillaBonus)
                            }
                            ModalListParsingTypes.STRUCTURE -> {
                                if (!LevelledMobs.instance.ver.allowStructureConditions){
                                    Log.war("Structure conditions are not available on this server version")
                                    continue
                                }

                                val structuresModalList = cachedModalList as CachedModalList<Structure>
                                val modalList =
                                    if (i == 0) structuresModalList.includedList else structuresModalList.excludedList

                                val input = item.trim().split(":")
                                if (input.isEmpty()) continue
                                val namespace = if (input.size == 1) NamespacedKey.MINECRAFT_NAMESPACE else input[0]
                                val key = if (input.size == 1) input[0].lowercase() else input[1].lowercase()
                                val structure: Structure?

                                if (LevelledMobs.instance.ver.isRunningPaper && LevelledMobs.instance.ver.minorVersion >= 21){
                                    val registry = RegistryAccess.registryAccess().getRegistry(
                                        RegistryKey.STRUCTURE
                                    )
                                    structure = registry.get(
                                        NamespacedKey.minecraft(key.lowercase(Locale.getDefault()))
                                    )
                                }
                                else{
                                    // legacy versions < 1.21
                                    @Suppress("DEPRECATION")
                                    structure = Registry.STRUCTURE.get(NamespacedKey(namespace, key))
                                }

                                if (structure == null)
                                    Log.war("Invalid $invalidWord ${mlpi.itemName}: $item")
                                else
                                    modalList.add(structure)
                            }
                        }
                    } catch (_: IllegalArgumentException) {
                        Log.war("Invalid $invalidWord ${mlpi.itemName}: $item")
                    }
                }
            }

            return if (cachedModalList.isEmpty() && defaultValue == null)
                null
            else
                cachedModalList
        }
    }

    fun parseRulesMain(config: YamlConfiguration?) {
        if (config == null) {
            Log.war("rules config was null")
            return
        }

        val main = LevelledMobs.instance
        rulePresets.clear()
        main.rulesManager.rulesInEffect.clear()
        main.customMobGroups.clear()
        main.rulesManager.allCustomStrategyPlaceholders.clear()

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

        this.customBiomeGroups = TreeMap(String.CASE_INSENSITIVE_ORDER)

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

    private fun parseCustomRules(rulesSection: Any?): MutableList<RuleInfo> {
        val results = mutableListOf<RuleInfo>()
        if (rulesSection == null) {
            return results
        }

        for (hashMap in rulesSection as MutableList<MutableMap<String, Any>>) {
            val cs = YmlParsingHelper.objToCS2(hashMap)
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
        var ruleName = ymlHelper.getString( "name")
        if (ruleName == null) ruleName = ymlHelper.cs.get("custom-rule").toString()
        if (parsingInfo.ruleName != "defaults") parsingInfo.ruleName = ruleName

        mergePreset(ymlHelper)
        
        parsingInfo.ruleIsEnabled = ymlHelper.getBoolean( "is-enabled", true)

        parseStrategies(YmlParsingHelper.objToCS(ymlHelper.cs, "strategies"))
        parseConditions(YmlParsingHelper.objToCS(ymlHelper.cs, "conditions"))
        parseApplySettings(YmlParsingHelper.objToCS(ymlHelper.cs, "settings"))
        parseModifiers(YmlParsingHelper.objToCS(ymlHelper.cs, "modifiers"))

        for (key in ymlHelper.cs.getKeys(false)){
            if (!KeyValidation.mainRuleSection.contains(key))
                Log.war("Invalid option '$key', in rule: $ruleName")
        }
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
                        parsingInfo.tieredColoringInfos = mutableListOf()
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
        parseFineTuning(cs)
        parseEntityNameOverride(YmlParsingHelper.objToCS(cs, "entity-name-override"))
        parseTieredColoring(YmlParsingHelper.objToCS(cs, "tiered-coloring"))
        parseHealthIndicator(YmlParsingHelper.objToCS(cs, "health-indicator"))

        parsingInfo.constructLevel = ymlHelper.getString(
            "construct-level", parsingInfo.constructLevel)
        parsingInfo.restrictionsMinLevel = ymlHelper.getInt2(
            "minlevel", parsingInfo.restrictionsMinLevel
        )
        parsingInfo.restrictionsMaxLevel = ymlHelper.getInt2(
            "maxlevel", parsingInfo.restrictionsMaxLevel
        )
        parsingInfo.invalidPlaceholderReplacement = ymlHelper.getString(
            "invalid-placeholder-replacement", parsingInfo.invalidPlaceholderReplacement
        )
        parsingInfo.conditionsNoDropEntities = buildCachedModalListOfString(
                  cs, "no-drop-multipler-entities", parsingInfo.conditionsNoDropEntities, parsingInfo.ruleName
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
            } catch (_: Exception) {
                Log.war(
                    "Invalid value in nametag-visibility-method: $nametagVisibility" +
                            ", in rule: ${parsingInfo.ruleName}"
                )
            }
        }

        if (nametagVisibilityEnums.isNotEmpty())
            parsingInfo.nametagVisibilityEnum = nametagVisibilityEnums

        for (key in ymlHelper.cs.getKeys(false)){
            if (!KeyValidation.settings.contains(key))
                Log.war("Invalid setting '$key', in rule: ${parsingInfo.ruleName}")
        }
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
        } catch (_: Exception) {
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
            parsingInfo.conditionsWorlds, parsingInfo.ruleName
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

        parsingInfo.conditionsStructure = buildCachedModalOfType(
            cs,
            parsingInfo.conditionsStructure, ModalListParsingTypes.STRUCTURE, parsingInfo
        ) as CachedModalList<Structure>?

        val mobCustomNameStatus = ymlHelper.getString( "mob-customname-status")
        if (mobCustomNameStatus != null) {
            try {
                parsingInfo.conditionsMobCustomnameStatus = MobCustomNameStatus.valueOf(
                    mobCustomNameStatus.uppercase(Locale.getDefault())
                )
            } catch (_: Exception) {
                Log.war("Invalid value for $mobCustomNameStatus")
            }
        }

        val mobTamedStatus = ymlHelper.getString( "mob-tamed-status")
        if (mobTamedStatus != null) {
            try {
                parsingInfo.conditionsMobTamedStatus = MobTamedStatus.valueOf(
                    mobTamedStatus.uppercase(Locale.getDefault())
                )
            } catch (_: Exception) {
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
            "worldguard-regions", parsingInfo.conditionsWGregions, parsingInfo.ruleName
        )
        parsingInfo.conditionsWGregionOwners = buildCachedModalListOfString(
            cs,
            "worldguard-region-owners", parsingInfo.conditionsWGregionOwners, parsingInfo.ruleName
        )
        parsingInfo.conditionsSpawnReasons = buildCachedModalOfType(
            cs,
            parsingInfo.conditionsSpawnReasons, ModalListParsingTypes.SPAWN_REASON,
            parsingInfo
        ) as CachedModalList<String>?
        parsingInfo.conditionsCustomNames = buildCachedModalListOfString(
            cs, "custom-names",
            parsingInfo.conditionsCustomNames, parsingInfo.ruleName
        )
        parsingInfo.conditionsEntities = buildCachedModalListOfString(
            cs, "entities",
            parsingInfo.conditionsEntities, parsingInfo.ruleName
        )
        parsingInfo.conditionsBiomes = buildCachedModalOfType(
            cs,
            parsingInfo.conditionsBiomes, ModalListParsingTypes.BIOME,
            parsingInfo
        ) as CachedModalList<Biome>?
        parsingInfo.conditionsExternalPlugins = buildCachedModalListOfString(
            cs,
            "external-plugins", parsingInfo.conditionsExternalPlugins, parsingInfo.ruleName
        )
        parsingInfo.conditionsMMnames = buildCachedModalListOfString(
            cs,
            "mythicmobs-internal-names", parsingInfo.conditionsMMnames, parsingInfo.ruleName
        )
        parsingInfo.conditionsSpawnerNames = buildCachedModalListOfString(
            cs, "spawner-names",
            parsingInfo.conditionsSpawnerNames, parsingInfo.ruleName
        )
        parsingInfo.conditionsSpawnegEggNames = buildCachedModalListOfString(
            cs,
            "spawner-egg-names", parsingInfo.conditionsSpawnegEggNames, parsingInfo.ruleName
        )
        parsingInfo.conditionsWorldTickTime = parseWorldTimeTicks(
            cs,
            parsingInfo.conditionsWorldTickTime
        )
        parsingInfo.conditionsPermission = buildCachedModalListOfString(
            cs, "permission",
            parsingInfo.conditionsPermission, parsingInfo.ruleName
        )
        parsingInfo.conditionsPlayerNames = buildCachedModalListOfString(
            cs, "player-names",
            parsingInfo.conditionsPlayerNames, parsingInfo.ruleName
        )
        parsingInfo.conditionsGamemode = buildCachedModalListOfString(
            cs, "gamemode",
            parsingInfo.conditionsGamemode, parsingInfo.ruleName
        )
        parsingInfo.conditionsScoreboardTags = buildCachedModalListOfString(
            cs, "scoreboard-tags",
            parsingInfo.conditionsScoreboardTags, parsingInfo.ruleName
        )
        parsingInfo.conditionsSkyLightLevel = parseMinMaxValue(
            ymlHelper.getString( "skylight-level")
        )

        checkExternalPlugins()

        for (key in ymlHelper.cs.getKeys(false)){
            if (!KeyValidation.conditions.contains(key))
                Log.war("Invalid condition '$key', in rule: ${parsingInfo.ruleName}")
        }
    }

    private fun checkExternalPlugins(){
        val compats = parsingInfo.conditionsExternalPlugins

        if (compats == null || compats.isEmpty())
            return

        for (pluginName in compats.includedList){
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
                    Log.war("rule: ${parsingInfo.ruleName}, invalid value for $key: $value")
                }
            }
        }

        if (mdr.isEmpty) return

        parsingInfo.conditionsWithinCoords = mdr
    }

    private fun parseStrategies(cs: ConfigurationSection?) {
        if (cs == null) return

        val ymlHelper = YmlParsingHelper(cs)

        if (ymlHelper.getBoolean( "random"))
            parsingInfo.levellingStrategy[StrategyType.RANDOM] = RandomLevellingStrategy()

        val csYDistance = YmlParsingHelper.objToCS(cs, "y-coordinate")
        if (csYDistance != null) {
            val ymlHelper2 = YmlParsingHelper(csYDistance)
            val yDistanceStrategy = YDistanceStrategy()

            yDistanceStrategy.shouldMerge = ymlHelper2.getBoolean("merge")
            yDistanceStrategy.startingYLevel = ymlHelper2.getInt2(
                 "start-height", yDistanceStrategy.startingYLevel
            )
            yDistanceStrategy.endingYLevel = ymlHelper2.getInt2(
                 "end-height", yDistanceStrategy.endingYLevel
            )
            yDistanceStrategy.yPeriod = ymlHelper2.getInt2(
                 "period", yDistanceStrategy.yPeriod
            )

            if (parsingInfo.levellingStrategy.containsKey(StrategyType.Y_COORDINATE))
                parsingInfo.levellingStrategy[StrategyType.Y_COORDINATE]!!.mergeRule(yDistanceStrategy)
            else
                parsingInfo.levellingStrategy[StrategyType.Y_COORDINATE] = yDistanceStrategy
        }

        val csSpawnDistance = YmlParsingHelper.objToCS(cs, "distance-from-origin")
        if (csSpawnDistance != null) {
            val ymlHelper2 = YmlParsingHelper(csSpawnDistance)
            val spawnDistanceStrategy = SpawnDistanceStrategy()

            spawnDistanceStrategy.shouldMerge = ymlHelper2.getBoolean("merge")
            spawnDistanceStrategy.ringedTiers = ymlHelper2.getFloat2(
                "ringed-tiers", spawnDistanceStrategy.ringedTiers
            )
            spawnDistanceStrategy.bufferDistance = ymlHelper2.getFloat2(
                "buffer-distance", spawnDistanceStrategy.bufferDistance
            )
            spawnDistanceStrategy.enableHeightMod = ymlHelper2.getBoolean2(
                "enable-height-modifier", spawnDistanceStrategy.enableHeightMod
            )
            spawnDistanceStrategy.transitionYheight = ymlHelper2.getFloat2(
                "transition-y-height", spawnDistanceStrategy.transitionYheight
            )
            spawnDistanceStrategy.lvlMultiplier = ymlHelper2.getFloat2(
                "level-multiplier", spawnDistanceStrategy.lvlMultiplier
            )
            spawnDistanceStrategy.yHeightPeriod = ymlHelper2.getFloat2(
                "y-height-period", spawnDistanceStrategy.yHeightPeriod
            )
            spawnDistanceStrategy.scaleDownward = ymlHelper2.getBoolean2(
                "scale-increase-downward", spawnDistanceStrategy.scaleDownward
            )

            parseOptionalSpawnCoordinate(csSpawnDistance, spawnDistanceStrategy)

            if (parsingInfo.levellingStrategy.containsKey(StrategyType.SPAWN_DISTANCE))
                parsingInfo.levellingStrategy[StrategyType.SPAWN_DISTANCE]!!.mergeRule(spawnDistanceStrategy)
            else
                parsingInfo.levellingStrategy[StrategyType.SPAWN_DISTANCE] = spawnDistanceStrategy
        }

        parseWeightedRandom(cs)

        for (key in ymlHelper.cs.getKeys(false)){
            if (!KeyValidation.strategies.contains(key))
                Log.war("Invalid strategy '$key', in rule: ${parsingInfo.ruleName}")
        }
    }

    private fun parseModifiers(cs: ConfigurationSection?){
        if (cs == null) return

        val ymlHelper = YmlParsingHelper(cs)
        parsingInfo.maxRandomVariance = ymlHelper.getInt2(
            "max-random-variance", parsingInfo.maxRandomVariance
        )
        parsePlayerLevellingOptions(YmlParsingHelper.objToCS(cs, "player-variable-mod"))

        for (key in cs.getKeys(false)) {
            if (key.startsWith("custom", ignoreCase = true))
                parseCustomStrategy(cs, key)
        }

        for (key in ymlHelper.cs.getKeys(false)){
            if (!KeyValidation.modifiers.contains(key) &&
                !key.startsWith("custom", ignoreCase = true))
                Log.war("Invalid modifier '$key', in rule: ${parsingInfo.ruleName}")
        }
    }

    private fun parseCustomStrategy(cs: ConfigurationSection, keyName: String){
        val underScore = keyName.indexOf("_")
        val customName = if ("custom".equals(keyName, ignoreCase = true)) {
            null
        } else if (underScore <= 0 || underScore == keyName.length - 1 ||
            "custom".equals(keyName, ignoreCase = true)){
            Log.war("Modifier '$keyName' must have a unique name specified")
            return
        } else
            keyName.substring(underScore + 1)

        val csCustom = YmlParsingHelper.objToCS(cs, keyName) ?: return
        val customStrategy = CustomStrategy(customName)
        customStrategy.formula = csCustom.getString("formula")

        if (!customStrategy.formula.isNullOrEmpty()) {
            parsingInfo.customStrategy[customStrategy.placeholderName] = customStrategy
            RulesManager.instance.allCustomStrategyPlaceholders.add(customStrategy.placeholderName)
        }
    }

    private fun parseWeightedRandom(cs: ConfigurationSection) {
        val useWeightedRandom = cs.getString("weighted-random")

        // weighted-random: true
        val isDisabled = "false".equals(useWeightedRandom, ignoreCase = true)
        if (isDisabled || "true".equals(useWeightedRandom, ignoreCase = true)) {
            val randomLevelling = RandomLevellingStrategy()
            if (isDisabled)
                randomLevelling.enabled = false
            else
                randomLevelling.autoGenerate = true

            parsingInfo.levellingStrategy[StrategyType.WEIGHTED_RANDOM] = randomLevelling

            return
        }

        val csRandom = YmlParsingHelper.objToCS(cs, "weighted-random") ?: return
        val randomMap = mutableMapOf<String, Int>()
        val randomLevelling = RandomLevellingStrategy()
        randomLevelling.shouldMerge = YmlParsingHelper.getBoolean(csRandom, "merge")

        for (range in csRandom.getKeys(false)) {
            if ("merge".equals(range, ignoreCase = true)) continue

            val value = csRandom.getInt(range)
            randomMap[range] = value
        }

        if (randomMap.isNotEmpty())
            randomLevelling.weightedRandomMap.putAll(randomMap)

        if (parsingInfo.levellingStrategy.containsKey(StrategyType.WEIGHTED_RANDOM)) {
            if (randomLevelling.shouldMerge)
                parsingInfo.levellingStrategy[StrategyType.WEIGHTED_RANDOM]!!.mergeRule(randomLevelling)
            else
                parsingInfo.levellingStrategy[StrategyType.WEIGHTED_RANDOM] = randomLevelling
        }
        else
            parsingInfo.levellingStrategy[StrategyType.WEIGHTED_RANDOM] = randomLevelling
    }

    private fun parseWorldTimeTicks(
        cs: ConfigurationSection?,
        existingList: CachedModalList<MinAndMax>?
    ): CachedModalList<MinAndMax>? {
        if (cs == null) return existingList

        val configName = "world-time-tick"
        val temp = buildCachedModalListOfString(cs, configName, null, parsingInfo.ruleName) ?: return existingList
        val result = CachedModalList<MinAndMax>()
        result.includeAll = temp.includeAll
        result.excludeAll = temp.excludeAll
        result.excludedList.addAll(parseMinMaxValue(temp.excludedList, configName))
        result.includedList.addAll(parseMinMaxValue(temp.includedList, configName))

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
                        "Invalid value for $configName: '${split[i]}' in rule ${parsingInfo.ruleName}"
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
        indicator.maintainSpace = ymlHelper.getBoolean2( "maintain-space", indicator.maintainSpace)
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
        val options = PlayerLevellingStrategy()
        options.matchVariable = ymlHelper.getBoolean2(
             "match-variable", options.matchVariable
        )
        options.usevariableAsMax = ymlHelper.getBoolean2(
             "use-variable-as-max", options.usevariableAsMax
        )
        options.playerVariableScale = ymlHelper.getFloat2(
             "player-variable-scale", options.playerVariableScale
        )
        options.outputCap = ymlHelper.getFloat2( "output-cap", options.outputCap)
        options.enabled = ymlHelper.getBoolean2( "enabled", options.enabled)
        options.shouldMerge = ymlHelper.getBoolean( "merge")
        options.variable = ymlHelper.getString( "player-variable", options.variable)
        options.decreaseOutput = ymlHelper.getBoolean( "decrease-output", true)
        options.recheckPlayers = ymlHelper.getBoolean2( "recheck-players", options.recheckPlayers)
        options.preserveEntityTime = ymlHelper.getIntTimeUnitMS( "preserve-entity", options.preserveEntityTime)
        parsingInfo.levellingStrategy[options.strategyType] = options

        val csTiers = YmlParsingHelper.objToCS(cs, "player-variable-tiers") ?: return
        val levelTiers = mutableListOf<LevelTierMatching>()
        var defaultTier: LevelTierMatching? = null

        for (key in csTiers.getKeys(true)) {
            var isDefault = false

            if ("default".equals(key, ignoreCase = true))
                isDefault = true

            val value = csTiers.getString(key)
            val info = LevelTierMatching()

            if (value == null) {
                Log.war("No value was specified for: $key")
                continue
            }

            if (!key.contains("-") && !isInteger(key)) {
                // found a source tier name rather than number
                info.sourceTierName = key
            } else if (!info.setRangeFromString(key)) {
                Log.war("Invalid number range: $key")
                continue
            }

            val levelRange = MinAndMax.setAmountRangeFromString(value)

            if (levelRange == null) {
                Log.war("Invalid number range: $value")
                continue
            }

            info.valueRanges = levelRange
            if (isDefault)
                defaultTier = info
            else
                levelTiers.add(info)
        }

        if (levelTiers.isNotEmpty()) {
            options.levelTiers.addAll(levelTiers)
        }
        options.defaultLevelTier = defaultTier
    }

    private fun parseOptionalSpawnCoordinate(
        cs: ConfigurationSection,
        sds: SpawnDistanceStrategy
    ) {
        val spawnLocation = YmlParsingHelper.objToCS(cs, "origin-coordinates") ?: return

        if (!"spawn".equals(spawnLocation.getString("x"), ignoreCase = true)) {
            sds.originCoordX = spawnLocation.getDouble("x").toFloat()
        }

        if (!"spawn".equals(spawnLocation.getString("z"), ignoreCase = true)) {
            sds.originCoordZ = spawnLocation.getDouble("z").toFloat()
        }
    }

    private fun parseFineTuning(
        csBase: ConfigurationSection?
    ) {
        if (csBase == null) return

        parsingInfo.vanillaBonuses = buildCachedModalOfType(
            csBase,
            parsingInfo.vanillaBonuses, ModalListParsingTypes.VANILLA_BONUSES,
            parsingInfo
        ) as CachedModalList<VanillaBonusEnum>?
        var fineTuning: FineTuningAttributes? = null
        val namesList = mutableListOf("base-attribute-modifier", "attribute-modifier")
        val namesList2 = mutableListOf("custom-base-attribute-modifier", "custom-attribute-modifier")

        for ((loopNum, useName) in namesList.withIndex()) {
            // first loop = base mods, second loop = modifiers
            val isBaseModifier = (loopNum == 0)
            val useName2 = namesList2[loopNum]
            if (fineTuning == null) fineTuning = FineTuningAttributes()

            val useDefaults = if (isBaseModifier)
                fineTuning.baseAttributeModifiers
            else
                fineTuning.multipliers

            val cs = YmlParsingHelper.objToCS(csBase, useName)
            if (cs == null) continue

            val parseResult = parseFineTuningValues(cs, useDefaults, isBaseModifier)
            if (isBaseModifier) {
                fineTuning.baseAttributeModifiers = parseResult.result
                fineTuning.doNotMergeAllBaseMods = parseResult.doNotMerge
            }
            else {
                fineTuning.multipliers = parseResult.result
                fineTuning.doNotMergeAllMultipliers = parseResult.doNotMerge
            }

            if (parseResult.doNotMergeAny) fineTuning.doNotMerge = true

            val mobSpecificMultipliers = mutableMapOf<String, MutableMap<Addition, Multiplier>>()
            val csCustom = YmlParsingHelper.objToCS(cs, useName2) ?: continue
            for (mobName in csCustom.getKeys(false)) {
                var checkName = mobName
                var doNotMerge: Boolean? = null
                if (checkName.lowercase(Locale.getDefault()).startsWith("baby_"))
                    checkName = checkName.substring(5)

                if ("merge".equals(mobName, ignoreCase = true))
                    doNotMerge = !csCustom.getBoolean(mobName)
                else if ("do-not-merge".equals(mobName, ignoreCase = true))
                    doNotMerge = csCustom.getBoolean(mobName)

                if (doNotMerge != null){
                    if (isBaseModifier)
                        fineTuning.doNotMergeMobSpecificBaseMods = doNotMerge
                    else
                        fineTuning.doNotMergeMobSpecificMultipliers = doNotMerge

                    continue
                }

                try {
                    EntityType.valueOf(checkName.uppercase(Locale.getDefault()))
                } catch (_: IllegalArgumentException) {
                    Log.war(
                        "Invalid entity type: $mobName for fine-tuning in rule: ${parsingInfo.ruleName}"
                    )
                    continue
                }

                val parseResult2 = parseFineTuningValues(
                    YmlParsingHelper.objToCS(csCustom, mobName),
                    null,
                    isBaseModifier
                )

                if (parseResult2.doNotMerge){
                    if (isBaseModifier)
                        fineTuning.doNotMergeMobSpecificBaseMods = true
                    else
                        fineTuning.doNotMergeMobSpecificMultipliers = true
                }

                if (parseResult2.result == null) continue

                mobSpecificMultipliers[mobName] = parseResult2.result
            }

            if (!mobSpecificMultipliers.isEmpty()) {
                if (isBaseModifier)
                    fineTuning.mobSpecificBaseModifiers = mobSpecificMultipliers
                else
                    fineTuning.mobSpecificMultipliers = mobSpecificMultipliers
            }
        }

        if (fineTuning != null && !fineTuning.isEmpty)
            parsingInfo.mobMultipliers = fineTuning
    }

    private fun parseFineTuningValues(
        cs: ConfigurationSection?,
        defaults: MutableMap<Addition, Multiplier>?,
        isBaseModifier: Boolean,
    ): FineTuningParseResult {
        if (cs == null) return FineTuningParseResult(false
            , doNotMergeAny = false
            , result = defaults
        )

        var useStacked: Boolean? = null
        val ymlHelper = YmlParsingHelper(cs)
        var doNotMerge = !ymlHelper.getBoolean( "merge", true) // for backwards compat
        val doNotMergeAny = ymlHelper.getBoolean( "do-not-merge-any", false)

        val results = mutableMapOf<Addition, Multiplier>()

        for (item in cs.getKeys(false)) {
            when (item.lowercase(Locale.getDefault())) {
                "merge" -> doNotMerge = !ymlHelper.getBoolean(item, true)
                "use-stacked" -> useStacked = ymlHelper.getBoolean2(item, useStacked)
                "do-not-merge" -> doNotMerge = ymlHelper.getBoolean(item, false)
                "vanilla-bonus", "custom-attribute-modifier", "custom-base-attribute-modifier", "do-not-merge-any" -> {}
                else -> {
                    var lmMultiplier: LMMultiplier
                    try {
                        lmMultiplier = LMMultiplier
                            .valueOf(
                                item.replace("-", "_")
                                    .uppercase(Locale.getDefault())
                            )
                    } catch (_: Exception) {
                        Log.war("Invalid multiplier: $item")
                        continue
                    }

                    val addition = FineTuningAttributes.getAdditionFromLMMultiplier(lmMultiplier)
                    val multiplier = parseFineTuningValues2(cs, addition, item, isBaseModifier)
                    if (multiplier != null) results[addition] = multiplier
                }
            }
        }

        return if (results.isEmpty())
            FineTuningParseResult(doNotMerge, doNotMergeAny, defaults)
        else
            FineTuningParseResult(doNotMerge, doNotMergeAny, results)
    }

    private fun parseFineTuningValues2(
        cs: ConfigurationSection,
        addition: Addition,
        item: String,
        isBaseModifier: Boolean
    ): Multiplier? {
        val values = cs.getList(item)
        if (values == null) {
            val formula = YmlParsingHelper.getString(cs, item)
            val value = YmlParsingHelper.getFloat2(cs, item, null)

            return if (value != null)
                Multiplier(addition, false, value, null, isAddition = false, isBaseModifier)
            else if (formula != null)
                Multiplier(addition, false, null, formula, isAddition = true, isBaseModifier)
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
                is Float -> {
                    value = obj
                    customFormula = null
                }
                is Double -> {
                    value = obj.toFloat()
                    customFormula = null
                }
                is Int -> {
                    value = obj.toFloat()
                    customFormula = null
                }
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
                    else if ("stacked".equals(obj, ignoreCase = true))
                        useStacked = true
                    else if (isDouble(obj))
                        value = obj.toFloat()


                    valueStr = obj
                    if (useStacked) customFormula = null
                }
            }

            count++
        }

        if (value > Float.MIN_VALUE || !customFormula.isNullOrEmpty()) {
            return Multiplier(
                addition, useStacked, value, customFormula, isAddition, false
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

            if (ruleInfo.levellingStrategy[StrategyType.WEIGHTED_RANDOM] != null) {
                if (rls == null)
                    rls = ruleInfo.levellingStrategy[StrategyType.WEIGHTED_RANDOM]!!.cloneItem() as RandomLevellingStrategy
                else
                    rls.mergeRule(ruleInfo.levellingStrategy[StrategyType.WEIGHTED_RANDOM])
            }

            if (ruleInfo.restrictionsMinLevel != null) minLevel = ruleInfo.restrictionsMinLevel!!
            if (ruleInfo.restrictionsMaxLevel != null) maxLevel = ruleInfo.restrictionsMaxLevel!!
        }

        if (rls == null || !rls.autoGenerate || rls.weightedRandomMap.isNotEmpty()) return
        // TODO: next line probably shouldn't have i-i
        for (i in minLevel..maxLevel) rls.weightedRandomMap["$i-$i"] = maxLevel - i + 1

        rls.populateWeightedRandom(minLevel, maxLevel)
    }
}