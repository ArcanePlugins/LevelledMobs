package me.lokka30.levelledmobs.bukkit.data

import me.lokka30.levelledmobs.bukkit.api.data.EntityDataUtil
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType.INTEGER
import org.bukkit.persistence.PersistentDataType.STRING

/*
FIXME Comment
 */
object InternalEntityDataUtil : EntityDataUtil() {

    /*
    FIXME Comment
     */
    fun setLevel(entity: LivingEntity, to: Int) {
        getPdc(entity).set(EntityKeyStore.level, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setMadeOverallChance(entity: LivingEntity, to: Boolean) {
        getPdc(entity).set(EntityKeyStore.madeOverallChance, INTEGER, boolToInt(to))
    }

    /*
    FIXME Comment
     */
    fun setMinLevel(entity: LivingEntity, to: Int) {
        getPdc(entity).set(EntityKeyStore.minLevel, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setMaxLevel(entity: LivingEntity, to: Int) {
        getPdc(entity).set(EntityKeyStore.maxLevel, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setOverriddenName(entity: LivingEntity, to: String) {
        getPdc(entity).set(EntityKeyStore.overriddenName, STRING, to)
    }

    /*
    FIXME Comment
     */
    fun setSourceSpawnerName(entity: LivingEntity, to: String) {
        getPdc(entity).set(EntityKeyStore.sourceSpawnerName, STRING, to)
    }

    /*
    FIXME Comment
     */
    fun setSpawnTimeOfDay(entity: LivingEntity, to: Int) {
        getPdc(entity).set(EntityKeyStore.spawnTimeOfDay, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setSpawnSkyLightLevel(entity: LivingEntity, to: Int) {
        getPdc(entity).set(EntityKeyStore.spawnSkyLightLevel, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setWasBaby(entity: LivingEntity, to: Boolean) {
        getPdc(entity).set(EntityKeyStore.wasBaby, INTEGER, boolToInt(to))
    }

    /*
    FIXME Comment
     */
    fun setWasSummoned(entity: LivingEntity, to: Boolean) {
        getPdc(entity).set(EntityKeyStore.wasSummoned, INTEGER, boolToInt(to))
    }
}