package me.lokka30.levelledmobs.bukkit.data;

import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.api.data.EntityDataUtil;
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public final class InternalEntityDataUtil extends EntityDataUtil {

    private InternalEntityDataUtil() {
        throw new IllegalStateException("Instantiation of utility-type class");
    }

    public static void setLevel(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.level, INTEGER, to);
    }

    public static void setMadeOverallChance(final @NotNull LivingEntity entity, final boolean to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.madeOverallChance, INTEGER, boolToInt(to));
    }

    public static void setMinLevel(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.minLevel, INTEGER, to);
    }

    public static void setMaxLevel(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.maxLevel, INTEGER, to);
    }

    public static void setOverriddenName(final @NotNull LivingEntity entity, final @NotNull String to) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");
        getPdc(entity).set(EntityKeyStore.overriddenName, STRING, to);
    }

    public static void setSourceSpawnerName(final @NotNull LivingEntity entity, final @NotNull String to) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");
        getPdc(entity).set(EntityKeyStore.sourceSpawnerName, STRING, to);
    }

    public static void setSpawnTimeOfDay(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.spawnTimeOfDay, INTEGER, to);
    }

    public static void setSpawnSkyLightLevel(final @NotNull LivingEntity entity, final int to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.spawnSkyLightLevel, INTEGER, to);
    }

    public static void setWasBaby(final @NotNull LivingEntity entity, final boolean to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.wasBaby, INTEGER, boolToInt(to));
    }

    public static void setWasSummoned(final @NotNull LivingEntity entity, final boolean to) {
        Objects.requireNonNull(entity, "entity");
        getPdc(entity).set(EntityKeyStore.maxLevel, INTEGER, boolToInt(to));
    }

}
