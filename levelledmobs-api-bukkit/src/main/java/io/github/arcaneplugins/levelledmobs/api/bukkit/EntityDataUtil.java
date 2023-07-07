/*
This program is/was a part of the LevelledMobs project's source code.
Copyright (C) 2023  Lachlan Adamson (aka lokka30)
Copyright (C) 2023  LevelledMobs Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.arcaneplugins.levelledmobs.api.bukkit;

import static org.bukkit.persistence.PersistentDataType.DOUBLE;
import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

import io.github.arcaneplugins.levelledmobs.api.bukkit.keys.EntityKeyStore;
import io.github.arcaneplugins.levelledmobs.api.bukkit.misc.PluginUtil;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
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

    //TODO document
    @Nullable
    public static String getCreeperBlastRadiusMultiplierFormula(
        final @Nonnull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.CREEPER_BLAST_DAMAGE_MULTIPLIER_FORMULA, requirePersistence);
    }

    /**
     * TODO DOcument
     * @param entity TODO Document
     * @param requirePersistence TODO Document
     * @return death label formula
     */
    public static @Nullable String getDeathLabelFormula(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.DEATH_LABEL_FORMULA, requirePersistence);
    }

    /*
    TODO Comment
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
    TODO Comment
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
    public static String getExpDropMultiplierFormula(
        final @Nonnull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.EXP_DROP_MULTIPLIER_FORMULA, requirePersistence);
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
    TODO Comment
     */
    public static boolean isLevelled(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        //TODO use 'has key' instead of getting the value.
        return getLevel(entity, requirePersistence) != null;
    }

    //TODO document
    @Nullable
    public static String getItemDropMultiplierFormula(
        final @Nonnull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.ITEM_DROP_MULTIPLIER_FORMULA, requirePersistence);
    }

    /*
    TODO Comment
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
    TODO Comment
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
    TODO Comment
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
    TODO Comment
     */
    @Nullable
    public static Integer getMaxLevel(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataInt(entity, EntityKeyStore.MAX_LEVEL, requirePersistence);
    }

    /*
    TODO Comment
     */
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
    TODO Comment
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
    TODO Comment
     */
    @Nullable
    public static String getPrimaryLabelHandler(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.PRIMARY_LABEL_HANDLER, requirePersistence);
    }

    //TODO document
    @Nullable
    public static String getShieldBreakerMultiplierFormula(
        final @Nonnull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.SHIELD_BREAKER_MULTIPLIER_FORMULA,
            requirePersistence);
    }

    /*
    TODO Comment
     */
    @Nullable
    public static String getSourceSpawnerName(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        return getDataString(entity, EntityKeyStore.SOURCE_SPAWNER_NAME, requirePersistence);
    }

    //TODO Document
    @Nullable
    public static SpawnReason getSpawnReason(
        final @NotNull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");

        final String s = getDataString(entity, EntityKeyStore.SPAWN_REASON, requirePersistence);

        return s == null ? null : SpawnReason.valueOf(s);
    }

    /*
    TODO Comment
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
    TODO Comment
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
    TODO Comment
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
    TODO Comment
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
    TODO Comment
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
    TODO Comment
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
    TODO doc
     */
    @Nullable
    protected static PersistentDataContainer getPdc(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return entity.getPersistentDataContainer();
    }

    /*
    TODO doc
     */
    @NotNull
    protected static PersistentDataContainer getPdcNonNull(final @NotNull LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        return Objects.requireNonNull(entity.getPersistentDataContainer(), "PDC");
    }

    /*
    TODO doc
     */
    @SuppressWarnings("SameParameterValue")
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

    /*
    TODO doc
     */
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

    /*
    TODO doc
     */
    @Nullable
    protected static Double getDataDouble(
        final @NotNull LivingEntity entity,
        final @NotNull NamespacedKey namespacedKey,
        final boolean requirePersistence
    ) {
        final String namespacedKeyStr = namespacedKey.toString();

        if(entity.hasMetadata(namespacedKeyStr)) {
            return entity.getMetadata(namespacedKeyStr).get(0).asDouble();
        }

        Double ret = null;

        final PersistentDataContainer pdc = getPdc(entity);
        if(pdc == null) {
            if(requirePersistence)
                throw new NullPointerException(
                    "PersistentDataContainer is null where persistence is required"
                );
        } else {
            ret = pdc.get(namespacedKey, DOUBLE);
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
    TODO doc
     */
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

    /*
    TODO doc
     */
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
    TODO Doc
     */
    protected static int boolToInt(final boolean bool) {
        return bool ? 1 : 0;
    }

}