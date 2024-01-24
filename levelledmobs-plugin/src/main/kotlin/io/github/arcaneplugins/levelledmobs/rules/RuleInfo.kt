package io.github.arcaneplugins.levelledmobs.rules

import java.util.SortedMap
import java.util.TreeMap
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager.ExternalCompatibility
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.rules.strategies.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import org.bukkit.Particle
import org.bukkit.block.Biome
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

/**
 * Holds rules parsed from rules.yml to make up a list of rules
 *
 * @author stumper66
 * @since 3.0.0
 */
class RuleInfo(
    @ExcludeFromHash internal var ruleName: String = "Unnamed"
) {
    @DoNotMerge var ruleIsEnabled = true
    @DoNotMerge @ExcludeFromHash var isTempDisabled = false
    @ExcludeFromHash var useNoSpawnerParticles = false
    var babyMobsInheritAdultSetting: Boolean? = null
    var mobLevelInheritance: Boolean? = null
    @ExcludeFromHash var customDrops_UseForMobs: Boolean? = null
    var stopProcessingRules: Boolean? = null
    @ExcludeFromHash var mergeEntityNameOverrides: Boolean? = null
    var passengerMatchLevel: Boolean? = null
    @ExcludeFromHash var lockEntity: Boolean? = null
    @DoNotMerge @ExcludeFromHash var rulePriority = 0
    @ExcludeFromHash var spawnerParticlesCount: Int? = null
    var maxRandomVariance: Int? = null
    @ExcludeFromHash var creeperMaxDamageRadius: Int? = null
    var conditions_MinLevel: Int? = null
    var conditions_MaxLevel: Int? = null
    var restrictions_MinLevel: Int? = null
    var restrictions_MaxLevel: Int? = null
    var conditions_ApplyAboveY: Int? = null
    var conditions_ApplyBelowY: Int? = null
    var conditions_MinDistanceFromSpawn: Int? = null
    var conditions_MaxDistanceFromSpawn: Int? = null
    @ExcludeFromHash var nametagVisibleTime: Long? = null
    @ExcludeFromHash var maximumDeathInChunkThreshold: Int? = null
    @ExcludeFromHash var chunkMaxCoolDownTime: Int? = null
    @ExcludeFromHash var maxAdjacentChunks: Int? = null
    @ExcludeFromHash var conditions_CooldownTime: Long? = null
    @ExcludeFromHash var conditions_TimesToCooldownActivation: Int? = null
    @ExcludeFromHash var conditions_Chance: Float? = null
    @ExcludeFromHash var sunlightBurnAmount: Double? = null
    @ExcludeFromHash var nametag: String? = null
    @ExcludeFromHash var nametag_CreatureDeath: String? = null
    @ExcludeFromHash var nametag_Placeholder_Levelled: String? = null
    @ExcludeFromHash var nametag_Placeholder_Unlevelled: String? = null
    @DoNotMerge @ExcludeFromHash var presetName: String? = null
    @ExcludeFromHash val customDrop_DropTableIds = mutableListOf<String>()
    @ExcludeFromHash var healthIndicator: HealthIndicator? = null
    var conditions_MobCustomnameStatus = MobCustomNameStatus.NOT_SPECIFIED
    var conditions_MobTamedStatus = MobTamedStatus.NOT_SPECIFIED
    var levellingStrategy: LevellingStrategy? = null
    var playerLevellingOptions: PlayerLevellingOptions? = null
    @ExcludeFromHash var entityNameOverrides_Level: MutableMap<String, MutableList<LevelTierMatching>>? = null
    @ExcludeFromHash var entityNameOverrides: MutableMap<String, LevelTierMatching>? = null
    @ExcludeFromHash var deathMessages: DeathMessages? = null
    @ExcludeFromHash var nametagVisibilityEnum: MutableList<NametagVisibilityEnum>? = null
    @DoNotMerge @ExcludeFromHash val ruleSourceNames = mutableMapOf<String, String>()
    @ExcludeFromHash var spawnerParticle: Particle? = null
    @ExcludeFromHash var tieredColoringInfos: MutableList<TieredColoringInfo>? = null
    var enabledExtCompats: MutableMap<ExternalCompatibility, Boolean>? = null
    var mobNBT_Data: MergeableStringList? = null
    var allowedEntities: CachedModalList<String>? = null
    var conditions_SkyLightLevel: MinAndMax? = null
    var conditions_Worlds: CachedModalList<String>? = null
    var conditions_Entities: CachedModalList<String>? = null
    var conditions_Biomes: CachedModalList<Biome>? = null
    var conditions_ApplyPlugins: CachedModalList<String>? = null
    var conditions_CustomNames: CachedModalList<String>? = null
    var conditions_NoDropEntities: CachedModalList<String>? = null
    var conditions_WGRegions: CachedModalList<String>? = null
    var conditions_WGRegionOwners: CachedModalList<String>? = null
    var conditions_MM_Names: CachedModalList<String>? = null
    var conditions_SpawnerNames: CachedModalList<String>? = null
    var conditions_SpawnegEggNames: CachedModalList<String>? = null
    var conditions_ScoreboardTags: CachedModalList<String>? = null
    var conditions_WorldTickTime: CachedModalList<MinAndMax>? = null
    var vanillaBonuses: CachedModalList<VanillaBonusEnum>? = null
    var conditions_SpawnReasons: CachedModalList<LevelledMobSpawnReason>? = null
    var conditions_Permission: CachedModalList<String>? = null
    var conditions_WithinCoords: WithinCoordinates? = null
    var allMobMultipliers: FineTuningAttributes? = null
    var specificMobMultipliers: MutableMap<String, FineTuningAttributes>? = null
    @ExcludeFromHash var chunkKillOptions: ChunkKillOptions? = null

    fun mergePresetRules(preset: RuleInfo?) {
        if (preset == null) {
            return
        }

        try {
            for (p in preset::class.declaredMemberProperties) {
                if (p.visibility == KVisibility.PRIVATE) continue
                if (p.findAnnotation<DoNotMerge>() != null) continue
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
                } else if (p.name == "entityNameOverrides_Level" && this.entityNameOverrides_Level != null) {
                    entityNameOverrides_Level!!.putAll(
                        presetValue as MutableMap<String, MutableList<LevelTierMatching>>
                    )
                    skipSettingValue = true
                } else if (p.name == "specificMobMultipliers") {
                    val mergingPreset = presetValue as MutableMap<String, FineTuningAttributes>
                    if (this.specificMobMultipliers == null) {
                        this.specificMobMultipliers = TreeMap()
                    }

                    for ((key, value) in mergingPreset) {
                        specificMobMultipliers!![key] = value.cloneItem() as FineTuningAttributes
                    }

                    skipSettingValue = true
                } else if (p.name == "customDrop_DropTableIds") {
                    val mergingPreset = presetValue as MutableList<String>
                    customDrop_DropTableIds.addAll(mergingPreset)

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
                if (presetValue is LevellingStrategy) {
                    if (this.levellingStrategy != null && (levellingStrategy!!.javaClass
                                == presetValue.javaClass)
                    ) {
                        levellingStrategy!!.mergeRule(presetValue)
                    } else {
                        this.levellingStrategy = presetValue
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

    fun formatRulesVisually(): String {
        return formatRulesVisually(false, null)
    }

    fun formatRulesVisually(
        isForHash: Boolean,
        excludedKeys: MutableList<String>?
    ): String {
        val values: SortedMap<String, String> = TreeMap()
        val sb = StringBuilder()

        if (excludedKeys == null || excludedKeys.contains("id")) {
            sb.append("id: ").append(ruleName).append("\n")
        }

        try {
            for (f in this::class.declaredMemberProperties) {
                if (f.visibility == KVisibility.PRIVATE) {
                    continue
                }

                if (isForHash && f.findAnnotation<ExcludeFromHash>() != null) continue
                val value = f.getter.call(this) ?: continue

                if (f.name == "ruleSourceNames") {
                    continue
                }
                if (excludedKeys != null && excludedKeys.contains(f.name)) {
                    continue
                }
                if (value.toString() == "NOT_SPECIFIED") {
                    continue
                }
                if (value.toString() == "{}") {
                    continue
                }
                if (value.toString() == "[]") {
                    continue
                }
                if (value.toString() == "0" && f.name == "rulePriority") {
                    continue
                }
                if (value.toString() == "0.0") {
                    continue
                }
                if (value.toString().equals("false", ignoreCase = true) &&
                    f.name != "ruleIsEnabled"
                ) {
                    continue
                }
                if (value.toString().equals("NONE", ignoreCase = true)) {
                    continue
                }
                if (value is CachedModalList<*>) {
                    if (value.isEmpty() && !value.allowAll && !value.excludeAll) {
                        continue
                    }
                }
                val showValue = "&b${f.name}&r, value: &b$value&r"
                values[f.name] = showValue
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (s in values.values) {
            sb.append(colorizeAll(s))
            sb.append("\n")
        }

        if (sb.isNotEmpty())
            sb.setLength(sb.length - 1) // remove trailing newline
        return sb.toString()
    }

    override fun toString(): String {
        if (this.ruleName.isEmpty()) {
            return super.toString()
        }

        return this.ruleName
    }
}