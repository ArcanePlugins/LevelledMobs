package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs

import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import org.spongepowered.configurate.CommentedConfigurationNode
import java.util.function.Consumer

class SetBuffsAction(
    val process: Process,
    val node: CommentedConfigurationNode
): Action(process, node) {
    /*
    TODO: Add buffs
        - CUSTOM_RANGED_ATTACK_DAMAGE
        - CUSTOM_CREEPER_BLAST_DAMAGE
        - CUSTOM_ITEM_DROP
        - CUSTOM_XP_DROP
     */
    val buffs = mutableSetOf<Buff>()
    var enabled = false
    init {
        debug(DebugCategory.BUFFS) { "Initialized SetBuffsAction @ " + actionNode.path() }

        enabled = actionNode.node("enabled").getBoolean(true)
        if (enabled){
            for (buffNode in actionNode.node("buffs").childrenList()) {
                debug(DebugCategory.BUFFS) { "Parsing buff @ " + buffNode.path() }
                buffs.add(Buff(buffNode))
            }

            debug(DebugCategory.BUFFS) { "Parsed ${buffs.size} buffs" }
        }
    }

    override fun run(context: Context) {
        if (!enabled) return

        checkNotNull(context.livingEntity) {
            "SetBuffsAction at path '${actionNode.path()}' attempted to run " +
                    "without an entity context."
        }

        buffs.forEach(Consumer { buff: Buff -> buff.apply(context, context.livingEntity!!) })
    }
}