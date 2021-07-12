package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Interace used for wrapping LivingEntity to provide additions common commands
 * and information
 *
 * @author stumper66
 */
public interface LivingEntityInterface {

    @NotNull
    EntityType getEntityType();

    Location getLocation();

    World getWorld();

    @NotNull
    String getTypeName();

    @NotNull
    List<RuleInfo> getApplicableRules();

    @NotNull
    LevelledMobs getMainInstance();

    @Nullable
    Double getCalculatedDistanceFromSpawn();

    void setCalculatedDistanceFromSpawn(final double value);
}
