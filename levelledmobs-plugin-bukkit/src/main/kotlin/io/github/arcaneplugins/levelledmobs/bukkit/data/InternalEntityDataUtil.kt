package io.github.arcaneplugins.levelledmobs.bukkit.data

import com.jeff_media.morepersistentdatatypes.DataType
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.keys.EntityKeyStore
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.label.LabelRegistry
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.persistence.PersistentDataType
import java.util.*
import javax.annotation.Nonnull

object InternalEntityDataUtil : EntityDataUtil() {
    fun setCreeperBlastRadiusMultiplierFormula(
        entity: LivingEntity,
        formula: String,
        requirePersistence: Boolean
    ){
        setData(
            entity,
            EntityKeyStore.CREEPER_BLAST_DAMAGE_MULTIPLIER_FORMULA,
            PersistentDataType.STRING,
            formula,
            requirePersistence
        )
    }

    fun setDeathLabelFormula(
        entity: LivingEntity,
        formula: String,
        requirePersistence: Boolean
    ) {
        setData(
            entity,
            EntityKeyStore.DEATH_LABEL_FORMULA,
            PersistentDataType.STRING,
            formula,
            requirePersistence
        )
    }

    /*
    WARNING: This method does NOT use a memory data cache.
    It is therefore NOT applicable for use in EntitySpawnEvent.
     */
    fun getDropTableIds(
        entity: LivingEntity
    ): Set<String> {

        val result = getPdcNonNull(entity).get(
            EntityKeyStore.DROP_TABLE_IDS,
            DataType.asGenericCollection({ HashSet() }, DataType.STRING)
        )

        return result?: mutableSetOf()
    }

    /*
    WARNING: This method does NOT use a memory data cache.
    It is therefore NOT applicable for use in EntitySpawnEvent.
     */
    fun setDropTableIds(
        entity: LivingEntity,
        dropTableIds: Set<String>
    ) {
        getPdcNonNull(entity)
            .set(EntityKeyStore.DROP_TABLE_IDS, DataType.asSet(DataType.STRING), dropTableIds)
    }

    fun setExpDropMultiplierFormula(
        @Nonnull entity: LivingEntity?,
        @Nonnull formula: String,
        requirePersistence: Boolean
    ) {
        setData(
            entity!!,
            EntityKeyStore.EXP_DROP_MULTIPLIER_FORMULA,
            PersistentDataType.STRING,
            formula,
            requirePersistence
        )
    }

    fun setInheritanceBreedingFormula(
        entity: LivingEntity,
        to: String,
        requirePersistence: Boolean
    ) {
        setData(
            entity, EntityKeyStore.INHERITANCE_BREEDING_FORMULA,
            PersistentDataType.STRING, to, requirePersistence
        )
    }

    fun setInheritanceTransformationFormula(
        entity: LivingEntity,
        to: String,
        requirePersistence: Boolean
    ) {
        setData(
            entity, EntityKeyStore.INHERITANCE_TRANSFORMATION_FORMULA,
            PersistentDataType.STRING, to, requirePersistence
        )
    }

    fun setItemDropMultiplier(
        @Nonnull entity: LivingEntity?,
        @Nonnull formula: String,
        requirePersistence: Boolean
    ) {
        setData(
            entity!!,
            EntityKeyStore.ITEM_DROP_MULTIPLIER_FORMULA,
            PersistentDataType.STRING,
            formula,
            requirePersistence
        )
    }

    fun getLabelHandlerFormulaMap(
        entity: LivingEntity,
        requirePersistence: Boolean
    ): Map<String, String> {
        return getPdcNonNull(entity)[EntityKeyStore.LABEL_HANDLER_FORMULAS, DataType.asMap(
            DataType.STRING,
            DataType.STRING
        )]
            ?: return HashMap()
    }

    fun setLabelHandlerFormulaMap(
        entity: LivingEntity?,
        labelHandlerFormulaMap: Map<String, String>,
        requirePersistence: Boolean
    ) {
        getPdcNonNull(entity!!).set(
            EntityKeyStore.LABEL_HANDLER_FORMULAS,
            DataType.asMap(DataType.STRING, DataType.STRING),
            labelHandlerFormulaMap
        )
    }

    fun updateLabels(
        entity: LivingEntity,
        context: Context,
        requirePersistence: Boolean
    ) {
        val labelHandlerFormulaMap = getLabelHandlerFormulaMap(
            entity, requirePersistence
        )
        labelHandlerFormulaMap.forEach { (labelHandlerId: String?, formula: String?) ->
            LabelRegistry.labelHandlers
                .stream()
                .filter { lh -> lh.id.equals(labelHandlerId) }
                .findFirst()
                .ifPresent { labelHandler -> labelHandler.update(entity, context) }
        }
    }

    fun updateLabels(
        player: Player,
        context: Context,
        requirePersistence: Boolean
    ) {
        player.getNearbyEntities(50.0, 50.0, 50.0) //TODO configurable distance
            .stream()
            .filter { entity: Entity? -> entity is LivingEntity }
            .map<LivingEntity?> { entity: Entity? -> entity as LivingEntity? }
            .filter { entity: LivingEntity? ->
                isLevelled(
                    entity!!, requirePersistence
                )
            }
            .forEach { entity: LivingEntity? ->
                val labelHandlerFormulaMap =
                    getLabelHandlerFormulaMap(
                        entity!!, requirePersistence
                    )
//                labelHandlerFormulaMap.forEach { (labelHandlerId: String?, formula: String?) ->
//                    LabelRegistry.getLabelHandlers()
//                        .stream()
//                        .filter { lh -> lh.getId().equals(labelHandlerId) }
//                        .findFirst()
//                        .ifPresent { labelHandler -> labelHandler.update(entity, player, context) }
//                }
            }
    }

    fun setLevel(
        entity: LivingEntity,
        to: Int,
        requirePersistence: Boolean
    ) {
        Objects.requireNonNull(entity, "entity")
        setData(entity, EntityKeyStore.LEVEL, PersistentDataType.INTEGER, to, requirePersistence)
    }

    fun setMadeOverallChance(
        entity: LivingEntity,
        to: Boolean,
        requirePersistence: Boolean
    ) {
        setData(
            entity,
            EntityKeyStore.MADE_OVERALL_CHANCE,
            PersistentDataType.INTEGER,
            boolToInt(to),
            requirePersistence
        )
    }

    fun setMinLevel(
        entity: LivingEntity,
        to: Int,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.MIN_LEVEL, PersistentDataType.INTEGER, to, requirePersistence)
    }

    fun setMaxLevel(
        entity: LivingEntity,
        to: Int,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.MAX_LEVEL, PersistentDataType.INTEGER, to, requirePersistence)
    }

    fun setFather(
        child: LivingEntity,
        father: LivingEntity,
        requirePersistence: Boolean
    ) {
        setData(child, EntityKeyStore.FATHER, PersistentDataType.STRING, father.uniqueId.toString(), requirePersistence)
    }

    fun setMother(
        child: LivingEntity,
        mother: LivingEntity,
        requirePersistence: Boolean
    ) {
        setData(child, EntityKeyStore.MOTHER, PersistentDataType.STRING, mother.uniqueId.toString(), requirePersistence)
    }

    fun setOverriddenName(
        entity: LivingEntity,
        to: String,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.OVERRIDEN_ENTITY_NAME, PersistentDataType.STRING, to, requirePersistence)
    }

    fun setPrimaryLabelHandler(
        entity: LivingEntity,
        to: String,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.PRIMARY_LABEL_HANDLER, PersistentDataType.STRING, to, requirePersistence)
    }

    fun setShieldBreakerMultiplierFormula(
        entity: LivingEntity,
        formula: String,
        requirePersistence: Boolean
    ) {
        setData(
            entity,
            EntityKeyStore.SHIELD_BREAKER_MULTIPLIER_FORMULA,
            PersistentDataType.STRING,
            formula,
            requirePersistence
        )
    }

    fun setSourceSpawnerName(
        entity: LivingEntity,
        to: String,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.SOURCE_SPAWNER_NAME, PersistentDataType.STRING, to, requirePersistence)
    }

    fun setSpawnReason(
        entity: LivingEntity,
        to: CreatureSpawnEvent.SpawnReason,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.SPAWN_REASON, PersistentDataType.STRING, to.name, requirePersistence)
    }

    fun setSpawnTimeOfDay(
        entity: LivingEntity,
        to: Int,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.SPAWNED_TIME_OF_DAY, PersistentDataType.INTEGER, to, requirePersistence)
    }

    fun setSpawnSkyLightLevel(
        entity: LivingEntity,
        to: Int,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.SPAWNED_SKY_LIGHT_LEVEL, PersistentDataType.INTEGER, to, requirePersistence)
    }

    fun setWasBaby(
        entity: LivingEntity,
        to: Boolean,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.WAS_BABY, PersistentDataType.INTEGER, boolToInt(to), requirePersistence)
    }

    fun setWasBred(
        entity: LivingEntity,
        to: Boolean,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.WAS_BRED, PersistentDataType.INTEGER, boolToInt(to), requirePersistence)
    }

    fun setWasSummoned(
        entity: LivingEntity,
        to: Boolean,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.WAS_SUMMONED, PersistentDataType.INTEGER, boolToInt(to), requirePersistence)
    }

    fun setWasTransformed(
        entity: LivingEntity,
        to: Boolean,
        requirePersistence: Boolean
    ) {
        setData(entity, EntityKeyStore.WAS_TRANSFORMED, PersistentDataType.INTEGER, boolToInt(to), requirePersistence)
    }

    fun unlevelMob(
        entity: LivingEntity
    ) {
        val pdc = entity.persistentDataContainer

        // Stores the health ratio of the mob so it can be adjusted when the mob is unlevelled
        var healthRatio: Double? = null

        // Removes attribute modifiers created by LevelledMobs
        for (attribute in Attribute.entries) {
            val inst = entity.getAttribute(attribute) ?: continue
            if (attribute == Attribute.GENERIC_MAX_HEALTH) {
                healthRatio = entity.health / inst.value
            }
            inst.modifiers.removeIf { modifier: AttributeModifier ->
                modifier.name.startsWith("levelledmobs:")
            }
            if (attribute == Attribute.GENERIC_MAX_HEALTH) {
                entity.health = healthRatio!! * inst.value
            }
        }

        // remove PDC and metadata keys
        for (key in EntityKeyStore.LEVEL_RELATED_KEYS) {
            pdc.remove(key)
            entity.removeMetadata(key.toString(), LevelledMobs.lmInstance)
        }

        // TODO remove any items from the mob's inventory that are from LM
        // TODO remove label on entity (nametag)

        fun summonMob(
            entityType: EntityType,
            location: Location,
            level: Int,
            minLevel: Int,
            maxLevel: Int
        ): LivingEntity {
            val entityClass = entityType.entityClass
            require(
                !(entityClass == null ||
                        !LivingEntity::class.java.isAssignableFrom(entityClass))
            ) { "$entityType is not summonable" }
            return location.world.spawn(location, entityClass) { entity: Entity? ->
                val lent = entity as LivingEntity?
                setLevel(lent!!, level, true)
                setMinLevel(lent, minLevel, true)
                setMaxLevel(lent, maxLevel, true)
                setWasSummoned(lent, true, true)
            } as LivingEntity
        }
    }
}