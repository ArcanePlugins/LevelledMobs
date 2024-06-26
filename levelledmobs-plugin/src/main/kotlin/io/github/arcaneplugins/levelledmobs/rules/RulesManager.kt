package io.github.arcaneplugins.levelledmobs.rules

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.time.Instant
import java.util.TreeMap
import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.managers.WorldGuardIntegration.getWorldGuardRegionOwnersForLocation
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.MobCustomNameStatus
import io.github.arcaneplugins.levelledmobs.enums.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.result.RuleCheckResult
import io.github.arcaneplugins.levelledmobs.rules.strategies.CustomStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.PlayerLevellingStrategy
import io.github.arcaneplugins.levelledmobs.rules.strategies.StrategyType
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.util.Utils.capitalize
import io.github.arcaneplugins.levelledmobs.util.Utils.isBiomeInModalList
import io.github.arcaneplugins.levelledmobs.util.Utils.isIntegerInModalList
import io.github.arcaneplugins.levelledmobs.util.Utils.isLivingEntityInModalList
import io.github.arcaneplugins.levelledmobs.util.Utils.round
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import java.util.TreeSet
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Manages all rules that are parsed from rules.yml and applied to various defined mobs
 *
 * @author stumper66
 * @since 3.0.0
 */
@Suppress("DEPRECATION")
class RulesManager {
    val rulesInEffect = mutableListOf<RuleInfo>()
    val ruleNameMappings: MutableMap<String, RuleInfo> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    val biomeGroupMappings: MutableMap<String, MutableList<String>> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    val rulesCooldown = mutableMapOf<String, MutableList<Instant>>()
    val allCustomStrategyPlaceholders: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    var anyRuleHasChance = false
    var hasAnyWGCondition = false
    private var lastRulesCheck: Instant? = null
    var currentRulesHash = ""
        private set

    init {
        instance = this
    }

    fun getRuleIsWorldAllowedInAnyRule(world: World?): Boolean {
        if (world == null) return false

        var result = false

        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
            if (!ruleInfo.ruleIsEnabled) continue

            if (ruleInfo.conditionsWorlds != null && ruleInfo.conditionsWorlds!!.isIncludedInList(
                    world.name, null
                )
            ) {
                result = true
                break
            }
        }

        return result
    }

    fun addCustomRule(ri: RuleInfo?) {
        if (ri == null) return

        LevelledMobs.instance.rulesParsingManager.customRules.add(ri)
        LevelledMobs.instance.rulesParsingManager.checkCustomRules()
    }

    fun getRuleNbtData(lmEntity: LivingEntityWrapper): MutableList<String> {
        val nbtData = mutableListOf<String>()

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.mobNBTData != null) {
                val nbt = ruleInfo.mobNBTData
                if (!nbt!!.doMerge) {
                    nbtData.clear()
                }

                nbtData.addAll(nbt.items)
            }
        }

        return nbtData
    }

    fun getRuleSunlightBurnIntensity(lmEntity: LivingEntityWrapper): Double {
        var result = 0.0

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.sunlightBurnAmount != null) {
                result = ruleInfo.sunlightBurnAmount!!
            }
        }

        return result
    }

    fun getRuleMaxRandomVariance(lmEntity: LivingEntityWrapper): Int? {
        var result: Int? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.maxRandomVariance != null) {
                result = ruleInfo.maxRandomVariance
            }
        }

        return result
    }

    fun getRuleCheckIfNoDropMultiplierEntitiy(
        lmEntity: LivingEntityWrapper
    ): Boolean {
        var entitiesList: CachedModalList<String>? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.conditionsNoDropEntities != null) {
                entitiesList = ruleInfo.conditionsNoDropEntities
            }
        }

        return entitiesList != null && entitiesList.isIncludedInList(
            lmEntity.nameIfBaby,
            lmEntity
        )
    }

    fun getRuleUseCustomDropsForMob(
        lmEntity: LivingEntityWrapper
    ): CustomDropsRuleSet {
        val dropRules = CustomDropsRuleSet()

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.customDropsUseForMobs != null) {
                dropRules.useDrops = ruleInfo.customDropsUseForMobs!!
            }
            if (ruleInfo.chunkKillOptions != null) {
                if (dropRules.chunkKillOptions == null) dropRules.chunkKillOptions = ruleInfo.chunkKillOptions
                else {
                    dropRules.chunkKillOptions!!.merge(ruleInfo.chunkKillOptions)
                }
            }
            dropRules.useDropTableIds.addAll(ruleInfo.customDropDropTableIds)
        }

        if (lmEntity.lockedCustomDrops != null && lmEntity.lockedCustomDrops!!.isNotEmpty()) {
            dropRules.useDropTableIds.clear()
            dropRules.useDropTableIds.addAll(lmEntity.lockedCustomDrops!!)
            dropRules.useDrops = true
        }

        if (dropRules.chunkKillOptions == null) dropRules.chunkKillOptions = ChunkKillOptions()

        if (lmEntity.hasLockedDropsOverride) dropRules.chunkKillOptions!!.disableVanillaDrops = true

        return dropRules
    }

    fun getRuleDoLockEntity(lmEntity: LivingEntityWrapper): Boolean {
        var result = false

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.lockEntity != null) {
                result = ruleInfo.lockEntity!!
            }
        }

        return result
    }

    fun getRuleIsMobAllowedInEntityOverride(
        lmInterface: LivingEntityInterface
    ): Boolean {
        // check if it should be denied thru the entity override list
        var babyMobsInheritAdultSetting = true // default
        var allowedEntitiesList: CachedModalList<String>? = null
        for (ruleInfo in lmInterface.getApplicableRules()) {
            if (ruleInfo.conditionsEntities != null) {
                allowedEntitiesList = ruleInfo.conditionsEntities
            }
            if (ruleInfo.babyMobsInheritAdultSetting != null) {
                babyMobsInheritAdultSetting = ruleInfo.babyMobsInheritAdultSetting!!
            }
        }

        return if (lmInterface is LivingEntityWrapper) {
            allowedEntitiesList == null ||
                    (!babyMobsInheritAdultSetting && lmInterface.isBabyMob
                            && isLivingEntityInModalList(allowedEntitiesList, lmInterface, true)) ||
                    isLivingEntityInModalList(
                        allowedEntitiesList, lmInterface,
                        babyMobsInheritAdultSetting
                    )
        } else {
            allowedEntitiesList == null || allowedEntitiesList.isIncludedInList(
                lmInterface.typeName, null
            )
        }
    }

    fun getFineTuningAttributes(
        lmEntity: LivingEntityWrapper
    ): FineTuningAttributes? {
        var allMobAttribs: FineTuningAttributes? = null
        var thisMobAttribs: FineTuningAttributes? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.allMobMultipliers != null) {
                val multipliers = ruleInfo.allMobMultipliers!!
                if (allMobAttribs == null || multipliers.doNotMerge) {
                    allMobAttribs = multipliers.cloneItem() as FineTuningAttributes
                    if (multipliers.doNotMerge) {
                        thisMobAttribs = null
                    }
                } else {
                    allMobAttribs.merge(multipliers)
                }
            }

            if (ruleInfo.specificMobMultipliers != null
                && ruleInfo.specificMobMultipliers!!.containsKey(lmEntity.nameIfBaby)
            ) {
                val tempAttribs = ruleInfo.specificMobMultipliers!![lmEntity.nameIfBaby]
                if (thisMobAttribs == null || tempAttribs!!.doNotMerge) {
                    thisMobAttribs = tempAttribs!!.cloneItem() as FineTuningAttributes

                    if (tempAttribs.doNotMerge) allMobAttribs = null
                    else allMobAttribs?.merge(thisMobAttribs)
                } else {
                    thisMobAttribs.merge(tempAttribs)
                }
            }
        }

        if (allMobAttribs != null) {
            if (thisMobAttribs != null)
                allMobAttribs.merge(thisMobAttribs)
            return allMobAttribs
        } else {
            return thisMobAttribs
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getRuleExternalPlugins(
        lmEntity: LivingEntityWrapper
    ): CachedModalList<String>? {
        var result: CachedModalList<String>? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.conditionsExternalPlugins != null) {
                if (result != null)
                    result.mergeCachedModal(ruleInfo.conditionsExternalPlugins!!)
                else
                    result = ruleInfo.conditionsExternalPlugins!!.clone() as CachedModalList<String>
            }
        }

        return result
    }

    fun isPlayerLevellingEnabled(): Boolean {
        for (ruleInfo in rulesInEffect) {
            if (ruleInfo.ruleIsEnabled &&
                ruleInfo.levellingStrategy.containsKey(StrategyType.PLAYER_VARIABLE)) {
                return true
            }
        }

        return false
    }

    fun getRuleCreeperMaxBlastRadius(lmEntity: LivingEntityWrapper): Int {
        var maxBlast = 5
        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.creeperMaxDamageRadius != null) {
                maxBlast = ruleInfo.creeperMaxDamageRadius!!
            }
        }

        return maxBlast
    }

    fun getRuleLevellingStrategies(
        lmEntity: LivingEntityWrapper
    ): MutableList<LevellingStrategy> {
        val strategies = mutableMapOf<StrategyType, LevellingStrategy>()

        for (ruleInfo in lmEntity.getApplicableRules()) {
            val theseStrategies = ruleInfo.levellingStrategy

            for (strategy in theseStrategies) {
                val existingStrategy = strategies[strategy.key]
                if (existingStrategy == null) {
                    strategies[strategy.key] = strategy.value.cloneItem()
                } else {
                    strategies[strategy.key]!!.mergeRule(strategy.value)
                }
            }
        }

        return strategies.values.toMutableList()
    }

    fun getRuleCustomStrategies(
        lmEntity: LivingEntityWrapper
    ): MutableList<CustomStrategy> {
        val results = mutableListOf<CustomStrategy>()
        var strategies: MutableMap<String, CustomStrategy>? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.customStrategy.isNotEmpty())
                strategies = ruleInfo.customStrategy
        }

        if (strategies != null) results.addAll(strategies.values)
        return results
    }

    fun getRuleConstructLevel(
        lmEntity: LivingEntityWrapper
    ): String?{
        var result: String? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.constructLevel == null) continue

            result = ruleInfo.constructLevel
        }

        return result
    }

    fun getRuleMobLevelInheritance(lmEntity: LivingEntityWrapper): Boolean {
        var result = true
        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.mobLevelInheritance != null) {
                result = ruleInfo.mobLevelInheritance!!
            }
        }

        return result
    }

    fun getRuleMobCustomNameStatus(
        lmEntity: LivingEntityWrapper
    ): MobCustomNameStatus {
        var result = MobCustomNameStatus.NOT_SPECIFIED

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.conditionsMobCustomnameStatus != MobCustomNameStatus.NOT_SPECIFIED) {
                result = ruleInfo.conditionsMobCustomnameStatus
            }
        }

        return result
    }

    fun getRuleMobTamedStatus(lmEntity: LivingEntityWrapper): MobTamedStatus {
        var result = MobTamedStatus.NOT_SPECIFIED

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.conditionsMobTamedStatus != MobTamedStatus.NOT_SPECIFIED) {
                result = ruleInfo.conditionsMobTamedStatus
            }
        }

        return result
    }

    fun getRuleMobMinLevel(lmInterface: LivingEntityInterface): Int {
        if (lmInterface.summonedLevel != null) {
            return lmInterface.summonedLevel!!
        }

        var minLevel = 1

        for (ruleInfo in lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictionsMinLevel != null) {
                minLevel = ruleInfo.restrictionsMinLevel!!
            }
        }

        return minLevel
    }

    fun getRuleMobMaxLevel(lmInterface: LivingEntityInterface): Int {
        var maxLevel = 0
        var firstMaxLevel = -1

        for (ruleInfo in lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictionsMaxLevel != null) {
                maxLevel = ruleInfo.restrictionsMaxLevel!!
                if (firstMaxLevel < 0 && maxLevel > 0) {
                    firstMaxLevel = maxLevel
                }
            }
        }

        if (maxLevel <= 0 && lmInterface.summonedLevel != null) {
            if (maxLevel == 0 && firstMaxLevel > 0) {
                maxLevel = firstMaxLevel
            }

            val summonedLevel = lmInterface.summonedLevel!!
            if (summonedLevel > maxLevel) {
                maxLevel = summonedLevel
            }
        }

        return maxLevel
    }

    fun getRulePlayerLevellingOptions(
        lmEntity: LivingEntityWrapper
    ): PlayerLevellingStrategy? {
        var levellingOptions: PlayerLevellingStrategy? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.levellingStrategy.containsKey(StrategyType.PLAYER_VARIABLE)) {
                val thisPL = ruleInfo.levellingStrategy[StrategyType.PLAYER_VARIABLE]!!
                if (levellingOptions == null || !levellingOptions.doMerge) levellingOptions =
                    thisPL.cloneItem() as PlayerLevellingStrategy
                else
                    levellingOptions.mergeRule(thisPL)
            }
        }

        return levellingOptions
    }

    fun getRuleNametag(lmEntity: LivingEntityWrapper): String {
        var nametag = ""
        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (!ruleInfo.nametag.isNullOrEmpty()) {
                nametag = if ("disabled".equals(ruleInfo.nametag, ignoreCase = true)) "" else ruleInfo.nametag!!
            }
        }

        return nametag
    }

    fun getRuleNametagPlaceholder(lmEntity: LivingEntityWrapper): String? {
        var nametag: String? = null
        val isLevelled = lmEntity.isLevelled

        for (ruleInfo in lmEntity.getApplicableRules()) {
            val nametagRule =
                if (isLevelled) ruleInfo.nametagPlaceholderLevelled else ruleInfo.nametagPlaceholderUnlevelled
            if (nametagRule != null) {
                nametag = nametagRule
            }
        }

        return nametag
    }

    fun getRuleNametagCreatureDeath(lmEntity: LivingEntityWrapper): String {
        var nametag = ""
        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (!ruleInfo.nametagCreatureDeath.isNullOrEmpty()) {
                nametag = ruleInfo.nametagCreatureDeath!!
            }
        }

        return nametag
    }

    fun getRuleNametagIndicator(lmEntity: LivingEntityWrapper): HealthIndicator? {
        var indicator: HealthIndicator? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.healthIndicator != null) {
                if (indicator == null || !ruleInfo.healthIndicator!!.doMerge) {
                    indicator = ruleInfo.healthIndicator!!.cloneItem() as HealthIndicator
                } else {
                    indicator.merge(ruleInfo.healthIndicator!!.cloneItem() as HealthIndicator)
                }
            }
        }

        return indicator
    }

    fun getRuleCreatureNametagVisbility(
        lmEntity: LivingEntityWrapper
    ): MutableList<NametagVisibilityEnum> {
        // TODO: cache this value
        var result: MutableList<NametagVisibilityEnum>? = null

        try {
            for (ruleInfo in lmEntity.getApplicableRules()) {
                if (ruleInfo.nametagVisibilityEnum != null) {
                    result = ruleInfo.nametagVisibilityEnum
                }
            }
        } catch (e: ConcurrentModificationException) {
//            Log.war(
//                "Got ConcurrentModificationException in getRuleCreatureNametagVisbility"
//            )
        }

        return if (result.isNullOrEmpty()) {
            mutableListOf(NametagVisibilityEnum.MELEE)
        } else {
            result
        }
    }

    fun getRuleNametagVisibleTime(lmEntity: LivingEntityWrapper): Long {
        var result = 4000L

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.nametagVisibleTime != null) {
                result = ruleInfo.nametagVisibleTime!!
            }
        }

        return result
    }

    fun getRuleTieredPlaceholder(lmEntity: LivingEntityWrapper): String? {
        var coloringInfo: MutableList<TieredColoringInfo>? = null
        var tieredText: String? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.tieredColoringInfos != null) {
                coloringInfo = ruleInfo.tieredColoringInfos
            }
        }

        if (coloringInfo == null) {
            return null
        }

        val mobLevel = lmEntity.getMobLevel
        for (info in coloringInfo) {
            if (info.isDefault) {
                tieredText = info.text
            }
            if (mobLevel >= info.minLevel && mobLevel <= info.maxLevel) {
                tieredText = info.text
                break
            }
        }

        return tieredText
    }

    fun getRulePassengerMatchLevel(lmEntity: LivingEntityWrapper): Boolean {
        var result = false

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.passengerMatchLevel != null) {
                result = ruleInfo.passengerMatchLevel!!
            }
        }

        return result
    }

    fun getRuleEntityOverriddenName(
        lmEntity: LivingEntityWrapper,
        forceCustomName: Boolean
    ): String? {
        var entityNameOverridesLevel: MutableMap<String, MutableList<LevelTierMatching>>? = null
        var entityNameOverrides: MutableMap<String, LevelTierMatching>? = null

        if (lmEntity.hasOverridenEntityName) {
            return lmEntity.overridenEntityName
        }

        for (ruleInfo in lmEntity.getApplicableRules()) {
            val doMerge =
                ruleInfo.mergeEntityNameOverrides != null && ruleInfo.mergeEntityNameOverrides!!
            if (ruleInfo.entityNameOverrides != null) {
                if (entityNameOverrides != null && doMerge) {
                    entityNameOverrides.putAll(ruleInfo.entityNameOverrides!!)
                } else {
                    entityNameOverrides = ruleInfo.entityNameOverrides
                }
            }

            if (ruleInfo.entityNameOverridesLevel != null) {
                if (entityNameOverridesLevel != null && doMerge) {
                    entityNameOverridesLevel.putAll(ruleInfo.entityNameOverridesLevel!!)
                } else {
                    entityNameOverridesLevel = ruleInfo.entityNameOverridesLevel
                }
            }
        }

        if (entityNameOverrides == null && entityNameOverridesLevel == null) {
            return null
        }

        var namesInfo: MutableList<String>? = null
        val matchedTiers = getEntityNameOverrideLevel(
            entityNameOverridesLevel,
            lmEntity
        )
        if (matchedTiers != null) {
            namesInfo = matchedTiers.names
        } else if (entityNameOverrides != null) {
            if (entityNameOverrides.containsKey("all_entities")) {
                namesInfo = entityNameOverrides["all_entities"]!!.names
            } else if (entityNameOverrides.containsKey(lmEntity.typeName)) {
                namesInfo = entityNameOverrides[lmEntity.typeName]!!.names
            }
        }

        if (namesInfo.isNullOrEmpty()) {
            return null
        } else if (namesInfo.size > 1) {
            namesInfo.shuffle()
        }

        val entityName = capitalize(lmEntity.typeName.replace("_".toRegex(), " "))
        var result = namesInfo[0]
        result = result.replace("%entity-name%", entityName)
        result = result.replace(
            "%displayname%",
            ((if (lmEntity.livingEntity.customName == null || forceCustomName) entityName else lmEntity.livingEntity.customName)!!)
        )

        if (namesInfo.size > 1) {
            // set a PDC key with the name otherwise the name will constantly change
            lmEntity.overridenEntityName = result
        }

        return result
    }

    private fun getEntityNameOverrideLevel(
        entityNameOverridesLevel: MutableMap<String, MutableList<LevelTierMatching>>?,
        lmEntity: LivingEntityWrapper
    ): LevelTierMatching? {
        if (entityNameOverridesLevel == null) {
            return null
        }

        var allEntities: LevelTierMatching? = null
        var thisMob: LevelTierMatching? = null

        for (tiers in entityNameOverridesLevel.values) {
            for (tier in tiers) {
                if (tier.isApplicableToMobLevel(lmEntity.getMobLevel)) {
                    if ("all_entities".equals(tier.mobName, ignoreCase = true)
                        && tier.isApplicableToMobLevel(lmEntity.getMobLevel)
                    ) {
                        allEntities = tier
                    } else if (lmEntity.nameIfBaby.equals(tier.mobName, ignoreCase = true)
                        && tier.isApplicableToMobLevel(lmEntity.getMobLevel)
                    ) {
                        thisMob = tier
                    }
                }
            }
        }

        return thisMob ?: allEntities
    }

    fun getSpawnerParticle(lmEntity: LivingEntityWrapper): Particle? {
        var result: Particle? = Particle.SOUL

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.spawnerParticle != null) {
                result = ruleInfo.spawnerParticle
            } else if (ruleInfo.useNoSpawnerParticles) {
                result = null
            }
        }

        return result
    }

    fun getSpawnerParticleCount(lmEntity: LivingEntityWrapper): Int {
        var result = 10

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.spawnerParticlesCount != null) {
                result = ruleInfo.spawnerParticlesCount!!
            }
        }

        // max limit of 100 counts which would take 5 seconds to show
        if (result > 100) {
            result = 100
        }

        return result
    }

    fun getAllowedVanillaBonuses(lmEntity: LivingEntityWrapper): CachedModalList<VanillaBonusEnum> {
        var result: CachedModalList<VanillaBonusEnum>? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.vanillaBonuses != null) {
                result = ruleInfo.vanillaBonuses
            }
        }

        return result ?: CachedModalList()
    }

    fun getMaximumDeathInChunkThreshold(lmEntity: LivingEntityWrapper): Int {
        var result = 0

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.maximumDeathInChunkThreshold != null) {
                result = ruleInfo.maximumDeathInChunkThreshold!!
            }
        }

        return result
    }

    fun getMaxChunkCooldownTime(lmEntity: LivingEntityWrapper): Int {
        var result = 0

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.chunkMaxCoolDownTime != null) {
                result = ruleInfo.chunkMaxCoolDownTime!!
            }
        }

        return result
    }

    fun getAdjacentChunksToCheck(lmEntity: LivingEntityWrapper): Int {
        var result = 0

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.maxAdjacentChunks != null) {
                result = ruleInfo.maxAdjacentChunks!!
            }
        }

        return result
    }

    fun getDeathMessage(lmEntity: LivingEntityWrapper): String? {
        var deathMessages: DeathMessages? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.deathMessages != null) {
                deathMessages = ruleInfo.deathMessages
            }
        }

        return deathMessages?.getDeathMessage()
    }

    fun getApplicableRules(lmInterface: LivingEntityInterface): ApplicableRulesResult {
        val applicableRules = ApplicableRulesResult()

        if (this.lastRulesCheck == null
            || Duration.between(this.lastRulesCheck, Instant.now()).toMillis() > 100
        ) {
            // check temp disabled rules every 100ms minimum
            checkTempDisabledRules()
            this.lastRulesCheck = Instant.now()
        }

        for (ruleInfo in rulesInEffect) {
            if (!ruleInfo.ruleIsEnabled || ruleInfo.isTempDisabled) {
                continue
            }

            if (lmInterface is LivingEntityWrapper && !isRuleApplicableEntity(
                    lmInterface, ruleInfo
                )
            ) {
                continue
            }

            val checkResult = isRuleApplicableInterface(
                lmInterface,
                ruleInfo
            )

            if (!checkResult.useResult) {
                if (checkResult.ruleMadeChance != null && !checkResult.ruleMadeChance!!) {
                    applicableRules.allApplicableRulesDidNotMakeChance.add(ruleInfo)
                }
                continue
            } else if (checkResult.ruleMadeChance != null && checkResult.ruleMadeChance!!) {
                applicableRules.allApplicableRulesMadeChance.add(ruleInfo)
            }

            applicableRules.allApplicableRules.add(ruleInfo)
            checkIfRuleShouldBeTempDisabled(ruleInfo, lmInterface)

            if (ruleInfo.stopProcessingRules != null) {
                val result = ruleInfo.stopProcessingRules!!
                DebugManager.log(
                    DebugType.SETTING_STOP_PROCESSING,
                    ruleInfo, lmInterface, result
                ) {
                    "rule count: &b${applicableRules.allApplicableRules.size}&r"
                }
                if (result) break
            }
        }

        var hasWorldListSpecified = false
        for (ri in applicableRules.allApplicableRules) {
            if (ri.conditionsWorlds != null && (!ri.conditionsWorlds!!.isEmpty()
                        || ri.conditionsWorlds!!.includeAll)
            ) {
                hasWorldListSpecified = true
                break
            }
        }

        return if (hasWorldListSpecified) applicableRules else ApplicableRulesResult()
    }

    private fun checkIfRuleShouldBeTempDisabled(
        ruleInfo: RuleInfo,
        lmInterface: LivingEntityInterface
    ) {
        if (lmInterface !is LivingEntityWrapper) {
            return
        }

        // don't increment the count when just checking nametags, etc
        if (!lmInterface.isNewlySpawned && !lmInterface.isRulesForceAll) {
            return
        }

        synchronized(ruleLocker) {
            if (!rulesCooldown.containsKey(ruleInfo.ruleName)) {
                rulesCooldown[ruleInfo.ruleName] = mutableListOf()
            }
            val instants: MutableList<Instant>? = rulesCooldown[ruleInfo.ruleName]
            instants!!.add(Instant.now())
            if (ruleInfo.conditionsTimesToCooldownActivation == null
                || instants.size >= ruleInfo.conditionsTimesToCooldownActivation!!
            ) {
                if (ruleInfo.conditionsCooldownTime == null
                    || ruleInfo.conditionsCooldownTime!! <= 0
                ) {
                    return
                }
                DebugManager.log(DebugType.SETTING_COOLDOWN, ruleInfo, lmInterface,true) {
                    "cooldown reached, disabling rule"
                }
                ruleInfo.isTempDisabled = true
            }
        }
    }

    private fun isRuleApplicableEntity(
        lmEntity: LivingEntityWrapper,
        ri: RuleInfo
    ): Boolean {
        if (ri.conditionsMinLevel != null) {
            val result = (lmEntity.isLevelled &&
                    lmEntity.getMobLevel >= ri.conditionsMinLevel!!)

            DebugManager.log(
                DebugType.CONDITION_MAXLEVEL, ri, lmEntity, result
            ) {
                "mob lvl: &b${lmEntity.getMobLevel}&7, rule minlvl: &b${ri.conditionsMinLevel}&7"
            }
            if (!result) return false
        }

        if (ri.conditionsMaxLevel != null) {
            val result = (lmEntity.isLevelled &&
                    lmEntity.getMobLevel <= ri.conditionsMaxLevel!!)
            DebugManager.log(
                DebugType.CONDITION_MAXLEVEL, ri, lmEntity, result
            ) {
                "mob lvl: &b${lmEntity.getMobLevel}&7, rule maxlvl: &b${ri.conditionsMaxLevel}&7"
            }
            if (!result) return false
        }

        if (ri.conditionsWithinCoords != null && !ri.conditionsWithinCoords!!.isEmpty &&
            !meetsMaxDistanceCriteria(lmEntity, ri)
        ) {
            // debug entries are inside the last function
            return false
        }

        if (ri.conditionsCustomNames != null) {
            val customName = if (lmEntity.livingEntity.customName != null) lmEntity.livingEntity.customName!!
                .replace("§", "&") else "(none)"

            val result = ri.conditionsCustomNames!!.isIncludedInList(customName, lmEntity)

            DebugManager.log(
                DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, result
            ) {
                "name: &b$customName&7"
            }

            if (!result) return false
        }

        if (ri.conditionsSpawnReasons != null) {
            val result = ri.conditionsSpawnReasons!!.isIncludedInList(
                lmEntity.spawnReason, lmEntity
            )
            DebugManager.log(
                DebugType.CONDITION_SPAWN_REASON, ri, lmEntity, result
            ) {
                "spawn reason: &b${lmEntity.spawnReason}&7"
            }
            if (!result) return false
        }

        if (ri.conditionsExternalPlugins != null) {
            ExternalCompatibilityManager.updateAllExternalCompats(lmEntity)
            val mobCompats = lmEntity.mobExternalTypes
            //if (!lmEntity.isMobOfExternalType) mobCompats.add("NOT-APPLICABLE")

            var madeIt = false
            for (compat in mobCompats) {
                if (ri.conditionsExternalPlugins!!.isIncludedInList(compat, lmEntity)) {
                    madeIt = true
                    break
                }
            }

            DebugManager.log(
                DebugType.CONDITION_PLUGIN_COMPAT, ri, lmEntity, madeIt
            ) { "mob plugins: &b$mobCompats&7" }
            if (!madeIt) return false
        }

        if (ri.conditionsMMnames != null) {
            var mmName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity)
            if (mmName.isEmpty()) {
                mmName = "(none)"
            }

            val result = ri.conditionsMMnames!!.isIncludedInList(mmName, lmEntity)
            val mmNameFinal = mmName
            DebugManager.log(
                DebugType.CONDITION_MYTHICMOBS_INTERNAL_NAME, ri, lmEntity, result
            ) {
                "mm_name: &b$mmNameFinal&7"
            }

            if (!result) return false
        }

        if (ri.conditionsSpawnerNames != null) {
            val checkName = if (lmEntity.sourceSpawnerName != null) lmEntity.sourceSpawnerName else "(none)"

            val result = ri.conditionsSpawnerNames!!.isIncludedInList(checkName!!, lmEntity)
            DebugManager.log(
                DebugType.CONDITION_SPAWNER_NAME, ri, lmEntity, result
            ) {
                "spawner: &b$checkName&7"
            }

            if (!result) return false
        }

        if (ri.conditionsSpawnegEggNames != null) {
            val checkName = if (lmEntity.sourceSpawnEggName != null) lmEntity.sourceSpawnEggName else "(none)"

            val result = ri.conditionsSpawnegEggNames!!.isIncludedInList(checkName!!, lmEntity)
            DebugManager.log(
                DebugType.CONDITION_SPAWNER_NAME, ri, lmEntity, result
            ) {
                "spawn_egg: &b$checkName&7"
            }

            if (!result) return false
        }

        if (ri.conditionsPermission != null) {
            if (lmEntity.associatedPlayer == null) {
                DebugManager.log(
                    DebugType.CONDITION_PERMISSION, ri, lmEntity, false
                ) {
                    "no player was provided"
                }
                return false
            }

            if (!doesPlayerPassPermissionChecks(
                    ri.conditionsPermission!!,
                    lmEntity.associatedPlayer!!
                )
            ) {
                DebugManager.log(
                    DebugType.CONDITION_PERMISSION, ri, lmEntity, false
                ) {
                    "player: &b${lmEntity.associatedPlayer!!.name}&7, permission denied"
                }
                return false
            }

            DebugManager.log(
                DebugType.CONDITION_PERMISSION, ri, lmEntity, true
            ) {
                "player: &b${lmEntity.associatedPlayer!!.name}&7, permission granted"
            }
        }

        if (ri.conditionsMobCustomnameStatus != MobCustomNameStatus.NOT_SPECIFIED
            && ri.conditionsMobCustomnameStatus != MobCustomNameStatus.EITHER
        ) {
            val hasCustomName = lmEntity.livingEntity.customName != null

            if (hasCustomName && ri.conditionsMobCustomnameStatus == MobCustomNameStatus.NOT_NAMETAGGED ||
                !hasCustomName && ri.conditionsMobCustomnameStatus == MobCustomNameStatus.NAMETAGGED
            ) {
                DebugManager.log(
                    DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, false
                ) {
                    "nametag: ${lmEntity.livingEntity.customName}"
                }
                return false
            }

            DebugManager.log(
                DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, true
            ) {
                "nametag: ${lmEntity.livingEntity.customName}"
            }
        }

        if (ri.conditionsMobTamedStatus != MobTamedStatus.NOT_SPECIFIED
            && ri.conditionsMobTamedStatus != MobTamedStatus.EITHER
        ) {
            if (lmEntity.isMobTamed && ri.conditionsMobTamedStatus == MobTamedStatus.NOT_TAMED ||
                !lmEntity.isMobTamed && ri.conditionsMobTamedStatus == MobTamedStatus.TAMED
            ) {
                DebugManager.log(
                    DebugType.ENTITY_TAME, ri, lmEntity, false
                ) {
                    "tamed: ${lmEntity.isMobTamed}"
                }
                return false
            }

            DebugManager.log(
                DebugType.ENTITY_TAME, ri, lmEntity, true
            ) {
                "tamed: ${lmEntity.isMobTamed}"
            }
        }

        if (ri.conditionsScoreboardTags != null) {
            val tags = lmEntity.livingEntity.scoreboardTags
            if (tags.isEmpty()) {
                tags.add("(none)")
            }

            var madeCriteria = false
            for (tag in tags) {
                if (ri.conditionsScoreboardTags!!.isIncludedInList(tag, lmEntity)) {
                    madeCriteria = true
                }
            }

            DebugManager.log(
                DebugType.SCOREBOARD_TAGS, ri, lmEntity, madeCriteria
            ) { "" }

            if (!madeCriteria) return false
        }

        if (ri.conditionsStructure != null){
            val structures = lmEntity.location.chunk.structures
            var madeCriteria = false
            for (structure in structures){
                if (ri.conditionsStructure!!.isIncludedInList(structure.structure, lmEntity)){
                    madeCriteria = true
                    break
                }
            }

            DebugManager.log(
                DebugType.CONDITION_STRUCTURES, ri, lmEntity, madeCriteria
            ) { "" }

            if (!madeCriteria) return false
        }

        if (ri.conditionsSkyLightLevel != null) {
            val lightLevel = lmEntity.skylightLevel
            val result = (lightLevel >= ri.conditionsSkyLightLevel!!.min
                    && lightLevel <= ri.conditionsSkyLightLevel!!.max)
            DebugManager.log(
                DebugType.SKYLIGHT_LEVEL, ri, lmEntity, result
            ) {
                "skylight: $lightLevel, criteria: ${ri.conditionsSkyLightLevel}"
            }
            return result
        }

        return true
    }

    private fun meetsMaxDistanceCriteria(
        lmEntity: LivingEntityWrapper,
        rule: RuleInfo
    ): Boolean {
        val mdr = rule.conditionsWithinCoords!!

        if (mdr.getHasX && !mdr.isLocationWithinRange(lmEntity.location.blockX, WithinCoordinates.Axis.X)) {
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false) {
                "xCoord: ${lmEntity.location.blockX}, startX: ${mdr.startX}, endX: ${mdr.endX}"
            }
            return false
        }

        if (mdr.getHasY && !mdr.isLocationWithinRange(lmEntity.location.blockY, WithinCoordinates.Axis.Y)) {
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false) {
                "yCoord: ${lmEntity.location.blockY}, startY: ${mdr.startY}, endY: ${mdr.endY}"
            }
            return false
        }

        if (mdr.getHasZ && !mdr.isLocationWithinRange(lmEntity.location.blockZ, WithinCoordinates.Axis.Z)) {
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false) {
                "zCoord: ${lmEntity.location.blockZ}, startZ: ${mdr.startZ}, endZ: ${mdr.endZ}"
            }
            return false
        }

        DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, true) {
            "zCoord: ${lmEntity.location.blockZ}, startZ: ${mdr.startZ}, endZ: ${mdr.endZ}"
        }

        return true
    }

    private fun isRuleApplicableInterface(
        lmInterface: LivingEntityInterface,
        ri: RuleInfo
    ): RuleCheckResult {
        if (ri.conditionsEntities != null) {
            if (lmInterface is LivingEntityWrapper) {
                val result = isLivingEntityInModalList(
                    ri.conditionsEntities!!, lmInterface, true
                )
                DebugManager.log(
                    DebugType.CONDITION_ENTITIES_LIST, ri, lmInterface, result
                ) { "" }

                if (!result) return RuleCheckResult(false)
            } else {
                // can't check groups if not a living entity wrapper
                val result = ri.conditionsEntities!!.isIncludedInList(
                    lmInterface.typeName, null
                )

                DebugManager.log(
                    DebugType.CONDITION_ENTITIES_LIST, ri, lmInterface, result
                ) { "" }

                if (!result) return RuleCheckResult(false)
            }
        }

        if (ri.conditionsWorlds != null) {
            val result = (lmInterface.wasSummoned ||
                    ri.conditionsWorlds!!.isIncludedInList(lmInterface.world!!.name, null))
            DebugManager.log(
                DebugType.CONDITION_WORLD_LIST, ri, lmInterface, result
            ) {
                "mob world: &b${lmInterface.world!!.name}&7"
            }
            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditionsBiomes != null) {
            val result = isBiomeInModalList(
                ri.conditionsBiomes!!,
                lmInterface.location!!.block.biome, LevelledMobs.instance.rulesManager
            )
            DebugManager.log(
                DebugType.CONDITION_BIOME_LIST, ri, lmInterface, result
            ) {
                "mob biome: &b${lmInterface.location!!.block.biome.name}&7"
            }
            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditionsWGregions != null
            && ExternalCompatibilityManager.hasWorldGuardInstalled
        ) {
            var isInList = false
            val wgRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(
                lmInterface
            )
            if (wgRegions.isEmpty()) {
                wgRegions.add("(none)")
            }

            for (regionName in wgRegions) {
                if (ri.conditionsWGregions!!.isIncludedInList(regionName, null)) {
                    isInList = true
                    break
                }
            }

            DebugManager.log(
                DebugType.CONDITION_WG_REGION, ri, lmInterface, isInList
            ) {
                "wg_regions: &b$wgRegions&7"
            }
            if (!isInList) return RuleCheckResult(false)
        }

        if (ri.conditionsWGregionOwners != null
            && ExternalCompatibilityManager.hasWorldGuardInstalled
        ) {
            var isInList = false
            val wgRegionOwners = getWorldGuardRegionOwnersForLocation(
                lmInterface
            )
            if (wgRegionOwners.isEmpty()) {
                wgRegionOwners.add("(none)")
            }

            for (ownerName in wgRegionOwners) {
                if (ri.conditionsWGregionOwners!!.isIncludedInList(ownerName, null)) {
                    isInList = true
                    break
                }
            }

            DebugManager.log(
                DebugType.CONDITION_WG_REGION_OWNER, ri, lmInterface, isInList
            ) {
                "wg_owners: &b$wgRegionOwners&7"
            }

            if (!isInList) return RuleCheckResult(false)
        }

        if (ri.conditionsApplyAboveY != null) {
            val result = lmInterface.location!!.blockY > ri.conditionsApplyAboveY!!
            DebugManager.log(
                DebugType.CONDITION_Y_LEVEL, ri, lmInterface, result
            ) {
                "y-level: &b${lmInterface.location!!.blockY}&7, max-y: &b${ri.conditionsApplyAboveY}&7"
            }
            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditionsApplyBelowY != null) {
            val result = lmInterface.location!!.blockY < ri.conditionsApplyBelowY!!
            DebugManager.log(
                DebugType.CONDITION_Y_LEVEL, ri, lmInterface, result
            ) {
                "y-level: &b${lmInterface.location!!.blockY}&7, min-y: &b${ri.conditionsApplyBelowY}&7"
            }
            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditionsMinDistanceFromSpawn != null) {
            val result = lmInterface.distanceFromSpawn >= ri.conditionsMinDistanceFromSpawn!!
            DebugManager.log(
                DebugType.CONDITION_MIN_SPAWN_DISTANCE, ri, lmInterface, result
            ) {
                "spawn-distance: &b${round(lmInterface.distanceFromSpawn)}&7, min-sd: &b${ri.conditionsMinDistanceFromSpawn}&7"
            }

            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditionsMaxDistanceFromSpawn != null) {
            val result = lmInterface.distanceFromSpawn <= ri.conditionsMaxDistanceFromSpawn!!
            DebugManager.log(
                DebugType.CONDITION_MAX_SPAWN_DISTANCE, ri, lmInterface, result
            ) {
                "spawn-distance: &b${round(lmInterface.distanceFromSpawn)}&7, min-sd: &b${ri.conditionsMaxDistanceFromSpawn}&7"
            }

            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditionsWorldTickTime != null) {
            val currentWorldTickTime = lmInterface.spawnedTimeOfDay
            val result = isIntegerInModalList(ri.conditionsWorldTickTime!!, currentWorldTickTime)
            DebugManager.log(
                DebugType.CONDITION_WORLD_TIME_TICK, ri, lmInterface, result
            ) {
                "tick time: &b$currentWorldTickTime&7"
            }

            if (!result) return RuleCheckResult(useResult = false, ruleMadeChance = false)
        }

        var ruleMadeChance: Boolean? = null

        if (ri.conditionsChance != null && ri.conditionsChance!! < 1.0) {
            if (lmInterface is LivingEntityWrapper) {
                // find out if this entity previously lost or won the chance previously and use that result if present
                val prevChanceResults = lmInterface.prevChanceRuleResults
                if (prevChanceResults != null && prevChanceResults.containsKey(ri.ruleName)) {
                    val prevResult = prevChanceResults[ri.ruleName]!!
                    return RuleCheckResult(prevResult)
                }
            }

            val chanceRole =
                ThreadLocalRandom.current().nextInt(0, 100001).toFloat() * 0.00001f
            val result = chanceRole >= (1.0f - ri.conditionsChance!!)
            DebugManager.log(
                DebugType.CONDITION_CHANCE, ri, lmInterface, result
            ) {
                "chance: &b${ri.conditionsChance}&7, chance role: &b${round(chanceRole.toDouble(), 4)}&7"
            }

            if (!result) return RuleCheckResult(useResult = false, ruleMadeChance = false)

            ruleMadeChance = true
        }

        return RuleCheckResult(true, ruleMadeChance)
    }

    private fun doesPlayerPassPermissionChecks(
        perms: CachedModalList<String>,
        player: Player
    ): Boolean {
        if (perms.includeAll) {
            return true
        }
        if (perms.excludeAll) {
            return false
        }
        if (perms.isEmpty()) {
            return true
        }

        for (perm in perms.excludedList) {
            val permCheck = "levelledmobs.permission.$perm"
            if (player.hasPermission(permCheck)) {
                return false
            }
        }

        for (perm in perms.includedList) {
            val permCheck = "levelledmobs.permission.$perm"
            if (player.hasPermission(permCheck)) {
                return true
            }
        }

        return perms.isBlacklist
    }

    fun buildBiomeGroupMappings(
        customBiomeGroups: MutableMap<String, MutableSet<String>>?
    ) {
        biomeGroupMappings.clear()

        if (customBiomeGroups == null) {
            return
        }

        for ((key, groupMembers) in customBiomeGroups) {
            val newList: MutableList<String> = ArrayList(groupMembers.size)
            newList.addAll(groupMembers)
            biomeGroupMappings[key] = newList
        }
    }

    fun clearTempDisabledRulesCounts() {
        synchronized(ruleLocker) {
            rulesCooldown.clear()
        }
    }

    private fun checkTempDisabledRules() {
        synchronized(ruleLocker) {
            if (rulesCooldown.isEmpty()) {
                return
            }
            val iterator =
                rulesCooldown.keys.iterator()
            while (iterator.hasNext()) {
                val ruleName = iterator.next()
                val rule = ruleNameMappings[ruleName]
                if (rule?.conditionsCooldownTime == null || rule.conditionsCooldownTime!! <= 0
                ) {
                    if (rule != null) {
                        rule.isTempDisabled = false
                    }
                    iterator.remove()
                    continue
                }

                val instants: MutableList<Instant>? = rulesCooldown[ruleName]
                val preCount = instants!!.size
                if (instants.removeIf { k: Instant? ->
                        (Duration.between(k, Instant.now()).toMillis()
                                > rule.conditionsCooldownTime!!)
                    }) {
                    DebugManager.log(DebugType.SETTING_COOLDOWN) {
                        "removed cooldown entries, pre: $preCount, post: ${instants.size}"
                    }
                    if (instants.isEmpty()) {
                        rule.isTempDisabled = false
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun showTempDisabledRules(sender: CommandSender) {
        synchronized(ruleLocker) {
            if (rulesCooldown.isEmpty()) {
                sender.sendMessage("No rules are currently temporarily disabled")
                return
            }
            checkTempDisabledRules()

            val sb = StringBuilder()
            sb.append(" ${rulesCooldown.size} rule(s) currently disabled:")

            for (ruleName in rulesCooldown.keys) {
                val rule = ruleNameMappings[ruleName]
                if (rule?.conditionsCooldownTime == null) {
                    continue
                }
                sb.append(System.lineSeparator())

                sb.append(ruleName)
                sb.append(": seconds left: ")
                val instant = rulesCooldown[ruleName]!![0]
                val millisecondsSince = Duration.between(instant, Instant.now()).toMillis()
                val duration = Duration.ofMillis(
                    rule.conditionsCooldownTime!! - millisecondsSince
                )
                sb.append(duration.toSeconds())
            }

            sender.sendMessage(sb.toString())
        }
    }

    fun updateRulesHash() {
        val sb = StringBuilder()

        synchronized(ruleLocker) {
            for (rule in rulesInEffect) {
                if (!rule.ruleIsEnabled) {
                    continue
                }
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append(rule.formatRulesVisually(true, mutableListOf("id")))
            }
        }

        try {
            val digest = MessageDigest.getInstance("SHA3-256")
            val hashbytes = digest.digest(
                sb.toString().toByteArray(StandardCharsets.UTF_8)
            )
            this.currentRulesHash = bytesToHex(hashbytes)
        } catch (e: NoSuchAlgorithmException) {
            Log.war("Unable to run SHA-256 hash: " + e.message)
            this.currentRulesHash = "1234"
        }
    }

    companion object{
        @JvmStatic
        lateinit var instance: RulesManager
            private set

        val ruleLocker = Any()

        // taken from https://www.baeldung.com/sha-256-hashing-java
        private fun bytesToHex(hash: ByteArray): String {
            val hexString = StringBuilder(2 * hash.size)
            for (b in hash) {
                val hex = Integer.toHexString(0xff and b.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            return hexString.toString()
        }
    }
}