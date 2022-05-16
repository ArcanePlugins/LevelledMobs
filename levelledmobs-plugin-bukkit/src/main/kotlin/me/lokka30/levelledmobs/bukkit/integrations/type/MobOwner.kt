package me.lokka30.levelledmobs.bukkit.integrations.type

import me.lokka30.levelledmobs.bukkit.utils.TriState
import org.bukkit.entity.LivingEntity

/*
FIXME Comment
 */
interface MobOwner {

    /*
    FIXME Comment
     */
    fun ownsMob(mob: LivingEntity): TriState

}