/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.wrappers;

import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.CustomUniversalGroups;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.rules.ApplicableRulesResult;
import me.lokka30.levelledmobs.rules.FineTuningAttributes;
import me.lokka30.levelledmobs.rules.LevelledMobSpawnReason;
import me.lokka30.levelledmobs.rules.RuleInfo;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boss;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Flying;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper for the LivingEntity class that provides various common function and settings used for
 * processing rules
 *
 * @author stumper66
 * @since 3.0.0
 */
public class LivingEntityWrapper extends LivingEntityWrapperBase implements LivingEntityInterface {

    private LivingEntityWrapper(final @NotNull LevelledMobs main) {
        super(main);
        this.applicableGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.applicableRules = new LinkedList<>();
        this.mobExternalTypes = new LinkedList<>();
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM;
        this.cacheLock = new ReentrantLock(true);
        this.pdcLock = new ReentrantLock(true);
    }

    // privates:
    private LivingEntity livingEntity;
    private @NotNull Set<String> applicableGroups;
    private boolean hasCache;
    private boolean isBuildingCache;
    private boolean groupsAreBuilt;
    private boolean wasSummoned;
    public int chunkKillcount;
    private Integer mobLevel;
    private Integer skylightLevelAtSpawn;
    private long nametagCooldownTime;
    private String sourceSpawnerName;
    private String sourceSpawnEggName;
    private @NotNull final List<RuleInfo> applicableRules;
    private List<String> spawnedWGRegions;
    private @NotNull final List<ExternalCompatibilityManager.ExternalCompatibility> mobExternalTypes;
    private FineTuningAttributes fineTuningAttributes;
    private LevelledMobSpawnReason spawnReason;
    private Player playerForLevelling;
    private Map<String, Boolean> prevChanceRuleResults;
    private final ReentrantLock cacheLock;
    private final ReentrantLock pdcLock;
    private final static Object playerLock = new Object();
    private final static Object cachedLM_Wrappers_Lock = new Object();
    private final static Stack<LivingEntityWrapper> cache = new Stack<>();
    private final static int lockMaxRetryTimes = 3;
    // publics:
    public boolean reEvaluateLevel;
    public boolean wasPreviouslyLevelled;
    public boolean isRulesForceAll;
    public boolean isNewlySpawned;
    public boolean lockEntitySettings;
    public boolean hasLockedDropsOverride;
    public Boolean playerLevellingAllowDecrease;
    public Object libsDisguiseCache;
    public Set<Player> playersNeedingNametagCooldownUpdate;
    public EntityDamageEvent.DamageCause deathCause;
    public List<String> nbtData;
    public List<String> lockedCustomDrops;
    public String pendingPlayerIdToSet;
    public String lockedNametag;
    public String lockedOverrideName;
    public Player associatedPlayer;
    public CommandSender summonedSender;

    public @NotNull static LivingEntityWrapper getInstance(final LivingEntity livingEntity,
        final @NotNull LevelledMobs main) {
        final LivingEntityWrapper lew;

        synchronized (cachedLM_Wrappers_Lock) {
            if (cache.empty()) {
                lew = new LivingEntityWrapper(main);
            } else {
                lew = cache.pop();
            }
        }

        if (main.cacheCheck == null) {
            main.cacheCheck = LivingEntityWrapper.cache;
        }

        lew.setLivingEntity(livingEntity);
        lew.inUseCount.set(1);
        return lew;
    }

    public void free() {
        if (inUseCount.decrementAndGet() > 0) {
            return;
        }
        if (!getIsPopulated()) {
            return;
        }

        clearEntityData();
        synchronized (cachedLM_Wrappers_Lock) {
            cache.push(this);
        }
    }

    public static @NotNull String getLEWDebug(){
        int totalSize;
        int nonEmpties = 0;

        synchronized (cachedLM_Wrappers_Lock) {
            totalSize = cache.size();
            Enumeration<LivingEntityWrapper> enumeration = cache.elements();
            while (enumeration.hasMoreElements()){
                final LivingEntityWrapper lew = enumeration.nextElement();
                if (lew.hasCache) nonEmpties++;
            }
        }

        return String.format("size: %s, nonempties: %s", totalSize, nonEmpties);
    }

    public static void clearCache(){
        final List<LivingEntityWrapper> nonEmpties = new LinkedList<>();

        synchronized (cachedLM_Wrappers_Lock) {
            while (!cache.isEmpty()) {
                final LivingEntityWrapper lew = cache.pop();
                if (lew.hasCache) nonEmpties.add(lew);
            }

            for (LivingEntityWrapper lew : nonEmpties){
                cache.push(lew);
            }
            nonEmpties.clear();
        }
    }

    private void setLivingEntity(final @NotNull LivingEntity livingEntity) {
        this.livingEntity = livingEntity;
        super.populateData(livingEntity.getWorld(), livingEntity.getLocation());
    }

    public void clearEntityData() {
        this.livingEntity = null;
        this.libsDisguiseCache = null;
        this.chunkKillcount = 0;
        this.applicableGroups.clear();
        this.applicableRules.clear();
        this.mobExternalTypes.clear();
        this.spawnReason = null;
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM;
        this.isBuildingCache = false;
        this.hasCache = false;
        this.mobLevel = null;
        this.spawnedWGRegions = null;
        this.fineTuningAttributes = null;
        this.reEvaluateLevel = false;
        this.isRulesForceAll = false;
        this.wasPreviouslyLevelled = false;
        this.groupsAreBuilt = false;
        this.playerForLevelling = null;
        this.prevChanceRuleResults = null;
        this.sourceSpawnerName = null;
        this.sourceSpawnEggName = null;
        this.associatedPlayer = null;
        this.playersNeedingNametagCooldownUpdate = null;
        this.nametagCooldownTime = 0;
        this.nbtData = null;
        this.summonedSender = null;
        this.playerLevellingAllowDecrease = null;
        this.pendingPlayerIdToSet = null;
        this.skylightLevelAtSpawn = null;
        this.wasSummoned = false;
        this.lockedNametag = null;
        this.lockedOverrideName = null;
        this.isNewlySpawned = false;
        this.lockEntitySettings = false;
        this.hasLockedDropsOverride = false;
        this.lockedCustomDrops = null;

        super.clearEntityData();
    }

    private void buildCache() {
        if (isBuildingCache || this.hasCache) {
            return;
        }

        try {
            if (!this.cacheLock.tryLock(500, TimeUnit.MILLISECONDS)) {
                Utils.logger.warning("lock timed out building cache");
                return;
            }

            if (this.hasCache) {
                return;
            }
            isBuildingCache = true;
            this.mobLevel = main.levelInterface.isLevelled(livingEntity) ?
                main.levelInterface.getLevelOfMob(livingEntity) : null;

            try {
                this.wasSummoned = getPDC().has(main.namespacedKeys.wasSummoned,
                    PersistentDataType.INTEGER);
            } catch (Exception ignored) {
            }

            if (main.rulesManager.hasAnyWGCondition)
                this.spawnedWGRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(this);

            this.hasCache = true;
            // the lines below must remain after hasCache = true to prevent stack overflow
            cachePrevChanceResults();
            final ApplicableRulesResult applicableRulesResult = main.rulesManager.getApplicableRules(
                this);
            this.applicableRules.clear();
            this.applicableRules.addAll(applicableRulesResult.allApplicableRules);
            checkChanceRules(applicableRulesResult);
            this.fineTuningAttributes = main.rulesManager.getFineTuningAttributes(this);
            this.nametagCooldownTime = main.rulesManager.getRuleNametagVisibleTime(this);
            this.isBuildingCache = false;
        } catch (final InterruptedException e) {
            Utils.logger.warning("exception in buildCache: " + e.getMessage());
        } finally {
            if (cacheLock.isHeldByCurrentThread()) {
                cacheLock.unlock();
            }
        }
    }

    private boolean getPDCLock() {
        try {
            // try up to 3 times to get a lock
            int retryCount = 0;
            while (true) {
                if (this.pdcLock.tryLock(15, TimeUnit.MILLISECONDS)) {
                    return true;
                }

                final StackTraceElement callingFunction = Thread.currentThread().getStackTrace()[1];
                retryCount++;
                if (retryCount > lockMaxRetryTimes) {
                    DebugManager.log(DebugType.THREAD_LOCKS, () ->
                            String.format("getPDCLock could not lock thread - %s:%s",
                            callingFunction.getFileName(), callingFunction.getLineNumber()));
                    return false;
                }

                final int retryCountFinal = retryCount;
                DebugManager.log(DebugType.THREAD_LOCKS, () ->
                        String.format("getPDCLock retry %s - %s:%s",
                        retryCountFinal, callingFunction.getFileName(),
                        callingFunction.getLineNumber()));
            }
        } catch (final InterruptedException e) {
            Utils.logger.warning("getPDCLock InterruptedException: " + e.getMessage());
            return false;
        }
    }

    private void releasePDCLock() {
        if (pdcLock.isHeldByCurrentThread()) {
            pdcLock.unlock();
        }
    }

    public void invalidateCache() {
        this.hasCache = false;
        this.groupsAreBuilt = false;
        this.applicableGroups.clear();
        this.applicableRules.clear();
    }

    private void checkChanceRules(final @NotNull ApplicableRulesResult result) {
        if (result.allApplicableRules_MadeChance.isEmpty()
            && result.allApplicableRules_DidNotMakeChance.isEmpty()) {
            return;
        }

        final StringBuilder sbAllowed = new StringBuilder();
        for (final RuleInfo ruleInfo : result.allApplicableRules_MadeChance) {
            if (!sbAllowed.isEmpty()) {
                sbAllowed.append(";");
            }
            sbAllowed.append(ruleInfo.getRuleName());
        }

        final StringBuilder sbDenied = new StringBuilder();
        for (final RuleInfo ruleInfo : result.allApplicableRules_DidNotMakeChance) {
            if (!sbDenied.isEmpty()) {
                sbDenied.append(";");
            }
            sbDenied.append(ruleInfo.getRuleName());
        }

        if (!getPDCLock()) {
            return;
        }

        try {
            for (int i = 0; i < 2; i++) {
                try {
                    if (!sbAllowed.isEmpty()) {
                        this.livingEntity.getPersistentDataContainer()
                            .set(main.namespacedKeys.chanceRuleAllowed, PersistentDataType.STRING,
                                sbAllowed.toString());
                    }
                    if (!sbDenied.isEmpty()) {
                        this.livingEntity.getPersistentDataContainer()
                            .set(main.namespacedKeys.chanceRuleDenied, PersistentDataType.STRING,
                                sbDenied.toString());
                    }
                    break;
                } catch (final java.util.ConcurrentModificationException ignored) {
                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException ignored2) {
                        break;
                    }
                }
            }
        } finally {
            releasePDCLock();
        }
    }

    private void cachePrevChanceResults() {
        if (!main.rulesManager.anyRuleHasChance) {
            return;
        }

        String rulesPassed = null;
        String rulesDenied = null;

        if (getPDCLock()) {
            try {
                if (this.livingEntity.getPersistentDataContainer()
                    .has(main.namespacedKeys.chanceRuleAllowed, PersistentDataType.STRING)) {
                    rulesPassed = this.livingEntity.getPersistentDataContainer()
                        .get(main.namespacedKeys.chanceRuleAllowed, PersistentDataType.STRING);
                }
                if (this.livingEntity.getPersistentDataContainer()
                    .has(main.namespacedKeys.chanceRuleDenied, PersistentDataType.STRING)) {
                    rulesDenied = this.livingEntity.getPersistentDataContainer()
                        .get(main.namespacedKeys.chanceRuleDenied, PersistentDataType.STRING);
                }
            } finally {
                releasePDCLock();
            }
        }

        if (rulesPassed == null && rulesDenied == null) {
            return;
        }
        this.prevChanceRuleResults = new TreeMap<>();

        if (rulesPassed != null) {
            for (final String ruleName : rulesPassed.split(";")) {
                this.prevChanceRuleResults.put(ruleName, true);
            }
        }

        if (rulesDenied != null) {
            for (final String ruleName : rulesDenied.split(";")) {
                this.prevChanceRuleResults.put(ruleName, false);
            }
        }
    }

    public @Nullable Map<String, Boolean> getPrevChanceRuleResults() {
        return this.prevChanceRuleResults;
    }

    public LivingEntity getLivingEntity() {
        return this.livingEntity;
    }

    public @NotNull String getTypeName() {
        return this.livingEntity.getType().toString();
    }

    public @NotNull Set<String> getApplicableGroups() {
        if (!groupsAreBuilt) {
            this.applicableGroups = buildApplicableGroupsForMob();
            groupsAreBuilt = true;
        }

        return this.applicableGroups;
    }

    public long getNametagCooldownTime() {
        if (!hasCache) {
            buildCache();
        }

        return this.nametagCooldownTime;
    }

    public @Nullable Player getPlayerForLevelling() {
        synchronized (playerLock) {
            return this.playerForLevelling;
        }
    }

    public void setPlayerForLevelling(final Player player) {
        synchronized (playerLock) {
            this.playerForLevelling = player;
        }
        this.associatedPlayer = player;
    }

    public @Nullable FineTuningAttributes getFineTuningAttributes() {
        if (!hasCache) {
            buildCache();
        }

        return this.fineTuningAttributes;
    }

    public @NotNull List<RuleInfo> getApplicableRules() {
        if (!hasCache) {
            buildCache();
        }

        return this.applicableRules;
    }

    public int getMobLevel() {
        if (!hasCache) {
            buildCache();
        }

        return this.mobLevel == null ?
            0 :
            this.mobLevel;
    }

    public void setMobPrelevel(int level){
        this.mobLevel = level;
    }

    public boolean isLevelled() {
        return main.levelInterface.isLevelled(this.livingEntity);
    }

    public @NotNull EntityType getEntityType() {
        return this.livingEntity.getType();
    }

    public @NotNull PersistentDataContainer getPDC() {
        return livingEntity.getPersistentDataContainer();
    }

    public boolean isBabyMob() {
        if (livingEntity instanceof final Zombie zombie) {
            // for backwards compatibility
            try {
                zombie.isAdult();
                return !zombie.isAdult();
            } catch (final NoSuchMethodError err) {
                //noinspection deprecation
                return zombie.isBaby();
            }
        } else if (livingEntity instanceof Ageable) {
            return !(((Ageable) livingEntity).isAdult());
        }

        return false;
    }

    public @NotNull LevelledMobSpawnReason getSpawnReason() {
        if (this.spawnReason != null) {
            return this.spawnReason;
        }

        if (!getPDCLock()) {
            return LevelledMobSpawnReason.DEFAULT;
        }
        boolean hadError = false;
        boolean succeeded = false;

        try {
            for (int i = 0; i < 2; i++) {
                try {
                    if (livingEntity.getPersistentDataContainer()
                        .has(main.namespacedKeys.spawnReasonKey, PersistentDataType.STRING)) {
                        this.spawnReason = LevelledMobSpawnReason.valueOf(
                            livingEntity.getPersistentDataContainer()
                                .get(main.namespacedKeys.spawnReasonKey, PersistentDataType.STRING)
                        );
                    }
                    succeeded = true;
                    break;
                } catch (ConcurrentModificationException ignored) {
                    hadError = true;
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ignored2) {
                        return LevelledMobSpawnReason.DEFAULT;
                    }
                } finally {
                    releasePDCLock();
                }
            }
        } finally {
            releasePDCLock();
        }

        if (hadError) {
            if (succeeded) {
                Utils.logger.warning(
                    "Got ConcurrentModificationException in LivingEntityWrapper getting spawn reason, succeeded on retry");
            } else {
                Utils.logger.warning(
                    "Got ConcurrentModificationException (2x) in LivingEntityWrapper getting spawn reason");
            }
        }

        return this.spawnReason != null ?
            this.spawnReason : LevelledMobSpawnReason.DEFAULT;
    }

    public int getSkylightLevel() {
        if (this.skylightLevelAtSpawn != null) {
            return this.skylightLevelAtSpawn;
        }

        if (!getPDCLock()) {
            return getCurrentSkyLightLevel();
        }
        boolean hadError = false;
        boolean succeeded = false;

        try {
            for (int i = 0; i < 2; i++) {
                try {
                    if (livingEntity.getPersistentDataContainer()
                        .has(main.namespacedKeys.skyLightLevel, PersistentDataType.INTEGER)) {
                        this.skylightLevelAtSpawn = livingEntity.getPersistentDataContainer()
                            .get(main.namespacedKeys.skyLightLevel, PersistentDataType.INTEGER);
                    }
                    succeeded = true;
                    break;
                } catch (ConcurrentModificationException ignored) {
                    hadError = true;
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException ignored2) {
                        return 0;
                    }
                } finally {
                    releasePDCLock();
                }
            }
        } finally {
            releasePDCLock();
        }

        if (hadError) {
            if (succeeded) {
                Utils.logger.warning(
                    "Got ConcurrentModificationException in LivingEntityWrapper getting skyLightLevel, succeeded on retry");
            } else {
                Utils.logger.warning(
                    "Got ConcurrentModificationException (2x) in LivingEntityWrapper getting skyLightLevel");
            }
        }

        return this.skylightLevelAtSpawn != null ?
            this.skylightLevelAtSpawn : getCurrentSkyLightLevel();
    }

    public void setSkylightLevelAtSpawn() {
        this.skylightLevelAtSpawn = getCurrentSkyLightLevel();

        if (!getPDCLock()) {
            return;
        }

        try {
            if (!livingEntity.getPersistentDataContainer()
                .has(main.namespacedKeys.skyLightLevel, PersistentDataType.INTEGER)) {
                livingEntity.getPersistentDataContainer()
                    .set(main.namespacedKeys.skyLightLevel, PersistentDataType.INTEGER,
                        this.skylightLevelAtSpawn);
            }
        } finally {
            releasePDCLock();
        }
    }

    private int getCurrentSkyLightLevel() {
        return this.getLocation().getBlock().getLightFromSky();
    }

    public void setSpawnReason(final LevelledMobSpawnReason spawnReason) {
        setSpawnReason(spawnReason, false);
    }

    public void setSpawnReason(final LevelledMobSpawnReason spawnReason, final boolean doForce) {
        this.spawnReason = spawnReason;

        if (!getPDCLock()) {
            return;
        }

        try {
            if (doForce || !livingEntity.getPersistentDataContainer()
                .has(main.namespacedKeys.spawnReasonKey, PersistentDataType.STRING)) {
                livingEntity.getPersistentDataContainer()
                    .set(main.namespacedKeys.spawnReasonKey, PersistentDataType.STRING,
                        spawnReason.toString());
            }
        } finally {
            releasePDCLock();
        }
    }

    public void setSourceSpawnerName(final String name) {
        this.sourceSpawnerName = name;

        if (!getPDCLock()) {
            return;
        }
        try {
            if (name == null && getPDC().has(main.namespacedKeys.sourceSpawnerName,
                PersistentDataType.STRING)) {
                getPDC().remove(main.namespacedKeys.sourceSpawnerName);
            } else if (name != null) {
                getPDC().set(main.namespacedKeys.sourceSpawnerName, PersistentDataType.STRING,
                    name);
            }
        } finally {
            releasePDCLock();
        }
    }

    public @Nullable String getSourceSpawnerName() {
        if (this.sourceSpawnerName != null) {
            return this.sourceSpawnerName;
        }

        if (getPDCLock()) {
            try {
                if (getPDC().has(main.namespacedKeys.sourceSpawnerName,
                    PersistentDataType.STRING)) {
                    this.sourceSpawnerName = getPDC().get(main.namespacedKeys.sourceSpawnerName,
                        PersistentDataType.STRING);
                }
            } finally {
                releasePDCLock();
            }
        }

        if (this.sourceSpawnerName == null) {
            this.sourceSpawnerName = "(none)";
        }

        return this.sourceSpawnerName;
    }

    public @Nullable String getSourceSpawnEggName() {
        if (this.sourceSpawnEggName != null) {
            return this.sourceSpawnEggName;
        }

        if (getPDCLock()) {
            try {
                if (getPDC().has(main.namespacedKeys.spawnerEggName, PersistentDataType.STRING)) {
                    this.sourceSpawnEggName = getPDC().get(main.namespacedKeys.spawnerEggName,
                        PersistentDataType.STRING);
                }
            } finally {
                releasePDCLock();
            }
        }

        if (this.sourceSpawnEggName == null) {
            this.sourceSpawnEggName = "(none)";
        }

        return this.sourceSpawnEggName;
    }

    public @NotNull String getNameIfBaby() {
        return this.isBabyMob() ?
            "BABY_" + getTypeName() :
            getTypeName();
    }

    public boolean isMobTamed() {
        return (this.livingEntity instanceof Tameable && ((Tameable) this.livingEntity).isTamed());
    }

    public void setMobExternalType(
        final ExternalCompatibilityManager.ExternalCompatibility externalType) {
        if (!this.mobExternalTypes.contains(externalType)) {
            this.mobExternalTypes.add(externalType);
        }
    }

    public @NotNull List<ExternalCompatibilityManager.ExternalCompatibility> getMobExternalTypes() {
        return this.mobExternalTypes;
    }

    public boolean isMobOfExternalType() {
        return !this.mobExternalTypes.isEmpty();
    }

    public boolean isMobOfExternalType(
        final ExternalCompatibilityManager.ExternalCompatibility externalType) {
        return this.mobExternalTypes.contains(externalType);
    }

    public boolean hasOverridenEntityName() {
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            return livingEntity.getPersistentDataContainer()
                .has(main.namespacedKeys.overridenEntityNameKey, PersistentDataType.STRING);
        }
    }

    public @Nullable String getOverridenEntityName() {
        if (!getPDCLock()) {
            return null;
        }

        try {
            return livingEntity.getPersistentDataContainer()
                .get(main.namespacedKeys.overridenEntityNameKey, PersistentDataType.STRING);
        } finally {
            releasePDCLock();
        }
    }

    public @NotNull String getWGRegionName() {
        if (this.spawnedWGRegions == null || this.spawnedWGRegions.isEmpty()) {
            return "";
        }
        return this.spawnedWGRegions.get(0) == null ?
            "" : this.spawnedWGRegions.get(0);
    }

    public void setOverridenEntityName(final String name) {
        if (!getPDCLock()) {
            return;
        }
        try {
            livingEntity.getPersistentDataContainer()
                .set(main.namespacedKeys.overridenEntityNameKey, PersistentDataType.STRING, name);
        } finally {
            releasePDCLock();
        }
    }

    public void setShouldShowLM_Nametag(final boolean doShow) {
        if (!getPDCLock()) {
            return;
        }

        try {
            if (doShow && getPDC().has(main.namespacedKeys.denyLmNametag,
                PersistentDataType.INTEGER)) {
                getPDC().remove(main.namespacedKeys.denyLmNametag);
            } else if (!doShow && !getPDC().has(main.namespacedKeys.denyLmNametag,
                PersistentDataType.INTEGER)) {
                getPDC().set(main.namespacedKeys.denyLmNametag, PersistentDataType.INTEGER, 1);
            }
        } finally {
            releasePDCLock();
        }
    }

    public boolean getShouldShowLM_Nametag() {
        if (!getPDCLock()) {
            return true;
        }

        try {
            return !getPDC().has(main.namespacedKeys.denyLmNametag, PersistentDataType.INTEGER);
        } finally {
            releasePDCLock();
        }
    }

    public void setSpawnedTimeOfDay(final int ticks) {
        if (!getPDCLock()) {
            return;
        }

        try {
            for (int i = 0; i < 2; i++) {
                try {
                    if (getPDC().has(main.namespacedKeys.spawnedTimeOfDay,
                        PersistentDataType.INTEGER)) {
                        return;
                    }

                    getPDC().set(main.namespacedKeys.spawnedTimeOfDay, PersistentDataType.INTEGER,
                        ticks);
                } catch (final java.util.ConcurrentModificationException ignored) {
                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException ignored2) {
                        break;
                    }
                }
            }
        } finally {
            releasePDCLock();
        }

        this.spawnedTimeOfDay = ticks;
    }

    public int getSpawnedTimeOfDay() {
        if (this.spawnedTimeOfDay != null) {
            return this.spawnedTimeOfDay;
        }

        synchronized (livingEntity.getPersistentDataContainer()) {
            if (getPDC().has(main.namespacedKeys.spawnedTimeOfDay, PersistentDataType.INTEGER)) {
                final Integer result = getPDC().get(main.namespacedKeys.spawnedTimeOfDay,
                    PersistentDataType.INTEGER);
                if (result != null) {
                    return result;
                }
            }
        }

        final int result = (int) getWorld().getTime();
        setSpawnedTimeOfDay(result);

        return result;
    }

    public Integer getSummonedLevel() {
        return summonedLevel;
    }

    public boolean isWasSummoned() {
        if (!hasCache) {
            buildCache();
        }

        return this.wasSummoned;
    }

    private @NotNull Set<String> buildApplicableGroupsForMob() {
        final Set<String> groups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (final Map.Entry<String, Set<String>> mobGroup : main.customMobGroups.entrySet()) {
            final Set<String> mobNames = mobGroup.getValue();
            if (mobNames.contains(this.getTypeName())) {
                groups.add(mobGroup.getKey());
            }
        }

        groups.add(CustomUniversalGroups.ALL_MOBS.toString());

        if (this.mobLevel != null) {
            groups.add(CustomUniversalGroups.ALL_LEVELLABLE_MOBS.toString());
        }
        final EntityType eType = livingEntity.getType();

        if (livingEntity instanceof Monster || livingEntity instanceof Boss
            || main.companion.hostileMobsGroup.contains(eType)) {
            groups.add(CustomUniversalGroups.ALL_HOSTILE_MOBS.toString());
        }

        if (livingEntity instanceof WaterMob || main.companion.aquaticMobsGroup.contains(eType)) {
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS.toString());
        }

        if (livingEntity.getWorld().getEnvironment() == World.Environment.NORMAL) {
            groups.add(CustomUniversalGroups.ALL_OVERWORLD_MOBS.toString());
        } else if (livingEntity.getWorld().getEnvironment() == World.Environment.NETHER) {
            groups.add(CustomUniversalGroups.ALL_NETHER_MOBS.toString());
        }

        if (livingEntity instanceof Flying || eType == EntityType.PARROT
            || eType == EntityType.BAT) {
            groups.add(CustomUniversalGroups.ALL_FLYING_MOBS.toString());
        }

        // why bats aren't part of Flying interface is beyond me
        if (!(livingEntity instanceof Flying) && !(livingEntity instanceof WaterMob)
            && !(livingEntity instanceof Boss) && eType != EntityType.BAT) {
            groups.add(CustomUniversalGroups.ALL_GROUND_MOBS.toString());
        }

        if (livingEntity instanceof WaterMob || main.companion.aquaticMobsGroup.contains(eType)) {
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS.toString());
        }

        if (livingEntity instanceof Animals && !(livingEntity instanceof Hoglin) || livingEntity instanceof WaterMob
            || main.companion.passiveMobsGroup.contains(eType)) {
            groups.add(CustomUniversalGroups.ALL_PASSIVE_MOBS.toString());
        }

        return groups;
    }

    public boolean equals(final Object obj) {
        //null instanceof Object will always return false
        if (!(obj instanceof LivingEntityWrapper)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        return this.livingEntity == ((LivingEntityWrapper) obj).livingEntity;
    }

    public int hashCode() {
        return livingEntity.hashCode();
    }
}
