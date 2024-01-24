package io.github.arcaneplugins.levelledmobs.wrappers

import java.util.Stack
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.LivingEntityInterface
import io.github.arcaneplugins.levelledmobs.managers.DebugManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager
import io.github.arcaneplugins.levelledmobs.managers.ExternalCompatibilityManager.ExternalCompatibility
import io.github.arcaneplugins.levelledmobs.misc.CustomUniversalGroups
import io.github.arcaneplugins.levelledmobs.misc.DebugType
import io.github.arcaneplugins.levelledmobs.misc.NamespacedKeys
import io.github.arcaneplugins.levelledmobs.rules.ApplicableRulesResult
import io.github.arcaneplugins.levelledmobs.rules.FineTuningAttributes
import io.github.arcaneplugins.levelledmobs.rules.LevelledMobSpawnReason
import io.github.arcaneplugins.levelledmobs.rules.RuleInfo
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Ageable
import org.bukkit.entity.Animals
import org.bukkit.entity.Boss
import org.bukkit.entity.EntityType
import org.bukkit.entity.Flying
import org.bukkit.entity.Hoglin
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.entity.WaterMob
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

class LivingEntityWrapper private constructor() : LivingEntityWrapperBase(), LivingEntityInterface {
    // privates:
    private var applicableGroups: MutableSet<String> = TreeSet<String>(String.CASE_INSENSITIVE_ORDER)
    private var hasCache = false
    private var _livingEntity: LivingEntity? = null
    private var isBuildingCache = false
    private var groupsAreBuilt = false
    var chunkKillcount: Int = 0
    private var mobLevel: Int? = null
    private var skylightLevelAtSpawn: Int? = null
    private var nametagCooldownTime: Long = 0
    private var sourceSpawnerName: String? = null
    private var sourceSpawnEggName: String? = null
    private val applicableRules = mutableListOf<RuleInfo>()
    private var spawnedWGRegions: List<String>? = null
    val mobExternalTypes = mutableListOf<ExternalCompatibility>()
    private var fineTuningAttributes: FineTuningAttributes? = null
    private var spawnReason: LevelledMobSpawnReason? = null
    var prevChanceRuleResults: MutableMap<String, Boolean>? = null
        private set
    private val cacheLock = ReentrantLock()
    private val pdcLock = ReentrantLock()

    // publics:
    var reEvaluateLevel: Boolean = false
    var wasPreviouslyLevelled: Boolean = false
    var isRulesForceAll: Boolean = false
    var isNewlySpawned: Boolean = false
    var lockEntitySettings: Boolean = false
    var hasLockedDropsOverride: Boolean = false
    var playerLevellingAllowDecrease: Boolean? = null
    var libsDisguiseCache: Any? = null
    var playersNeedingNametagCooldownUpdate: MutableSet<Player>? = null
    var deathCause = EntityDamageEvent.DamageCause.CUSTOM
    var nbtData: MutableList<String>? = null
    var lockedCustomDrops: MutableList<String>? = null
    var pendingPlayerIdToSet: String? = null
    var lockedNametag: String? = null
    var lockedOverrideName: String? = null
    var associatedPlayer: Player? = null
    var summonedSender: CommandSender? = null

    companion object{
        private val playerLock = Any()
        private val cachedLM_Wrappers_Lock = Any()
        private val cache = Stack<LivingEntityWrapper>()
        private const val LOCKMAXRETRYTIMES = 3

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
        this._livingEntity = null
        this.libsDisguiseCache = null
        this.chunkKillcount = 0
        applicableGroups.clear()
        applicableRules.clear()
        mobExternalTypes.clear()
        this.spawnReason = null
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM
        this.isBuildingCache = false
        this.hasCache = false
        this.mobLevel = null
        this.spawnedWGRegions = null
        this.fineTuningAttributes = null
        this.reEvaluateLevel = false
        this.isRulesForceAll = false
        this.wasPreviouslyLevelled = false
        this.groupsAreBuilt = false
        this.playerForLevelling = null
        this.prevChanceRuleResults = null
        this.sourceSpawnerName = null
        this.sourceSpawnEggName = null
        this.associatedPlayer = null
        this.playersNeedingNametagCooldownUpdate = null
        this.nametagCooldownTime = 0
        this.nbtData = null
        this.summonedSender = null
        this.playerLevellingAllowDecrease = null
        this.pendingPlayerIdToSet = null
        this.skylightLevelAtSpawn = null
        this.wasSummoned = false
        this.lockedNametag = null
        this.lockedOverrideName = null
        this.isNewlySpawned = false
        this.lockEntitySettings = false
        this.hasLockedDropsOverride = false
        this.lockedCustomDrops = null

        super.clearEntityData()
    }

    private fun buildCache() {
        if (isBuildingCache || this.hasCache) {
            return
        }

        try {
            if (!cacheLock.tryLock(500, TimeUnit.MILLISECONDS)) {
                Utils.logger.warning("lock timed out building cache")
                return
            }

            if (this.hasCache) {
                return
            }
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
            this.isBuildingCache = false
        } catch (e: InterruptedException) {
            Utils.logger.warning("exception in buildCache: " + e.message)
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
                        String.format(
                            "getPDCLock could not lock thread - %s:%s",
                            callingFunction.fileName, callingFunction.lineNumber
                        )
                    }
                    return false
                }

                val retryCountFinal = retryCount
                DebugManager.log(DebugType.THREAD_LOCKS) {
                    String.format(
                        "getPDCLock retry %s - %s:%s",
                        retryCountFinal, callingFunction.fileName,
                        callingFunction.lineNumber
                    )
                }
            }
        } catch (e: InterruptedException) {
            Utils.logger.warning("getPDCLock InterruptedException: " + e.message)
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

    private fun checkChanceRules(
        result: ApplicableRulesResult
    ) {
        if (result.allApplicableRules_MadeChance.isEmpty()
            && result.allApplicableRules_DidNotMakeChance.isEmpty()
        ) {
            return
        }

        val sbAllowed = StringBuilder()
        for (ruleInfo in result.allApplicableRules_MadeChance) {
            if (sbAllowed.isNotEmpty()) {
                sbAllowed.append(";")
            }
            sbAllowed.append(ruleInfo.ruleName)
        }

        val sbDenied = StringBuilder()
        for (ruleInfo in result.allApplicableRules_DidNotMakeChance) {
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

    fun getFineTuningAttributes(): FineTuningAttributes? {
        if (!hasCache) {
            buildCache()
        }

        return this.fineTuningAttributes
    }

    override fun getApplicableRules(): MutableList<RuleInfo> {
        if (!hasCache) {
            buildCache()
        }

        return this.applicableRules
    }

    fun getMobLevel(): Int {
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

    @Suppress("DEPRECATION")
    val isBabyMob: Boolean
        get() {
            if (livingEntity is Zombie) {
                val zombie = livingEntity as Zombie
                // for backwards compatibility
                try {
                    zombie.isAdult
                    return !zombie.isAdult
                } catch (err: NoSuchMethodError) {
                    return zombie.isBaby
                }
            } else if (livingEntity is Ageable) {
                return !((livingEntity as Ageable).isAdult)
            }

            return false
        }

    fun getSpawnReason(): LevelledMobSpawnReason {
        if (this.spawnReason != null) {
            return spawnReason!!
        }

        if (!getPDCLock()) {
            return LevelledMobSpawnReason.DEFAULT
        }
        var hadError = false
        var succeeded = false

        try {
            for (i in 0..1) {
                try {
                    if (livingEntity.persistentDataContainer
                            .has(NamespacedKeys.spawnReasonKey, PersistentDataType.STRING)
                    ) {
                        this.spawnReason = LevelledMobSpawnReason.valueOf(
                            pdc[NamespacedKeys.spawnReasonKey, PersistentDataType.STRING]!!
                        )
                    }
                    succeeded = true
                    break
                } catch (ignored: java.util.ConcurrentModificationException) {
                    hadError = true
                    try {
                        Thread.sleep(5)
                    } catch (ignored2: InterruptedException) {
                        return LevelledMobSpawnReason.DEFAULT
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
                Utils.logger.warning(
                    "Got ConcurrentModificationException in LivingEntityWrapper getting spawn reason, succeeded on retry"
                )
            } else {
                Utils.logger.warning(
                    "Got ConcurrentModificationException (2x) in LivingEntityWrapper getting spawn reason"
                )
            }
        }

        return if (this.spawnReason != null) this.spawnReason!! else LevelledMobSpawnReason.DEFAULT
    }

    fun getSkylightLevel(): Int {
        if (this.skylightLevelAtSpawn != null) {
            return skylightLevelAtSpawn!!
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
                        this.skylightLevelAtSpawn = livingEntity.persistentDataContainer
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
                Utils.logger.warning(
                    "Got ConcurrentModificationException in LivingEntityWrapper getting skyLightLevel, succeeded on retry"
                )
            } else {
                Utils.logger.warning(
                    "Got ConcurrentModificationException (2x) in LivingEntityWrapper getting skyLightLevel"
                )
            }
        }

        return if (this.skylightLevelAtSpawn != null) this.skylightLevelAtSpawn!! else currentSkyLightLevel
    }

    fun setSkylightLevelAtSpawn() {
        this.skylightLevelAtSpawn = currentSkyLightLevel

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
                        skylightLevelAtSpawn!!
                    )
            }
        } finally {
            releasePDCLock()
        }
    }

    val currentSkyLightLevel: Int
        get() = location.block.lightFromSky.toInt()

    fun setSpawnReason(spawnReason: LevelledMobSpawnReason) {
        setSpawnReason(spawnReason, false)
    }

    fun setSpawnReason(
        spawnReason: LevelledMobSpawnReason,
        doForce: Boolean
    ) {
        this.spawnReason = spawnReason

        if (!getPDCLock()) {
            return
        }

        try {
            if (doForce || !pdc.has(NamespacedKeys.spawnReasonKey, PersistentDataType.STRING)) {
                pdc.set(
                        NamespacedKeys.spawnReasonKey, PersistentDataType.STRING,
                        spawnReason.toString()
                    )
            }
        } finally {
            releasePDCLock()
        }
    }

    fun setSourceSpawnerName(name: String?) {
        this.sourceSpawnerName = name

        if (!getPDCLock()) {
            return
        }
        try {
            if (name == null && pdc.has(
                    NamespacedKeys.sourceSpawnerName,
                    PersistentDataType.STRING
                )
            ) {
                pdc.remove(NamespacedKeys.sourceSpawnerName)
            } else if (name != null) {
                pdc.set(
                    NamespacedKeys.sourceSpawnerName, PersistentDataType.STRING,
                    name
                )
            }
        } finally {
            releasePDCLock()
        }
    }

    fun getSourceSpawnerName(): String? {
        if (this.sourceSpawnerName != null) {
            return this.sourceSpawnerName
        }

        if (getPDCLock()) {
            try {
                if (pdc.has(
                        NamespacedKeys.sourceSpawnerName,
                        PersistentDataType.STRING
                    )
                ) {
                    this.sourceSpawnerName = pdc.get(
                        NamespacedKeys.sourceSpawnerName,
                        PersistentDataType.STRING
                    )
                }
            } finally {
                releasePDCLock()
            }
        }

        if (this.sourceSpawnerName == null) {
            this.sourceSpawnerName = "(none)"
        }

        return this.sourceSpawnerName
    }

    fun getSourceSpawnEggName(): String? {
        if (this.sourceSpawnEggName != null) {
            return this.sourceSpawnEggName
        }

        if (getPDCLock()) {
            try {
                if (pdc.has(NamespacedKeys.spawnerEggName, PersistentDataType.STRING)) {
                    this.sourceSpawnEggName = pdc.get(
                        NamespacedKeys.spawnerEggName,
                        PersistentDataType.STRING
                    )
                }
            } finally {
                releasePDCLock()
            }
        }

        if (this.sourceSpawnEggName == null) {
            this.sourceSpawnEggName = "(none)"
        }

        return this.sourceSpawnEggName
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
        externalType: ExternalCompatibility
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

    val getOverridenEntityName: String?
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
            if (this.spawnedWGRegions == null || spawnedWGRegions!!.isEmpty()) {
                return ""
            }
            return spawnedWGRegions!![0]
        }

    fun setOverridenEntityName(name: String) {
        if (!getPDCLock()) {
            return
        }
        try {
            pdc.set(NamespacedKeys.overridenEntityNameKey, PersistentDataType.STRING, name)
        } finally {
            releasePDCLock()
        }
    }

    fun setShouldShowLM_Nametag(doShow: Boolean) {
        if (!getPDCLock()) {
            return
        }

        try {
            if (doShow && pdc.has(
                    NamespacedKeys.denyLmNametag,
                    PersistentDataType.INTEGER
                )
            ) {
                pdc.remove(NamespacedKeys.denyLmNametag)
            } else if (!doShow && !pdc.has(
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

    fun getShouldShowLM_Nametag(): Boolean {
        if (!getPDCLock()) {
            return true
        }

        try {
            return !pdc.has(NamespacedKeys.denyLmNametag, PersistentDataType.INTEGER)
        } finally {
            releasePDCLock()
        }
    }

    private fun setSpawnedTimeOfDay(
        ticks: Int
    ) {
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
                        ticks
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

        this.spawnedTimeOfDay = ticks
    }

    private fun getSpawnedTimeOfDay(): Int{
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

    override var spawnedTimeOfDay: Int? = null
        get() {
            if (field == null) {
                field = getSpawnedTimeOfDay()
            }

            return field
        }
        set(value) {
            field = value
            if (value != null)
                setSpawnedTimeOfDay(value)
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

        if (livingEntity is Monster || livingEntity is Boss
            || main.companion.hostileMobsGroup.contains(eType)
        ) {
            groups.add(CustomUniversalGroups.ALL_HOSTILE_MOBS.toString())
        }

        if (livingEntity is WaterMob || main.companion.aquaticMobsGroup.contains(eType)) {
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS.toString())
        }

        if (livingEntity.world.environment == World.Environment.NORMAL) {
            groups.add(CustomUniversalGroups.ALL_OVERWORLD_MOBS.toString())
        } else if (livingEntity.world.environment == World.Environment.NETHER) {
            groups.add(CustomUniversalGroups.ALL_NETHER_MOBS.toString())
        }

        if (livingEntity is Flying || eType == EntityType.PARROT || eType == EntityType.BAT) {
            groups.add(CustomUniversalGroups.ALL_FLYING_MOBS.toString())
        }

        // why bats aren't part of Flying interface is beyond me
        if ((livingEntity !is Flying && livingEntity !is WaterMob
                    && livingEntity !is Boss) && eType != EntityType.BAT
        ) {
            groups.add(CustomUniversalGroups.ALL_GROUND_MOBS.toString())
        }

        if (livingEntity is WaterMob || main.companion.aquaticMobsGroup.contains(eType)) {
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS.toString())
        }

        if (livingEntity is Animals && livingEntity !is Hoglin || livingEntity is WaterMob
            || main.companion.passiveMobsGroup.contains(eType)
        ) {
            groups.add(CustomUniversalGroups.ALL_PASSIVE_MOBS.toString())
        }

        return groups
    }

    val hashCode: Int
        get() = livingEntity.hashCode()
}