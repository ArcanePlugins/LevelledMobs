package me.lokka30.levelledmobs.bukkit.integrations.internal

import me.lokka30.levelledmobs.bukkit.integrations.Integration
import me.lokka30.levelledmobs.bukkit.integrations.type.MobOwner
import me.lokka30.levelledmobs.bukkit.utils.TriState
import org.bukkit.entity.LivingEntity

/*
This integration allows detection of NPCs from the Citizens2 plugin.
 */
class CitizensIntegration : Integration(
    "Detects mobs which are Citizens NPCs",
    true,
    true
), MobOwner {

    /*
    Citizens NPCs have the metadata `NPC`. We can simply check if the mob has that metadata, to
    determine whether a mob is a Citizens NPC or not.
     */
    override fun ownsMob(mob: LivingEntity): TriState {
        // FIXME This needs to be tested.
        return TriState.of(mob.hasMetadata("NPC"))
    }

}