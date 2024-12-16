package io.github.arcaneplugins.levelledmobs.wrappers

import java.util.Stack
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.debug.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.misc.CustomUniversalGroups
import io.github.arcaneplugins.levelledmobs.debug.DebugType
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.rules.ApplicableRulesResult
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes
import io.github.arcaneplugins.levelledmobs.enums.NametagVisibilityEnum
import io.github.arcaneplugins.levelledmobs.misc.LMSpawnReason
import io.github.arcaneplugins.levelledmobs.rules.RuleInfo
import io.github.arcaneplugins.levelledmobs.rules.strategies.StrategyType
import io.github.arcaneplugins.levelledmobs.util.Log
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.command.CommandSender
import org.bukkit.entity.AbstractVillager
import org.bukkit.entity.Ageable
import org.bukkit.entity.Animals
import org.bukkit.entity.Bat
import org.bukkit.entity.Boss
import org.bukkit.entity.Enemy
import org.bukkit.entity.EntityType
import org.bukkit.entity.Flying
import org.bukkit.entity.Frog
import org.bukkit.entity.Guardian
import org.bukkit.entity.Hoglin
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.entity.Turtle
import org.bukkit.entity.WaterMob
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

/**
 * A wrapper for the LivingEntity class that provides various common function and settings used for
 * processing rules
 *
 * @author stumper66
 * @since 3.0.0
 */
class LivingEntityWrapper private constructor() : LivingEntityWrapperBase(), LivingEntityInterface {
    // privates:
    private var applicableGroups: MutableSet<String> = TreeSet<String>(String.CASE_INSENSITIVE_ORDER)
    private var hasCache = false
    private var isClearingData = false
    private var _livingEntity: LivingEntity? = null
    private var isBuildingCache = false
    private var groupsAreBuilt = false
    private var _shouldShowLMNametag: Boolean? = null
    private var _spawnedTimeOfDay: Int? = null
    private var _skylightLevelAtSpawn: Int? = null
    private var nametagCooldownTime = 0L
    private var _sourceSpawnerName: String? = null
    private var _sourceSpawnEggName: String? = null
    private val applicableRules = mutableListOf<RuleInfo>()
    var spawnedWGRegions = mutableSetOf<String>()
        private set
    val spawnReason = LMSpawnReason()
    private var _nametagVisibilityEnum = mutableListOf<NametagVisibilityEnum>()
    private val cacheLock = ReentrantLock()
    private val pdcLock = ReentrantLock()

    // publics:
    var prevChanceRuleResults: MutableMap<String, Boolean>? = null
        private set
    var chunkKillcount = 0
    var mobLevel: Int? = null
    val mobExternalTypes: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    var rangedDamage: Float? = null
    var attributeValuesCache: MutableMap<Attribute, AttributeInstance>? = null
    val strategyResults = mutableMapOf<StrategyType, Float>()
    var customStrategyResults = mutableMapOf<String, Float>()
    var reEvaluateLevel = false
    var wasPreviouslyLevelled = false
    var isRulesForceAll = false
    var isNewlySpawned = false
    var lockEntitySettings = false
    var hasLockedDropsOverride = false
    var playerLevellingAllowDecrease: Boolean? = null
    var libsDisguiseCache: Any? = null
    var playersNeedingNametagCooldownUpdate: MutableSet<Player>? = null
    var deathCause = EntityDamageEvent.DamageCause.CUSTOM
    var nbtData: MutableList<String>? = null
    var lockedCustomDrops: MutableList<String>? = null
    var pendingPlayerIdToSet: String? = null
    var lockedNametag: String? = null
    var lockedOverrideName: String? = null
    var invalidPlaceholderReplacement: String? = null
    var associatedPlayer: Player? = null
    var summonedSender: CommandSender? = null

    companion object{
        private val playerLock = Any()
        private val cachedLM_Wrappers_Lock = Any()
        private val cache = Stack<LivingEntityWrapper>()
        private const val LOCKMAXRETRYTIMES = 3
        private val flyingMobNames = mutableListOf(
            "ALLAY", "BEE", "BLAZE", "ENDER_DRAGON", "VEX", "WITHER", "PARROT", "BAT"
        )

        fun getInstance(
            livingEntity: LivingEntity
        ): LivingEntityWrapper {
            val lew: LivingEntityWrapper

            synchronized(cachedLM_Wrappers_Lock) {
                lew = if (cache.empty()) {
                    LivingEntityWrapper()
                } else {
                    cache.pop()
                }
            }

            if (LevelledMobs.instance.cacheCheck == null) {
                LevelledMobs.instance.cacheCheck = cache
            }

            lew.livingEntity = livingEntity
            lew.inUseCount.set(1)
            return lew
        }

        fun getLEWDebug(): String {
            var totalSize: Int
            var nonEmpties = 0

            synchronized(cachedLM_Wrappers_Lock) {
                totalSize = cache.size
                val enumeration = cache.elements()
                while (enumeration.hasMoreElements()) {
                    val lew = enumeration.nextElement()
                    if (lew.hasCache) nonEmpties++
                }
            }

            return "size: $totalSize, nonempties: $nonEmpties"
        }

        fun clearCache() {
            val nonEmpties = mutableListOf<LivingEntityWrapper>()

            synchronized(cachedLM_Wrappers_Lock) {
                while (!cache.isEmpty()) {
                    val lew = cache.pop()
                    if (lew.hasCache) nonEmpties.add(lew)
                }
                for (lew in nonEmpties) {
                    cache.push(lew)
                }
                nonEmpties.clear()
            }
        }
    }

    override fun free() {
        if (inUseCount.decrementAndGet() > 0) {
            return
        }
        if (!isPopulated) {
            return
        }

        clearEntityData()
        synchronized(cachedLM_Wrappers_Lock) {
            cache.push(this)
        }
    }

    override fun clearEntityData() {
        this.isClearingData = true
        this._livingEntity = null
        this.attributeValuesCache = null
        this.nametagVisibilityEnum.clear()
        this.rangedDamage = null
        this.strategyResults.clear()
        this.customStrategyResults.clear()
        this.libsDisguiseCache = null
        this.chunkKillcount = 0
        applicableGroups.clear()
        applicableRules.clear()
        mobExternalTypes.clear()
        this._spawnedTimeOfDay = null
        this._shouldShowLMNametag = null
        this.spawnReason.clear()
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM
        this.isBuildingCache = false
        this.hasCache = false
        this.mobLevel = null
        this.spawnedWGRegions.clear()
        this.fineTuningAttributes = null
        this.reEvaluateLevel = false
        this.isRulesForceAll = false
        this.wasPreviouslyLevelled = false
        this.groupsAreBuilt = false
        this.playerForLevelling = null
        this.prevChanceRuleResults = null
        this._sourceSpawnerName = null
        this._sourceSpawnEggName = null
        this.associatedPlayer = null
        this.playersNeedingNametagCooldownUpdate = null
        this.nametagCooldownTime = 0
        this.nbtData = null
        this.invalidPlaceholderReplacement = null
        this.summonedSender = null
        this.playerLevellingAllowDecrease = null
        this.pendingPlayerIdToSet = null
        this._skylightLevelAtSpawn = null
        this.wasSummoned = false
        this.lockedNametag = null
        this.lockedOverrideName = null
        this.isNewlySpawned = false
        this.lockEntitySettings = false
        this.hasLockedDropsOverride = false
        this.lockedCustomDrops = null

        super.clearEntityData()
        this.isClearingData = false
    }

    private fun buildCache() {
        if (this.hasCache) return

        try {
            if (!cacheLock.tryLock(500, TimeUnit.MILLISECONDS)) return
            if (this.hasCache) return

            isBuildingCache = true
            this.mobLevel =
                if (main.levelInterface.isLevelled(livingEntity)) main.levelInterface.getLevelOfMob(livingEntity) else null

            try {
                this.wasSummoned = pdc.has(
                    NamespacedKeys.wasSummoned,
                    PersistentDataType.INTEGER
                )
            } catch (ignored: Exception) {}

            if (main.rulesManager.hasAnyWGCondition) this.spawnedWGRegions =
                ExternalCompatibilityManager.getWGRegionsAtLocation(
                    this
                )

            this.hasCache = true
            // the lines below must remain after hasCache = true to prevent stack overflow
            cachePrevChanceResults()
            val applicableRulesResult = main.rulesManager.getApplicableRules(this)
            this.applicableRules.clear()
            this.applicableRules.addAll(applicableRulesResult.allApplicableRules)
            checkChanceRules(applicableRulesResult)
            this.fineTuningAttributes = main.rulesManager.getFineTuningAttributes(this)
            this.nametagCooldownTime = main.rulesManager.getRuleNametagVisibleTime(this)
            this.nametagVisibilityEnum.clear()
            this.nametagVisibilityEnum.addAll(main.rulesManager.getRuleCreatureNametagVisbility(this))
            ExternalCompatibilityManager.updateAllExternalCompats(this)
            this.invalidPlaceholderReplacement = main.rulesManager.getRuleInvalidPlaceholderReplacement(this)
            this.isBuildingCache = false
        } catch (e: InterruptedException) {
            Log.war("exception in buildCache: " + e.message)
        } finally {
            if (cacheLock.isHeldByCurrentThread) {
                cacheLock.unlock()
            }
        }
    }

    private fun getPDCLock(): Boolean {
        try {
            // try up to 3 times to get a lock
            var retryCount = 0
            while (true) {
                if (pdcLock.tryLock(15, TimeUnit.MILLISECONDS)) {
                    return true
                }

                val callingFunction = Thread.currentThread().stackTrace[1]
                retryCount++
                if (retryCount > LOCKMAXRETRYTIMES) {
                    DebugManager.log(DebugType.THREAD_LOCKS) {
                        "getPDCLock could not lock thread - ${callingFunction.fileName}:${callingFunction.lineNumber}"
                    }
                    return false
                }

                val retryCountFinal = retryCount
                DebugManager.log(DebugType.THREAD_LOCKS) {
                    "getPDCLock retry $retryCountFinal - ${callingFunction.fileName}:${callingFunction.lineNumber}"
                }
            }
        } catch (e: InterruptedException) {
            Log.war("getPDCLock InterruptedException: " + e.message)
            return false
        }
    }

    private fun releasePDCLock() {
        if (pdcLock.isHeldByCurrentThread) {
            pdcLock.unlock()
        }
    }

    fun invalidateCache() {
        this.hasCache = false
        this.groupsAreBuilt = false
        applicableGroups.clear()
        applicableRules.clear()
    }

    fun buildCacheIfNeeded(){
        if (!hasCache) {
            buildCache()
        }
    }

    private fun checkChanceRules(
        result: ApplicableRulesResult
    ) {
        if (result.allApplicableRulesMadeChance.isEmpty()
            && result.allApplicableRulesDidNotMakeChance.isEmpty()
        ) {
            return
        }

        val sbAllowed = StringBuilder()
        for (ruleInfo in result.allApplicableRulesMadeChance) {
            if (sbAllowed.isNotEmpty()) {
                sbAllowed.append(";")
            }
            sbAllowed.append(ruleInfo.ruleName)
        }

        val sbDenied = StringBuilder()
        for (ruleInfo in result.allApplicableRulesDidNotMakeChance) {
            if (sbDenied.isNotEmpty()) {
                sbDenied.append(";")
            }
            sbDenied.append(ruleInfo.ruleName)
        }

        if (!getPDCLock()) {
            return
        }

        try {
            for (i in 0..1) {
                try {
                    if (sbAllowed.isNotEmpty()) {
                        livingEntity.persistentDataContainer
                            .set(
                                NamespacedKeys.chanceRuleAllowed, PersistentDataType.STRING,
                                sbAllowed.toString()
                            )
                    }
                    if (sbDenied.isNotEmpty()) {
                        livingEntity.persistentDataContainer
                            .set(
                                NamespacedKeys.chanceRuleDenied, PersistentDataType.STRING,
                                sbDenied.toString()
                            )
                    }
                    break
                } catch (ignored: ConcurrentModificationException) {
                    try {
                        Thread.sleep(10)
                    } catch (ignored2: InterruptedException) {
                        break
                    }
                }
            }
        } finally {
            releasePDCLock()
        }
    }

    private fun cachePrevChanceResults() {
        if (!main.rulesManager.anyRuleHasChance) {
            return
        }

        var rulesPassed: String? = null
        var rulesDenied: String? = null

        if (getPDCLock()) {
            val le = livingEntity
            try {
                if (le.persistentDataContainer
                        .has(NamespacedKeys.chanceRuleAllowed, PersistentDataType.STRING)
                ) {
                    rulesPassed = le.persistentDataContainer
                        .get(NamespacedKeys.chanceRuleAllowed, PersistentDataType.STRING)
                }
                if (le.persistentDataContainer
                        .has(NamespacedKeys.chanceRuleDenied, PersistentDataType.STRING)
                ) {
                    rulesDenied = le.persistentDataContainer
                        .get(NamespacedKeys.chanceRuleDenied, PersistentDataType.STRING)
                }
            } finally {
                releasePDCLock()
            }
        }

        if (rulesPassed == null && rulesDenied == null) {
            return
        }
        val results = mutableMapOf<String, Boolean>()

        if (rulesPassed != null) {
            for (ruleName in rulesPassed.split(";")) {
                results[ruleName] = true
            }
        }

        if (rulesDenied != null) {
            for (ruleName in rulesDenied.split(";")) {
                results[ruleName] = false
            }
        }

        this.prevChanceRuleResults = results
    }

    var livingEntity: LivingEntity
        get() = _livingEntity!!
        set(value) {
            _livingEntity = value
            super.populateData(livingEntity.world, livingEntity.location)
        }

    override val typeName: String
        get() = livingEntity.type.name

    fun getApplicableGroups(): MutableSet<String> {
        if (!groupsAreBuilt) {
            this.applicableGroups = buildApplicableGroupsForMob()
            groupsAreBuilt = true
        }

        return this.applicableGroups
    }

    fun getNametagCooldownTime(): Long {
        if (!hasCache) {
            buildCache()
        }

        return this.nametagCooldownTime
    }

    var playerForLevelling: Player? = null
        get() {
            synchronized(playerLock) {
                return field
            }
        }
        set(value) {
            synchronized(playerLock) {
                field = value
            }
            this.associatedPlayer = value
        }

    var fineTuningAttributes: FineTuningAttributes? = null
        private set
        get() {
            if (isClearingData) return field
            if (!hasCache) buildCache()

            return field
    }

    val nametagVisibilityEnum = mutableListOf<NametagVisibilityEnum>()
        get() {
            if (isClearingData) return field
            if (!hasCache) buildCache()

            return field
        }

    override fun getApplicableRules(): MutableList<RuleInfo> {
        if (!hasCache) {
            buildCache()
        }

        return this.applicableRules
    }

    val getMobLevel: Int
        get() {
            if (!hasCache) {
                buildCache()
            }

            return if (this.mobLevel == null) 0
            else mobLevel!!
    }

    fun setMobPrelevel(level: Int) {
        this.mobLevel = level
    }

    val isLevelled: Boolean
        get() = main.levelInterface.isLevelled(this.livingEntity)

    override val entityType: EntityType
        get() = this.livingEntity.type

    val pdc: PersistentDataContainer
        get() = this.livingEntity.persistentDataContainer

    val isBabyMob: Boolean
        get() {
            if (livingEntity is Ageable) {
                return !((livingEntity as Ageable).isAdult)
            }

            return false
        }

    var skylightLevel: Int
        set(value) {
            this._skylightLevelAtSpawn = value

            if (!getPDCLock()) {
                return
            }

            try {
                if (!livingEntity.persistentDataContainer
                        .has(NamespacedKeys.skyLightLevel, PersistentDataType.INTEGER)
                ) {
                    livingEntity.persistentDataContainer
                        .set(
                            NamespacedKeys.skyLightLevel, PersistentDataType.INTEGER,
                            _skylightLevelAtSpawn!!
                        )
                }
            } finally {
                releasePDCLock()
            }
        }
        get() {
            if (this._skylightLevelAtSpawn != null) {
                return _skylightLevelAtSpawn!!
            }

            if (!getPDCLock()) {
                return currentSkyLightLevel
            }
            var hadError = false
            var succeeded = false

            try {
                for (i in 0..1) {
                    try {
                        if (livingEntity.persistentDataContainer
                                .has(NamespacedKeys.skyLightLevel, PersistentDataType.INTEGER)
                        ) {
                            this._skylightLevelAtSpawn = livingEntity.persistentDataContainer
                                .get(NamespacedKeys.skyLightLevel, PersistentDataType.INTEGER)
                        }
                        succeeded = true
                        break
                    } catch (ignored: java.util.ConcurrentModificationException) {
                        hadError = true
                        try {
                            Thread.sleep(5)
                        } catch (ignored2: InterruptedException) {
                            return 0
                        }
                    } finally {
                        releasePDCLock()
                    }
                }
            } finally {
                releasePDCLock()
            }

            if (hadError) {
                if (succeeded) {
                    Log.war(
                        "Got ConcurrentModificationException in LivingEntityWrapper getting skyLightLevel, succeeded on retry"
                    )
                } else {
                    Log.war(
                        "Got ConcurrentModificationException (2x) in LivingEntityWrapper getting skyLightLevel"
                    )
                }
            }

            return if (this._skylightLevelAtSpawn != null)
                this._skylightLevelAtSpawn!!
            else
                currentSkyLightLevel
        }

    val currentSkyLightLevel: Int
        get() = location.block.lightFromSky.toInt()

    var sourceSpawnerName: String?
        set(value) {
            this._sourceSpawnerName = value

            if (!getPDCLock()) {
                return
            }
            try {
                if (value == null && pdc.has(
                        NamespacedKeys.sourceSpawnerName,
                        PersistentDataType.STRING
                    )
                ) {
                    pdc.remove(NamespacedKeys.sourceSpawnerName)
                } else if (value != null) {
                    pdc.set(
                        NamespacedKeys.sourceSpawnerName, PersistentDataType.STRING,
                        value
                    )
                }
            } finally {
                releasePDCLock()
            }
        }
        get() {
            if (this._sourceSpawnerName != null) {
                return this._sourceSpawnerName
            }

            if (getPDCLock()) {
                try {
                    if (pdc.has(
                            NamespacedKeys.sourceSpawnerName,
                            PersistentDataType.STRING
                        )
                    ) {
                        this._sourceSpawnerName = pdc.get(
                            NamespacedKeys.sourceSpawnerName,
                            PersistentDataType.STRING
                        )
                    }
                } finally {
                    releasePDCLock()
                }
            }

            if (this._sourceSpawnerName == null) {
                this._sourceSpawnerName = "(none)"
            }

            return this._sourceSpawnerName
        }

    var sourceSpawnEggName: String?
        set(value) {
            this._sourceSpawnerName = value

            if (!getPDCLock()) {
                return
            }
            try {
                if (value == null && pdc.has(
                        NamespacedKeys.sourceSpawnerName,
                        PersistentDataType.STRING
                    )
                ) {
                    pdc.remove(NamespacedKeys.sourceSpawnerName)
                } else if (value != null) {
                    pdc.set(
                        NamespacedKeys.sourceSpawnerName, PersistentDataType.STRING,
                        value
                    )
                }
            } finally {
                releasePDCLock()
            }
        }
        get() {
            if (this._sourceSpawnEggName != null) {
                return this._sourceSpawnEggName
            }

            if (getPDCLock()) {
                try {
                    if (pdc.has(NamespacedKeys.spawnerEggName, PersistentDataType.STRING)) {
                        this._sourceSpawnEggName = pdc.get(
                            NamespacedKeys.spawnerEggName,
                            PersistentDataType.STRING
                        )
                    }
                } finally {
                    releasePDCLock()
                }
            }

            if (this._sourceSpawnEggName == null) {
                this._sourceSpawnEggName = "(none)"
            }

            return this._sourceSpawnEggName
        }

    val nameIfBaby: String
        get() {
            return if (this.isBabyMob) "BABY_$typeName"
            else typeName
        }

    val isMobTamed: Boolean
        get() {
            return (livingEntity is Tameable && (livingEntity as Tameable).isTamed)
        }

    fun setMobExternalType(
        externalType: String
    ) {
        if (!mobExternalTypes.contains(externalType)) {
            mobExternalTypes.add(externalType)
        }
    }

    val isMobOfExternalType: Boolean
        get() = this.mobExternalTypes.isNotEmpty()

    val hasOverridenEntityName: Boolean
        get() {
            synchronized(livingEntity.persistentDataContainer) {
                return pdc.has(
                        NamespacedKeys.overridenEntityNameKey,
                        PersistentDataType.STRING
                    )
            }
        }

    var overridenEntityName: String?
        set(value) {
            if (!getPDCLock()) {
                return
            }
            try {
                if (value != null)
                    pdc.set(NamespacedKeys.overridenEntityNameKey, PersistentDataType.STRING, value)
                else if (pdc.has(NamespacedKeys.overridenEntityNameKey))
                    pdc.remove(NamespacedKeys.overridenEntityNameKey)
            } finally {
                releasePDCLock()
            }
        }
        get() {
            if (!getPDCLock()) {
                return null
            }

            try {
                return pdc.get(NamespacedKeys.overridenEntityNameKey, PersistentDataType.STRING)
            } finally {
                releasePDCLock()
            }
        }

    val wgRegionName: String
        get() {
            if (spawnedWGRegions.isEmpty()) {
                return ""
            }
            return spawnedWGRegions.first()
        }

    fun populateShowShowLMNametag(){
        this._shouldShowLMNametag = !pdc.has(NamespacedKeys.denyLmNametag, PersistentDataType.INTEGER)
    }

    var shouldShowLMNametag: Boolean
        set(value) {
            _shouldShowLMNametag = value
            if (!getPDCLock()) {
                return
            }

            try {
                if (value && pdc.has(
                        NamespacedKeys.denyLmNametag,
                        PersistentDataType.INTEGER
                    )
                ) {
                    pdc.remove(NamespacedKeys.denyLmNametag)
                } else if (!value && !pdc.has(
                        NamespacedKeys.denyLmNametag,
                        PersistentDataType.INTEGER
                    )
                ) {
                    pdc.set(NamespacedKeys.denyLmNametag, PersistentDataType.INTEGER, 1)
                }
            } finally {
                releasePDCLock()
            }
        }
        get() {
            if (_shouldShowLMNametag != null) return _shouldShowLMNametag!!

            if (!getPDCLock()) {
                return true
            }

            try {
                _shouldShowLMNametag = !pdc.has(NamespacedKeys.denyLmNametag, PersistentDataType.INTEGER)
            } finally {
                releasePDCLock()
            }
            return _shouldShowLMNametag!!
        }


    override var spawnedTimeOfDay: Int
        set(value) {
            if (!getPDCLock()) {
                return
            }

            try {
                for (i in 0..1) {
                    try {
                        if (pdc.has(
                                NamespacedKeys.spawnedTimeOfDay,
                                PersistentDataType.INTEGER
                            )
                        ) {
                            return
                        }

                        pdc.set(
                            NamespacedKeys.spawnedTimeOfDay, PersistentDataType.INTEGER,
                            value
                        )
                    } catch (ignored: ConcurrentModificationException) {
                        try {
                            Thread.sleep(10)
                        } catch (ignored2: InterruptedException) {
                            break
                        }
                    }
                }
            } finally {
                releasePDCLock()
            }

            this._spawnedTimeOfDay = value
        }
        get() {
            synchronized(livingEntity.persistentDataContainer) {
                if (pdc.has(
                        NamespacedKeys.spawnedTimeOfDay,
                        PersistentDataType.INTEGER
                    )
                ) {
                    val result = pdc.get(
                        NamespacedKeys.spawnedTimeOfDay,
                        PersistentDataType.INTEGER
                    )
                    if (result != null) {
                        return result
                    }
                }
            }

            return world.time.toInt()
        }

    override var wasSummoned: Boolean = false
        private set
        get() {
        if (!hasCache) {
            buildCache()
        }

        return field
    }

    private fun buildApplicableGroupsForMob(): MutableSet<String> {
        val groups: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)

        for ((key, mobNames) in main.customMobGroups) {
            if (mobNames.contains(this.typeName)) {
                groups.add(key)
            }
        }

        groups.add(CustomUniversalGroups.ALL_MOBS.toString())

        if (this.mobLevel != null) {
            groups.add(CustomUniversalGroups.ALL_LEVELLABLE_MOBS.toString())
        }
        val eType = livingEntity.type

        if (livingEntity is Monster || livingEntity is Enemy
            || main.mainCompanion.hostileMobsGroup.contains(eType)
        ) {
            groups.add(CustomUniversalGroups.ALL_HOSTILE_MOBS.toString())
        }

        if (livingEntity is WaterMob || livingEntity is Turtle ||
            livingEntity is Frog || livingEntity is Guardian) {
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS.toString())
        }

        if (livingEntity.world.environment == World.Environment.NORMAL) {
            groups.add(CustomUniversalGroups.ALL_OVERWORLD_MOBS.toString())
        } else if (livingEntity.world.environment == World.Environment.NETHER) {
            groups.add(CustomUniversalGroups.ALL_NETHER_MOBS.toString())
        }

        if (livingEntity is Flying || flyingMobNames.contains(eType.name.uppercase())) {
            groups.add(CustomUniversalGroups.ALL_FLYING_MOBS.toString())
        }

        // why bats aren't part of Flying interface is beyond me
        if ((livingEntity !is Flying && livingEntity !is WaterMob
                    && livingEntity !is Boss) && eType != EntityType.BAT
        ) {
            groups.add(CustomUniversalGroups.ALL_GROUND_MOBS.toString())
        }

        if (livingEntity is Animals && livingEntity !is Hoglin || livingEntity is WaterMob
            || main.mainCompanion.passiveMobsGroup.contains(eType)
        ) {
            groups.add(CustomUniversalGroups.ALL_PASSIVE_MOBS.toString())
        }

        if (livingEntity is AbstractVillager || livingEntity is Bat){
            groups.add(CustomUniversalGroups.ALL_PASSIVE_MOBS.toString())
        }

        return groups
    }

    val hashCode: Int
        get() = livingEntity.hashCode()
}