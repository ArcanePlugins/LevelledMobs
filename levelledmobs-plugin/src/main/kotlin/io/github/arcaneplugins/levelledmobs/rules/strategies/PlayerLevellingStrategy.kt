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
    var matchVariable: Boolean? = null
    var enabled: Boolean? = null
    var usevariableAsMax: Boolean? = null
    var recheckPlayers: Boolean? = null
    var outputCap: Float? = null
    var preserveEntityTime: Long? = null
    var playerVariableScale: Float? = null
    var variable: String? = null
    var decreaseOutput = true
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
        val scale = if (options.playerVariableScale != null) options.playerVariableScale!! else 1f
        val playerLevelSourceResult = lmEntity.main.levelManager.getPlayerLevelSourceNumber(
            lmEntity.playerForLevelling, lmEntity, variableToUse
        )

        val origLevelSource =
                if (playerLevelSourceResult.isNumericResult) playerLevelSourceResult.numericResult
                else 1f

        applyValueToPdc(lmEntity, playerLevelSourceResult)
        val levelSource = (origLevelSource * scale).coerceAtLeast(0f)

        val results = MinAndMaxHolder(0f, 0f)
        var tierMatched: String? = null
        val capDisplay = if (options.outputCap == null) "" else "cap: ${options.outputCap}, "

        if (options.getMatchVariable) {
            results.min = levelSource
            results.max = results.min
        } else if (options.getVariableAsMax) {
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
                        "player: ${player.name}, input: $origLevelSource, scale: $levelSource,${capDisplay}sno tiers matched"
                    }
                } else {
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                        "player: ${player.name}, input: '${playerLevelSourceResult.stringResult}', ${capDisplay}no tiers matched"
                    }
                }
                if (options.outputCap != null) {
                    results.max = results.max.coerceAtMost(options.outputCap!!)
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

        if (options.outputCap != null) {
            results.max = results.max.coerceAtMost(options.outputCap!!)
        }

        val homeName = if (playerLevelSourceResult.homeNameUsed != null)
            " (${playerLevelSourceResult.homeNameUsed})"
        else ""

        if (tierMatched == null) {
            DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                "player: ${player.name}, input: ${origLevelSource}${homeName}${varianceDebug}, scale: ${levelSource}, ${capDisplay}result: $results"
            }
        } else {
            val tierMatchedFinal: String = tierMatched
            if (playerLevelSourceResult.isNumericResult) {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                    "player: ${player.name}, input: ${origLevelSource}${homeName}$varianceDebug, scale: $levelSource, tier: $tierMatchedFinal, ${capDisplay}result: $results"
                }
            } else {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity) {
                    "player: ${player.name}, input: '${playerLevelSourceResult.stringResult}'$varianceDebug, tier: $tierMatchedFinal, ${capDisplay}result: $results"
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
        lmEntity.playerLevellingAllowDecrease = options.decreaseOutput

        return calculateResult(results)
    }

    private fun calculateResult(minAndMax: MinAndMaxHolder): Float{
        if (minAndMax.min == minAndMax.max) return minAndMax.min

        val useMin = minAndMax.min.roundToInt()
        val useMax = minAndMax.max.roundToInt().coerceAtLeast(useMin)
        if (useMin == useMax) return useMin.toFloat()

        return ThreadLocalRandom.current().nextInt(useMin, useMax + 1).toFloat()
    }

    override fun mergeRule(levellingStrategy: LevellingStrategy?) {
        if (levellingStrategy == null || levellingStrategy !is PlayerLevellingStrategy) {
            return
        }

        levelTiers.addAll(levellingStrategy.levelTiers)
        if (levellingStrategy.matchVariable != null) {
            this.matchVariable = levellingStrategy.matchVariable
        }
        if (levellingStrategy.usevariableAsMax != null) {
            this.usevariableAsMax = levellingStrategy.usevariableAsMax
        }
        if (levellingStrategy.playerVariableScale != null) {
            this.playerVariableScale = levellingStrategy.playerVariableScale
        }
        if (levellingStrategy.outputCap != null) {
            this.outputCap = levellingStrategy.outputCap
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

    val getMatchVariable: Boolean
        get() = this.matchVariable != null && matchVariable!!

    val getEnabled: Boolean
        // enabled is true by default unless specifically disabled
        get() = this.enabled == null || enabled!!

    val getVariableAsMax: Boolean
        get() = this.usevariableAsMax != null && usevariableAsMax!!

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

        if (getMatchVariable) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("match-plr-lvl")
        }

        if (getVariableAsMax) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("use-plr-max-lvl")
        }

        if (playerVariableScale != null) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("scale: ")
            sb.append(playerVariableScale)
        }

        if (outputCap != null) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("cap: ")
            sb.append(outputCap)
        }

        if (levelTiers.isNotEmpty()) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append(levelTiers)
        }

        if (decreaseOutput) {
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