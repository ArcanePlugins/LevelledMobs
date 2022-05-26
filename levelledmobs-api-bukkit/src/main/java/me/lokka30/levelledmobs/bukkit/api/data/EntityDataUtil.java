package me.lokka30.levelledmobs.bukkit.api.data;

import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
FIXME Comment

Anything that this utility does not provide for levelled mobs is purposefully only accessible via
LM's plugin internals.
 */
@SuppressWarnings("unused")
public class EntityDataUtil {

    /*
    FIXME Comment
     */
    public static boolean getDeniesLabel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if(getPdc(entity).has(EntityKeyStore.deniesLabel, INTEGER)) {
            // noinspection ConstantConditions
            return getPdc(entity).get(EntityKeyStore.wasBaby, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    FIXME Comment
     */
    public static void setDeniesLabel(final @NotNull LivingEntity entity, final boolean to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.deniesLabel, INTEGER, boolToInt(to));
    }

    /*
    FIXME Comment
     */
    public static boolean isLevelled(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getPdc(entity).has(EntityKeyStore.level, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getLevel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        // noinspection ConstantConditions
        return getPdc(entity).get(EntityKeyStore.level, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static boolean getMadeOverallChance(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if(getPdc(entity).has(EntityKeyStore.madeOverallChance, INTEGER)) {
            // noinspection ConstantConditions
            return getPdc(entity).get(EntityKeyStore.wasBaby, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    FIXME Comment
     */
    public static int getMinLevel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        // noinspection ConstantConditions
        return getPdc(entity).get(EntityKeyStore.minLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getMaxLevel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        // noinspection ConstantConditions
        return getPdc(entity).get(EntityKeyStore.maxLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static String getOverriddenName(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getPdc(entity).get(EntityKeyStore.overriddenName, STRING);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static String getSourceSpawnerName(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getPdc(entity).get(EntityKeyStore.spawnSkyLightLevel, STRING);
    }

    /*
    FIXME Comment
     */
    public static int getSpawnTimeOfDay(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        // noinspection ConstantConditions
        return getPdc(entity).get(EntityKeyStore.spawnTimeOfDay, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getSpawnSkyLightLevel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        // noinspection ConstantConditions
        return getPdc(entity).get(EntityKeyStore.spawnSkyLightLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static boolean getWasBabyMob(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if(getPdc(entity).has(EntityKeyStore.wasBaby, INTEGER)) {
            // noinspection ConstantConditions
            return getPdc(entity).get(EntityKeyStore.wasBaby, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    FIXME Comment
     */
    public static boolean getWasSummoned(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if(getPdc(entity).has(EntityKeyStore.wasSummoned, INTEGER)) {
            // noinspection ConstantConditions
            return getPdc(entity).get(EntityKeyStore.wasSummoned, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    Short hand of grabbing the entity's PDC. That's all! :)

    Warning: Please only read or modify the PDC on the main thread.
     */
    protected static PersistentDataContainer getPdc(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return entity.getPersistentDataContainer();
    }

    /*
    Short hand of converting a boolean to an integer.
     */
    protected static int boolToInt(final boolean bool) {
        return bool ? 1 : 0;
    }

}
