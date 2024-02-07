package io.github.arcaneplugins.levelledmobs.rules

import java.util.Locale
import java.util.TreeMap
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.enums.ExternalCompatibility
import io.github.arcaneplugins.levelledmobs.rules.strategies.RandomLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.SpawnDistanceStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.YDistanceStrategy

/**
 * Holds function to generate metrics to be sent to bstats
 *
 * @author stumper66
 * @since 3.1.0
 */
class MetricsInfo {
    private fun convertBooleanToString(result: Boolean): String {
        return if (result) "Yes" else "No"
    }

    private fun isCustomDropsEnabed(): Boolean {
        for (ruleInfo in LevelledMobs.instance.rulesManager.rulesInEffect) {
            if (ruleInfo.customDropsUseForMobs != null && ruleInfo.customDropsUseForMobs!!) {
                return true
            }
        }

        return false
    }

    val getUsesCustomDrops: String
        get() = convertBooleanToString(isCustomDropsEnabed())

    fun getUsesHealthIndicator(): String {
        val main = LevelledMobs.instance
        val usesHealthIndicator =
            (main.rulesParsingManager.defaultRule!!.healthIndicator != null) && main.rulesParsingManager.defaultRule!!.nametag != null &&
                    main.rulesParsingManager.defaultRule!!.nametag!!.lowercase(Locale.getDefault())
                        .contains("%health-indicator%")

        return convertBooleanToString(usesHealthIndicator)
    }

    fun getMaxLevelRange(): String {
        // 1-10, 11-24, 25-50, 51-100, 101-499, 500+
        val maxLevel =
            if (LevelledMobs.instance.rulesParsingManager.defaultRule!!.restrictionsMaxLevel == null) 1
            else LevelledMobs.instance.rulesParsingManager.defaultRule!!.restrictionsMaxLevel!!

        return if (maxLevel >= 500) {
            "500+"
        } else if (maxLevel > 100) {
            "101-499"
        } else if (maxLevel > 50) {
            "51-100"
        } else if (maxLevel > 24) {
            "25-50"
        } else if (maxLevel > 10) {
            "11-24"
        } else {
            "1-10"
        }
    }

    fun getCustomRulesUsed(): String {
        // 0, 1-2, 3-4, 5+
        var rulesEnabledCount = 0
        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.customRules) {
            if (ruleInfo.ruleIsEnabled) {
                rulesEnabledCount++
            }
        }

        return if (rulesEnabledCount > 4) {
            "5+"
        } else if (rulesEnabledCount > 2) {
            "3-4"
        } else if (rulesEnabledCount > 0) {
            "1-2"
        } else {
            "0"
        }
    }

    fun getLevellingStrategy(): String {
        // Random, Weighted Random, Spawn Distance, Blended, Y-Levelling
        val defaultRule = LevelledMobs.instance.rulesParsingManager.defaultRule!!

        if (defaultRule.levellingStrategy != null) {
            if (defaultRule.levellingStrategy is SpawnDistanceStrategy) {
                val sds = defaultRule.levellingStrategy as SpawnDistanceStrategy
                return if (sds.blendedLevellingEnabled == null || !sds.blendedLevellingEnabled!!) {
                    "Spawn Distance"
                } else {
                    "Blended"
                }
            } else if (defaultRule.levellingStrategy is YDistanceStrategy) {
                return "Y-Levelling"
            } else if (defaultRule.levellingStrategy is RandomLevellingStrategy) {
                val random = defaultRule.levellingStrategy as RandomLevellingStrategy
                if (random.weightedRandom.isNotEmpty()) {
                    return "Weighted Random"
                }
            }
        }

        return "Random"
    }

    fun usesPlayerLevelling(): String {
        return convertBooleanToString(LevelledMobs.instance.rulesManager.isPlayerLevellingEnabled())
    }

    fun usesAutoUpdateChecker(): String {
        return convertBooleanToString(
            LevelledMobs.instance.helperSettings.getBoolean("use-update-checker", true))
    }

    fun levelMobsUponSpawn(): String {
        return convertBooleanToString(
            LevelledMobs.instance.helperSettings.getBoolean(
                "level-mobs-upon-spawn", true)
        )
    }

    fun checkMobsOnChunkLoad(): String {
        return convertBooleanToString(
            LevelledMobs.instance.helperSettings.getBoolean(
                "ensure-mobs-are-levelled-on-chunk-load", true
            )
        )
    }

    fun customEntityNamesCount(): String {
        // 0, 1-3, 4-8, 9-12, 13+
        var count = 0
        if (LevelledMobs.instance.rulesParsingManager.defaultRule!!.entityNameOverrides != null) {
            count += LevelledMobs.instance.rulesParsingManager.defaultRule!!.entityNameOverrides!!.size
        }
        if (LevelledMobs.instance.rulesParsingManager.defaultRule!!.entityNameOverridesLevel != null) {
            count += LevelledMobs.instance.rulesParsingManager.defaultRule!!.entityNameOverridesLevel!!.size
        }

        return if (count > 12) {
            "13+"
        } else if (count > 8) {
            "9-12"
        } else if (count > 3) {
            "4-8"
        } else if (count > 0) {
            "1-3"
        } else {
            "0"
        }
    }

    fun usesNbtData(): String {
        if (!ExternalCompatibilityManager.hasNbtApiInstalled) {
            return "No"
        }

        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
            if (!ruleInfo.ruleIsEnabled) {
                continue
            }

            if (ruleInfo.mobNBTData != null && ruleInfo.mobNBTData!!.isNotEmpty) {
                return "Yes"
            }
        }

        if (isCustomDropsEnabed() && LevelledMobs.instance.customDropsHandler.customDropsParser.dropsUtilizeNBTAPI) {
            return "Yes"
        }

        return "No"
    }

    fun enabledCompats(): Map<String, Int> {
        val results: MutableMap<String, Int> = TreeMap()

        for (compat in ExternalCompatibility.entries.toTypedArray()) {
            if (compat == ExternalCompatibility.NOT_APPLICABLE ||
                compat == ExternalCompatibility.PLACEHOLDER_API
            ) {
                continue
            }

            results[compat.toString()] = 0
        }

        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
            if (!ruleInfo.ruleIsEnabled) {
                continue
            }

            if (ruleInfo.enabledExtCompats != null) {
                for ((compat, enabled) in ruleInfo.enabledExtCompats!!.entries) {
                    if (compat
                        == ExternalCompatibility.NOT_APPLICABLE
                    ) {
                        continue
                    }
                    if (enabled) {
                        results[compat.toString()] = 1
                    }
                }
            }
        }

        return results
    }

    fun nametagVisibility(): String {
        if (LevelledMobs.instance.rulesParsingManager.defaultRule!!.nametagVisibilityEnum == null
            || LevelledMobs.instance.rulesParsingManager.defaultRule!!.nametagVisibilityEnum!!.isEmpty()
        ) {
            return "Undefined"
        }

        return LevelledMobs.instance.rulesParsingManager.defaultRule!!.nametagVisibilityEnum!!
            .asSequence()
            .sorted()
            .toString().replace("[", "").replace("]", "")
    }
}