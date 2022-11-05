package io.github.arcaneplugins.levelledmobs.bukkit.api.data;

import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import io.github.arcaneplugins.levelledmobs.bukkit.api.util.PluginUtil;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
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
    public static boolean getDeniesLabel(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        final Boolean val = getDataBool(entity, EntityKeyStore.DENIES_LABEL, requirePersistence);
        return val != null && val;
    }

    /*
    FIXME Comment
     */
    public static void setDeniesLabel(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence,
        final boolean to
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.DENIES_LABEL, INTEGER, boolToInt(to), requirePersistence);
    }

    //TODO document
    @Nullable
    public static LivingEntity getFather(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");

        final String uuidStr = getDataString(entity, EntityKeyStore.FATHER, requirePersistence);
        if(uuidStr == null) return null;
        return (LivingEntity) Bukkit.getEntity(UUID.fromString(uuidStr));
    }

    /**
     * TODO Document
     *
     * @param entity TODO Document
     * @return TODO Document
     */
    @Nullable
    public static String getInheritanceBreedingFormula(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.INHERITANCE_BREEDING_FORMULA, requirePersistence);
    }

    /**
     * TODO Document
     *
     * @param entity TODO Document
     * @return TODO Document
     */
    @Nullable
    public static String getInheritanceTransformationFormula(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.INHERITANCE_TRANSFORMATION_FORMULA, requirePersistence);
    }

    /*
    FIXME Comment
     */
    public static boolean isLevelled(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        //TODO use 'has key' instead of getting the value.
        return getLevel(entity, requirePersistence) != null;
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Integer getLevel(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataInt(entity, EntityKeyStore.LEVEL, requirePersistence);
    }

    //TODO document
    @Nullable
    public static Float getLevelRatio(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        final Integer level = getLevel(entity, requirePersistence);
        if(level == null) return null;
        final Integer minLevel = getMinLevel(entity, requirePersistence);
        if(minLevel == null) return null;
        final Integer maxLevel = getMaxLevel(entity, requirePersistence);
        if(maxLevel == null) return null;

        return (level - minLevel) * 1.0f / (maxLevel - minLevel);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Boolean madeOverallChance(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataBool(entity, EntityKeyStore.MADE_OVERALL_CHANCE, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Integer getMinLevel(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataInt(entity, EntityKeyStore.MIN_LEVEL, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Integer getMaxLevel(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataInt(entity, EntityKeyStore.MAX_LEVEL, requirePersistence);
    }

    @Nullable
    public static LivingEntity getMother(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");

        final String uuidStr = getDataString(entity, EntityKeyStore.MOTHER, requirePersistence);
        if(uuidStr == null) return null;
        return (LivingEntity) Bukkit.getEntity(UUID.fromString(uuidStr));
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static String getOverriddenName(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.OVERRIDEN_ENTITY_NAME, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static String getPrimaryLabelHandler(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.PRIMARY_LABEL_HANDLER, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static String getSourceSpawnerName(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.SOURCE_SPAWNER_NAME, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Integer getSpawnTimeOfDay(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataInt(entity, EntityKeyStore.SPAWNED_TIME_OF_DAY, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Integer getSpawnSkyLightLevel(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataInt(entity, EntityKeyStore.SPAWNED_SKY_LIGHT_LEVEL, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Boolean wasBaby(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataBool(entity, EntityKeyStore.WAS_BABY, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Boolean wasBred(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataBool(entity, EntityKeyStore.WAS_BRED, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Boolean wasSummoned(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataBool(entity, EntityKeyStore.WAS_SUMMONED, requirePersistence);
    }

    /*
    FIXME Comment
     */
    @Nullable
    public static Boolean wasTransformed(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataBool(entity, EntityKeyStore.WAS_TRANSFORMED, requirePersistence);
    }

    /*
    Short hand of grabbing the entity's PDC. That's all! :)

    Warning: Please only read or modify the PDC on the main thread.
     */
    @Nullable
    protected static PersistentDataContainer getPdc(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return entity.getPersistentDataContainer();
    }

    @NotNull
    protected static PersistentDataContainer getPdcNonNull(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Objects.requireNonNull(entity.getPersistentDataContainer(), "PDC");
    }

    protected static <T, Z> void setData(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey,
        final @NotNull PersistentDataType<T, Z> type,
        final @NotNull Z value,
        final boolean requirePersistence
    ) {
        entity.setMetadata(
            namespacedKey.toString(),
            new FixedMetadataValue(PluginUtil.getMainInstance(), value)
        );

        final PersistentDataContainer pdc = getPdc(entity);
        if(pdc == null) {
            if(requirePersistence)
                throw new NullPointerException(
                    "Unable to access the PersistentDataContainer of a for imperative setData call"
                );

            return;
        }
        pdc.set(namespacedKey, type, value);
    }

    @Nullable
    protected static Integer getDataInt(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey,
        final boolean requirePersistence
    ) {
        final String namespacedKeyStr = namespacedKey.toString();

        if(entity.hasMetadata(namespacedKeyStr)) {
            return entity.getMetadata(namespacedKeyStr).get(0).asInt();
        }

        Integer ret = null;

        final PersistentDataContainer pdc = getPdc(entity);
        if(pdc == null) {
            if(requirePersistence)
                throw new NullPointerException(
                    "PersistentDataContainer is null where persistence is required"
                );
        } else {
            ret = pdc.get(namespacedKey, INTEGER);
        }


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

    @Nullable
    protected static Boolean getDataBool(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey,
        final boolean requirePersistence
    ) {
        final String namespacedKeyStr = namespacedKey.toString();

        if(entity.hasMetadata(namespacedKeyStr)) {
            return entity.getMetadata(namespacedKeyStr).get(0).asBoolean();
        }

        final boolean ret;
        final PersistentDataContainer pdc = getPdc(entity);
        if(pdc == null) {
            if(requirePersistence)
                throw new NullPointerException
                    ("PersistentDataContainer is null where persistence is required");

            entity.removeMetadata(namespacedKeyStr, PluginUtil.getMainInstance());
            return null;
        } else {
            final Integer pdcVal = pdc.get(namespacedKey, INTEGER);
            ret = pdcVal != null && pdcVal == 1;
        }

        entity.setMetadata(namespacedKeyStr, new FixedMetadataValue(
            PluginUtil.getMainInstance(),
            ret
        ));

        return ret;
    }

    @Nullable
    protected static String getDataString(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey,
        final boolean requirePersistence
    ) {
        final String namespacedKeyStr = namespacedKey.toString();

        if(entity.hasMetadata(namespacedKeyStr)) {
            return entity.getMetadata(namespacedKeyStr).get(0).asString();
        }

        String ret = null;

        final PersistentDataContainer pdc = getPdc(entity);
        if(pdc == null) {
            if(requirePersistence)
                throw new NullPointerException(
                    "PersistentDataContainer is null where persistence is required"
                );
        } else {
            ret = pdc.get(namespacedKey, STRING);
        }

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
