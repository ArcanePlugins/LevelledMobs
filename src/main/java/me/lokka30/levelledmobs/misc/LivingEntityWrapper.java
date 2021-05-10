package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.rules.FineTuningAttributes;
import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LivingEntityWrapper implements LivingEntityInterface {
    public LivingEntityWrapper(final @NotNull LivingEntity livingEntity, final @NotNull LevelledMobs main){
        this.main = main;
        this.livingEntity = livingEntity;
        this.applicableGroups = new LinkedList<>();
        this.applicableRules = new LinkedList<>();
        this.mobExternalType = ExternalCompatibilityManager.ExternalCompatibility.NOT_APPLICABLE;
        this.spawnReason = CreatureSpawnEvent.SpawnReason.DEFAULT;
    }

    private final LevelledMobs main;
    private final LivingEntity livingEntity;
    @NotNull
    private List<CustomUniversalGroups> applicableGroups;
    private boolean hasCache;
    private boolean groupsAreBuilt;
    private Integer mobLevel;
    @NotNull
    private List<RuleInfo> applicableRules;
    private List<String> spawnedWGRegions;
    private ExternalCompatibilityManager.ExternalCompatibility mobExternalType;
    private FineTuningAttributes fineTuningAttributes;
    private CreatureSpawnEvent.SpawnReason spawnReason;

    @NotNull
    public LevelledMobs getMainInstance(){
        return this.main;
    }

    private void buildCache(){
        this.mobLevel = main.levelInterface.isLevelled(livingEntity) ?
                main.levelInterface.getLevelOfMob(livingEntity) : null;
        this.spawnedWGRegions = ExternalCompatibilityManager.getWGRegionsAtLocation(this);

        this.hasCache = true;
        // the lines below must remain after hasCache = true to prevent stack overflow
        this.applicableRules = main.rulesManager.getApplicableRules(this);
        this.fineTuningAttributes = main.rulesManager.getFineTuningAttributes(this);
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
    public List<CustomUniversalGroups> getApplicableGroups(){
        if (!groupsAreBuilt){
            this.applicableGroups = buildApplicableGroupsForMob();
            groupsAreBuilt = true;
        }

        return this.applicableGroups;
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

    public boolean isLevelled(){
        if (!hasCache) buildCache();

        return this.mobLevel != null;
    }

    @NotNull
    public World getWorld(){
        return this.livingEntity.getWorld();
    }

    @NotNull
    public Location getLocation(){
        return this.livingEntity.getLocation();
    }

    public EntityType getEntityType(){
        return this.livingEntity.getType();
    }

    @NotNull
    public String getWorldName(){
        return livingEntity.getWorld().getName();
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
        if (livingEntity.getPersistentDataContainer().has(main.levelManager.spawnReasonKey, PersistentDataType.STRING)){
            return CreatureSpawnEvent.SpawnReason.valueOf(
                    livingEntity.getPersistentDataContainer().get(main.levelManager.spawnReasonKey, PersistentDataType.STRING)
            );
        }

        return this.spawnReason;
    }

    public void setSpawnReason(final CreatureSpawnEvent.SpawnReason spawnReason){
        if (!livingEntity.getPersistentDataContainer().has(main.levelManager.spawnReasonKey, PersistentDataType.STRING)){
            livingEntity.getPersistentDataContainer().set(main.levelManager.spawnReasonKey, PersistentDataType.STRING, spawnReason.toString());
        }
        this.spawnReason = spawnReason;
    }

    @NotNull
    public String getNameIfBaby(){
        return this.isBabyMob() ?
                "BABY_" + getTypeName() :
                getTypeName();
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
        return livingEntity.getPersistentDataContainer().has(main.levelManager.overridenEntityNameKey, PersistentDataType.STRING);
    }

    @Nullable
    public String getOverridenEntityName(){
        return livingEntity.getPersistentDataContainer().get(main.levelManager.overridenEntityNameKey, PersistentDataType.STRING);
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
        livingEntity.getPersistentDataContainer().set(main.levelManager.overridenEntityNameKey, PersistentDataType.STRING, name);
    }

    @NotNull
    private List<CustomUniversalGroups> buildApplicableGroupsForMob(){
        final List<CustomUniversalGroups> groups = new ArrayList<>();
        groups.add(CustomUniversalGroups.ALL_MOBS);

        final boolean isLevellable = true;

        //if (isLevelled || isLevellable)
        if (this.mobLevel != null)
            groups.add(CustomUniversalGroups.ALL_LEVELLABLE_MOBS);
        final EntityType eType = livingEntity.getType();

        if (livingEntity instanceof Monster || livingEntity instanceof Boss || main.companion.groups_HostileMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_HOSTILE_MOBS);
        }

        if (livingEntity instanceof WaterMob || main.companion.groups_AquaticMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS);
        }

        if (livingEntity.getWorld().getEnvironment().equals(World.Environment.NORMAL)){
            groups.add(CustomUniversalGroups.ALL_OVERWORLD_MOBS);
        } else if (livingEntity.getWorld().getEnvironment().equals(World.Environment.NETHER)){
            groups.add(CustomUniversalGroups.ALL_NETHER_MOBS);
        }

        if (livingEntity instanceof Flying || eType.equals(EntityType.PARROT) || eType.equals(EntityType.BAT)){
            groups.add(CustomUniversalGroups.ALL_FLYING_MOBS);
        }

        // why bats aren't part of Flying interface is beyond me
        if (!(livingEntity instanceof Flying) && !(livingEntity instanceof WaterMob) && !(livingEntity instanceof Boss) && !(eType.equals(EntityType.BAT))){
            groups.add(CustomUniversalGroups.ALL_GROUND_MOBS);
        }

        if (livingEntity instanceof WaterMob || main.companion.groups_AquaticMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_AQUATIC_MOBS);
        }

        if (livingEntity instanceof Animals || livingEntity instanceof WaterMob || main.companion.groups_PassiveMobs.contains(eType)){
            groups.add(CustomUniversalGroups.ALL_PASSIVE_MOBS);
        }

        return groups;
    }
}
