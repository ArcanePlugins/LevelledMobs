package io.github.arcaneplugins.levelledmobs.rules.strategies

import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.result.MinAndMaxHolder
import io.github.arcaneplugins.levelledmobs.result.PlayerLevelSourceResult
import io.github.arcaneplugins.levelledmobs.rules.LevelTierMatching
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.concurrent.ThreadLocalRandom
import org.bukkit.persistence.PersistentDataType
import kotlin.math.roundToInt

/**
 * Holds any rules relating to player levelling
 *
 * @author stumper66
 * @since 3.1.0
 */
class PlayerLevellingStrategy : LevellingStrategy, Cloneable {
    val levelTiers = mutableListOf<LevelTierMatching>()
    var defaultLevelTier: LevelTierMatching? = null
    var matchPlayerLevel: Boolean? = null
    var enabled: Boolean? = null
    var usePlayerMaxLevel: Boolean? = null
    var recheckPlayers: Boolean? = null
    var assignmentCap: Float? = null
    var preserveEntityTime: Long? = null
    var playerLevelScale: Float? = null
    var variable: String? = null
    var decreaseLevel = true
    var doMerge = false

    override val strategyType = StrategyType.PLAYER_VARIABLE

    override fun generateNumber(
        lmEntity: LivingEntityWrapper,
        minLevel: Int,
        maxLevel: Int
    ): Float{
        val options = lmEntity.main.rulesManager.getRulePlayerLevellingOptions(
            lmEntity
        )

        if (options == null || !options.getEnabled) {
            return 0f
        }

        val player = lmEntity.playerForLevelling ?: return 0f

        val variableToUse =
            if (options.variable.isNullOrEmpty()) "%level%" else options.variable!!
        val scale = if (options.playerLevelScale != null) options.playerLevelScale!! else 1f
        val playerLevelSourceResult = lmEntity.main.levelManager.getPlayerLevelSourceNumber(
            lmEntity.playerForLevelling, lmEntity, variableToUse
        )

        val origLevelSource =
                if (playerLevelSourceResult.isNumericResult) playerLevelSourceResult.numericResult
                else 1f

        applyValueToPdc(lmEntity, playerLevelSourceResult)
        val levelSource = origLevelSource * scale.coerceAtLeast(1f)

        val results = MinAndMaxHolder(0f, 0f)
        var tierMatched: String? = null
        val capDisplay = if (options.assignmentCap == null) "" else "cap: ${options.assignmentCap}, "

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
                    if (tier.valueRanges!!.min> 0f) {
                        results.min = tier.valueRanges!!.min
                    }
                    if (tier.valueRanges!!.max > 0f) {
                        results.max = tier.valueRanges!!.max
                    }
                    tierMatched = tier.toString()
                    foundMatch = true
                    break
                }
            }

            if (!foundMatch && options.defaultLevelTier != null){
                foundMatch = true
                tierMatched = options.defaultLevelTier.toString()
                results.min = options.defaultLevelTier!!.valueRanges!!.min
                results.max = options.defaultLevelTier!!.valueRanges!!.max
            }

            if (!foundMatch) {
                if (playerLevelSourceResult.isNumericResult) {
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        String.format(
                            "mob: %s, player: %s, input: %s, scale: %s, %sno tiers matched",
                            lmEntity.nameIfBaby, player.name, origLevelSource, levelSource,
                            capDisplay
                        )
                    }
                } else {
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        String.format(
                            "mob: %s, player: %s, input: '%s', %sno tiers matched",
                            lmEntity.nameIfBaby, player.name,
                            playerLevelSourceResult.stringResult, capDisplay
                        )
                    }
                }
                if (options.assignmentCap != null) {
                    results.max = results.max.coerceAtMost(options.assignmentCap!!)
                    return calculateResult(results)
                } else {
                    return 0f
                }
            }
        }

        val varianceDebug: String
        if (playerLevelSourceResult.randomVarianceResult != null) {
            playerLevelSourceResult.randomVarianceResult =
                playerLevelSourceResult.randomVarianceResult!! + playerLevelSourceResult.randomVarianceResult!!
            // ensure the min value is at least 1
            results.min = results.min.coerceAtLeast(1f)
            // ensure the min value is not higher than the max value
            results.min = results.min.coerceAtMost(results.max)

            varianceDebug = ", var: ${playerLevelSourceResult.randomVarianceResult}"
        } else {
            varianceDebug = ""
        }

        if (options.assignmentCap != null) {
            results.max = results.max.coerceAtMost(options.assignmentCap!!)
        }

        val homeName = if (playerLevelSourceResult.homeNameUsed != null)
            " (${playerLevelSourceResult.homeNameUsed})"
        else ""

        if (tierMatched == null) {
            DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                String.format(
                    "mob: %s, player: %s, input: %s%s%s, scale: %s, %sresult: %s",
                    lmEntity.nameIfBaby, player.name, origLevelSource, homeName,
                    varianceDebug, levelSource, capDisplay, results
                )
            }
        } else {
            val tierMatchedFinal: String = tierMatched
            if (playerLevelSourceResult.isNumericResult) {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                    String.format(
                        "mob: %s, player: %s, input: %s%s%s, scale: %s, tier: %s, %sresult: %s",
                        lmEntity.nameIfBaby, player.name, origLevelSource, homeName,
                        varianceDebug, levelSource, tierMatchedFinal, capDisplay, results
                    )
                }
            } else {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                    String.format(
                        "mob: %s, player: %s, input: '%s'%s, tier: %s, %sresult: %s",
                        lmEntity.nameIfBaby, player.name,
                        playerLevelSourceResult.stringResult, varianceDebug, tierMatchedFinal,
                        capDisplay, results
                    )
                }
            }
        }

        if (options.getRecheckPlayers) {
            val numberOrString =
                if (playerLevelSourceResult.isNumericResult)
                    playerLevelSourceResult.numericResult.toString()
                else
                    playerLevelSourceResult.stringResult

            if (numberOrString != null) lmEntity.pdc.set(
                NamespacedKeys.playerLevellingSourceNumber,
                PersistentDataType.STRING,
                numberOrString
            )
        }
        lmEntity.playerLevellingAllowDecrease = options.decreaseLevel

        return calculateResult(results)
    }

    private fun calculateResult(minAndMax: MinAndMaxHolder): Float{
        if (minAndMax.min == minAndMax.max) return minAndMax.min

        val useMin = minAndMax.min.roundToInt()
        val useMax = minAndMax.max.roundToInt()
        if (useMin == useMax) return useMin.toFloat()

        return ThreadLocalRandom.current().nextInt(useMin, useMax + 1).toFloat()
    }

    override fun mergeRule(levellingStrategy: LevellingStrategy?) {
        if (levellingStrategy == null || levellingStrategy !is PlayerLevellingStrategy) {
            return
        }

        levelTiers.addAll(levellingStrategy.levelTiers)
        if (levellingStrategy.matchPlayerLevel != null) {
            this.matchPlayerLevel = levellingStrategy.matchPlayerLevel
        }
        if (levellingStrategy.usePlayerMaxLevel != null) {
            this.usePlayerMaxLevel = levellingStrategy.usePlayerMaxLevel
        }
        if (levellingStrategy.playerLevelScale != null) {
            this.playerLevelScale = levellingStrategy.playerLevelScale
        }
        if (levellingStrategy.assignmentCap != null) {
            this.assignmentCap = levellingStrategy.assignmentCap
        }
        if (variable != null) {
            this.variable = levellingStrategy.variable
        }
        if (levellingStrategy.enabled != null) {
            this.enabled = levellingStrategy.enabled
        }
        if (levellingStrategy.recheckPlayers != null) {
            this.recheckPlayers = levellingStrategy.recheckPlayers
        }
    }

    override fun cloneItem(): LevellingStrategy {
        var copy: PlayerLevellingStrategy? = null
        try {
            copy = super.clone() as PlayerLevellingStrategy
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return copy as LevellingStrategy
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

    private fun applyValueToPdc(
        lmEntity: LivingEntityWrapper,
        playerLevel: PlayerLevelSourceResult
    ) {
        val value =
            if (playerLevel.isNumericResult)
                playerLevel.numericResult.toString()
            else
                playerLevel.stringResult?: "(null)"

        try {
            lmEntity.pdc.set(
                NamespacedKeys.playerLevellingValue,
                PersistentDataType.STRING,
                value
            )
        } catch (ignored: Exception) { }
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

        if (assignmentCap != null) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("cap: ")
            sb.append(assignmentCap)
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