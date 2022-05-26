package me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands

import me.lokka30.levelledmobs.bukkit.LevelledMobs
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.prefixInf
import me.lokka30.levelledmobs.bukkit.api.data.keys.EntityKeyStore
import me.lokka30.levelledmobs.bukkit.commands.CommandWrapper
import org.bukkit.ChatColor.AQUA
import org.bukkit.ChatColor.GRAY
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.metadata.FixedMetadataValue

/*
[command structure]
    index.. 0   1
    size... 1   2
            :   :
          - /lm |
          - /lm summon ... FIXME ...
 */
class SummonSubcommand : CommandWrapper() {

    companion object {
        const val usage = "/lm summon"

        val labels = mutableSetOf(
            "summon"
        )
    }

    override fun labels(): MutableSet<String> {
        return labels
    }

    override fun usage(): String {
        return usage
    }

    override fun run(
        sender: CommandSender,
        args: Array<String>
    ) {
        if (!hasPerm(sender, "levelledmobs.command.levelledmobs.summon", true)) return
        if (!senderIsPlayer(sender, true)) return
        sender as Player // giving the compiler a hand figuring out the smart casting

        sender.sendMessage("${prefixInf}Summoning entity...")
        val entity = sender.world.spawn(sender.location, Zombie::class.java) {
            /*
            this code block runs *before* entity spawn event is fired!
            */

            // we want to make sure EntitySpawnListener knows that this mob was summoned.
            // EntitySpawnListener will convert this non-persistent metadata to be persistent.
            it.setMetadata(
                EntityKeyStore.wasSummoned.toString(),
                FixedMetadataValue(LevelledMobs.instance!!, 1)
            )
        }
        sender.sendMessage("${prefixInf}Successfully summoned a levelled '${AQUA}${entity.name}" +
                "${GRAY}' mob.")
    }

}