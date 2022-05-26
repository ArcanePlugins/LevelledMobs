package me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands

import me.lokka30.levelledmobs.bukkit.LevelledMobs
import me.lokka30.levelledmobs.bukkit.api.data.MobDataUtil
import me.lokka30.levelledmobs.bukkit.commands.CommandWrapper
import org.bukkit.Bukkit
import org.bukkit.ChatColor.GRAY
import org.bukkit.ChatColor.GREEN
import org.bukkit.ChatColor.RED
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
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

        if(sender !is Player) {
            sender.sendMessage("${RED}This command is currently only available for players.")
            return
        }

        sender.sendMessage("${GRAY}Spawning entity...")
        sender.world.spawn(sender.location, Zombie::class.java) {
            it.setMetadata("LevelledMobs:WasSummoned".lowercase(), FixedMetadataValue(LevelledMobs.instance!!, 1))
            it.customName = "${GREEN}Summoned Mob"
            it.isCustomNameVisible = true
            it.removeWhenFarAway = true
        }
        // ...event fires...
        sender.sendMessage("${GREEN}Entity spawned.")
        sender.sendMessage("${GRAY}Searching for summoned mobs...")

        //TODO remove test
        for (mob in Bukkit.getWorlds()[0].livingEntities) {
            if (MobDataUtil.getWasSummoned(mob)) {
                sender.sendMessage("${GREEN}Entity found: ${formatLocation(mob)}")
            }
        }

        sender.sendMessage("${GRAY}Search complete.")
    }

    //TODO remove test
    private fun formatLocation(mob: LivingEntity): String {
        return "${mob.location.blockX}, ${mob.location.blockY}, " +
                "${mob.location.blockZ} in ${mob.location.world!!.name}"
    }

}