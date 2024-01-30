package io.github.arcaneplugins.levelledmobs.rules

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Duration
import java.time.Instant
import java.util.LinkedList
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.ThreadLocalRandom
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager.ExternalCompatibility
import io.github.arcaneplugins.levelledmobs.managers.WorldGuardIntegration.getWorldGuardRegionOwnersForLocation
import io.github.arcaneplugins.levelledmobs.misc.CachedModalList
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.enums.MobCustomNameStatus
import io.github.arcaneplugins.levelledmobs.enums.MobTamedStatus
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.enums.VanillaBonusEnum
import io.github.arcaneplugins.levelledmobs.result.RuleCheckResult
import io.github.arcaneplugins.levelledmobs.rules.strategies.LevellingStrategy
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.util.Utils.capitalize
import io.github.arcaneplugins.levelledmobs.util.Utils.isBiomeInModalList
import io.github.arcaneplugins.levelledmobs.util.Utils.isIntegerInModalList
import io.github.arcaneplugins.levelledmobs.util.Utils.isLivingEntityInModalList
import io.github.arcaneplugins.levelledmobs.util.Utils.round
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Player

/**
 * Manages all rules that are parsed from rules.yml and applied to various defined mobs
 *
 * @author stumper66
 * @since 3.0.0
 */
@Suppress("DEPRECATION")
class RulesManager {
    val rulesInEffect: SortedMap<Int, MutableList<RuleInfo>> = TreeMap()
    val ruleNameMappings: MutableMap<String, RuleInfo> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    val biomeGroupMappings: MutableMap<String, MutableList<String>> = TreeMap(String.CASE_INSENSITIVE_ORDER)
    val rulesCooldown = mutableMapOf<String, MutableList<Instant>>()
    var anyRuleHasChance = false
    var hasAnyWGCondition = false
    private var lastRulesCheck: Instant? = null
    var currentRulesHash = ""
        private set

    fun getRuleIsWorldAllowedInAnyRule(world: World?): Boolean {
        if (world == null) {
            return false
        }
        var result = false

        for (ruleInfo in LevelledMobs.instance.rulesParsingManager.getAllRules()) {
            if (!ruleInfo.ruleIsEnabled) {
                continue
            }
            if (ruleInfo.conditions_Worlds != null && ruleInfo.conditions_Worlds!!.isEnabledInList(
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
            if (ruleInfo.mobNBT_Data != null) {
                val nbt = ruleInfo.mobNBT_Data
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
            if (ruleInfo.conditions_NoDropEntities != null) {
                entitiesList = ruleInfo.conditions_NoDropEntities
            }
        }

        return entitiesList != null && entitiesList.isEnabledInList(
            lmEntity.nameIfBaby,
            lmEntity
        )
    }

    fun getRuleUseCustomDropsForMob(
        lmEntity: LivingEntityWrapper
    ): CustomDropsRuleSet {
        val dropRules = CustomDropsRuleSet()

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.customDrops_UseForMobs != null) {
                dropRules.useDrops = ruleInfo.customDrops_UseForMobs!!
            }
            if (ruleInfo.chunkKillOptions != null) {
                if (dropRules.chunkKillOptions == null) dropRules.chunkKillOptions = ruleInfo.chunkKillOptions
                else {
                    dropRules.chunkKillOptions!!.merge(ruleInfo.chunkKillOptions)
                }
            }
            dropRules.useDropTableIds.addAll(ruleInfo.customDrop_DropTableIds)
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
            if (ruleInfo.allowedEntities != null) {
                allowedEntitiesList = ruleInfo.allowedEntities
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
            allowedEntitiesList == null || allowedEntitiesList.isEnabledInList(
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

    fun getRuleExternalCompatibility(
        lmEntity: LivingEntityWrapper
    ): MutableMap<ExternalCompatibility, Boolean> {
        val result = mutableMapOf<ExternalCompatibility, Boolean>()

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.enabledExtCompats != null) {
                result.putAll(ruleInfo.enabledExtCompats!!)
            }
        }

        return result
    }

    fun isPlayerLevellingEnabled(): Boolean {
        for (rules in rulesInEffect.values) {
            if (rules == null) {
                continue
            }

            for (ruleInfo in rules) {
                if (ruleInfo.ruleIsEnabled && ruleInfo.playerLevellingOptions != null) {
                    return true
                }
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

    fun getRuleLevellingStrategy(
        lmEntity: LivingEntityWrapper
    ): LevellingStrategy? {
        var levellingStrategy: LevellingStrategy? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.levellingStrategy == null) continue

            if (levellingStrategy != null && (levellingStrategy.javaClass
                        == ruleInfo.levellingStrategy!!.javaClass)
            ) {
                levellingStrategy.mergeRule(ruleInfo.levellingStrategy!!)
            } else {
                levellingStrategy = ruleInfo.levellingStrategy!!.cloneItem()
            }
        }

        return levellingStrategy
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
            if (ruleInfo.conditions_MobCustomnameStatus != MobCustomNameStatus.NOT_SPECIFIED) {
                result = ruleInfo.conditions_MobCustomnameStatus
            }
        }

        return result
    }

    fun getRuleMobTamedStatus(lmEntity: LivingEntityWrapper): MobTamedStatus {
        var result = MobTamedStatus.NOT_SPECIFIED

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.conditions_MobTamedStatus != MobTamedStatus.NOT_SPECIFIED) {
                result = ruleInfo.conditions_MobTamedStatus
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
            if (ruleInfo.restrictions_MinLevel != null) {
                minLevel = ruleInfo.restrictions_MinLevel!!
            }
        }

        return minLevel
    }

    fun getRuleMobMaxLevel(lmInterface: LivingEntityInterface): Int {
        var maxLevel = 0
        var firstMaxLevel = -1

        for (ruleInfo in lmInterface.getApplicableRules()) {
            if (ruleInfo.restrictions_MaxLevel != null) {
                maxLevel = ruleInfo.restrictions_MaxLevel!!
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
    ): PlayerLevellingOptions? {
        var levellingOptions: PlayerLevellingOptions? = null

        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (ruleInfo.playerLevellingOptions != null) {
                if (levellingOptions == null || !levellingOptions.doMerge) levellingOptions =
                    ruleInfo.playerLevellingOptions!!.cloneItem()
                else levellingOptions.mergeRule(ruleInfo.playerLevellingOptions)
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
                if (isLevelled) ruleInfo.nametag_Placeholder_Levelled else ruleInfo.nametag_Placeholder_Unlevelled
            if (nametagRule != null) {
                nametag = nametagRule
            }
        }

        return nametag
    }

    fun getRuleNametagCreatureDeath(lmEntity: LivingEntityWrapper): String {
        var nametag = ""
        for (ruleInfo in lmEntity.getApplicableRules()) {
            if (!ruleInfo.nametag_CreatureDeath.isNullOrEmpty()) {
                nametag = ruleInfo.nametag_CreatureDeath!!
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
    ): List<NametagVisibilityEnum?> {
        var result: List<NametagVisibilityEnum>? = null

        try {
            for (ruleInfo in lmEntity.getApplicableRules()) {
                if (ruleInfo.nametagVisibilityEnum != null) {
                    result = ruleInfo.nametagVisibilityEnum
                }
            }
        } catch (e: ConcurrentModificationException) {
            Utils.logger.info(
                "Got ConcurrentModificationException in getRule_CreatureNametagVisbility"
            )
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

        val mobLevel = lmEntity.getMobLevel()
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
        var entityNameOverrides_Level: MutableMap<String, MutableList<LevelTierMatching>>? = null
        var entityNameOverrides: MutableMap<String, LevelTierMatching>? = null

        if (lmEntity.hasOverridenEntityName) {
            return lmEntity.getOverridenEntityName
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

            if (ruleInfo.entityNameOverrides_Level != null) {
                if (entityNameOverrides_Level != null && doMerge) {
                    entityNameOverrides_Level.putAll(ruleInfo.entityNameOverrides_Level!!)
                } else {
                    entityNameOverrides_Level = ruleInfo.entityNameOverrides_Level
                }
            }
        }

        if (entityNameOverrides == null && entityNameOverrides_Level == null) {
            return null
        }

        var namesInfo: MutableList<String>? = null
        val matchedTiers = getEntityNameOverrideLevel(
            entityNameOverrides_Level,
            lmEntity
        )
        if (matchedTiers != null) {
            namesInfo = matchedTiers.names
        } else if (entityNameOverrides != null) {
            if (entityNameOverrides.containsKey("all_entities")) {
                namesInfo = entityNameOverrides["all_entities"]!!.names
            } else if (entityNameOverrides.containsKey(lmEntity.nameIfBaby)) {
                namesInfo = entityNameOverrides[lmEntity.nameIfBaby]!!.names
            }
        }

        if (namesInfo.isNullOrEmpty()) {
            return null
        } else if (namesInfo.size > 1) {
            namesInfo.shuffle()
        }

        val useCustomNameForNametags = LevelledMobs.instance.helperSettings.getBoolean(
            LevelledMobs.instance.settingsCfg, "use-customname-for-mob-nametags"
        )
        val entityName = capitalize(lmEntity.nameIfBaby.replace("_".toRegex(), " "))
        var result = namesInfo[0]
        result = result.replace("%entity-name%", entityName)
        result = result.replace(
            "%displayname%",
            ((if (lmEntity.livingEntity.customName == null || forceCustomName || useCustomNameForNametags) entityName else lmEntity.livingEntity.customName)!!)
        )

        if (namesInfo.size > 1) {
            // set a PDC key with the name otherwise the name will constantly change
            lmEntity.setOverridenEntityName(result)
        }

        return result
    }

    private fun getEntityNameOverrideLevel(
        entityNameOverrides_Level: MutableMap<String, MutableList<LevelTierMatching>>?,
        lmEntity: LivingEntityWrapper
    ): LevelTierMatching? {
        if (entityNameOverrides_Level == null) {
            return null
        }

        var allEntities: LevelTierMatching? = null
        var thisMob: LevelTierMatching? = null

        for (tiers in entityNameOverrides_Level.values) {
            for (tier in tiers) {
                if (tier.isApplicableToMobLevel(lmEntity.getMobLevel())) {
                    if ("all_entities".equals(tier.mobName, ignoreCase = true)
                        && tier.isApplicableToMobLevel(lmEntity.getMobLevel())
                    ) {
                        allEntities = tier
                    } else if (lmEntity.nameIfBaby.equals(tier.mobName, ignoreCase = true)
                        && tier.isApplicableToMobLevel(lmEntity.getMobLevel())
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

        for (rules in rulesInEffect.values) {
            for (ruleInfo in rules) {
                if (!ruleInfo.ruleIsEnabled || ruleInfo.isTempDisabled) {
                    continue
                }

                if (lmInterface is LivingEntityWrapper && !isRuleApplicableEntity(
                        lmInterface, ruleInfo
                    )
                ) {
                    continue
                }

                val checkResult: RuleCheckResult = isRuleApplicableInterface(
                    lmInterface,
                    ruleInfo
                )
                if (!checkResult.useResult) {
                    if (checkResult.ruleMadeChance != null && !checkResult.ruleMadeChance!!) {
                        applicableRules.allApplicableRules_DidNotMakeChance.add(ruleInfo)
                    }
                    continue
                } else if (checkResult.ruleMadeChance != null && checkResult.ruleMadeChance!!) {
                    applicableRules.allApplicableRules_MadeChance.add(ruleInfo)
                }

                applicableRules.allApplicableRules.add(ruleInfo)
                checkIfRuleShouldBeTempDisabled(ruleInfo, lmInterface)

                if (ruleInfo.stopProcessingRules != null) {
                    val result = ruleInfo.stopProcessingRules!!
                    DebugManager.log(
                        DebugType.SETTING_STOP_PROCESSING,
                        ruleInfo, lmInterface, result
                    ) {
                        String.format(
                            "&b%s&7, mob: &b%s&7, rule count: &b%s",
                            ruleInfo.ruleName, lmInterface.typeName,
                            applicableRules.allApplicableRules.size
                        )
                    }
                    if (!result) break
                }
            }
        }

        var hasWorldListSpecified = false
        for (ri in applicableRules.allApplicableRules) {
            if (ri.conditions_Worlds != null && (!ri.conditions_Worlds!!.isEmpty()
                        || ri.conditions_Worlds!!.allowAll)
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
                rulesCooldown[ruleInfo.ruleName] = LinkedList()
            }
            val instants: MutableList<Instant>? = rulesCooldown[ruleInfo.ruleName]
            instants!!.add(Instant.now())
            if (ruleInfo.conditions_TimesToCooldownActivation == null
                || instants.size >= ruleInfo.conditions_TimesToCooldownActivation!!
            ) {
                if (ruleInfo.conditions_CooldownTime == null
                    || ruleInfo.conditions_CooldownTime!! <= 0
                ) {
                    return
                }
                DebugManager.log(DebugType.SETTING_COOLDOWN) { ruleInfo.ruleName + ": cooldown reached, disabling rule" }
                ruleInfo.isTempDisabled = true
            }
        }
    }

    private fun isRuleApplicableEntity(
        lmEntity: LivingEntityWrapper,
        ri: RuleInfo
    ): Boolean {
        if (ri.conditions_MinLevel != null) {
            val result = (lmEntity.isLevelled &&
                    lmEntity.getMobLevel() >= ri.conditions_MinLevel!!)

            DebugManager.log(
                DebugType.CONDITION_MAXLEVEL, ri, lmEntity, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, mob lvl: &b%s&7, rule minlvl: &b%s&7",
                    ri.ruleName, lmEntity.typeName, lmEntity.getMobLevel(),
                    ri.conditions_MinLevel
                )
            }
            if (!result) return false
        }

        if (ri.conditions_MaxLevel != null) {
            val result = (lmEntity.isLevelled &&
                    lmEntity.getMobLevel() <= ri.conditions_MaxLevel!!)
            DebugManager.log(
                DebugType.CONDITION_MAXLEVEL, ri, lmEntity, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, mob lvl: &b%s&7, rule maxlvl: &b%s&7",
                    ri.ruleName, lmEntity.typeName, lmEntity.getMobLevel(),
                    ri.conditions_MaxLevel
                )
            }
            if (!result) return false
        }

        if (ri.conditions_WithinCoords != null && !ri.conditions_WithinCoords!!.isEmpty &&
            !meetsMaxDistanceCriteria(lmEntity, ri)
        ) {
            // debug entries are inside the last function
            return false
        }

        if (ri.conditions_CustomNames != null) {
            val customName = if (lmEntity.livingEntity.customName != null) lmEntity.livingEntity.customName!!
                .replace("§", "&") else "(none)"

            val result = ri.conditions_CustomNames!!.isEnabledInList(customName, lmEntity)

            DebugManager.log(
                DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, name: &b%s&7",
                    ri.ruleName, lmEntity.typeName, customName
                )
            }

            if (!result) return false
        }

        if (ri.conditions_SpawnReasons != null) {
            val result = ri.conditions_SpawnReasons!!.isEnabledInList(
                lmEntity.getSpawnReason(), lmEntity
            )
            DebugManager.log(
                DebugType.CONDITION_SPAWN_REASON, ri, lmEntity, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, spawn reason: &b%s&7",
                    ri.ruleName, lmEntity.typeName, lmEntity.getSpawnReason()
                )
            }
            if (!result) return false
        }

        if (ri.conditions_ApplyPlugins != null) {
            ExternalCompatibilityManager.updateAllExternalCompats(lmEntity)
            val mobCompats = lmEntity.mobExternalTypes
            if (!lmEntity.isMobOfExternalType) mobCompats.add(ExternalCompatibility.NOT_APPLICABLE)

            var madeIt = false
            for (compat in mobCompats) {
                if (ri.conditions_ApplyPlugins!!.isEnabledInList(compat.name, lmEntity)) {
                    madeIt = true
                    break
                }
            }

            DebugManager.log(
                DebugType.CONDITION_PLUGIN_COMPAT, ri, lmEntity, madeIt
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, mob plugins: &b%s&7",
                    ri.ruleName, lmEntity.nameIfBaby, mobCompats
                )
            }
            if (!madeIt) return false
        }

        if (ri.conditions_MM_Names != null) {
            var mmName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity)
            if (mmName.isEmpty()) {
                mmName = "(none)"
            }

            val result = ri.conditions_MM_Names!!.isEnabledInList(mmName, lmEntity)
            val mmNameFinal = mmName
            DebugManager.log(
                DebugType.CONDITION_MYTHICMOBS_INTERNAL_NAME, ri, lmEntity, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, mm_name: &b%s&7",
                    ri.ruleName, lmEntity.nameIfBaby, mmNameFinal
                )
            }

            if (!result) return false
        }

        if (ri.conditions_SpawnerNames != null) {
            val checkName = if (lmEntity.getSourceSpawnerName() != null) lmEntity.getSourceSpawnerName() else "(none)"

            val result = ri.conditions_SpawnerNames!!.isEnabledInList(checkName!!, lmEntity)
            DebugManager.log(
                DebugType.CONDITION_SPAWNER_NAME, ri, lmEntity, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, spawner: &b%s&7",
                    ri.ruleName, lmEntity.nameIfBaby, checkName
                )
            }

            if (!result) return false
        }

        if (ri.conditions_SpawnegEggNames != null) {
            val checkName = if (lmEntity.getSourceSpawnEggName() != null) lmEntity.getSourceSpawnEggName() else "(none)"

            val result = ri.conditions_SpawnegEggNames!!.isEnabledInList(checkName!!, lmEntity)
            DebugManager.log(
                DebugType.CONDITION_SPAWNER_NAME, ri, lmEntity, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, spawn_egg: &b%s&7",
                    ri.ruleName, lmEntity.nameIfBaby, checkName
                )
            }

            if (!result) return false
        }

        if (ri.conditions_Permission != null) {
            if (lmEntity.associatedPlayer == null) {
                DebugManager.log(
                    DebugType.CONDITION_PERMISSION, ri, lmEntity, false
                ) {
                    String.format(
                        "&b%s&7, mob: &b%s&7, no player was provided",
                        ri.ruleName, lmEntity.nameIfBaby
                    )
                }
                return false
            }

            if (!doesPlayerPassPermissionChecks(
                    ri.conditions_Permission!!,
                    lmEntity.associatedPlayer!!
                )
            ) {
                DebugManager.log(
                    DebugType.CONDITION_PERMISSION, ri, lmEntity, false
                ) {
                    String.format(
                        "&b%s&7, mob: &b%s&7, player: &b%s&7, permission denied",
                        ri.ruleName, lmEntity.nameIfBaby,
                        lmEntity.associatedPlayer!!.name
                    )
                }
                return false
            }

            DebugManager.log(
                DebugType.CONDITION_PERMISSION, ri, lmEntity, true
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, player: &b%s&7, permission granted",
                    ri.ruleName, lmEntity.nameIfBaby,
                    lmEntity.associatedPlayer!!.name
                )
            }
        }

        if (ri.conditions_MobCustomnameStatus != MobCustomNameStatus.NOT_SPECIFIED
            && ri.conditions_MobCustomnameStatus != MobCustomNameStatus.EITHER
        ) {
            val hasCustomName = lmEntity.livingEntity.customName != null

            if (hasCustomName && ri.conditions_MobCustomnameStatus == MobCustomNameStatus.NOT_NAMETAGGED ||
                !hasCustomName && ri.conditions_MobCustomnameStatus == MobCustomNameStatus.NAMETAGGED
            ) {
                DebugManager.log(
                    DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, false
                ) {
                    String.format(
                        "&b%s&7, mob: &b%s&7, nametag: %s, rule: %s",
                        ri.ruleName, lmEntity.nameIfBaby, lmEntity.livingEntity.customName,
                        ri.conditions_MobCustomnameStatus
                    )
                }
                return false
            }

            DebugManager.log(
                DebugType.CONDITION_CUSTOM_NAME, ri, lmEntity, true
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, nametag: %s, rule: %s",
                    ri.ruleName, lmEntity.nameIfBaby, lmEntity.livingEntity.customName,
                    ri.conditions_MobCustomnameStatus
                )
            }
        }

        if (ri.conditions_MobTamedStatus != MobTamedStatus.NOT_SPECIFIED
            && ri.conditions_MobTamedStatus != MobTamedStatus.EITHER
        ) {
            if (lmEntity.isMobTamed && ri.conditions_MobTamedStatus == MobTamedStatus.NOT_TAMED ||
                !lmEntity.isMobTamed && ri.conditions_MobTamedStatus == MobTamedStatus.TAMED
            ) {
                DebugManager.log(
                    DebugType.ENTITY_TAME, ri, lmEntity, false
                ) {
                    String.format(
                        "&b%s&7, mob: &b%s&7, tamed: %s, rule: %s",
                        ri.ruleName, lmEntity.nameIfBaby, lmEntity.isMobTamed,
                        ri.conditions_MobTamedStatus
                    )
                }
                return false
            }

            DebugManager.log(
                DebugType.ENTITY_TAME, ri, lmEntity, true
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, tamed: %s, rule: %s",
                    ri.ruleName, lmEntity.nameIfBaby, lmEntity.isMobTamed,
                    ri.conditions_MobTamedStatus
                )
            }
        }

        if (ri.conditions_ScoreboardTags != null) {
            val tags = lmEntity.livingEntity.scoreboardTags
            if (tags.isEmpty()) {
                tags.add("(none)")
            }

            var madeCriteria = false
            for (tag in tags) {
                if (ri.conditions_ScoreboardTags!!.isEnabledInList(tag, lmEntity)) {
                    madeCriteria = true
                }
            }

            DebugManager.log(
                DebugType.SCOREBOARD_TAGS, ri, lmEntity, madeCriteria
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7",
                    ri.ruleName, lmEntity.nameIfBaby
                )
            }

            if (!madeCriteria) return false
        }

        if (ri.conditions_SkyLightLevel != null) {
            val lightLevel = lmEntity.getSkylightLevel()
            val result = (lightLevel >= ri.conditions_SkyLightLevel!!.min
                    && lightLevel <= ri.conditions_SkyLightLevel!!.max)
            DebugManager.log(
                DebugType.SKYLIGHT_LEVEL, ri, lmEntity, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, skylight: %s, criteria: %s",
                    ri.ruleName, lmEntity.nameIfBaby, lightLevel,
                    ri.conditions_SkyLightLevel
                )
            }
            return result
        }

        return true
    }

    private fun meetsMaxDistanceCriteria
                (lmEntity: LivingEntityWrapper,
                 rule: RuleInfo
    ): Boolean {
        val mdr = rule.conditions_WithinCoords!!

        if (mdr.getHasX && !mdr.isLocationWithinRange(lmEntity.location.blockX, WithinCoordinates.Axis.X)) {
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false) {
                java.lang.String.format(
                    "entity: %s, xCoord: %s, startX: %s, endX: %s",
                    lmEntity.nameIfBaby, lmEntity.location.blockX, mdr.startX, mdr.endX
                )
            }
            return false
        }

        if (mdr.getHasY && !mdr.isLocationWithinRange(lmEntity.location.blockY, WithinCoordinates.Axis.Y)) {
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false) {
                java.lang.String.format(
                    "entity: %s, yCoord: %s, startY: %s, endY: %s",
                    lmEntity.nameIfBaby, lmEntity.location.blockY, mdr.startY, mdr.endY
                )
            }
            return false
        }

        if (mdr.getHasZ && !mdr.isLocationWithinRange(lmEntity.location.blockZ, WithinCoordinates.Axis.Z)) {
            DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, false) {
                java.lang.String.format(
                    "entity: %s, zCoord: %s, startZ: %s, endZ: %s",
                    lmEntity.nameIfBaby, lmEntity.location.blockZ, mdr.startZ, mdr.endZ
                )
            }
            return false
        }

        DebugManager.log(DebugType.CONDITION_WITH_COORDINATES, rule, lmEntity, true) {
            java.lang.String.format(
                "entity: %s, zCoord: %s, startZ: %s, endZ: %s",
                lmEntity.nameIfBaby, lmEntity.location.blockZ, mdr.startZ, mdr.endZ
            )
        }

        return true
    }

    private fun isRuleApplicableInterface(
        lmInterface: LivingEntityInterface, ri: RuleInfo
    ): RuleCheckResult {
        if (ri.conditions_Entities != null) {
            if (lmInterface is LivingEntityWrapper) {
                val result = isLivingEntityInModalList(
                    ri.conditions_Entities!!, lmInterface, true
                )
                DebugManager.log(
                    DebugType.CONDITION_ENTITIES_LIST, ri, lmInterface, result
                ) {
                    String.format(
                        "&b%s&7, mob: &b%s&7", ri.ruleName,
                        lmInterface.nameIfBaby
                    )
                }

                if (!result) return RuleCheckResult(false)
            } else {
                // can't check groups if not a living entity wrapper
                val result = ri.conditions_Entities!!.isEnabledInList(
                    lmInterface.typeName, null
                )

                DebugManager.log(
                    DebugType.CONDITION_ENTITIES_LIST, ri, lmInterface, result
                ) {
                    String.format(
                        "&b%s&7, mob: &b%s&7", ri.ruleName,
                        lmInterface.typeName
                    )
                }

                if (!result) return RuleCheckResult(false)
            }
        }

        if (ri.conditions_Worlds != null) {
            val result = (lmInterface.wasSummoned ||
                    ri.conditions_Worlds!!.isEnabledInList(lmInterface.world!!.name, null))
            DebugManager.log(
                DebugType.CONDITION_WORLD_LIST, ri, lmInterface, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, mob world: &b%s&7",
                    ri.ruleName, lmInterface.typeName, lmInterface.world!!.name
                )
            }
            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditions_Biomes != null) {
            val result = isBiomeInModalList(
                ri.conditions_Biomes!!,
                lmInterface.location!!.block.biome, LevelledMobs.instance.rulesManager
            )
            DebugManager.log(
                DebugType.CONDITION_BIOME_LIST, ri, lmInterface, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, mob biome: &b%s&7",
                    ri.ruleName, lmInterface.typeName,
                    lmInterface.location!!.block.biome.name
                )
            }
            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditions_WGRegions != null
            && ExternalCompatibilityManager.hasWorldGuardInstalled()
        ) {
            var isInList = false
            val wgRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(
                lmInterface
            )
            if (wgRegions.isEmpty()) {
                wgRegions.add("(none)")
            }

            for (regionName in wgRegions) {
                if (ri.conditions_WGRegions!!.isEnabledInList(regionName, null)) {
                    isInList = true
                    break
                }
            }

            DebugManager.log(
                DebugType.CONDITION_WG_REGION, ri, lmInterface, isInList
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, wg_regions: &b%s&7",
                    ri.ruleName, lmInterface.typeName, wgRegions
                )
            }
            if (!isInList) return RuleCheckResult(false)
        }

        if (ri.conditions_WGRegionOwners != null
            && ExternalCompatibilityManager.hasWorldGuardInstalled()
        ) {
            var isInList = false
            val wgRegionOwners = getWorldGuardRegionOwnersForLocation(
                lmInterface
            )
            if (wgRegionOwners.isEmpty()) {
                wgRegionOwners.add("(none)")
            }

            for (ownerName in wgRegionOwners) {
                if (ri.conditions_WGRegionOwners!!.isEnabledInList(ownerName, null)) {
                    isInList = true
                    break
                }
            }

            DebugManager.log(
                DebugType.CONDITION_WG_REGION_OWNER, ri, lmInterface, isInList
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, wg_owners: &b%s&7",
                    ri.ruleName, lmInterface.typeName, wgRegionOwners
                )
            }

            if (!isInList) return RuleCheckResult(false)
        }

        if (ri.conditions_ApplyAboveY != null) {
            val result = lmInterface.location!!.blockY > ri.conditions_ApplyAboveY!!
            DebugManager.log(
                DebugType.CONDITION_Y_LEVEL, ri, lmInterface, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, y-level: &b%s&7, max-y: &b%s&7",
                    ri.ruleName, lmInterface.typeName,
                    lmInterface.location!!.blockY, ri.conditions_ApplyAboveY
                )
            }
            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditions_ApplyBelowY != null) {
            val result = lmInterface.location!!.blockY < ri.conditions_ApplyBelowY!!
            DebugManager.log(
                DebugType.CONDITION_Y_LEVEL, ri, lmInterface, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, y-level: &b%s&7, min-y: &b%s&7",
                    ri.ruleName, lmInterface.typeName,
                    lmInterface.location!!.blockY, ri.conditions_ApplyBelowY
                )
            }
            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditions_MinDistanceFromSpawn != null) {
            val result = lmInterface.distanceFromSpawn >= ri.conditions_MinDistanceFromSpawn!!
            DebugManager.log(
                DebugType.CONDITION_MIN_SPAWN_DISTANCE, ri, lmInterface, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, spawn-distance: &b%s&7, min-sd: &b%s&7",
                    ri.ruleName, lmInterface.typeName,
                    round(lmInterface.distanceFromSpawn),
                    ri.conditions_MinDistanceFromSpawn
                )
            }

            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditions_MaxDistanceFromSpawn != null) {
            val result = lmInterface.distanceFromSpawn <= ri.conditions_MaxDistanceFromSpawn!!
            DebugManager.log(
                DebugType.CONDITION_MAX_SPAWN_DISTANCE, ri, lmInterface, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, spawn-distance: &b%s&7, min-sd: &b%s&7",
                    ri.ruleName, lmInterface.typeName,
                    round(lmInterface.distanceFromSpawn),
                    ri.conditions_MaxDistanceFromSpawn
                )
            }

            if (!result) return RuleCheckResult(false)
        }

        if (ri.conditions_WorldTickTime != null) {
            val currentWorldTickTime = lmInterface.spawnedTimeOfDay!!
            val result = isIntegerInModalList(ri.conditions_WorldTickTime!!, currentWorldTickTime)
            DebugManager.log(
                DebugType.CONDITION_WORLD_TIME_TICK, ri, lmInterface, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, tick time: &b%s&7",
                    ri.ruleName, lmInterface.typeName, currentWorldTickTime
                )
            }

            if (!result) return RuleCheckResult(useResult = false, ruleMadeChance = false)
        }

        var ruleMadeChance: Boolean? = null

        if (ri.conditions_Chance != null && ri.conditions_Chance!! < 1.0) {
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
            val result = chanceRole >= (1.0f - ri.conditions_Chance!!)
            DebugManager.log(
                DebugType.CONDITION_CHANCE, ri, lmInterface, result
            ) {
                String.format(
                    "&b%s&7, mob: &b%s&7, chance: &b%s&7, chance role: &b%s&7",
                    ri.ruleName, lmInterface.typeName, ri.conditions_Chance,
                    round(chanceRole.toDouble(), 4)
                )
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
        if (perms.allowAll) {
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

        for (perm in perms.allowedList) {
            val permCheck = "levelledmobs.permission.$perm"
            if (player.hasPermission(permCheck)) {
                return true
            }
        }

        return perms.isBlacklist
    }

    fun buildBiomeGroupMappings(customBiomeGroups: MutableMap<String, MutableSet<String>>?) {
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
                if (rule?.conditions_CooldownTime == null || rule.conditions_CooldownTime!! <= 0
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
                                > rule.conditions_CooldownTime!!)
                    }) {
                    DebugManager.log(DebugType.SETTING_COOLDOWN) {
                        String.format(
                            "rule: %s, removed cooldown entries, pre: %s, post: %s",
                            rule.ruleName, preCount, instants.size
                        )
                    }
                    if (instants.isEmpty()) {
                        rule.isTempDisabled = false
                        iterator.remove()
                    }
                }
            }
        }
    }

    fun showTempDisabledRules(isFromConsole: Boolean): String {
        synchronized(ruleLocker) {
            if (rulesCooldown.isEmpty()) {
                val message = "No rules are currently temporarily disabled"
                return if (isFromConsole) {
                    String.format("%s %s", LevelledMobs.instance.configUtils.getPrefix(), message)
                } else {
                    message
                }
            }
            checkTempDisabledRules()

            val sb = StringBuilder()
            if (isFromConsole) {
                sb.append(LevelledMobs.instance.configUtils.getPrefix())
                sb.append(
                    String.format(" %s rule(s) currently disabled:", rulesCooldown.size)
                )
            }

            for (ruleName in rulesCooldown.keys) {
                val rule = ruleNameMappings[ruleName]
                if (rule?.conditions_CooldownTime == null) {
                    continue
                }
                sb.append(System.lineSeparator())

                sb.append(ruleName)
                sb.append(": seconds left: ")
                val instant = rulesCooldown[ruleName]!![0]
                val millisecondsSince = Duration.between(instant, Instant.now()).toMillis()
                val duration = Duration.ofMillis(
                    rule.conditions_CooldownTime!! - millisecondsSince
                )
                sb.append(duration.toSeconds())
            }
            return sb.toString()
        }
    }

    fun updateRulesHash() {
        val sb = java.lang.StringBuilder()

        synchronized(ruleLocker) {
            for (rulePri in rulesInEffect.keys) {
                val rules = rulesInEffect[rulePri]!!
                for (rule in rules) {
                    if (!rule.ruleIsEnabled) {
                        continue
                    }
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(rule.formatRulesVisually(true, mutableListOf("id")))
                }
            }
        }

        try {
            val digest = MessageDigest.getInstance("SHA3-256")
            val hashbytes = digest.digest(
                sb.toString().toByteArray(StandardCharsets.UTF_8)
            )
            this.currentRulesHash = bytesToHex(hashbytes)
        } catch (e: NoSuchAlgorithmException) {
            Utils.logger.error("Unable to run SHA-256 hash: " + e.message)
            this.currentRulesHash = "1234"
        }
    }

    companion object{
        val ruleLocker: Any = Any()

        // taken from https://www.baeldung.com/sha-256-hashing-java
        private fun bytesToHex(hash: ByteArray): String {
            val hexString = java.lang.StringBuilder(2 * hash.size)
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