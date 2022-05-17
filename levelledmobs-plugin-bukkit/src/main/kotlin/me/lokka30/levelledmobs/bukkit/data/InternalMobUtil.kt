package me.lokka30.levelledmobs.bukkit.data

import me.lokka30.levelledmobs.bukkit.api.data.MobUtil
import me.lokka30.levelledmobs.bukkit.api.data.keys.MobKeyStore
import org.bukkit.entity.LivingEntity
import org.bukkit.persistence.PersistentDataType.INTEGER
import org.bukkit.persistence.PersistentDataType.STRING

/*
FIXME Comment
 */
object InternalMobUtil : MobUtil() {

    /*
    FIXME Comment
     */
    fun setLevel(mob: LivingEntity, level: Int) {
        pdcOf(mob).set(MobKeyStore.level, INTEGER, level)
    }

    /*
    FIXME Comment
     */
    fun setMinLevel(mob: LivingEntity, minLevel: Int) {
        pdcOf(mob).set(MobKeyStore.minLevel, INTEGER, minLevel)
    }

    /*
    FIXME Comment
     */
    fun setMaxLevel(mob: LivingEntity, maxLevel: Int) {
        pdcOf(mob).set(MobKeyStore.maxLevel, INTEGER, maxLevel)
    }

    /*
    FIXME Comment
     */
    fun setSourceSpawnerName(mob: LivingEntity, spawnerName: String) {
        pdcOf(mob).set(MobKeyStore.sourceSpawnerName, STRING, spawnerName)
    }

    /*
    FIXME Comment
     */
    fun setSpawnSkylightLevel(mob: LivingEntity, skylightLevel: Int) {
        pdcOf(mob).set(MobKeyStore.spawnSkyLightLevel, INTEGER, skylightLevel)
    }

    /*
    FIXME Comment
     */
    fun setWasBaby(mob: LivingEntity, wasBaby: Boolean) {
        pdcOf(mob).set(MobKeyStore.wasBabyMob, INTEGER, if(wasBaby) 1 else 0)
    }
}