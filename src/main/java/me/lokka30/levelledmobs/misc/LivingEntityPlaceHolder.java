package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A wrapper for the LivingEntity class that provides various common function
 * and settings used for processing rules
 * Used only with the summon command
 *
 * @author stumper66
 */
public class LivingEntityPlaceHolder implements LivingEntityInterface {

    public LivingEntityPlaceHolder(final EntityType entityType, final @NotNull Location location, final @NotNull World world, final @NotNull LevelledMobs main){
        this.entityType = entityType;
        this.location = location;
        this.world = world;
        this.main = main;
    }

    private final LevelledMobs main;
    private final EntityType entityType;
    private final Location location;
    private final World world;
    private boolean groupsAreBuilt;
    private List<CustomUniversalGroups> applicableGroups;
    private Double calculatedDistanceFromSpawn;

    @Nullable
    public Double getCalculatedDistanceFromSpawn(){
        return calculatedDistanceFromSpawn;
    }

    public void setCalculatedDistanceFromSpawn(final double value){
        calculatedDistanceFromSpawn = value;
    }

    @NotNull
    public EntityType getEntityType() {
        return this.entityType;
    }

    @NotNull
    public Location getLocation() {
        return this.location;
    }

    @NotNull
    public World getWorld() {
        return this.world;
    }

    @NotNull
    public String getTypeName(){
        return this.entityType.name();
    }

    public List<RuleInfo> getApplicableRules(){
        return main.rulesManager.getApplicableRules(this);
    }

    @NotNull
    public LevelledMobs getMainInstance() {
        return this.main;
    }
}
