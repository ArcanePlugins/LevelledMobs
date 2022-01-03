/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.rules.ApplicableRulesResult;
import me.lokka30.levelledmobs.rules.FineTuningAttributes;
import me.lokka30.levelledmobs.rules.LevelledMobSpawnReason;
import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boss;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Flying;
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

import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 A wrapper for the LivingEntity class that provides various common function
 * and settings used for processing rules
 *
 * @author stumper66
 * @since 3.0.0
 */
public class LivingEntityWrapper extends LivingEntityWrapperBase implements LivingEntityInterface {
    private LivingEntityWrapper(final @NotNull LevelledMobs main){
        super(main);
        this.applicableGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.applicableRules = new LinkedList<>();
        this.mobExternalTypes = new LinkedList<>();
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM;
        this.cacheLock = new ReentrantLock(true);
        this.pdcLock = new ReentrantLock(true);
    }

    @Deprecated(since = "3.2.0")
    public LivingEntityWrapper(final @NotNull LivingEntity livingEntity, final @NotNull LevelledMobs main){
        // this constructor is provided for backwards compatibility only
        // to get an instance, LivingEntityWrapper#getInstance should be called instead
        // when finished with it, LivingEntityWrapper#free should be called

        super(main);
        this.applicableGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.applicableRules = new LinkedList<>();
        this.mobExternalTypes = new LinkedList<>();
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM;
        this.cacheLock = new ReentrantLock(true);
        this.pdcLock = new ReentrantLock(true);

        setLivingEntity(livingEntity);
    }

    // privates:
    private LivingEntity livingEntity;
    @NotNull
    private Set<String> applicableGroups;
    private boolean hasCache;
    private boolean isBuildingCache;
    private boolean groupsAreBuilt;
    private Integer mobLevel;
    private int nametagCooldownTime;
    private String sourceSpawnerName;
    private String sourceSpawnEggName;
    @NotNull
    private final List<RuleInfo> applicableRules;
    private List<String> spawnedWGRegions;
    @NotNull
    private final List<ExternalCompatibilityManager.ExternalCompatibility> mobExternalTypes;
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
    public Boolean playerLevellingAllowDecrease;
    public Set<Player> playersNeedingNametagCooldownUpdate;
    public EntityDamageEvent.DamageCause deathCause;
    public List<String> nbtData;
    public String pendingPlayerIdToSet;
    public Player playerForPermissionsCheck;
    public CommandSender summonedSender;

    @NotNull
    public static LivingEntityWrapper getInstance(final LivingEntity livingEntity, final @NotNull LevelledMobs main){
        final LivingEntityWrapper lew;

        synchronized (cachedLM_Wrappers_Lock) {
            if (cache.empty())
                lew = new LivingEntityWrapper(main);
            else
                lew = cache.pop();
        }

        if (main.cacheCheck == null)
            main.cacheCheck = LivingEntityWrapper.cache;

        lew.setLivingEntity(livingEntity);
        lew.inUseCount.set(1);
        return lew;
    }

    public void free(){
        if (inUseCount.decrementAndGet() > 0) return;
        if (!getIsPopulated()) return;

        clearEntityData();
        synchronized (cachedLM_Wrappers_Lock) {
            cache.push(this);
        }
    }

    private void setLivingEntity(final @NotNull LivingEntity livingEntity){
        this.livingEntity = livingEntity;
        super.populateData(livingEntity.getWorld(), livingEntity.getLocation());
    }

    public void clearEntityData(){
        this.livingEntity = null;
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
        this.playerForPermissionsCheck = null;
        this.playersNeedingNametagCooldownUpdate = null;
        this.nametagCooldownTime = 0;
        this.nbtData = null;
        this.summonedSender = null;
        this.playerLevellingAllowDecrease = null;
        this.pendingPlayerIdToSet = null;

        super.clearEntityData();
    }

    private void buildCache(){
        if (isBuildingCache || this.hasCache) return;

        try{
            if (!this.cacheLock.tryLock(500, TimeUnit.MILLISECONDS)) {
                Utils.logger.warning("lock timed out building cache");
                return;
            }

            if (this.hasCache) return;
            isBuildingCache = true;
            this.mobLevel = main.levelInterface.isLevelled(livingEntity) ?
                    main.levelInterface.getLevelOfMob(livingEntity) : null;

            this.spawnedWGRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(this);

            this.hasCache = true;
            // the lines below must remain after hasCache = true to prevent stack overflow
            cachePrevChanceResults();
            final ApplicableRulesResult applicableRulesResult = main.rulesManager.getApplicableRules(this);
            this.applicableRules.clear();
            this.applicableRules.addAll(applicableRulesResult.allApplicableRules);
            checkChanceRules(applicableRulesResult);
            this.fineTuningAttributes = main.rulesManager.getFineTuningAttributes(this);
            this.nametagCooldownTime = main.rulesManager.getRule_nametagVisibleTime(this);
            this.isBuildingCache = false;
        } catch (final InterruptedException e) {
            Utils.logger.warning("exception in buildCache: " + e.getMessage());
        } finally {
            if (cacheLock.isHeldByCurrentThread())
                cacheLock.unlock();
        }
    }

    private boolean getPDCLock(){
        try{
            // try up to 3 times to get a lock
            int retryCount = 0;
            while (true) {
                if (this.pdcLock.tryLock(15, TimeUnit.MILLISECONDS))
                    return true;

                final StackTraceElement callingFunction = Thread.currentThread().getStackTrace()[1];
                retryCount++;
                if (retryCount > lockMaxRetryTimes){
                    Utils.debugLog(main, DebugType.THREAD_LOCKS, String.format("getPDCLock could not lock thread - %s:%s",
                            callingFunction.getFileName(), callingFunction.getLineNumber()));
                    return false;
                }

                Utils.debugLog(main, DebugType.THREAD_LOCKS, String.format("getPDCLock retry %s - %s:%s",
                        retryCount, callingFunction.getFileName(), callingFunction.getLineNumber()));
            }
        }
        catch (final InterruptedException e){
            Utils.logger.warning("getPDCLock InterruptedException: " + e.getMessage());
            return false;
        }
    }

    private void releasePDCLock(){
        if (pdcLock.isHeldByCurrentThread())
            pdcLock.unlock();
    }

    public void invalidateCache(){
        this.hasCache = false;
        this.groupsAreBuilt = false;
        this.applicableGroups.clear();
        this.applicableRules.clear();
    }

    private void checkChanceRules(final @NotNull ApplicableRulesResult result){
        if (result.allApplicableRules_MadeChance.isEmpty() && result.allApplicableRules_DidNotMakeChance.isEmpty())
            return;

        final StringBuilder sbAllowed = new StringBuilder();
        for (final RuleInfo ruleInfo : result.allApplicableRules_MadeChance){
            if (sbAllowed.length() > 0) sbAllowed.append(";");
            sbAllowed.append(ruleInfo.getRuleName());
        }

        final StringBuilder sbDenied = new StringBuilder();
        for (final RuleInfo ruleInfo : result.allApplicableRules_DidNotMakeChance){
            if (sbDenied.length() > 0) sbDenied.append(";");
            sbDenied.append(ruleInfo.getRuleName());
        }

        if (!getPDCLock()) return;

        try {
            for (int i = 0; i < 2; i++) {
                try {
                    if (sbAllowed.length() > 0)
                        this.livingEntity.getPersistentDataContainer().set(main.namespaced_keys.chanceRule_Allowed, PersistentDataType.STRING, sbAllowed.toString());
                    if (sbDenied.length() > 0)
                        this.livingEntity.getPersistentDataContainer().set(main.namespaced_keys.chanceRule_Denied, PersistentDataType.STRING, sbDenied.toString());
                    break;
                } catch (final java.util.ConcurrentModificationException ignored) {
                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException ignored2) {
                        break;
                    }
                }
            }
        }
        finally {
            releasePDCLock();
        }
    }

    private void cachePrevChanceResults(){
        if (!main.rulesManager.anyRuleHasChance) return;

        String rulesPassed = null;
        String rulesDenied = null;

        if (getPDCLock()) {
            try {
                if (this.livingEntity.getPersistentDataContainer().has(main.namespaced_keys.chanceRule_Allowed, PersistentDataType.STRING)) {
                    rulesPassed = this.livingEntity.getPersistentDataContainer().get(main.namespaced_keys.chanceRule_Allowed, PersistentDataType.STRING);
                }
                if (this.livingEntity.getPersistentDataContainer().has(main.namespaced_keys.chanceRule_Denied, PersistentDataType.STRING)) {
                    rulesDenied = this.livingEntity.getPersistentDataContainer().get(main.namespaced_keys.chanceRule_Denied, PersistentDataType.STRING);
                }
            }
            finally {
                releasePDCLock();
            }
        }

        if (rulesPassed == null && rulesDenied == null) return;
        this.prevChanceRuleResults = new TreeMap<>();

        if (rulesPassed != null){
            for (final String ruleName : rulesPassed.split(";")){
                this.prevChanceRuleResults.put(ruleName, true);
            }
        }

        if (rulesDenied != null){
            for (final String ruleName : rulesDenied.split(";")){
                this.prevChanceRuleResults.put(ruleName, false);
            }
        }
    }

    @Nullable
    public Map<String, Boolean> getPrevChanceRuleResults(){
        return this.prevChanceRuleResults;
    }

    public LivingEntity getLivingEntity(){
        return this.livingEntity;
    }

    @NotNull
    public String getTypeName(){
        return this.livingEntity.getType().toString();
    }

    @NotNull
    public Set<String> getApplicableGroups(){
        if (!groupsAreBuilt){
            this.applicableGroups = buildApplicableGroupsForMob();
            groupsAreBuilt = true;
        }

        return this.applicableGroups;
    }

    public int getNametagCooldownTime(){
        if (!hasCache) buildCache();

        return this.nametagCooldownTime;
    }

    @Nullable
    public Player getPlayerForLevelling(){
        synchronized (playerLock){
            return this.playerForLevelling;
        }
    }

    public void setPlayerForLevelling(final Player player){
        synchronized (playerLock){
            this.playerForLevelling = player;
        }
        this.playerForPermissionsCheck = player;
    }

    @Nullable
    public FineTuningAttributes getFineTuningAttributes(){
        if (!hasCache) buildCache();

        return this.fineTuningAttributes;
    }

    @NotNull
    public List<RuleInfo> getApplicableRules(){
        if (!hasCache) buildCache();

        return this.applicableRules;
    }

    public int getMobLevel(){
        if (!hasCache) buildCache();

        return this.mobLevel == null ?
                0 :
                this.mobLevel;
    }

    public boolean isLevelled() {
        return main.levelInterface.isLevelled(this.livingEntity);
    }

    public @NotNull EntityType getEntityType(){
        return this.livingEntity.getType();
    }

    @NotNull
    public PersistentDataContainer getPDC(){
        return livingEntity.getPersistentDataContainer();
    }

    public boolean isBabyMob() {
        if (livingEntity instanceof Zombie) {
            // for backwards compatibility
            final Zombie zombie = (Zombie) livingEntity;
            try {
                zombie.isAdult();
                return !zombie.isAdult();
            } catch (final NoSuchMethodError err) {
                //noinspection deprecation
                return zombie.isBaby();
            }
        } else if (livingEntity instanceof Ageable){
            return !(((Ageable) livingEntity).isAdult());
        }

        return false;
    }

    @NotNull
    public LevelledMobSpawnReason getSpawnReason() {
        if (this.spawnReason != null) return this.spawnReason;

        if (!getPDCLock()) return LevelledMobSpawnReason.DEFAULT;
        boolean hadError = false;
        boolean succeeded = false;

        try {
            for (int i = 0; i < 2; i++) {
                try {
                    if (livingEntity.getPersistentDataContainer().has(main.namespaced_keys.spawnReasonKey, PersistentDataType.STRING)) {
                        this.spawnReason = LevelledMobSpawnReason.valueOf(
                                livingEntity.getPersistentDataContainer().get(main.namespaced_keys.spawnReasonKey, PersistentDataType.STRING)
                        );
                    }
                    succeeded = true;
                    break;
                } catch (ConcurrentModificationException ignored) {
                    hadError = true;
                    try
                    { Thread.sleep(5); }
                    catch (InterruptedException ignored2) { return LevelledMobSpawnReason.DEFAULT; }
                }
                finally {
                    releasePDCLock();
                }
            }
        }
        finally {
            releasePDCLock();
        }

        if (hadError) {
            if (succeeded)
                Utils.logger.warning("Got ConcurrentModificationException in LivingEntityWrapper getting spawn reason, succeeded on retry");
            else
                Utils.logger.warning("Got ConcurrentModificationException (2x) in LivingEntityWrapper getting spawn reason");
        }

        return this.spawnReason != null ?
                this.spawnReason : LevelledMobSpawnReason.DEFAULT;
    }

    public void setSpawnReason(final LevelledMobSpawnReason spawnReason) {
        this.spawnReason = spawnReason;

        if (!getPDCLock()) return;

        try {
            if (!livingEntity.getPersistentDataContainer().has(main.namespaced_keys.spawnReasonKey, PersistentDataType.STRING)) {
                livingEntity.getPersistentDataContainer().set(main.namespaced_keys.spawnReasonKey, PersistentDataType.STRING, spawnReason.toString());
            }
        }
        finally {
            releasePDCLock();
        }
    }

    public void setSourceSpawnerName(final String name) {
        this.sourceSpawnerName = name;

        if (!getPDCLock()) return;
        try {
            if (name == null && getPDC().has(main.namespaced_keys.sourceSpawnerName, PersistentDataType.STRING))
                getPDC().remove(main.namespaced_keys.sourceSpawnerName);
            else if (name != null)
                getPDC().set(main.namespaced_keys.sourceSpawnerName, PersistentDataType.STRING, name);
        }
        finally {
            releasePDCLock();
        }
    }

    @Nullable
    public String getSourceSpawnerName(){
        if (this.sourceSpawnerName != null) return this.sourceSpawnerName;

        if (getPDCLock()) {
            try {
                if (getPDC().has(main.namespaced_keys.sourceSpawnerName, PersistentDataType.STRING))
                    this.sourceSpawnerName = getPDC().get(main.namespaced_keys.sourceSpawnerName, PersistentDataType.STRING);
            } finally {
                releasePDCLock();
            }
        }

        if (this.sourceSpawnerName == null)
            this.sourceSpawnerName = "(none)";

        return this.sourceSpawnerName;
    }

    @Nullable
    public String getSourceSpawnEggName(){
        if (this.sourceSpawnEggName != null) return this.sourceSpawnEggName;

        if (getPDCLock()) {
            try {
                if (getPDC().has(main.namespaced_keys.spawnerEggName, PersistentDataType.STRING))
                    this.sourceSpawnEggName = getPDC().get(main.namespaced_keys.spawnerEggName, PersistentDataType.STRING);
            } finally {
                releasePDCLock();
            }
        }

        if (this.sourceSpawnEggName == null)
            this.sourceSpawnEggName = "(none)";

        return this.sourceSpawnEggName;
    }

    @NotNull
    public String getNameIfBaby(){
        return this.isBabyMob() ?
                "BABY_" + getTypeName() :
                getTypeName();
    }

    public boolean isMobTamed(){
        return (this.livingEntity instanceof Tameable && ((Tameable) this.livingEntity).isTamed());
    }

    public void setMobExternalType(final ExternalCompatibilityManager.ExternalCompatibility externalType){
        if (!this.mobExternalTypes.contains(externalType))
            this.mobExternalTypes.add(externalType);
    }

    @NotNull
    public List<ExternalCompatibilityManager.ExternalCompatibility> getMobExternalTypes() {
        return this.mobExternalTypes;
    }

    public boolean isMobOfExternalType(){
        return !this.mobExternalTypes.isEmpty();
    }

    public boolean isMobOfExternalType(final ExternalCompatibilityManager.ExternalCompatibility externalType){
        return this.mobExternalTypes.contains(externalType);
    }

    public boolean hasOverridenEntityName(){
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            return livingEntity.getPersistentDataContainer().has(main.namespaced_keys.overridenEntityNameKey, PersistentDataType.STRING);
        }
    }

    @Nullable
    public String getOverridenEntityName(){
        if (!getPDCLock()) return null;

        try {
            return livingEntity.getPersistentDataContainer().get(main.namespaced_keys.overridenEntityNameKey, PersistentDataType.STRING);
        }
        finally {
            releasePDCLock();
        }
    }

    @NotNull
    public String getWGRegionName(){
        if (this.spawnedWGRegions == null || this.spawnedWGRegions.isEmpty()) return "";
        return this.spawnedWGRegions.get(0) == null ?
                "" : this.spawnedWGRegions.get(0);
    }

    public void setOverridenEntityName(final String name){
        if (!getPDCLock()) return;
        try {
            livingEntity.getPersistentDataContainer().set(main.namespaced_keys.overridenEntityNameKey, PersistentDataType.STRING, name);
        }
        finally {
            releasePDCLock();
        }
    }

    public void setShouldShowLM_Nametag(final boolean doShow){
        if (!getPDCLock()) return;

        try {
            if (doShow && getPDC().has(main.namespaced_keys.denyLM_Nametag, PersistentDataType.INTEGER))
                getPDC().remove(main.namespaced_keys.denyLM_Nametag);
            else if (!doShow && !getPDC().has(main.namespaced_keys.denyLM_Nametag, PersistentDataType.INTEGER))
                getPDC().set(main.namespaced_keys.denyLM_Nametag, PersistentDataType.INTEGER, 1);
        }
        finally {
            releasePDCLock();
        }
    }

    public boolean getShouldShowLM_Nametag(){
        if (!getPDCLock()) return true;

        try {
            return !getPDC().has(main.namespaced_keys.denyLM_Nametag, PersistentDataType.INTEGER);
        }
        finally {
            releasePDCLock();
        }
    }

    public void setSpawnedTimeOfDay(final int ticks) {
        if (!getPDCLock()) return;

        try {
            for (int i = 0; i < 2; i++) {
                try {
                    if (getPDC().has(main.namespaced_keys.spawnedTimeOfDay, PersistentDataType.INTEGER))
                        return;

                    getPDC().set(main.namespaced_keys.spawnedTimeOfDay, PersistentDataType.INTEGER, ticks);
                } catch (final java.util.ConcurrentModificationException ignored) {
                    try {
                        Thread.sleep(10);
                    } catch (final InterruptedException ignored2) {
                        break;
                    }
                }
            }
        }
        finally {
            releasePDCLock();
        }

        this.spawnedTimeOfDay = ticks;
    }

    public int getSpawnedTimeOfDay(){
        if (this.spawnedTimeOfDay != null)
            return this.spawnedTimeOfDay;

        synchronized (livingEntity.getPersistentDataContainer()) {
            if (getPDC().has(main.namespaced_keys.spawnedTimeOfDay, PersistentDataType.INTEGER)) {
                final Integer result = getPDC().get(main.namespaced_keys.spawnedTimeOfDay, PersistentDataType.INTEGER);
                if (result != null) return result;
            }
        }

        final int result = (int) getWorld().getTime();
        setSpawnedTimeOfDay(result);

        return result;
    }

    @NotNull
    private Set<String> buildApplicableGroupsForMob(){
        final Set<String> groups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (final Map.Entry<String, Set<String>> mobGroup : main.customMobGroups.entrySet()){
            final Set<String> mobNames = mobGroup.getValue();
            if (mobNames.contains(this.getTypeName()))
                groups.add(mobGroup.getKey());
        }

        groups.add(CustomUniversalGroups.ALL_MOBS.toString());

        if (this.mobLevel != null)
            groups.add(CustomUniversalGroups.ALL_LEVELLABLE_MOBS.toString());
        final EntityType eType = livingEntity.getType();

        if (livingEntity instanceof Monster || livingEntity instanceof Boss || main.companion.groups_HostileMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_HOSTILE_MOBS.toString());
        }

        if (livingEntity instanceof WaterMob || main.companion.groups_AquaticMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS.toString());
        }

        if (livingEntity.getWorld().getEnvironment() == World.Environment.NORMAL){
            groups.add(CustomUniversalGroups.ALL_OVERWORLD_MOBS.toString());
        } else if (livingEntity.getWorld().getEnvironment() == World.Environment.NETHER){
            groups.add(CustomUniversalGroups.ALL_NETHER_MOBS.toString());
        }

        if (livingEntity instanceof Flying || eType == EntityType.PARROT || eType == EntityType.BAT){
            groups.add(CustomUniversalGroups.ALL_FLYING_MOBS.toString());
        }

        // why bats aren't part of Flying interface is beyond me
        if (!(livingEntity instanceof Flying) && !(livingEntity instanceof WaterMob) && !(livingEntity instanceof Boss) && eType != EntityType.BAT){
            groups.add(CustomUniversalGroups.ALL_GROUND_MOBS.toString());
        }

        if (livingEntity instanceof WaterMob || main.companion.groups_AquaticMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS.toString());
        }

        if (livingEntity instanceof Animals || livingEntity instanceof WaterMob || main.companion.groups_PassiveMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_PASSIVE_MOBS.toString());
        }

        return groups;
    }

    public boolean equals(final Object obj) {
        //null instanceof Object will always return false
        if (!(obj instanceof LivingEntityWrapper))
            return false;
        if (obj == this)
            return true;

        return this.livingEntity == ((LivingEntityWrapper) obj).livingEntity;
    }

    public int hashCode() {
        return livingEntity.hashCode();
    }
}
