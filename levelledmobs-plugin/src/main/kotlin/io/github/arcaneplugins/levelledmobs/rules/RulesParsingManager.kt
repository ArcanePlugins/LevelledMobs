package io.github.arcaneplugins.levelledmobs.rules

import java.util.EnumMap
import java.util.LinkedList
import java.util.Locale
import java.util.TreeMap
import java.util.TreeSet
import java.util.regex.Pattern
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager.ExternalCompatibility
import io.github.arcaneplugins.levelledmobs.enums.Addition
import io.github.arcaneplugins.levelledmobs.enums.LevelledMobSpawnReason
import io.github.arcaneplugins.levelledmobs.enums.MobCustomNameStatus
import io.github.arcaneplugins.levelledmobs.enums.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.enums.ModalListParsingTypes
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.misc.CustomUniversalGroups
import io.github.arcaneplugins.levelledmobs.misc.YmlParsingHelper
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes.Multiplier
import io.github.arcaneplugins.levelledmobs.rules.strategies.RandomLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.SpawnDistanceStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.YDistanceStrategy
import io.github.arcaneplugins.levelledmobs.util.Utils
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
    private val ymlHelper = YmlParsingHelper()
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
            Utils.logger.info("rules config was null")
            return
        }

        val main = LevelledMobs.instance
        rulePresets.clear()
        main.rulesManager.rulesInEffect.clear()
        main.customMobGroups.clear()

        parseCustomMobGroups(objToCS(config, "mob-groups"))
        parseCustomBiomeGroups(objToCS(config, "biome-groups"))

        val presets = parsePresets(objToCS(config, "presets"))
        for (ri in presets) {
            rulePresets[ri.presetName!!] = ri
        }

        this.defaultRule = parseDefaults(objToCS(config, "default-rule"))
        main.rulesManager.rulesInEffect[Int.MIN_VALUE] = mutableListOf()
        main.rulesManager.rulesInEffect[Int.MIN_VALUE]!!.add(defaultRule!!)
        main.rulesManager.anyRuleHasChance = defaultRule!!.conditionsChance != null
        main.rulesManager.hasAnyWGCondition = (defaultRule!!.conditionsWGregions != null
                || defaultRule!!.conditionsWGregionOwners != null)

        main.rulesManager.buildBiomeGroupMappings(customBiomeGroups)
        this.customRules = parseCustomRules(
            config[ymlHelper.getKeyNameFromConfig(config, "custom-rules")]
        )

        checkCustomRules()
        autoGenerateWeightedRandom()
    }

    fun checkCustomRules() {
        val ruleMappings: MutableMap<String, RuleInfo> = TreeMap(java.lang.String.CASE_INSENSITIVE_ORDER)
        val main = LevelledMobs.instance

        for (ruleInfo in customRules) {
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
        Utils.logger.info("Current rules hash: " + main.rulesManager.currentRulesHash)
    }

    fun getAllRules(): List<RuleInfo> {
        return getAllRules(true)
    }

    fun getAllRules(includePresets: Boolean): List<RuleInfo> {
        val results = mutableListOf<RuleInfo>()
        if (this.defaultRule != null) {
            results.add(this.defaultRule!!)
        }
        if (includePresets) results.addAll(rulePresets.values)
        results.addAll(this.customRules)

        return results
    }

    private fun parseCustomMobGroups(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        for (groupName in cs.getKeys(false)) {
            val names = cs.getStringList(groupName)
            val groupMembers: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
            groupMembers.addAll(names)
            LevelledMobs.instance.customMobGroups[groupName] = groupMembers
        }
    }

    private fun parseCustomBiomeGroups(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }
        this.customBiomeGroups = mutableMapOf()

        for (groupName in cs.getKeys(false)) {
            val names = cs.getStringList(groupName)
            val groupMembers: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
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
            Utils.logger.info("default-rule section was null")
            return this.parsingInfo
        }

        parseValues(cs)
        return this.parsingInfo
    }

    private fun parsePresets(
        cs: ConfigurationSection?
    ): MutableList<RuleInfo> {
        val results = mutableListOf<RuleInfo>()
        if (cs == null) {
            return results
        }

        var count = -1
        for (key in cs.getKeys(false)) {
            count++
            val csKey = objToCS(cs, key)
            if (csKey == null) {
                Utils.logger.warning("nothing was specified for preset: $key")
                continue
            }

            this.parsingInfo = RuleInfo("preset $count")
            parsingInfo.presetName = key
            parseValues(csKey)
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
        if (cs == null) {
            return defaultValue
        }

        val cachedModalList = mlpi.cachedModalList
        val simpleStringOrArray = cs[ymlHelper.getKeyNameFromConfig(cs, mlpi.configurationKey!!)]
        var cs2: ConfigurationSection? = null
        var useList: List<String?>? = null

        if (simpleStringOrArray is ArrayList<*>) {
            useList = simpleStringOrArray as ArrayList<String>
        } else if (simpleStringOrArray is String) {
            useList = mutableListOf(simpleStringOrArray)
        }

        if (useList == null) {
            val useKeyName = ymlHelper.getKeyNameFromConfig(cs, mlpi.configurationKey!!)
            cs2 = objToCS(cs, useKeyName)
        }
        if (cs2 == null && useList == null) {
            return defaultValue
        }

        cachedModalList!!.doMerge = ymlHelper.getBoolean(cs2, "merge")

        if (cs2 != null && mlpi.supportsGroups) {
            cachedModalList.allowedGroups = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
            cachedModalList.excludedGroups = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)

            for (group in YmlParsingHelper.getListFromConfigItem(
                cs2,
                MLALLOWEDGROUPS
            )) {
                if (group.trim { it <= ' ' }.isEmpty()) {
                    continue
                }
                if (mlpi.groupMapping == null || !mlpi.groupMapping!!.containsKey(group)) {
                    Utils.logger.info(String.format("invalid %s group: %s", mlpi.itemName, group))
                } else {
                    cachedModalList.allowedGroups.add(group)
                }
            }

            for (group in YmlParsingHelper.getListFromConfigItem(
                cs2,
                MLEXCLUDEDGROUPS
            )) {
                if (group.trim { it <= ' ' }.isEmpty()) {
                    continue
                }
                if (!LevelledMobs.instance.rulesManager.biomeGroupMappings.containsKey(group)) {
                    Utils.logger.info(String.format("invalid %s group: %s", mlpi.itemName, group))
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
                if (item!!.trim { it <= ' ' }.isEmpty()) {
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
                            val biomeModalList = cachedModalList as CachedModalList<Biome>?
                            val modalList = if (i == 0) biomeModalList!!.allowedList else biomeModalList!!.excludedList

                            val biome = Biome.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                            modalList.add(biome)
                        }
                        ModalListParsingTypes.SPAWN_REASON -> {
                            val spawnReasonModalList = cachedModalList as CachedModalList<LevelledMobSpawnReason>?
                            val modalList =
                                if (i == 0) spawnReasonModalList!!.allowedList else spawnReasonModalList!!.excludedList

                            val spawnReason =
                                LevelledMobSpawnReason.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                            modalList.add(spawnReason)
                        }
                        ModalListParsingTypes.VANILLA_BONUSES -> {
                            val vanillaBonusModalList = cachedModalList as CachedModalList<VanillaBonusEnum>?
                            val modalList =
                                if (i == 0) vanillaBonusModalList!!.allowedList else vanillaBonusModalList!!.excludedList

                            val vanillaBonus =
                                VanillaBonusEnum.valueOf(item.trim { it <= ' ' }.uppercase(Locale.getDefault()))
                            modalList.add(vanillaBonus)
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    Utils.logger.warning(
                        String.format(
                            "Invalid %s %s: %s", invalidWord, mlpi.itemName, item
                        )
                    )
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
        if (cs == null) {
            return defaultValue
        }

        val cachedModalList = CachedModalList(
            TreeSet(String.CASE_INSENSITIVE_ORDER),
            TreeSet(String.CASE_INSENSITIVE_ORDER)
        )
        val useKeyName = ymlHelper.getKeyNameFromConfig(cs, name)
        val simpleStringOrArray = cs[useKeyName]
        var cs2: ConfigurationSection? = null
        var useList: MutableList<String>? = null

        if (simpleStringOrArray is java.util.ArrayList<*>) {
            useList = (simpleStringOrArray as java.util.ArrayList<String>).toMutableList()
        } else if (simpleStringOrArray is String) {
            useList = mutableListOf(simpleStringOrArray)
        }

        if (useList == null) {
            cs2 = objToCS(cs, useKeyName)
        }
        if (cs2 == null && useList == null) {
            return defaultValue
        }

        cachedModalList.doMerge = ymlHelper.getBoolean(cs2, "merge")

        if (cs2 != null) {
            val allowedList = ymlHelper.getKeyNameFromConfig(cs2, MLALLOWEDLIST)
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

        val allowedGroups = ymlHelper.getKeyNameFromConfig(cs2, MLALLOWEDGROUPS)
        val excludedList = ymlHelper.getKeyNameFromConfig(cs2, MLEXCLUDEDITEMS)
        val excludedGroups = ymlHelper.getKeyNameFromConfig(cs2, MLEXCLUDEDGROUPS)
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

        val results: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
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
                } catch (e: java.lang.IllegalArgumentException) {
                    invalidGroup = true
                }
            }
            if (LevelledMobs.instance.customMobGroups.containsKey(group)) {
                results.add(group)
            } else {
                invalidGroup = true
            }

            if (invalidGroup) {
                Utils.logger.warning("Invalid group: $group")
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
                Utils.logger.info("cs was null (parsing custom-rules)")
                continue
            }

            this.parsingInfo = RuleInfo("rule " + results.size)
            parseValues(cs)
            results.add(this.parsingInfo)
        }

        return results
    }

    private fun parseValues(cs: ConfigurationSection) {
        mergePreset(cs)

        parsingInfo.ruleIsEnabled = ymlHelper.getBoolean(cs, "enabled", true)
        //final String ruleName = cs.getString(ymlHelper.getKeyNameFromConfig(cs, "name"));
        val ruleName = ymlHelper.getString(cs, "name")
        if (ruleName != null) {
            parsingInfo.ruleName = ymlHelper.getString(cs, "name")!!
        }

        parseStrategies(objToCS(cs, "strategies"))
        parseConditions(objToCS(cs, "conditions"))
        parseApplySettings(objToCS(cs, "apply-settings"))

        parsingInfo.allowedEntities = buildCachedModalListOfString(
            cs, "allowed-entities",
            parsingInfo.allowedEntities
        )
    }

    private fun mergePreset(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        val usePresetName = ymlHelper.getKeyNameFromConfig(cs, "use-preset")
        val presets = cs.getStringList(usePresetName)
        if (presets.isEmpty() && cs.getString(usePresetName) != null)
            presets.addAll(cs.getString(usePresetName)!!.split(","))

        if (presets.isEmpty()) {
            return
        }

        for (checkName: String in presets) {
            val checkName = checkName.trim { it <= ' ' }
            if (!rulePresets.containsKey(checkName)) {
                Utils.logger.info(
                    parsingInfo.ruleName + ", specified preset name '" + checkName
                            + "' but none was found"
                )
                continue
            }

            parsingInfo.mergePresetRules(rulePresets[checkName])
        }
    }

    private fun parseExternalCompat(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        val results: MutableMap<ExternalCompatibility, Boolean> = EnumMap(
            ExternalCompatibility::class.java
        )

        for (key in cs.getKeys(false)) {
            val value = cs.getBoolean(key)

            val compat: ExternalCompatibility
            try {
                compat = ExternalCompatibility.valueOf(
                    key.uppercase(Locale.getDefault())
                )
                results[compat] = value
            } catch (e: java.lang.IllegalArgumentException) {
                Utils.logger.warning("Invalid level-plugins key: $key")
            }
        }

        if (results.isNotEmpty()) {
            parsingInfo.enabledExtCompats = results
        }
    }

    private fun parseTieredColoring(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

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
        if (cs == null) {
            return
        }

        val levelTiers: MutableMap<String, MutableList<LevelTierMatching>> = TreeMap(
            java.lang.String.CASE_INSENSITIVE_ORDER
        )
        val entityNames: MutableMap<String, LevelTierMatching> = TreeMap(
            java.lang.String.CASE_INSENSITIVE_ORDER
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
                val names2: MutableList<String> = LinkedList()

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
                        objToCS(cs, name),
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
        if (cs == null) {
            return null
        }

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
                Utils.logger.warning("Invalid number range: $keyName")
            } else if (tier.names!!.isNotEmpty()) {
                levelTiers.add(tier)
            }
        }

        return levelTiers
    }

    private fun parseApplySettings(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        parseFineTuning(objToCS(cs, "multipliers"))
        parseEntityNameOverride(objToCS(cs, "entity-name-override"))
        parseTieredColoring(objToCS(cs, "tiered-coloring"))
        parseHealthIndicator(objToCS(cs, "health-indicator"))

        parsingInfo.restrictionsMinLevel = ymlHelper.getInt2(
            cs, "minlevel",
            parsingInfo.restrictionsMinLevel
        )
        parsingInfo.restrictionsMaxLevel = ymlHelper.getInt2(
            cs, "maxlevel",
            parsingInfo.restrictionsMaxLevel
        )

        parsingInfo.conditionsNoDropEntities = buildCachedModalListOfString(
            cs,
            "no-drop-multipler-entities", parsingInfo.conditionsNoDropEntities
        )
        parsingInfo.babyMobsInheritAdultSetting = ymlHelper.getBoolean2(
            cs,
            "baby-mobs-inherit-adult-setting", parsingInfo.babyMobsInheritAdultSetting
        )
        parsingInfo.mobLevelInheritance = ymlHelper.getBoolean2(
            cs, "level-inheritance",
            parsingInfo.mobLevelInheritance
        )
        parsingInfo.creeperMaxDamageRadius = ymlHelper.getInt2(
            cs, "creeper-max-damage-radius",
            parsingInfo.creeperMaxDamageRadius
        )
        parsingInfo.customDropsUseForMobs = ymlHelper.getBoolean2(
            cs,
            "use-custom-item-drops-for-mobs", parsingInfo.customDropsUseForMobs
        )
        parseChunkKillOptions(cs)
        parsingInfo.customDropDropTableIds.addAll(
            YmlParsingHelper.getListFromConfigItem(cs, "use-droptable-id")
        )
        parsingInfo.nametag = ymlHelper.getString(cs, "nametag", parsingInfo.nametag)
        parsingInfo.nametagCreatureDeath = ymlHelper.getString(
            cs, "creature-death-nametag",
            parsingInfo.nametagCreatureDeath
        )
        parsingInfo.nametagPlaceholderLevelled = ymlHelper.getString(
            cs,
            "nametag-placeholder-levelled", parsingInfo.nametagPlaceholderLevelled
        )
        parsingInfo.nametagPlaceholderUnlevelled = ymlHelper.getString(
            cs,
            "nametag-placeholder-unlevelled", parsingInfo.nametagPlaceholderUnlevelled
        )
        parsingInfo.sunlightBurnAmount = ymlHelper.getDouble2(
            cs, "sunlight-intensity",
            parsingInfo.sunlightBurnAmount
        )
        parsingInfo.lockEntity = ymlHelper.getBoolean2(cs, "lock-entity", parsingInfo.lockEntity)
        parseNBTData(cs)
        parsingInfo.passengerMatchLevel = ymlHelper.getBoolean2(
            cs, "passenger-match-level",
            parsingInfo.passengerMatchLevel
        )
        parsingInfo.nametagVisibleTime = ymlHelper.getIntTimeUnitMS(
            cs, "nametag-visible-time",
            parsingInfo.nametagVisibleTime
        )
        parsingInfo.maximumDeathInChunkThreshold = ymlHelper.getInt2(
            cs,
            "maximum-death-in-chunk-threshold", parsingInfo.maximumDeathInChunkThreshold
        )
        parsingInfo.chunkMaxCoolDownTime = ymlHelper.getIntTimeUnit(
            cs,
            "chunk-max-cooldown-seconds", parsingInfo.chunkMaxCoolDownTime
        )
        parsingInfo.spawnerParticlesCount = ymlHelper.getInt2(
            cs, "spawner-particles-count",
            parsingInfo.spawnerParticlesCount
        )
        parsingInfo.maxAdjacentChunks = ymlHelper.getInt2(
            cs, "max-adjacent-chunks",
            parsingInfo.maxAdjacentChunks
        )
        if (parsingInfo.maxAdjacentChunks != null && parsingInfo.maxAdjacentChunks!! > 10) {
            parsingInfo.maxAdjacentChunks = 10
        }
        parseSpawnerParticle(ymlHelper.getString(cs, "spawner-particles"))
        parseDeathMessages(cs)

        val nametagVisibility = ymlHelper.getStringSet(
            cs,
            "nametag-visibility-method"
        )
        val nametagVisibilityEnums: MutableList<NametagVisibilityEnum> = LinkedList()
        for (nametagVisEnum: String in nametagVisibility) {
            try {
                val nametagVisibilityEnum = NametagVisibilityEnum.valueOf(
                    nametagVisEnum.uppercase(Locale.getDefault())
                )
                nametagVisibilityEnums.add(nametagVisibilityEnum)
            } catch (ignored: Exception) {
                Utils.logger.warning(
                    "Invalid value in nametag-visibility-method: " + nametagVisibility
                            + ", in rule: " + parsingInfo.ruleName
                )
            }
        }

        if (nametagVisibilityEnums.isNotEmpty()) {
            parsingInfo.nametagVisibilityEnum = nametagVisibilityEnums
        } else if ((cs[ymlHelper.getKeyNameFromConfig(cs, "creature-nametag-always-visible")]
                    != null)
        ) {
            val nametagVisibilityBackwardsComat = ymlHelper.getBoolean2(
                cs,
                "creature-nametag-always-visible", null
            )
            if (nametagVisibilityBackwardsComat != null) {
                if (nametagVisibilityBackwardsComat) {
                    parsingInfo.nametagVisibilityEnum = mutableListOf(NametagVisibilityEnum.ALWAYS_ON)
                } else {
                    parsingInfo.nametagVisibilityEnum = mutableListOf(NametagVisibilityEnum.MELEE)
                }
            } else {
                parsingInfo.nametagVisibilityEnum = mutableListOf(NametagVisibilityEnum.MELEE)
            }
        }
    }

    private fun parseChunkKillOptions(cs: ConfigurationSection) {
        val opts = ChunkKillOptions()

        opts.disableVanillaDrops = ymlHelper.getBoolean2(cs, "disable-vanilla-drops-on-chunk-max", null)
        opts.disableItemBoost = ymlHelper.getBoolean2(cs, "disable-item-boost-on-chunk-max", null)
        opts.disableXpDrops = ymlHelper.getBoolean2(cs, "disable-xp-boost-on-chunk-max", null)

        if (!opts.isDefault) {
            parsingInfo.chunkKillOptions = opts
        }
    }

    private fun parseDeathMessages(csParent: ConfigurationSection) {
        val cs: ConfigurationSection = objToCS(csParent, "death-messages") ?: return

        val deathMessages = DeathMessages()

        for (weightStr in cs.getKeys(false)) {
            val messages = cs.getStringList(weightStr)
            if (messages.isEmpty()) {
                val temp = cs.getString(weightStr)
                if (!temp.isNullOrEmpty()) messages.add(temp)
            }

            for (message in messages) {
                if (!isInteger(weightStr)) {
                    Utils.logger.warning("Invalid number in DeathMessages section: $weightStr")
                    continue
                }

                var weight = weightStr.toInt()
                if (weight > 100) {
                    Utils.logger.warning(
                        String.format(
                            "value of %s is over the limit of 100 for death message weight",
                            weight
                        )
                    )
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
        if (particle == null) {
            return
        }

        if ("none".equals(particle, ignoreCase = true)) {
            parsingInfo.spawnerParticle = null
            parsingInfo.useNoSpawnerParticles = true
            return
        }

        try {
            parsingInfo.spawnerParticle = Particle.valueOf(particle.uppercase(Locale.getDefault()))
        } catch (ignored: java.lang.Exception) {
            Utils.logger.warning(
                "Invalid value in spawner-particles: " + particle + ", in rule: "
                        + parsingInfo.ruleName
            )
        }
    }

    private fun parseNBTData(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        val keyName = ymlHelper.getKeyNameFromConfig(cs, "nbt-data")
        val temp = cs[keyName] ?: return

        if (temp is MemorySection || temp is java.util.LinkedHashMap<*, *>) {
            val cs2 = ymlHelper.objToCS(cs, keyName) ?: return

            val nbt = ymlHelper.getString(cs2, "data", null)
            val nbtList = ymlHelper.getStringSet(cs2, "data")
            if (nbt == null && nbtList.isEmpty()) {
                return
            }
            val doMerge = ymlHelper.getBoolean(cs2, "merge", false)

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
        if (cs == null) {
            return
        }

        parsingInfo.conditionsWorlds = buildCachedModalListOfString(
            cs, "worlds",
            parsingInfo.conditionsWorlds
        )

        parseExternalCompat(objToCS(cs, "level-plugins"))

        parsingInfo.conditionsMinLevel = ymlHelper.getInt2(
            cs, "minlevel",
            parsingInfo.conditionsMinLevel
        )
        parsingInfo.conditionsMaxLevel = ymlHelper.getInt2(
            cs, "maxlevel",
            parsingInfo.conditionsMaxLevel
        )

        parsingInfo.stopProcessingRules = ymlHelper.getBoolean2(
            cs, "stop-processing",
            parsingInfo.stopProcessingRules
        )
        parsingInfo.conditionsChance = ymlHelper.getFloat2(
            cs, "chance",
            parsingInfo.conditionsChance
        )

        val mobCustomNameStatus = ymlHelper.getString(cs, "mob-customname-status")
        if (mobCustomNameStatus != null) {
            try {
                parsingInfo.conditionsMobCustomnameStatus = MobCustomNameStatus.valueOf(
                    mobCustomNameStatus.uppercase(Locale.getDefault())
                )
            } catch (e: java.lang.Exception) {
                Utils.logger.warning("Invalid value for $mobCustomNameStatus")
            }
        }

        val mobTamedStatus = ymlHelper.getString(cs, "mob-tamed-status")
        if (mobTamedStatus != null) {
            try {
                parsingInfo.conditionsMobTamedStatus = MobTamedStatus.valueOf(
                    mobTamedStatus.uppercase(Locale.getDefault())
                )
            } catch (e: java.lang.Exception) {
                Utils.logger.warning("Invalid value for $mobTamedStatus")
            }
        }

        parsingInfo.conditionsApplyAboveY = ymlHelper.getInt2(
            cs, "apply-above-y",
            parsingInfo.conditionsApplyAboveY
        )
        parsingInfo.conditionsApplyBelowY = ymlHelper.getInt2(
            cs, "apply-below-y",
            parsingInfo.conditionsApplyBelowY
        )
        parsingInfo.conditionsMinDistanceFromSpawn = ymlHelper.getInt2(
            cs,
            "min-distance-from-spawn", parsingInfo.conditionsMinDistanceFromSpawn
        )
        parsingInfo.conditionsMaxDistanceFromSpawn = ymlHelper.getInt2(
            cs,
            "max-distance-from-spawn", parsingInfo.conditionsMaxDistanceFromSpawn
        )
        parsingInfo.conditionsCooldownTime = ymlHelper.getIntTimeUnitMS(
            cs, "cooldown-duration",
            parsingInfo.conditionsCooldownTime
        )
        parsingInfo.conditionsTimesToCooldownActivation = ymlHelper.getInt2(
            cs, "cooldown-limit",
            parsingInfo.conditionsTimesToCooldownActivation
        )
        parseWithinCoordinates(objToCS(cs, "within-coordinates"))

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
        parsingInfo.conditionsApplyPlugins = buildCachedModalListOfString(
            cs, "apply-plugins",
            parsingInfo.conditionsApplyPlugins
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
            ymlHelper.getString(cs, "skylight-level"), "skylight-level"
        )
    }

    private fun parseWithinCoordinates(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        val mdr = WithinCoordinates()

        for (axis in mutableListOf(
            WithinCoordinates.Axis.X,
            WithinCoordinates.Axis.Y,
            WithinCoordinates.Axis.Z
        )) {
            for (keyStart in listOf("start-", "end-")) {
                val key = keyStart + axis.name.lowercase(Locale.getDefault())
                val isStart = "start-" == keyStart
                val value = ymlHelper.getString(cs, key)

                if (!mdr.parseAxis(value, axis, isStart)) {
                    Utils.logger.warning(
                        java.lang.String.format(
                            "rule: %s, invalid value for %s: %s",
                            parsingInfo.ruleName, key, value
                        )
                    )
                }
            }
        }

        if (mdr.isEmpty) {
            return
        }

        parsingInfo.conditionsWithinCoords = mdr
    }

    private fun parseStrategies(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        parsingInfo.maxRandomVariance = ymlHelper.getInt2(
            cs, "max-random-variance",
            parsingInfo.maxRandomVariance
        )
        if (ymlHelper.getBoolean(cs, "random")) parsingInfo.levellingStrategy = RandomLevellingStrategy()

        val csYDistance = objToCS(cs, "y-coordinate")
        if (csYDistance != null) {
            val yDistanceStrategy =
                if (parsingInfo.levellingStrategy is YDistanceStrategy) parsingInfo.levellingStrategy as YDistanceStrategy? else YDistanceStrategy()

            yDistanceStrategy!!.startingYLevel = ymlHelper.getInt2(
                csYDistance, "start",
                yDistanceStrategy.startingYLevel
            )
            yDistanceStrategy.endingYLevel = ymlHelper.getInt2(
                csYDistance, "end",
                yDistanceStrategy.endingYLevel
            )
            yDistanceStrategy.yPeriod = ymlHelper.getInt2(
                csYDistance, "period",
                yDistanceStrategy.yPeriod
            )

            if (parsingInfo.levellingStrategy != null
                && parsingInfo.levellingStrategy is YDistanceStrategy
            ) {
                parsingInfo.levellingStrategy!!.mergeRule(yDistanceStrategy)
            } else {
                parsingInfo.levellingStrategy = yDistanceStrategy
            }
        }

        val csSpawnDistance = objToCS(cs, "distance-from-spawn")
        if (csSpawnDistance != null) {
            val spawnDistanceStrategy =
                if (parsingInfo.levellingStrategy is SpawnDistanceStrategy) parsingInfo.levellingStrategy as SpawnDistanceStrategy?
                else SpawnDistanceStrategy()

            spawnDistanceStrategy!!.increaseLevelDistance = ymlHelper.getInt2(
                csSpawnDistance,
                "increase-level-distance", spawnDistanceStrategy.increaseLevelDistance
            )
            spawnDistanceStrategy.startDistance = ymlHelper.getInt2(
                csSpawnDistance,
                "start-distance", spawnDistanceStrategy.startDistance
            )

            parseOptionalSpawnCoordinate(csSpawnDistance, spawnDistanceStrategy)

            if (ymlHelper.getString(csSpawnDistance, "blended-levelling") != null) {
                parseBlendedLevelling(
                    objToCS(csSpawnDistance, "blended-levelling"),
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
        parsePlayerLevellingOptions(objToCS(cs, "player-levelling"))
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

        val cs_Random: ConfigurationSection = objToCS(cs, "weighted-random") ?: return

        val randomMap: MutableMap<String, Int> = TreeMap()
        val randomLevelling = RandomLevellingStrategy()
        randomLevelling.doMerge = ymlHelper.getBoolean(cs_Random, "merge")

        for (range in cs_Random.getKeys(false)) {
            if ("merge".equals(range, ignoreCase = true)) {
                continue
            }
            val value = cs_Random.getInt(range)
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
        if (cs == null) {
            return existingList
        }

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
        configName: String
    ): MinAndMax? {
        if (numberPair == null) {
            return null
        }

        val result = parseMinMaxValue(mutableSetOf(numberPair), configName)

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
            val split = numberPair.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val minAndMax = MinAndMax()
            var hadInvalidValue = false
            for (i in 0..1) {
                if (!isInteger(split[i])) {
                    Utils.logger.info(
                        java.lang.String.format(
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
        if (cs == null) {
            return
        }

        val indicator = HealthIndicator()
        indicator.indicator = ymlHelper.getString(cs, "indicator", indicator.indicator)
        indicator.indicatorHalf = ymlHelper.getString(
            cs, "indicator-half",
            indicator.indicatorHalf
        )
        indicator.maxIndicators = ymlHelper.getInt2(cs, "max", indicator.maxIndicators)
        indicator.scale = ymlHelper.getDouble2(cs, "scale", indicator.scale)
        indicator.merge = ymlHelper.getBoolean2(cs, "merge", indicator.merge)

        val cs_Tiers = objToCS(cs, "colored-tiers")
        if (cs_Tiers != null) {
            val tiers = mutableMapOf<Int, String>()

            for (name in cs_Tiers.getKeys(false)) {
                val name2 = name.lowercase(Locale.getDefault()).replace("tier-", "")

                if ("default".equals(name, ignoreCase = true)) {
                    if (cs_Tiers.getString(name).isNullOrEmpty()) {
                        Utils.logger.warning("No value entered for colored tier: $name")
                    } else {
                        tiers[0] = cs_Tiers.getString(name)!!
                    }

                    continue
                }

                if (!isInteger(name2)) {
                    Utils.logger.warning("Not a valid colored tier, missing number: $name")
                    continue
                }

                val tierValue = cs_Tiers.getString(name)
                if (tierValue.isNullOrEmpty()) {
                    Utils.logger.warning("No value entered for colored tier: $name")
                    continue
                }

                val tierNumber = name2.toInt()
                if (tiers.containsKey(tierNumber)) {
                    Utils.logger.warning("Duplicate tier: $name")
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
        if (cs == null) {
            return
        }

        val options = PlayerLevellingOptions()
        options.matchPlayerLevel = ymlHelper.getBoolean2(
            cs, "match-level",
            options.matchPlayerLevel
        )
        options.usePlayerMaxLevel = ymlHelper.getBoolean2(
            cs, "use-player-max-level",
            options.usePlayerMaxLevel
        )
        options.playerLevelScale = ymlHelper.getDouble2(
            cs, "player-level-scale",
            options.playerLevelScale
        )
        options.levelCap = ymlHelper.getInt2(cs, "level-cap", options.levelCap)
        options.enabled = ymlHelper.getBoolean2(cs, "enabled", options.enabled)
        options.doMerge = ymlHelper.getBoolean(cs, "merge", options.doMerge)
        options.variable = ymlHelper.getString(cs, "variable", options.variable)
        options.decreaseLevel = ymlHelper.getBoolean(cs, "decrease-level", true)
        options.recheckPlayers = ymlHelper.getBoolean2(cs, "recheck-players", options.recheckPlayers)
        options.preserveEntityTime = ymlHelper.getIntTimeUnitMS(cs, "preserve-entity", options.preserveEntityTime)
        parsingInfo.playerLevellingOptions = options

        val csTiers: ConfigurationSection = objToCS(cs, "tiers") ?: return

        val levelTiers: MutableList<LevelTierMatching> = LinkedList()

        for (name in csTiers.getKeys(false)) {
            val info = LevelTierMatching()

            val value = csTiers.getString(name)
            if (value == null) {
                Utils.logger.warning("No value was specified for: $name")
                continue
            }

            if (!name.contains("-") && !isInteger(name)) {
                // found a source tier name rather than number
                info.sourceTierName = name
            } else if (!info.setRangeFromString(name)) {
                Utils.logger.warning("Invalid number range: $name")
                continue
            }

            val levelRange = LevelTierMatching.getRangeFromString(value)
            if (levelRange.size < 2) {
                Utils.logger.warning("Invalid number range (len): $value")
                continue
            }
            if (levelRange[0] == -1 && levelRange[1] == -1) {
                Utils.logger.warning("Invalid number range: $value")
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
        if (cs == null) {
            return
        }

        spawnDistanceStrategy.blendedLevellingEnabled = ymlHelper.getBoolean2(
            cs, "enabled",
            spawnDistanceStrategy.blendedLevellingEnabled
        )
        spawnDistanceStrategy.transitionYheight = ymlHelper.getInt2(
            cs, "transition-y-height",
            spawnDistanceStrategy.transitionYheight
        )
        spawnDistanceStrategy.lvlMultiplier = ymlHelper.getDouble2(
            cs, "lvl-multiplier",
            spawnDistanceStrategy.lvlMultiplier
        )
        spawnDistanceStrategy.multiplierPeriod = ymlHelper.getInt2(
            cs, "multiplier-period",
            spawnDistanceStrategy.multiplierPeriod
        )
        spawnDistanceStrategy.scaleDownward = ymlHelper.getBoolean2(
            cs, "scale-downward",
            spawnDistanceStrategy.scaleDownward
        )
    }

    private fun parseOptionalSpawnCoordinate(
        cs: ConfigurationSection,
        sds: SpawnDistanceStrategy
    ) {
        val spawnLocation: ConfigurationSection = objToCS2(cs["spawn-location"]) ?: return

        if (!"default".equals(spawnLocation.getString("x"), ignoreCase = true)) {
            sds.spawnLocationX = spawnLocation.getInt("x")
        }

        if (!"default".equals(spawnLocation.getString("z"), ignoreCase = true)) {
            sds.spawnLocationZ = spawnLocation.getInt("z")
        }
    }

    private fun parseFineTuning(cs: ConfigurationSection?) {
        if (cs == null) {
            return
        }

        parsingInfo.vanillaBonuses = buildCachedModalOfType(
            cs,
            parsingInfo.vanillaBonuses, ModalListParsingTypes.VANILLA_BONUSES
        ) as CachedModalList<VanillaBonusEnum>?
        parsingInfo.allMobMultipliers = parseFineTuningValues(cs, parsingInfo.allMobMultipliers)

        val cs_Custom: ConfigurationSection = objToCS(cs, "custom-mob-level") ?: return

        val fineTuning: MutableMap<String, FineTuningAttributes> = TreeMap(
            java.lang.String.CASE_INSENSITIVE_ORDER
        )

        for (mobName in cs_Custom.getKeys(false)) {
            var checkName = mobName
            if (checkName.lowercase(Locale.getDefault()).startsWith("baby_")) {
                checkName = checkName.substring(5)
            }

            try {
                EntityType.valueOf(checkName.uppercase(Locale.getDefault()))
            } catch (e: java.lang.IllegalArgumentException) {
                Utils.logger.warning(
                    "Invalid entity type: " + mobName + " for fine-tuning in rule: "
                            + parsingInfo.ruleName
                )
                continue
            }

            val attribs = parseFineTuningValues(
                objToCS(cs_Custom, mobName), null
            )
            if (attribs == null) {
                continue
            }

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
        if (cs == null) {
            return defaults
        }

        val doMerge = ymlHelper.getBoolean(cs, "merge", true)

        val attribs =
            if (parsingInfo.allMobMultipliers != null && doMerge) parsingInfo.allMobMultipliers else FineTuningAttributes()

        for (item in cs.getKeys(false)) {
            when (item.lowercase(Locale.getDefault())) {
                "use-stacked" -> attribs!!.useStacked = ymlHelper.getBoolean2(cs, item, attribs.useStacked)
                "do-not-merge" -> attribs!!.doNotMerge = ymlHelper.getBoolean(cs, item, false)
                "merge" -> attribs!!.doNotMerge = !ymlHelper.getBoolean(cs, item, true)
                "vanilla-bonus", "custom-mob-level" -> {}
                else -> {
                    var lmMultiplier: LMMultiplier
                    try {
                        lmMultiplier = LMMultiplier
                            .valueOf(
                                item.replace("-", "_")
                                    .uppercase(Locale.getDefault())
                            )
                    } catch (ignored: java.lang.Exception) {
                        Utils.logger.warning("Invalid multiplier: $item")
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

        if (attribs!!.isEmpty) {
            return defaults
        }

        return attribs
    }

    private fun parseFineTuningValues2(
        cs: ConfigurationSection,
        addition: Addition,
        item: String
    ): Multiplier? {
        val values = cs.getList(item)
        if (values == null) {
            val value = ymlHelper.getFloat2(cs, item, null)
            return if (value != null) Multiplier(addition, false, value) else null
        }

        var value = Float.MIN_VALUE
        var useStacked = false
        var count = 0
        for (obj in values) {
            if (count > 2) break

            if (obj is Float) {
                value = obj
            } else if (obj is Double) {
                value = obj.toFloat()
            } else if (obj is Int) {
                value = obj.toFloat()
            } else if (obj is String) {
                if ("stacked".equals(obj, ignoreCase = true)) {
                    useStacked = true
                } else if (isDouble(obj)) {
                    value = obj.toFloat()
                }
            }

            count++
        }

        if (value > Float.MIN_VALUE) {
            return Multiplier(
                addition, useStacked, value
            )
        }

        return null
    }

    private fun autoGenerateWeightedRandom() {
        var rls: RandomLevellingStrategy? = null
        var minLevel = 1
        var maxLevel = 1

        for (rules in LevelledMobs.instance.rulesManager.rulesInEffect.values) {
            for (ruleInfo in rules) {
                if ("defaults" != ruleInfo.ruleName) continue

                if (ruleInfo.levellingStrategy is RandomLevellingStrategy) {
                    if (rls == null) rls = ruleInfo.levellingStrategy as RandomLevellingStrategy
                    else rls.mergeRule(rls)
                }

                if (ruleInfo.restrictionsMinLevel != null) minLevel = ruleInfo.restrictionsMinLevel!!
                if (ruleInfo.restrictionsMaxLevel != null) maxLevel = ruleInfo.restrictionsMaxLevel!!
            }
        }

        if (rls == null || !rls.autoGenerate || rls.weightedRandom.isNotEmpty()) return
        for (i in minLevel..maxLevel) rls.weightedRandom[String.format("%s-%s", i, i)] = maxLevel - i + 1

        rls.populateWeightedRandom(minLevel, maxLevel)
    }

    private fun objToCS(cs: ConfigurationSection?, path: String): ConfigurationSection? {
        if (cs == null) {
            return null
        }
        val useKey = ymlHelper.getKeyNameFromConfig(cs, path)
        val obj = cs.get(useKey) ?: return null

        when (obj) {
            is ConfigurationSection -> {
                return obj
            }

            is Map<*, *> -> {
                val result = MemoryConfiguration()

                // this is to convert any non-string objects
                val temp = obj as Map<Any, Any>
                val temp2: MutableMap<String, Any?> = HashMap(temp.size)
                for (obj2: Any in temp.keys) {
                    temp2[obj2.toString()] = temp[obj2]
                }

                result.addDefaults(temp2)
                return result.defaultSection
            }

            else -> {
                val currentPath = if (cs.currentPath.isNullOrEmpty()) path else cs.currentPath + "." + path
                Utils.logger.warning(
                    "$currentPath: couldn't parse config of type: " + obj.javaClass
                        .simpleName + ", value: " + obj
                )
                return null
            }
        }
    }

    private fun objToCS2(
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
                result.addDefaults((obj as Map<String?, Any>))
                return result.defaultSection
            }

            else -> {
                Utils.logger.warning(
                    "couldn't parse config of type: " + obj.javaClass.simpleName + ", value: "
                            + obj
                )
                return null
            }
        }
    }
}