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

@SuppressWarnings("unused")
public final class InternalEntityDataUtil extends EntityDataUtil {

    private InternalEntityDataUtil() {
        throw new IllegalStateException("Instantiation of utility-type class");
    }

    /*
    WARNING: This method does NOT use a memory data cache.
    It is therefore NOT applicable for use in EntitySpawnEvent.
     */
    public static Set<String> getDropTableIds(
        final @NotNull LivingEntity entity
    ) {
        Objects.requireNonNull(entity, "entity");

        final var result = getPdcNonNull(entity).get(
            EntityKeyStore.DROP_TABLE_IDS,
            DataType.asGenericCollection(HashSet::new, DataType.STRING)
        );

        if(result == null) return new HashSet<>();

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
        getPdcNonNull(entity)
            .set(EntityKeyStore.DROP_TABLE_IDS, DataType.asSet(DataType.STRING), dropTableIds);
    }

    /**
     * TODO
     *
     * @param entity TODO
     * @param to TODO
     * @param requirePersistence TODO
     */
    public static void setInheritanceBreedingFormula(
        final @NotNull LivingEntity entity,
        final @NotNull String to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");

        setData(entity, EntityKeyStore.INHERITANCE_BREEDING_FORMULA,
            STRING, to, requirePersistence);
    }

    /**
     * TODO
     *
     * @param entity TODO
     * @param to TODO
     * @param requirePersistence TODO
     */
    public static void setInheritanceTransformationFormula(
        final @NotNull LivingEntity entity,
        final @NotNull String to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");

        setData(entity, EntityKeyStore.INHERITANCE_TRANSFORMATION_FORMULA,
            STRING, to, requirePersistence);
    }

    public static void setLevel(
        final @NotNull LivingEntity entity,
        final int to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.LEVEL, INTEGER, to, requirePersistence);
    }

    public static void setMadeOverallChance(
        final @NotNull LivingEntity entity,
        final boolean to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.MADE_OVERALL_CHANCE, INTEGER, boolToInt(to), requirePersistence);
    }

    public static void setMinLevel(
        final @NotNull LivingEntity entity,
        final int to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.MIN_LEVEL, INTEGER, to, requirePersistence);
    }

    public static void setMaxLevel(
        final @NotNull LivingEntity entity,
        final int to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.MAX_LEVEL, INTEGER, to, requirePersistence);
    }

    public static void setFather(
        final @NotNull LivingEntity child,
        final @NotNull LivingEntity father,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(child, "child");
        Objects.requireNonNull(father, "father");
        setData(child, EntityKeyStore.MOTHER, STRING, father.getUniqueId().toString(), requirePersistence);
    }

    public static void setMother(
        final @NotNull LivingEntity child,
        final @NotNull LivingEntity mother,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(child, "child");
        Objects.requireNonNull(mother, "mother");
        setData(child, EntityKeyStore.MOTHER, STRING, mother.getUniqueId().toString(), requirePersistence);
    }

    public static void setOverriddenName(
        final @NotNull LivingEntity entity,
        final @NotNull String to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");
        setData(entity, EntityKeyStore.OVERRIDEN_ENTITY_NAME, STRING, to, requirePersistence);
    }

    public static void setSourceSpawnerName(
        final @NotNull LivingEntity entity,
        final @NotNull String to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");
        setData(entity, EntityKeyStore.SOURCE_SPAWNER_NAME, STRING, to, requirePersistence);
    }

    public static void setSpawnTimeOfDay(
        final @NotNull LivingEntity entity,
        final int to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.SPAWNED_TIME_OF_DAY, INTEGER, to, requirePersistence);
    }

    public static void setSpawnSkyLightLevel(
        final @NotNull LivingEntity entity,
        final int to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.SPAWNED_SKY_LIGHT_LEVEL, INTEGER, to, requirePersistence);
    }

    public static void setWasBaby(
        final @NotNull LivingEntity entity,
        final boolean to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.WAS_BABY, INTEGER, boolToInt(to), requirePersistence);
    }

    public static void setWasSummoned(
        final @NotNull LivingEntity entity,
        final boolean to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.WAS_SUMMONED, INTEGER, boolToInt(to), requirePersistence);
    }

    public static void unlevelMob(
        final @NotNull LivingEntity entity
    ) {
        //TODO remove some PDC keys
        //TODO remove attribute multipliers             - use new PDC keys to check which ones LM modified.
        //TODO if LM set a custom name then remove it   - use new pDC key to check if LM set the current custom name.
        //TODO update mob nametag
        Log.inf("can't unlevel mobs yet: logic missing.");
    }

}
