package io.github.arcaneplugins.levelledmobs.nametag

import io.github.arcaneplugins.levelledmobs.result.NametagResult
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

/**
 * A common interface for sending nametag packets
 *
 * @author PenalBuffalo (aka stumper66)
 * @since 3.6.0
 */
interface NametagSender {
    fun sendNametag(
        livingEntity: LivingEntity,
        nametag: NametagResult,
        player: Player,
        alwaysVisible: Boolean
    )
}