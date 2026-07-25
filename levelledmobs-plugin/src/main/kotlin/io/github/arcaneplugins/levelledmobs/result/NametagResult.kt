package io.github.arcaneplugins.levelledmobs.result

import java.util.Optional
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

    // Player-independent nametag component (an NMS/vanilla component) built lazily on the
    // first packet send for this result and reused for every other viewing player. Avoids
    // repeating MiniMessage/legacy deserialization and component conversion per player.
    var cachedComponent: Optional<Any>? = null

    val nametagNonNull: String
        get() = nametag?: ""

    val isNullOrEmpty: Boolean
        get() = nametag.isNullOrEmpty()

    val hadCustomDeathMessage: Boolean
        get() = customDeathMessage != null
}