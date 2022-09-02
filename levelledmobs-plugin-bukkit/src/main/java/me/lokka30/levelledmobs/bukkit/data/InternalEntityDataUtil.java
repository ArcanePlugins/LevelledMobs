package me.lokka30.levelledmobs.bukkit.data;

import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

import com.jeff_media.morepersistentdatatypes.DataType;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.api.data.EntityDataUtil;
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class InternalEntityDataUtil extends EntityDataUtil {

    private InternalEntityDataUtil() {
        throw new IllegalStateException("Instantiation of utility-type class");
    }

    /*
    WARNING: This method does NOT use a memory data cache.
    It is therefore NOT applicable for use in EntitySpawnEvent.
     */
    public static Set<String> getDropTableIds(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");

        final var result = getPdc(entity).get(EntityKeyStore.dropTableIds,
            DataType.asGenericCollection(HashSet::new, DataType.STRING));

        if(result == null) {
            return new HashSet<>();
        }

        return result;
    }

    /*
    WARNING: This method does NOT use a memory data cache.
    It is therefore NOT applicable for use in EntitySpawnEvent.
     */
    public static void setDropTableIds(
        final @NotNull LivingEntity entity,
        final @NotNull Set<String> dropTableIds
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(dropTableIds, "dropTableIds");
        getPdc(entity).set(EntityKeyStore.dropTableIds, DataType.asSet(DataType.STRING), dropTableIds);
    }

    public static void setLevel(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.level, INTEGER, to);
    }

    public static void setMadeOverallChance(final @NotNull LivingEntity entity, final boolean to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.madeOverallChance, INTEGER, boolToInt(to));
    }

    public static void setMinLevel(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.minLevel, INTEGER, to);
    }

    public static void setMaxLevel(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.maxLevel, INTEGER, to);
    }

    public static void setOverriddenName(final @NotNull LivingEntity entity, final @NotNull String to) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");
        setData(entity, EntityKeyStore.overriddenName, STRING, to);
    }

    public static void setSourceSpawnerName(final @NotNull LivingEntity entity, final @NotNull String to) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");
        setData(entity, EntityKeyStore.sourceSpawnerName, STRING, to);
    }

    public static void setSpawnTimeOfDay(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.spawnTimeOfDay, INTEGER, to);
    }

    public static void setSpawnSkyLightLevel(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.spawnSkyLightLevel, INTEGER, to);
    }

    public static void setWasBaby(final @NotNull LivingEntity entity, final boolean to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.wasBaby, INTEGER, boolToInt(to));
    }

    public static void setWasSummoned(final @NotNull LivingEntity entity, final boolean to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.wasSummoned, INTEGER, boolToInt(to));
    }

    public static void unlevelMob(final @NotNull LivingEntity entity) {
        //TODO remove some PDC keys
        //TODO remove attribute multipliers             - use new PDC keys to check which ones LM modified.
        //TODO if LM set a custom name then remove it   - use new pDC key to check if LM set the current custom name.
        //TODO update mob nametag
        Log.inf("can't unlevel mobs yet: logic missing.");
    }

}
