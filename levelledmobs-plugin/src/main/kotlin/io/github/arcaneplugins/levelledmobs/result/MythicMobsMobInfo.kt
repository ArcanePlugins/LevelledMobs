package io.github.arcaneplugins.levelledmobs.result

/**
 * Holds info that was obtained from internal
 * Mythic Mobs settings on a particular mob
 *
 * @author stumper66
 * @since 3.6.0
 */
class MythicMobsMobInfo {
    var preventOtherDrops: Boolean = false
    var preventRandomEquipment: Boolean = false
    var internalName: String? = null
}