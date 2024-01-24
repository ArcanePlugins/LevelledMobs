package io.github.arcaneplugins.levelledmobs.commands.subcommands

import io.github.arcaneplugins.levelledmobs.commands.subcommands.SummonSubcommand.SummonType
import io.github.arcaneplugins.levelledmobs.misc.LivingEntityPlaceholder
import io.github.arcaneplugins.levelledmobs.misc.RequestedLevel
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


/**
 * Holds information on used for creating a spawner cube, egg
 * or mob summon
 *
 * @author stumper66
 * @since 3.2.3
 */
class SummonMobOptions(
    val lmPlaceholder: LivingEntityPlaceholder,
    val sender: CommandSender
) {
    var summonType: SummonType? = null
    var amount = 0
    var requestedLevel: RequestedLevel? = null
    var player: Player? = null
    var override: Boolean = false
    var nbtData: String? = null
}