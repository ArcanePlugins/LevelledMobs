package me.lokka30.levelledmobs.bukkit.api.data;

import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.api.PluginUtil;
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
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
        return getDataBool(entity, EntityKeyStore.deniesLabel);
    }

    /*
    FIXME Comment
     */
    public static void setDeniesLabel(final @NotNull LivingEntity entity, final boolean to) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.deniesLabel, INTEGER, boolToInt(to));
    }

    /*
    FIXME Comment
     */
    public static boolean isLevelled(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getLevel(entity) != null;
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Integer getLevel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getDataInt(entity, EntityKeyStore.level);
    }

    /*
    FIXME Comment
     */
    public static boolean getMadeOverallChance(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getDataBool(entity, EntityKeyStore.madeOverallChance);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Integer getMinLevel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getDataInt(entity, EntityKeyStore.minLevel);
    }

    /*
    FIXME Comment
     */
    public static int getMaxLevel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Objects.requireNonNull(
            getDataInt(entity, EntityKeyStore.maxLevel),
            "maxLevel"
        );
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static String getOverriddenName(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.overriddenName);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static String getSourceSpawnerName(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.sourceSpawnerName);
    }

    /*
    FIXME Comment
     */
    public static int getSpawnTimeOfDay(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Objects.requireNonNull(
            getDataInt(entity, EntityKeyStore.spawnTimeOfDay),
            "spawnTimeOfDay"
        );
    }

    /*
    FIXME Comment
     */
    public static int getSpawnSkyLightLevel(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Objects.requireNonNull(
            getDataInt(entity, EntityKeyStore.spawnSkyLightLevel),
            "spawnSkyLightLevel"
        );
    }

    /*
    FIXME Comment
     */
    public static boolean getWasBaby(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getDataBool(entity, EntityKeyStore.wasBaby);
    }

    /*
    FIXME Comment
     */
    public static boolean getWasSummoned(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return getDataBool(entity, EntityKeyStore.wasSummoned);
    }

    /*
    Short hand of grabbing the entity's PDC. That's all! :)

    Warning: Please only read or modify the PDC on the main thread.
     */
    @NotNull
    protected static PersistentDataContainer getPdc(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Objects.requireNonNull(
            entity.getPersistentDataContainer(),
            "PersistentDataContainer"
        );
    }

    protected static <T, Z> void setData(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey,
        final @NotNull PersistentDataType<T, Z> type,
        final @NotNull Z value
    ) {
        entity.setMetadata(
            namespacedKey.toString(),
            new FixedMetadataValue(PluginUtil.getMainInstance(), value)
        );

        getPdc(entity).set(namespacedKey, type, value);
    }

    @Nullable
    protected static Integer getDataInt(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey
    ) {
        final String namespacedKeyStr = namespacedKey.toString();

        if(entity.hasMetadata(namespacedKeyStr)) {
            return entity.getMetadata(namespacedKeyStr).get(0).asInt();
        }

        final Integer ret = getPdc(entity).get(namespacedKey, INTEGER);
        if(ret == null) {
            entity.removeMetadata(namespacedKeyStr, PluginUtil.getMainInstance());
        } else {
            entity.setMetadata(namespacedKeyStr, new FixedMetadataValue(
                PluginUtil.getMainInstance(),
                ret
            ));
        }

        return ret;
    }

    protected static boolean getDataBool(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey
    ) {
        final String namespacedKeyStr = namespacedKey.toString();

        if(entity.hasMetadata(namespacedKeyStr)) {
            return entity.getMetadata(namespacedKeyStr).get(0).asBoolean();
        }

        final Integer pdcVal = getPdc(entity).get(namespacedKey, INTEGER);
        final boolean ret = pdcVal != null && pdcVal == 1;
        entity.setMetadata(namespacedKeyStr, new FixedMetadataValue(
            PluginUtil.getMainInstance(),
            ret
        ));

        return ret;
    }

    @Nullable
    protected static String getDataString(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey
    ) {
        final String namespacedKeyStr = namespacedKey.toString();

        if(entity.hasMetadata(namespacedKeyStr)) {
            return entity.getMetadata(namespacedKeyStr).get(0).asString();
        }

        final String ret = getPdc(entity).get(namespacedKey, STRING);
        if(ret == null) {
            entity.removeMetadata(namespacedKeyStr, PluginUtil.getMainInstance());
        } else {
            entity.setMetadata(namespacedKeyStr, new FixedMetadataValue(
                PluginUtil.getMainInstance(),
                ret
            ));
        }

        return ret;
    }

    /*
    Short hand of converting a boolean to an integer.
     */
    protected static int boolToInt(final boolean bool) {
        return bool ? 1 : 0;
    }

}
