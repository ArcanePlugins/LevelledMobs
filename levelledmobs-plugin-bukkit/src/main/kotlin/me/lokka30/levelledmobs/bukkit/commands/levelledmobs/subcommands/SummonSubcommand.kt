package me.lokka30.levelledmobs.bukkit.commands.levelledmobs.subcommands

import me.lokka30.levelledmobs.bukkit.api.data.keys.MobKeyStore
import me.lokka30.levelledmobs.bukkit.commands.CommandWrapper
import org.bukkit.ChatColor.RED
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.persistence.PersistentDataType

/*
[command structure]
    index.. 0   1
    size... 1   2
            :   :
          - /lm |
          - /lm about
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

        sender.world.spawn(sender.location, Zombie::class.java) {
            it.persistentDataContainer.set(MobKeyStore.wasSummoned, PersistentDataType.STRING, "true")
        }
    }

}