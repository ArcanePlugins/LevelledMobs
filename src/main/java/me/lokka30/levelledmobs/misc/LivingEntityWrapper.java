package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.rules.FineTuningAttributes;
import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
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
 */
public class LivingEntityWrapper extends LivingEntityWrapperBase implements LivingEntityInterface {
    public LivingEntityWrapper(final @NotNull LivingEntity livingEntity, final @NotNull LevelledMobs main){
        super(main, livingEntity.getWorld(), livingEntity.getLocation());
        this.livingEntity = livingEntity;
        this.applicableGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.applicableRules = new LinkedList<>();
        this.mobExternalType = ExternalCompatibilityManager.ExternalCompatibility.NOT_APPLICABLE;
        this.spawnReason = CreatureSpawnEvent.SpawnReason.DEFAULT;
        this.deathCause = EntityDamageEvent.DamageCause.CUSTOM;
        this.cacheLock = new ReentrantLock(true);
    }

    private final LivingEntity livingEntity;
    @NotNull
    private Set<String> applicableGroups;
    private boolean hasCache;
    private boolean isBuildingCache;
    private Integer mobLevel;
    private final static Object lockObj = new Object();
    @NotNull
    private List<RuleInfo> applicableRules;
    private List<String> spawnedWGRegions;
    private ExternalCompatibilityManager.ExternalCompatibility mobExternalType;
    private FineTuningAttributes fineTuningAttributes;
    private CreatureSpawnEvent.SpawnReason spawnReason;
    public EntityDamageEvent.DamageCause deathCause;
    public String mythicMobInternalName;
    public boolean reEvaluateLevel;
    private boolean groupsAreBuilt;
    private Double calculatedDistanceFromSpawn;
    private Player playerForLevelling;
    private final ReentrantLock cacheLock;
    private final static Object playerLock = new Object();

    private void buildCache(){
        if (isBuildingCache || this.hasCache) return;

        try{
            if (!this.cacheLock.tryLock(1000, TimeUnit.MILLISECONDS)) return;

            if (this.hasCache) return;
            isBuildingCache = true;
            this.mobLevel = main.levelInterface.isLevelled(livingEntity) ?
                    main.levelInterface.getLevelOfMob(livingEntity) : null;

            this.spawnedWGRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(this);

            this.hasCache = true;
            // the lines below must remain after hasCache = true to prevent stack overflow
            this.applicableRules = main.rulesManager.getApplicableRules(this);
            this.fineTuningAttributes = main.rulesManager.getFineTuningAttributes(this);
            this.isBuildingCache = false;
        } catch (InterruptedException e) {
            Utils.logger.warning("exception in buildCache: " + e.getMessage());
        }
        finally {
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
                -1 :
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
    public CreatureSpawnEvent.SpawnReason getSpawnReason() {
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            if (livingEntity.getPersistentDataContainer().has(main.levelManager.spawnReasonKey, PersistentDataType.STRING)) {
                return CreatureSpawnEvent.SpawnReason.valueOf(
                        livingEntity.getPersistentDataContainer().get(main.levelManager.spawnReasonKey, PersistentDataType.STRING)
                );
            }
        }

        return this.spawnReason;
    }

    public void setSpawnReason(final CreatureSpawnEvent.SpawnReason spawnReason){
        synchronized (this.livingEntity.getPersistentDataContainer()) {
            if (!livingEntity.getPersistentDataContainer().has(main.levelManager.spawnReasonKey, PersistentDataType.STRING)) {
                livingEntity.getPersistentDataContainer().set(main.levelManager.spawnReasonKey, PersistentDataType.STRING, spawnReason.toString());
            }
        }

        this.spawnReason = spawnReason;
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
        this.mobExternalType = externalType;
    }

    @NotNull
    public ExternalCompatibilityManager.ExternalCompatibility getMobExternalType(){
        return this.mobExternalType;
    }

    public boolean isMobOfExternalType(){
        return this.mobExternalType != null;
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
