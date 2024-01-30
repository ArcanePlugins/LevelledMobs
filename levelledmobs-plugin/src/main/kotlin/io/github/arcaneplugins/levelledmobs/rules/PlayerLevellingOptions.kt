package io.github.arcaneplugins.levelledmobs.rules

import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.result.MinAndMaxHolder
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.result.PlayerLevelSourceResult
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.persistence.PersistentDataType
import kotlin.math.max
import kotlin.math.min

/**
 * Holds any rules relating to player levelling
 *
 * @author stumper66
 * @since 3.1.0
 */
class PlayerLevellingOptions : Cloneable {
    val levelTiers = mutableListOf<LevelTierMatching>()
    var matchPlayerLevel: Boolean? = null
    var enabled: Boolean? = null
    var usePlayerMaxLevel: Boolean? = null
    var recheckPlayers: Boolean? = null
    var levelCap: Int? = null
    var preserveEntityTime: Long? = null
    var playerLevelScale: Double? = null
    var variable: String? = null
    var decreaseLevel = true
    var doMerge = false

    fun mergeRule(options: PlayerLevellingOptions?) {
        if (options == null) {
            return
        }

        levelTiers.addAll(options.levelTiers)
        if (options.matchPlayerLevel != null) {
            this.matchPlayerLevel = options.matchPlayerLevel
        }
        if (options.usePlayerMaxLevel != null) {
            this.usePlayerMaxLevel = options.usePlayerMaxLevel
        }
        if (options.playerLevelScale != null) {
            this.playerLevelScale = options.playerLevelScale
        }
        if (options.levelCap != null) {
            this.levelCap = options.levelCap
        }
        if (variable != null) {
            this.variable = options.variable
        }
        if (options.enabled != null) {
            this.enabled = options.enabled
        }
        if (options.recheckPlayers != null) {
            this.recheckPlayers = options.recheckPlayers
        }
    }

    fun cloneItem(): PlayerLevellingOptions? {
        var copy: PlayerLevellingOptions? = null
        try {
            copy = super.clone() as PlayerLevellingOptions
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy
    }


    val getMatchPlayerLevel: Boolean
        get() = this.matchPlayerLevel != null && matchPlayerLevel!!

    val getEnabled: Boolean
        // enabled is true by default unless specifically disabled
        get() = this.enabled == null || enabled!!

    val getUsePlayerMaxLevel: Boolean
        get() = this.usePlayerMaxLevel != null && usePlayerMaxLevel!!

    val getRecheckPlayers: Boolean
        get() = this.recheckPlayers != null && recheckPlayers!!

    fun getPlayerLevels(lmEntity: LivingEntityWrapper): MinAndMaxHolder? {
        val options = lmEntity.main.rulesManager.getRulePlayerLevellingOptions(
            lmEntity
        )

        if (options == null || !options.getEnabled) {
            return null
        }

        val player = lmEntity.playerForLevelling ?: return null

        val levelSource: Int
        val variableToUse =
            if (options.variable.isNullOrEmpty()) "%level%" else options.variable!!
        val scale = if (options.playerLevelScale != null) options.playerLevelScale!! else 1.0
        val playerLevelSourceResult = lmEntity.main.levelManager.getPlayerLevelSourceNumber(
            lmEntity.playerForLevelling, lmEntity, variableToUse
        )

        val origLevelSource =
            (if (playerLevelSourceResult.isNumericResult) playerLevelSourceResult.numericResult else 1).toDouble()

        applyValueToPdc(lmEntity, playerLevelSourceResult)
        levelSource = max(Math.round(origLevelSource * scale).toInt().toDouble(), 1.0).toInt()

        val results = MinAndMaxHolder(1, 1)
        var tierMatched: String? = null
        val capDisplay = if (options.levelCap == null) "" else "cap: " + options.levelCap + ", "

        if (options.getUsePlayerMaxLevel) {
            results.min = levelSource
            results.max = results.min
        } else if (options.getMatchPlayerLevel) {
            results.max = levelSource
        } else {
            var foundMatch = false
            for (tier in options.levelTiers) {
                var meetsMin = false
                var meetsMax = false
                var hasStringMatch = false


                if (tier.sourceTierName != null) {
                    hasStringMatch = playerLevelSourceResult.stringResult.equals(
                        tier.sourceTierName, ignoreCase = true
                    )
                } else if (playerLevelSourceResult.isNumericResult) {
                    meetsMin = (tier.minLevel == null || levelSource >= tier.minLevel!!)
                    meetsMax = (tier.maxLevel == null || levelSource <= tier.maxLevel!!)
                }

                if (meetsMin && meetsMax || hasStringMatch) {
                    if (tier.valueRanges!![0] > 0) {
                        results.min = tier.valueRanges!![0]
                    }
                    if (tier.valueRanges!![1] > 0) {
                        results.max = tier.valueRanges!![1]
                    }
                    tierMatched = tier.toString()
                    foundMatch = true
                    break
                }
            }

            if (!foundMatch) {
                if (playerLevelSourceResult.isNumericResult) {
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        String.format(
                            "mob: %s, player: %s, lvl-src: %s, lvl-scale: %s, %sno tiers matched",
                            lmEntity.nameIfBaby, player.name, origLevelSource, levelSource,
                            capDisplay
                        )
                    }
                } else {
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        String.format(
                            "mob: %s, player: %s, lvl-src: '%s', %sno tiers matched",
                            lmEntity.nameIfBaby, player.name,
                            playerLevelSourceResult.stringResult, capDisplay
                        )
                    }
                }
                if (options.levelCap != null) {
                    results.max = options.levelCap!!
                    results.useMin = false
                    return results
                } else {
                    return null
                }
            }
        }


        val varianceDebug: String
        if (playerLevelSourceResult.randomVarianceResult != null) {
            playerLevelSourceResult.randomVarianceResult =
                playerLevelSourceResult.randomVarianceResult!! + playerLevelSourceResult.randomVarianceResult!!
            // ensure the min value is at least 1
            results.min = max(results.min.toDouble(), 1.0).toInt()
            // ensure the min value is not higher than the max value
            results.min = min(results.min.toDouble(), results.max.toDouble()).toInt()

            varianceDebug = String.format(", var: %s", playerLevelSourceResult.randomVarianceResult)
        } else {
            varianceDebug = ""
        }

        if (options.levelCap != null) {
            results.ensureMinAndMax(1, options.levelCap!!)
        }

        val homeName = if (playerLevelSourceResult.homeNameUsed != null) String.format(
            " (%s)",
            playerLevelSourceResult.homeNameUsed
        ) else ""

        if (tierMatched == null) {
            DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                String.format(
                    "mob: %s, player: %s, lvl-src: %s%s%s, lvl-scale: %s, %sresult: %s",
                    lmEntity.nameIfBaby, player.name, origLevelSource, homeName,
                    varianceDebug, levelSource, capDisplay, results
                )
            }
        } else {
            val tierMatchedFinal: String = tierMatched
            if (playerLevelSourceResult.isNumericResult) {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                    String.format(
                        "mob: %s, player: %s, lvl-src: %s%s%s, lvl-scale: %s, tier: %s, %sresult: %s",
                        lmEntity.nameIfBaby, player.name, origLevelSource, homeName,
                        varianceDebug, levelSource, tierMatchedFinal, capDisplay, results
                    )
                }
            } else {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                    String.format(
                        "mob: %s, player: %s, lvl-src: '%s'%s, tier: %s, %sresult: %s",
                        lmEntity.nameIfBaby, player.name,
                        playerLevelSourceResult.stringResult, varianceDebug, tierMatchedFinal,
                        capDisplay, results
                    )
                }
            }
        }

        if (options.getRecheckPlayers) {
            val numberOrString =
                if (playerLevelSourceResult.isNumericResult) playerLevelSourceResult.numericResult.toString() else playerLevelSourceResult.stringResult
            if (numberOrString != null) lmEntity.pdc.set(
                NamespacedKeys.playerLevellingSourceNumber,
                PersistentDataType.STRING,
                numberOrString
            )
        }
        lmEntity.playerLevellingAllowDecrease = options.decreaseLevel

        return results
    }

    private fun applyValueToPdc(
        lmEntity: LivingEntityWrapper,
        playerLevel: PlayerLevelSourceResult
    ) {
        val value =
            if (playerLevel.isNumericResult) playerLevel.numericResult.toString() else playerLevel.stringResult!!

        try {
            lmEntity.pdc.set(
                NamespacedKeys.playerLevellingValue,
                PersistentDataType.STRING,
                value
            )
        } catch (ignored: java.lang.Exception) {
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()

        if (!getEnabled) {
            sb.append("(disabled)")
        }

        if (variable != null) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("var: ")
            sb.append(variable)
        }

        if (getMatchPlayerLevel) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("match-plr-lvl")
        }

        if (getUsePlayerMaxLevel) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("use-plr-max-lvl")
        }

        if (playerLevelScale != null) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("scale: ")
            sb.append(playerLevelScale)
        }

        if (levelCap != null) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("cap: ")
            sb.append(levelCap)
        }

        if (levelTiers.isNotEmpty()) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append(levelTiers)
        }

        if (decreaseLevel) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("decrease-lvl")
        }

        if (getRecheckPlayers) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("rechk-plr")
        }

        return if (sb.isEmpty()) {
            super.toString()
        } else {
            sb.toString()
        }
    }
}