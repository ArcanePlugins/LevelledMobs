package io.github.arcaneplugins.levelledmobs.result

import org.bukkit.entity.LivingEntity

/**
 * Used to hold the result of getting or updating nametags
 *
 * @author stumper66
 * @since 3.7.0
 */
class NametagResult(
    var nametag: String?
) {
    var overriddenName: String? = null
    var customDeathMessage: String? = null
    var killerMob: LivingEntity? = null

    val nametagNonNull: String
        get() = nametag?: ""

    val isNullOrEmpty: Boolean
        get() = nametag.isNullOrEmpty()

    val hadCustomDeathMessage: Boolean
        get() = customDeathMessage != null
}