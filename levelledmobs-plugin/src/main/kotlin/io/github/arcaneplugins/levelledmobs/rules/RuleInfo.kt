package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.annotations.DoNotMerge
import io.github.arcaneplugins.levelledmobs.annotations.DoNotShow
import io.github.arcaneplugins.levelledmobs.annotations.ExcludeFromHash
import io.github.arcaneplugins.levelledmobs.annotations.RuleFieldInfo
import io.github.arcaneplugins.levelledmobs.enums.LevelledMobSpawnReason
import io.github.arcaneplugins.levelledmobs.enums.MobCustomNameStatus
import io.github.arcaneplugins.levelledmobs.enums.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.enums.RuleType
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.enums.ExternalCompatibility
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.rules.strategies.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.StrategyType
import org.bukkit.Particle
import org.bukkit.block.Biome
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/**
 * Holds rules parsed from rules.yml to make up a list of rules
 *
 * @author stumper66
 * @since 3.0.0
 */
@Suppress("UNCHECKED_CAST")
class RuleInfo(
    @DoNotMerge @ExcludeFromHash @DoNotShow
    internal var ruleName: String = "Unnamed"
) {
    @DoNotMerge @DoNotShow
    var ruleIsEnabled = true
    @RuleFieldInfo("construct level", RuleType.APPLY_SETTING)
    var constructLevel: String? = null
    @DoNotMerge @ExcludeFromHash @RuleFieldInfo("is temp disabled", RuleType.MISC)
    var isTempDisabled = false
    @ExcludeFromHash @RuleFieldInfo("use no spawner particles", RuleType.APPLY_SETTING)
    var useNoSpawnerParticles = false
    @RuleFieldInfo("baby mobs inherit adult settings", RuleType.APPLY_SETTING)
    var babyMobsInheritAdultSetting: Boolean? = null
    @RuleFieldInfo("mob level inheritance", RuleType.APPLY_SETTING)
    var mobLevelInheritance: Boolean? = null
    @ExcludeFromHash @RuleFieldInfo("use custom drops", RuleType.APPLY_SETTING)
    var customDropsUseForMobs: Boolean? = null
    @RuleFieldInfo("stop proessing rules", RuleType.APPLY_SETTING)
    var stopProcessingRules: Boolean? = null
    @ExcludeFromHash @DoNotShow
    var mergeEntityNameOverrides: Boolean? = null
    @RuleFieldInfo("passenger match level", RuleType.APPLY_SETTING)
    var passengerMatchLevel: Boolean? = null
    @ExcludeFromHash @RuleFieldInfo("lock entity", RuleType.APPLY_SETTING)
    var lockEntity: Boolean? = null
    @ExcludeFromHash @RuleFieldInfo("spawner particles count", RuleType.APPLY_SETTING)
    var spawnerParticlesCount: Int? = null
    @RuleFieldInfo("max random variance", RuleType.STRATEGY)
    var maxRandomVariance: Int? = null
    @ExcludeFromHash @RuleFieldInfo("creeper max damage radius", RuleType.APPLY_SETTING)
    var creeperMaxDamageRadius: Int? = null
    @RuleFieldInfo("minlevel", RuleType.CONDITION)
    var conditionsMinLevel: Int? = null
    @RuleFieldInfo("maxlevel", RuleType.CONDITION)
    var conditionsMaxLevel: Int? = null
    @RuleFieldInfo("minlevel", RuleType.APPLY_SETTING)
    var restrictionsMinLevel: Int? = null
    @RuleFieldInfo("maxlevel", RuleType.APPLY_SETTING)
    var restrictionsMaxLevel: Int? = null
    @RuleFieldInfo("apply above Y", RuleType.CONDITION)
    var conditionsApplyAboveY: Int? = null
    @RuleFieldInfo("apply below Y", RuleType.CONDITION)
    var conditionsApplyBelowY: Int? = null
    @RuleFieldInfo("min distance from spawn", RuleType.CONDITION)
    var conditionsMinDistanceFromSpawn: Int? = null
    @RuleFieldInfo("max distance from spawn", RuleType.CONDITION)
    var conditionsMaxDistanceFromSpawn: Int? = null
    @ExcludeFromHash @RuleFieldInfo("nametag visible time", RuleType.APPLY_SETTING)
    var nametagVisibleTime: Long? = null
    @ExcludeFromHash @RuleFieldInfo("max death in chunk threshold", RuleType.APPLY_SETTING)
    var maximumDeathInChunkThreshold: Int? = null
    @ExcludeFromHash @RuleFieldInfo("chunk max cooldown time", RuleType.APPLY_SETTING)
    var chunkMaxCoolDownTime: Int? = null
    @ExcludeFromHash @RuleFieldInfo("max adjacent chunks", RuleType.APPLY_SETTING)
    var maxAdjacentChunks: Int? = null
    @ExcludeFromHash @RuleFieldInfo("cooldown time", RuleType.APPLY_SETTING)
    var conditionsCooldownTime: Long? = null
    @ExcludeFromHash @RuleFieldInfo("times to cooldown activation", RuleType.CONDITION)
    var conditionsTimesToCooldownActivation: Int? = null
    @ExcludeFromHash @RuleFieldInfo("rule chance", RuleType.CONDITION)
    var conditionsChance: Float? = null
    @ExcludeFromHash @RuleFieldInfo("sunlight burn amount", RuleType.APPLY_SETTING)
    var sunlightBurnAmount: Double? = null
    @ExcludeFromHash @RuleFieldInfo("nametag", RuleType.APPLY_SETTING)
    var nametag: String? = null
    @ExcludeFromHash @RuleFieldInfo("creature death nametag", RuleType.APPLY_SETTING)
    var nametagCreatureDeath: String? = null
    @ExcludeFromHash @RuleFieldInfo("nametag placeholder levelled", RuleType.APPLY_SETTING)
    var nametagPlaceholderLevelled: String? = null
    @ExcludeFromHash @RuleFieldInfo("nametag placeholder unlevelled", RuleType.APPLY_SETTING)
    var nametagPlaceholderUnlevelled: String? = null
    @DoNotMerge @ExcludeFromHash @DoNotShow
    var presetName: String? = null
    @ExcludeFromHash @RuleFieldInfo("drop table ids", RuleType.APPLY_SETTING)
    val customDropDropTableIds = mutableListOf<String>()
    @ExcludeFromHash @RuleFieldInfo("health indicator", RuleType.APPLY_SETTING)
    var healthIndicator: HealthIndicator? = null
    @RuleFieldInfo("mob customname", RuleType.CONDITION)
    var conditionsMobCustomnameStatus = MobCustomNameStatus.NOT_SPECIFIED
    @RuleFieldInfo("mob tamed status", RuleType.CONDITION)
    var conditionsMobTamedStatus = MobTamedStatus.NOT_SPECIFIED
    @RuleFieldInfo("levelling strategy", RuleType.STRATEGY)
    val levellingStrategy = mutableMapOf<StrategyType, LevellingStrategy>()
    @RuleFieldInfo("player mod options", RuleType.STRATEGY)
    var playerLevellingOptions: PlayerLevellingOptions? = null
    @ExcludeFromHash @RuleFieldInfo("entity name overrides with level", RuleType.APPLY_SETTING)
    var entityNameOverridesLevel: MutableMap<String, MutableList<LevelTierMatching>>? = null
    @ExcludeFromHash @RuleFieldInfo("entity name overrides", RuleType.APPLY_SETTING)
    var entityNameOverrides: MutableMap<String, LevelTierMatching>? = null
    @ExcludeFromHash @RuleFieldInfo("custom death messages", RuleType.APPLY_SETTING)
    var deathMessages: DeathMessages? = null
    @ExcludeFromHash @RuleFieldInfo("nametag visibility options", RuleType.APPLY_SETTING)
    var nametagVisibilityEnum: MutableList<NametagVisibilityEnum>? = null
    @DoNotMerge @ExcludeFromHash @DoNotShow
    val ruleSourceNames = mutableMapOf<String, String>()
    @ExcludeFromHash @RuleFieldInfo("spawner particle", RuleType.APPLY_SETTING)
    var spawnerParticle: Particle? = null
    @ExcludeFromHash @RuleFieldInfo("tiered coloring info", RuleType.APPLY_SETTING)
    var tieredColoringInfos: MutableList<TieredColoringInfo>? = null
    @ExcludeFromHash @RuleFieldInfo("enabled compatibilities", RuleType.APPLY_SETTING)
    var enabledExtCompats: MutableMap<ExternalCompatibility, Boolean>? = null
    @RuleFieldInfo("mob nbt data", RuleType.APPLY_SETTING)
    var mobNBTData: MergeableStringList? = null
    @RuleFieldInfo("skylight level", RuleType.CONDITION)
    var conditionsSkyLightLevel: MinAndMax? = null
    @RuleFieldInfo("allowed worlds", RuleType.CONDITION)
    var conditionsWorlds: CachedModalList<String>? = null
    @RuleFieldInfo("allowed entities", RuleType.CONDITION)
    var conditionsEntities: CachedModalList<String>? = null
    @RuleFieldInfo("allowed biomes", RuleType.CONDITION)
    var conditionsBiomes: CachedModalList<Biome>? = null
    @RuleFieldInfo("external plugins", RuleType.CONDITION)
    var conditionsExternalPlugins: CachedModalList<String>? = null
    @RuleFieldInfo("custom names", RuleType.CONDITION)
    var conditionsCustomNames: CachedModalList<String>? = null
    @RuleFieldInfo("no drop entities", RuleType.CONDITION)
    var conditionsNoDropEntities: CachedModalList<String>? = null
    @RuleFieldInfo("worldguard regions", RuleType.CONDITION)
    var conditionsWGregions: CachedModalList<String>? = null
    @RuleFieldInfo("worldguard region owners", RuleType.CONDITION)
    var conditionsWGregionOwners: CachedModalList<String>? = null
    @RuleFieldInfo("mythic mobs names", RuleType.CONDITION)
    var conditionsMMnames: CachedModalList<String>? = null
    @RuleFieldInfo("spawner names", RuleType.CONDITION)
    var conditionsSpawnerNames: CachedModalList<String>? = null
    @RuleFieldInfo("spawner egg names", RuleType.CONDITION)
    var conditionsSpawnegEggNames: CachedModalList<String>? = null
    @RuleFieldInfo("scoreboard tags", RuleType.CONDITION)
    var conditionsScoreboardTags: CachedModalList<String>? = null
    @RuleFieldInfo("world tick time", RuleType.CONDITION)
    var conditionsWorldTickTime: CachedModalList<MinAndMax>? = null
    @RuleFieldInfo("vanilla bonuses", RuleType.APPLY_SETTING)
    var vanillaBonuses: CachedModalList<VanillaBonusEnum>? = null
    @RuleFieldInfo("spawn reasons", RuleType.CONDITION)
    var conditionsSpawnReasons: CachedModalList<LevelledMobSpawnReason>? = null
    @RuleFieldInfo("player permissions", RuleType.CONDITION)
    var conditionsPermission: CachedModalList<String>? = null
    @RuleFieldInfo("within coordinates", RuleType.CONDITION)
    var conditionsWithinCoords: WithinCoordinates? = null
    @RuleFieldInfo("all mob multipliers", RuleType.APPLY_SETTING)
    var allMobMultipliers: FineTuningAttributes? = null
    @RuleFieldInfo("mob specific multipliers", RuleType.APPLY_SETTING)
    var specificMobMultipliers: MutableMap<String, FineTuningAttributes>? = null
    @ExcludeFromHash @RuleFieldInfo("chunk kill options", RuleType.APPLY_SETTING)
    var chunkKillOptions: ChunkKillOptions? = null

    fun mergePresetRules(preset: RuleInfo?) {
        if (preset == null) {
            return
        }

        try {
            for (p in preset::class.declaredMemberProperties) {
                if (p.visibility == KVisibility.PRIVATE) continue
                if (p.hasAnnotation<DoNotMerge>()) continue
                var presetValue = p.getter.call(preset) ?: continue
                val ruleValue = p.getter.call(this)
                var skipSettingValue = false

                if (p.name == "entityNameOverrides" && this.entityNameOverrides != null && presetValue is MutableMap<*, *>) {
                    entityNameOverrides!!.putAll(presetValue as MutableMap<String, LevelTierMatching>)
                    skipSettingValue = true
                } else if (presetValue is MergableRule) {
                    val mergableRule = presetValue
                    if (ruleValue != null && mergableRule.doMerge) {
                        (ruleValue as MergableRule).merge(mergableRule.cloneItem() as MergableRule)
                        skipSettingValue = true
                    } else {
                        presetValue = mergableRule.cloneItem()
                    }
                } else if (p.name == "entityNameOverrides_Level" && this.entityNameOverridesLevel != null) {
                    entityNameOverridesLevel!!.putAll(
                        presetValue as MutableMap<String, MutableList<LevelTierMatching>>
                    )
                    skipSettingValue = true
                } else if (p.name == "specificMobMultipliers") {
                    val mergingPreset = presetValue as MutableMap<String, FineTuningAttributes>
                    if (this.specificMobMultipliers == null) {
                        this.specificMobMultipliers = mutableMapOf()
                    }

                    for ((key, value) in mergingPreset) {
                        specificMobMultipliers!![key] = value.cloneItem() as FineTuningAttributes
                    }

                    skipSettingValue = true
                } else if (p.name == "customDrop_DropTableIds") {
                    val mergingPreset = presetValue as MutableList<String>
                    customDropDropTableIds.addAll(mergingPreset)

                    skipSettingValue = true
                } else if (presetValue is MergeableStringList
                    && ruleValue != null
                ) {
                    if (presetValue.doMerge && presetValue.isNotEmpty) {
                        val current = ruleValue as MergeableStringList
                        current.items.addAll(presetValue.items)
                        skipSettingValue = true
                    }
                }

                if (presetValue is CachedModalList<*>) {
                    val thisCachedModalList = ruleValue as CachedModalList<*>?

                    if (thisCachedModalList != null && presetValue.doMerge) {
                        thisCachedModalList.mergeCachedModal(presetValue)
                    } else {
                        updatePropertyValue(p, presetValue.clone())
                    }

                    skipSettingValue = true
                }
                if (p.name == "levellingStrategy") {
                    val mergingStrategies = presetValue as MutableMap<StrategyType, LevellingStrategy>

                    for (strategy in mergingStrategies){
                        if (this.levellingStrategy.containsKey(strategy.key))
                            this.levellingStrategy[strategy.key]!!.mergeRule(strategy.value)
                        else
                            this.levellingStrategy[strategy.key] = strategy.value.cloneItem()
                    }

                    skipSettingValue = true
                }

                if (presetValue is TieredColoringInfo) {
                    presetValue = presetValue.cloneItem()!!
                }

                if (presetValue == MobCustomNameStatus.NOT_SPECIFIED) {
                    continue
                }
                if (presetValue === MobTamedStatus.NOT_SPECIFIED) {
                    continue
                }

                // skip default values such as false, 0, 0.0
                if (presetValue is Boolean && !presetValue) {
                    continue
                }
                if (presetValue is Int && (presetValue == 0)) {
                    continue
                }
                if (presetValue is Double && (presetValue == 0.0)) {
                    continue
                }

                if (!skipSettingValue) {
                    updatePropertyValue(p, presetValue)
                }
                ruleSourceNames[p.name] = preset.ruleName
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private fun updatePropertyValue(
        property: KProperty1<*, *>,
        newValue: Any?
    ) {
        if (property is KMutableProperty<*>)
            property.setter.call(this, newValue)
    }

    class RuleSortingInfo(
        val ruleType: RuleType,
        val fieldName: String
    ){
        override fun toString(): String {
            return "$ruleType: $fieldName"
        }
    }

    fun formatRulesVisually(): String {
        return formatRulesVisually(false, null)
    }

    fun formatRulesVisually(
        isForHash: Boolean,
        excludedKeys: MutableList<String>?
    ): String {
        val values = mutableMapOf<RuleSortingInfo, String>()
        val sb = StringBuilder()

        sb.append("\n&lId:&r ")
        if (this.presetName.isNullOrEmpty())
            sb.append(ruleName)
        else
            sb.append(this.presetName)
        if (!this.ruleIsEnabled)
            sb.append("&6(disabled)&r")
        else
            sb.append("&r")

        try {
            for (f in this::class.declaredMemberProperties) {
                if (f.visibility == KVisibility.PRIVATE) continue
                if (isForHash && f.findAnnotation<ExcludeFromHash>() != null) continue
                if (!isForHash && f.hasAnnotation<DoNotShow>()) continue
                val value = f.getter.call(this) ?: continue
                var ruleName = f.name
                var ruleInfoType = RuleType.MISC
                val ruleTypeInfo = f.findAnnotation<RuleFieldInfo>()
                if (ruleTypeInfo != null) {
                    ruleInfoType = ruleTypeInfo.ruleType
                    ruleName = ruleTypeInfo.value
                }

                if (value.toString().isEmpty()) continue
                if (excludedKeys != null && excludedKeys.contains(f.name)) continue
                if (value.toString() == "NOT_SPECIFIED") continue
                if (value.toString() == "{}") continue
                if (value.toString() == "[]") continue
                if (value.toString() == "0") continue
                if (value.toString() == "0.0") continue
                if (value.toString().equals("NONE", ignoreCase = true)) continue
                if (value.toString().equals("false", ignoreCase = true)) continue

                if (value is CachedModalList<*>) {
                    if (value.isEmpty() && !value.includeAll && !value.excludeAll) {
                        continue
                    }
                }
                val ruleInfo = RuleSortingInfo(
                    ruleInfoType,
                    ruleName
                )
                values[ruleInfo] = "&b$value&r"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var hadConditions = false
        var hadApplySettings = false
        var hadStrategies = false
        var hadMisc = false

        for (item in values.asSequence()
            .filter { v -> v.key.ruleType == RuleType.CONDITION }
            .sortedBy { v -> v.key.fieldName }
            .iterator()
        ){
            if (!hadConditions){
                hadConditions = true
                sb.append("\n&lConditions:&r")
            }
            sb.append("\n   ").append(item.key.fieldName)
                .append(": ").append(item.value)
        }

        for (item in values.asSequence()
            .filter { v -> v.key.ruleType == RuleType.APPLY_SETTING }
            .sortedBy { v -> v.key.fieldName }
            .iterator()
        ){
            if (!hadApplySettings){
                hadApplySettings = true
                sb.append("\n&lApply Settings&r:")
            }
            sb.append("\n   ").append(item.key.fieldName)
                .append(": ").append(item.value)
        }

        for (item in values.asSequence()
            .filter { v -> v.key.ruleType == RuleType.STRATEGY }
            .sortedBy { v -> v.key.fieldName }
            .iterator()
        ){
            if (!hadStrategies){
                hadStrategies = true
                sb.append("\n&lStrategies:&r")
            }
            sb.append("\n   ").append(item.key.fieldName)
                .append(": ").append(item.value)
        }

        for (item in values.asSequence()
            .filter { v -> v.key.ruleType != RuleType.CONDITION &&
                    v.key.ruleType != RuleType.APPLY_SETTING &&
                    v.key.ruleType != RuleType.STRATEGY &&
                    v.key.ruleType != RuleType.NO_CATEGORY }
            .iterator()
        ){
            if (!hadMisc){
                hadMisc = true
                sb.append("\n&lMisc:&r")
            }
            sb.append("\n   ").append(item.key.fieldName)
                .append(": ").append(item.value)
        }

        return sb.toString()
    }

    override fun toString(): String {
        if (this.ruleName.isEmpty()) {
            return super.toString()
        }

        return this.ruleName
    }
}