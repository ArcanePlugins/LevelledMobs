package me.lokka30.levelledmobs.bukkit.api.data;

import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.api.data.keys.MobKeyStore;
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
public class MobDataUtil {

    /*
    FIXME Comment
     */
    public static boolean getDeniesLabel(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        if(getPdc(mob).has(MobKeyStore.deniesLabel, INTEGER)) {
            // noinspection ConstantConditions
            return getPdc(mob).get(MobKeyStore.wasBabyMob, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    FIXME Comment
     */
    public static void setDeniesLabel(final @NotNull LivingEntity mob, final boolean to) {
        Objects.requireNonNull(mob, "mob");
        getPdc(mob).set(MobKeyStore.deniesLabel, INTEGER, boolToInt(to));
    }

    /*
    FIXME Comment
     */
    public static boolean isLevelled(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        return getPdc(mob).has(MobKeyStore.level, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getLevel(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        // noinspection ConstantConditions
        return getPdc(mob).get(MobKeyStore.level, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static boolean getMadeOverallChance(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        if(getPdc(mob).has(MobKeyStore.madeOverallChance, INTEGER)) {
            // noinspection ConstantConditions
            return getPdc(mob).get(MobKeyStore.wasBabyMob, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    FIXME Comment
     */
    public static int getMinLevel(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        // noinspection ConstantConditions
        return getPdc(mob).get(MobKeyStore.minLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getMaxLevel(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        // noinspection ConstantConditions
        return getPdc(mob).get(MobKeyStore.maxLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static String getOverriddenName(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        return getPdc(mob).get(MobKeyStore.overriddenName, STRING);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static String getSourceSpawnerName(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        return getPdc(mob).get(MobKeyStore.spawnSkyLightLevel, STRING);
    }

    /*
    FIXME Comment
     */
    public static int getSpawnTimeOfDay(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        // noinspection ConstantConditions
        return getPdc(mob).get(MobKeyStore.spawnTimeOfDay, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getSpawnSkyLightLevel(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        // noinspection ConstantConditions
        return getPdc(mob).get(MobKeyStore.spawnSkyLightLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static boolean getWasBabyMob(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        if(getPdc(mob).has(MobKeyStore.wasBabyMob, INTEGER)) {
            // noinspection ConstantConditions
            return getPdc(mob).get(MobKeyStore.wasBabyMob, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    FIXME Comment
     */
    public static boolean getWasSummoned(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        if(getPdc(mob).has(MobKeyStore.wasSummoned, INTEGER)) {
            // noinspection ConstantConditions
            return getPdc(mob).get(MobKeyStore.wasSummoned, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    Short hand of grabbing the mob's PDC. That's all! :)

    Warning: Please only read or modify the PDC on the main thread.
     */
    protected static PersistentDataContainer getPdc(final @NotNull LivingEntity mob) {
        Objects.requireNonNull(mob, "mob");
        return mob.getPersistentDataContainer();
    }

    /*
    Short hand of converting a boolean to an integer.
     */
    protected static int boolToInt(final boolean bool) {
        return bool ? 1 : 0;
    }

}
