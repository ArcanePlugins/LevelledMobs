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
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
    public LivingEntityWrapper(final @NotNull LevelledMobs main){
        super(main);
        this.applicableGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.applicableRules = new LinkedList<>();
        this.mobExternalTypes = new LinkedList<>();
        this.spawnReason = LevelledMobSpawnReason.DEFAULT;
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM;
        this.cacheLock = new ReentrantLock(true);
    }

    private LivingEntity livingEntity;
    @NotNull
    private Set<String> applicableGroups;
    private boolean hasCache;
    private boolean isBuildingCache;
    private Integer mobLevel;
    private final static Object lockObj = new Object();
    @NotNull
    private List<RuleInfo> applicableRules;
    private List<String> spawnedWGRegions;
    @NotNull
    private final List<ExternalCompatibilityManager.ExternalCompatibility> mobExternalTypes;
    private FineTuningAttributes fineTuningAttributes;
    private LevelledMobSpawnReason spawnReason;
    public EntityDamageEvent.DamageCause deathCause;
    public boolean reEvaluateLevel;
    private boolean groupsAreBuilt;
    private Player playerForLevelling;
    private Map<String, Boolean> prevChanceRuleResults;
    private final ReentrantLock cacheLock;
    private final static Object playerLock = new Object();
    private String sourceSpawnerName;

    public void setLivingEntity(final @NotNull LivingEntity livingEntity){
        this.livingEntity = livingEntity;
        super.populateData(livingEntity.getWorld(), livingEntity.getLocation());
    }

    public void clearEntityData(){
        this.livingEntity = null;
        this.applicableGroups.clear();
        this.applicableRules.clear();
        this.mobExternalTypes.clear();
        this.spawnReason = LevelledMobSpawnReason.DEFAULT;
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM;
        this.isBuildingCache = false;
        this.hasCache = false;
        this.mobLevel = null;
        this.spawnedWGRegions = null;
        this.fineTuningAttributes = null;
        this.reEvaluateLevel = false;
        this.groupsAreBuilt = false;
        this.playerForLevelling = null;
        this.prevChanceRuleResults = null;
        this.sourceSpawnerName = null;

        super.clearEntityData();
    }

    private void buildCache(){
        if (isBuildingCache || this.hasCache) return;

        try{
            if (!this.cacheLock.tryLock(1000, TimeUnit.MILLISECONDS)) {
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
            this.applicableRules = applicableRulesResult.allApplicableRules;
            checkChanceRules(applicableRulesResult);
            this.fineTuningAttributes = main.rulesManager.getFineTuningAttributes(this);
            this.isBuildingCache = false;
        } catch (InterruptedException e) {
            Utils.logger.warning("exception in buildCache: " + e.getMessage());
        } finally {
            if (cacheLock.isHeldByCurrentThread())
                cacheLock.unlock();
        }
    }

    public void invalidateCache(){
        this.hasCache = false;
        this.groupsAreBuilt = false;
        this.applicableGroups.clear();
        this.applicableRules.clear();
    }

    private void checkChanceRules(final ApplicableRulesResult result){
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

        synchronized (this.livingEntity.getPersistentDataContainer()){
            if (sbAllowed.length() > 0)
                this.livingEntity.getPersistentDataContainer().set(main.levelManager.chanceRule_Allowed, PersistentDataType.STRING, sbAllowed.toString());
            if (sbDenied.length() > 0)
                this.livingEntity.getPersistentDataContainer().set(main.levelManager.chanceRule_Denied, PersistentDataType.STRING, sbDenied.toString());
        }
    }

    private void cachePrevChanceResults(){
        if (!main.rulesManager.anyRuleHasChance) return;

        String rulesPassed = null;
        String rulesDenied = null;

        synchronized (this.livingEntity.getPersistentDataContainer()){
            if (this.livingEntity.getPersistentDataContainer().has(main.levelManager.chanceRule_Allowed, PersistentDataType.STRING)){
                rulesPassed = this.livingEntity.getPersistentDataContainer().get(main.levelManager.chanceRule_Allowed, PersistentDataType.STRING);
            }
            if (this.livingEntity.getPersistentDataContainer().has(main.levelManager.chanceRule_Denied, PersistentDataType.STRING)){
                rulesDenied = this.livingEntity.getPersistentDataContainer().get(main.levelManager.chanceRule_Denied, PersistentDataType.STRING);
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

    @NotNull
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

    public EntityType getEntityType(){
        return this.livingEntity.getType();
    }

    @NotNull
    public PersistentDataContainer getPDC(){
        return livingEntity.getPersistentDataContainer();
    }

    public boolean isBabyMob() {
        if (livingEntity instanceof Zombie) {
            // for backwards compatibility
            Zombie zombie = (Zombie) livingEntity;
            try {
                zombie.isAdult();
                return !zombie.isAdult();
            } catch (NoSuchMethodError err) {
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
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            if (livingEntity.getPersistentDataContainer().has(main.levelManager.spawnReasonKey, PersistentDataType.STRING)) {
                return LevelledMobSpawnReason.valueOf(
                        livingEntity.getPersistentDataContainer().get(main.levelManager.spawnReasonKey, PersistentDataType.STRING)
                );
            }
        }

        return this.spawnReason;
    }

    public void setSpawnReason(final LevelledMobSpawnReason spawnReason) {
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            if (!livingEntity.getPersistentDataContainer().has(main.levelManager.spawnReasonKey, PersistentDataType.STRING)) {
                livingEntity.getPersistentDataContainer().set(main.levelManager.spawnReasonKey, PersistentDataType.STRING, spawnReason.toString());
            }
        }

        this.spawnReason = spawnReason;
    }

    public void setSourceSpawnerName(final String name) {
        this.sourceSpawnerName = name;
        synchronized (this.livingEntity.getPersistentDataContainer()){
            if (name == null && getPDC().has(main.levelManager.sourceSpawnerName, PersistentDataType.STRING))
                getPDC().remove(main.levelManager.sourceSpawnerName);
            else if (name != null)
                getPDC().set(main.levelManager.sourceSpawnerName, PersistentDataType.STRING, name);
        }
    }

    @Nullable
    public String getSourceSpawnerName(){
        String spawnerName = this.sourceSpawnerName;

        if (this.sourceSpawnerName == null){
            synchronized (livingEntity.getPersistentDataContainer()){
                if (getPDC().has(main.levelManager.sourceSpawnerName, PersistentDataType.STRING))
                    spawnerName = getPDC().get(main.levelManager.sourceSpawnerName, PersistentDataType.STRING);
            }
            if (spawnerName == null){
                this.sourceSpawnerName = "(none)";
                spawnerName = this.sourceSpawnerName;
            }
        }

        return spawnerName;
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
            return livingEntity.getPersistentDataContainer().has(main.levelManager.overridenEntityNameKey, PersistentDataType.STRING);
        }
    }

    @Nullable
    public String getOverridenEntityName(){
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            return livingEntity.getPersistentDataContainer().get(main.levelManager.overridenEntityNameKey, PersistentDataType.STRING);
        }
    }

    @Nullable
    public List<String> getSpawnedWGRegions(){
        if (!hasCache) buildCache();

        return this.spawnedWGRegions;
    }

    @NotNull
    public String getWGRegionName(){
        if (this.spawnedWGRegions == null || this.spawnedWGRegions.isEmpty()) return "";
        return this.spawnedWGRegions.get(0) == null ?
                "" : this.spawnedWGRegions.get(0);
    }

    public void setOverridenEntityName(final String name){
        synchronized (this.getLivingEntity().getPersistentDataContainer()) {
            livingEntity.getPersistentDataContainer().set(main.levelManager.overridenEntityNameKey, PersistentDataType.STRING, name);
        }
    }

    public void setShouldShowLM_Nametag(final boolean doShow){
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            if (doShow && getPDC().has(main.levelManager.denyLM_Nametag, PersistentDataType.INTEGER))
                getPDC().remove(main.levelManager.denyLM_Nametag);
            else if (!doShow && !getPDC().has(main.levelManager.denyLM_Nametag, PersistentDataType.INTEGER))
                getPDC().set(main.levelManager.denyLM_Nametag, PersistentDataType.INTEGER, 1);
        }
    }

    public boolean getShouldShowLM_Nametag(){
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            return !getPDC().has(main.levelManager.denyLM_Nametag, PersistentDataType.INTEGER);
        }
    }

    public void setSpawnedTimeOfDay(final int ticks){
        synchronized (livingEntity.getPersistentDataContainer()) {
            if (getPDC().has(main.levelManager.spawnedTimeOfDay, PersistentDataType.INTEGER))
                return;

            getPDC().set(main.levelManager.spawnedTimeOfDay, PersistentDataType.INTEGER, ticks);
        }

        this.spawnedTimeOfDay = ticks;
    }

    public int getSpawnedTimeOfDay(){
        if (this.spawnedTimeOfDay != null)
            return this.spawnedTimeOfDay;

        synchronized (livingEntity.getPersistentDataContainer()) {
            if (getPDC().has(main.levelManager.spawnedTimeOfDay, PersistentDataType.INTEGER)) {
                final Integer result = getPDC().get(main.levelManager.spawnedTimeOfDay, PersistentDataType.INTEGER);
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

        for (final String groupName : main.customMobGroups.keySet()){
            final Set<String> mobNames = main.customMobGroups.get(groupName);
            if (mobNames.contains(this.getTypeName()))
                groups.add(groupName);
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

        if (livingEntity.getWorld().getEnvironment().equals(World.Environment.NORMAL)){
            groups.add(CustomUniversalGroups.ALL_OVERWORLD_MOBS.toString());
        } else if (livingEntity.getWorld().getEnvironment().equals(World.Environment.NETHER)){
            groups.add(CustomUniversalGroups.ALL_NETHER_MOBS.toString());
        }

        if (livingEntity instanceof Flying || eType.equals(EntityType.PARROT) || eType.equals(EntityType.BAT)){
            groups.add(CustomUniversalGroups.ALL_FLYING_MOBS.toString());
        }

        // why bats aren't part of Flying interface is beyond me
        if (!(livingEntity instanceof Flying) && !(livingEntity instanceof WaterMob) && !(livingEntity instanceof Boss) && !(eType.equals(EntityType.BAT))){
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
