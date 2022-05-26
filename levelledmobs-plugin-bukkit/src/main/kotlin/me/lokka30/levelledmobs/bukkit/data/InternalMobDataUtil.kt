package me.lokka30.levelledmobs.bukkit.data

import me.lokka30.levelledmobs.bukkit.api.data.MobDataUtil
import me.lokka30.levelledmobs.bukkit.api.data.keys.MobKeyStore
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType.INTEGER
import org.bukkit.persistence.PersistentDataType.STRING

/*
FIXME Comment
 */
object InternalMobDataUtil : MobDataUtil() {

    /*
    FIXME Comment
     */
    fun setLevel(mob: LivingEntity, to: Int) {
        getPdc(mob).set(MobKeyStore.level, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setMadeOverallChance(mob: LivingEntity, to: Boolean) {
        getPdc(mob).set(MobKeyStore.madeOverallChance, INTEGER, boolToInt(to))
    }

    /*
    FIXME Comment
     */
    fun setMinLevel(mob: LivingEntity, to: Int) {
        getPdc(mob).set(MobKeyStore.minLevel, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setMaxLevel(mob: LivingEntity, to: Int) {
        getPdc(mob).set(MobKeyStore.maxLevel, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setOverriddenName(mob: LivingEntity, to: String) {
        getPdc(mob).set(MobKeyStore.overriddenName, STRING, to)
    }

    /*
    FIXME Comment
     */
    fun setSourceSpawnerName(mob: LivingEntity, to: String) {
        getPdc(mob).set(MobKeyStore.sourceSpawnerName, STRING, to)
    }

    /*
    FIXME Comment
     */
    fun setSpawnTimeOfDay(mob: LivingEntity, to: Int) {
        getPdc(mob).set(MobKeyStore.spawnTimeOfDay, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setSpawnSkyLightLevel(mob: LivingEntity, to: Int) {
        getPdc(mob).set(MobKeyStore.spawnSkyLightLevel, INTEGER, to)
    }

    /*
    FIXME Comment
     */
    fun setWasBaby(mob: LivingEntity, to: Boolean) {
        getPdc(mob).set(MobKeyStore.wasBabyMob, INTEGER, boolToInt(to))
    }

    /*
    FIXME Comment
     */
    fun setWasSummoned(mob: LivingEntity, to: Boolean) {
        getPdc(mob).set(MobKeyStore.wasSummoned, INTEGER, boolToInt(to))
    }
}