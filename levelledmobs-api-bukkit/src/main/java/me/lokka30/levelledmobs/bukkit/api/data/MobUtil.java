package me.lokka30.levelledmobs.bukkit.api.data;

import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

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
public class MobUtil {

    /*
    FIXME Comment
     */
    public static boolean isLevelled(final LivingEntity mob) {
        return pdcOf(mob).has(MobKeyStore.level, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getLevel(final LivingEntity mob) {
        // noinspection ConstantConditions
        return pdcOf(mob).get(MobKeyStore.level, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getMinLevel(final LivingEntity mob) {
        // noinspection ConstantConditions
        return pdcOf(mob).get(MobKeyStore.minLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static int getMaxLevel(final LivingEntity mob) {
        // noinspection ConstantConditions
        return pdcOf(mob).get(MobKeyStore.maxLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static String getSourceSpawnerName(final LivingEntity mob) {
        return pdcOf(mob).get(MobKeyStore.spawnSkyLightLevel, STRING);
    }

    /*
    FIXME Comment
     */
    public static int getSpawnSkylightLevel(final LivingEntity mob) {
        // noinspection ConstantConditions
        return pdcOf(mob).get(MobKeyStore.spawnSkyLightLevel, INTEGER);
    }

    /*
    FIXME Comment
     */
    public static boolean wasBabyMob(final LivingEntity mob) {
        if(pdcOf(mob).has(MobKeyStore.wasBabyMob, INTEGER)) {
            // noinspection ConstantConditions
            return pdcOf(mob).get(MobKeyStore.wasBabyMob, INTEGER) == 1;
        } else {
            return false;
        }
    }

    /*
    Short hand of grabbing the mob's PDC.
     */
    protected static PersistentDataContainer pdcOf(final LivingEntity mob) {
        return mob.getPersistentDataContainer();
    }

}
