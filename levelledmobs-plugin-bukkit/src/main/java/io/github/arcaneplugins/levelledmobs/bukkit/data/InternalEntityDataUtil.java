package io.github.arcaneplugins.levelledmobs.bukkit.data;

import static org.bukkit.persistence.PersistentDataType.INTEGER;
import static org.bukkit.persistence.PersistentDataType.STRING;

import com.jeff_media.morepersistentdatatypes.DataType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.keys.EntityKeyStore;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class InternalEntityDataUtil extends EntityDataUtil {

    private InternalEntityDataUtil() {
        throw new IllegalStateException("Instantiation of utility-type class");
    }

    //TODO doc
    public static void setCreeperBlastRadiusMultiplier(
        final @Nonnull LivingEntity entity,
        final double multiplier,
        final boolean requirePersistence
    ) {
        setData(
            entity,
            EntityKeyStore.CREEPER_BLAST_DAMAGE_MULT,
            PersistentDataType.DOUBLE,
            multiplier,
            requirePersistence
        );
    }

    /*
    WARNING: This method does NOT use a memory data cache.
    It is therefore NOT applicable for use in EntitySpawnEvent.
     */
    public static @NotNull Set<String> getDropTableIds(
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

    //TODO doc
    public static void setExpDropMultiplier(
        final @Nonnull LivingEntity entity,
        final double multiplier,
        final boolean requirePersistence
    ) {
        setData(
            entity,
            EntityKeyStore.EXP_DROP_MULT,
            PersistentDataType.DOUBLE,
            multiplier,
            requirePersistence
        );
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

    //TODO doc
    public static void setItemDropMultiplier(
        final @Nonnull LivingEntity entity,
        final double multiplier,
        final boolean requirePersistence
    ) {
        setData(
            entity,
            EntityKeyStore.ITEM_DROP_MULT,
            PersistentDataType.DOUBLE,
            multiplier,
            requirePersistence
        );
    }

    //TODO doc
    @Nonnull
    public static Map<String, String> getLabelHandlerFormulaMap(
        final @Nonnull LivingEntity entity,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");

        final Map<String, String> ret = getPdcNonNull(entity).get(
            EntityKeyStore.LABEL_HANDLER_FORMULAS,
            DataType.asMap(DataType.STRING, DataType.STRING)
        );

        if(ret == null) return new HashMap<>();

        return ret;
    }

    public static void setLabelHandlerFormulaMap(
        final @Nonnull LivingEntity entity,
        final @Nonnull Map<String, String> labelHandlerFormulaMap,
        final boolean requirePersistence
    ) {
        getPdcNonNull(entity).set(
            EntityKeyStore.LABEL_HANDLER_FORMULAS,
            DataType.asMap(DataType.STRING, DataType.STRING),
            labelHandlerFormulaMap
        );
    }

    public static void updateLabels(
        final @Nonnull LivingEntity entity,
        final @Nonnull Context context,
        final boolean requirePersistence
    ) {
        final Map<String, String> labelHandlerFormulaMap = getLabelHandlerFormulaMap(
            entity, requirePersistence);

        labelHandlerFormulaMap.forEach((labelHandlerId, formula) -> {
            LabelRegistry.getLabelHandlers()
                .stream()
                .filter(lh -> lh.getId().equals(labelHandlerId))
                .findFirst()
                .ifPresent(labelHandler -> labelHandler.update(entity, context));
        });
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
        setData(child, EntityKeyStore.FATHER, STRING, father.getUniqueId().toString(), requirePersistence);
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

    public static void setPrimaryLabelHandler(
        final @NotNull LivingEntity entity,
        final @NotNull String to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(to, "to");
        setData(entity, EntityKeyStore.PRIMARY_LABEL_HANDLER, STRING, to, requirePersistence);
    }

    //TODO doc
    public static void setShieldBreakerMultiplier(
        final @Nonnull LivingEntity entity,
        final double multiplier,
        final boolean requirePersistence
    ) {
        setData(
            entity,
            EntityKeyStore.SHIELD_BREAKER_MULT,
            PersistentDataType.DOUBLE,
            multiplier,
            requirePersistence
        );
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

    public static void setWasBred(
        final @NotNull LivingEntity entity,
        final boolean to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.WAS_BRED, INTEGER, boolToInt(to), requirePersistence);
    }

    public static void setWasSummoned(
        final @NotNull LivingEntity entity,
        final boolean to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.WAS_SUMMONED, INTEGER, boolToInt(to), requirePersistence);
    }

    public static void setWasTransformed(
        final @NotNull LivingEntity entity,
        final boolean to,
        final boolean requirePersistence
    ) {
        Objects.requireNonNull(entity, "entity");
        setData(entity, EntityKeyStore.WAS_TRANSFORMED, INTEGER, boolToInt(to), requirePersistence);
    }

    /**
     * TODO Document
     *
     * @param entity TODO DOcument
     */
    public static void unlevelMob(
        final @NotNull LivingEntity entity
    ) {
        final PersistentDataContainer pdc = entity.getPersistentDataContainer();

        // Stores the health ratio of the mob so it can be adjusted when the mob is unlevelled
        Double healthRatio = null;

        // Removes attribute modifiers created by LevelledMobs
        for(Attribute attribute : Attribute.values()) {

            final AttributeInstance inst = entity.getAttribute(attribute);

            if(inst == null) continue;

            if(attribute == Attribute.GENERIC_MAX_HEALTH) {
                healthRatio = entity.getHealth() / inst.getValue();
            }

            inst.getModifiers().removeIf(modifier ->
                modifier.getName().startsWith("levelledmobs:")
            );

            if(attribute == Attribute.GENERIC_MAX_HEALTH) {
                entity.setHealth(healthRatio * inst.getValue());
            }

        }

        // remove PDC and metadata keys
        for(final NamespacedKey key : EntityKeyStore.LEVEL_RELATED_KEYS) {
            pdc.remove(key);
            entity.removeMetadata(key.toString(), LevelledMobs.getInstance());
        }

        // TODO remove any items from the mob's inventory that are from LM

        // TODO remove label on entity (nametag)
    }

}
