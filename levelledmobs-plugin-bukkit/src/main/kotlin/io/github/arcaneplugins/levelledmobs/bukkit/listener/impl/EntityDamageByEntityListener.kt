package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl

import de.themoep.minedown.adventure.MineDown
import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugHandler.isCategoryEnabled
import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.evaluateExpression
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.replacePapiAndContextPlaceholders
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.runFunctionsWithTriggers
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs.Buff
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnumUtils.formatEnumConstant
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log
import io.github.arcaneplugins.levelledmobs.bukkit.util.PlayerUtils
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.scheduler.BukkitRunnable
import java.text.DecimalFormat
import java.util.function.Consumer

class EntityDamageByEntityListener : ListenerWrapper(true) {
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent){
        val defender = event.entity
        val attacker = event.damager //TODO see TODO below. need an attacker context.

        handleShieldBreaker(event)
        handleEntityInspector(event)
        val context = Context().withEntity(defender)
        if (attacker is Player){
            context.player = attacker
        }

        runFunctionsWithTriggers(
            context, mutableListOf("on-entity-damage-by-entity")
        //TODO also have 'attacker' context
        )
    }

    private fun handleEntityInspector(
        event: EntityDamageByEntityEvent
    ){
        if (!isCategoryEnabled(DebugCategory.ENTITY_INSPECTOR) ||
            event.entity !is LivingEntity ||
            event.damager !is Player
        ) {
            return
        }

        val enInspected = event.entity as LivingEntity
        val plInspector = event.damager as Player

        if (!plInspector.hasPermission("levelledmobs.debug") ||
            !EntityDataUtil.isLevelled(enInspected, true)){
            return
        }

        val context: Context = Context()
            .withEntity(enInspected)
            .withPlayer(plInspector)

        val messenger =
            Consumer { message: String? ->
                plInspector.sendMessage(
                    MineDown.parse(replacePapiAndContextPlaceholders(message, context))
                )
            }

        val to4dp = DecimalFormat("#,##0.0###")

        object : BukkitRunnable() {
            override fun run() {
                messenger.accept(
                    "&8┌ &f&lLM:&7 Inspecting &bLvl.%entity-level% %entity-type-formatted%"
                )
                messenger.accept("&8 • &7Name:&f %entity-name%")
                messenger.accept("&8 • &7Health:&f %entity-health-rounded%&8/&f%entity-max-health-rounded% HP")
                messenger.accept(String.format(
                    "&8 • &7Min Level: &f%s&7; Max Level: &f%s&7; Ratio: &f%s",
                    EntityDataUtil.getMinLevel(enInspected, false),
                    EntityDataUtil.getMaxLevel(enInspected, false),
                    to4dp.format(EntityDataUtil.getLevelRatio(enInspected, false))
                )
                )

                messenger.accept("&8 • &7Attributes:")
                for (a in Attribute.entries){
                    val ai = enInspected.getAttribute(a) ?: continue

                    val sb = StringBuilder(
                        "&8   • &7${formatEnumConstant(a)}: &f" +
                                "${to4dp.format(ai.value)}&7"
                    )

                    if (ai.value != ai.baseValue) sb.append(" (from &f")
                        .append(to4dp.format(ai.baseValue))
                        .append("&7)")

                    if (ai.modifiers.stream()
                            .anyMatch { mod: AttributeModifier ->
                                mod.name.startsWith(Buff.ATTRIBUTE_MODIFIER_PREFIX)
                            }
                    ) {
                        sb.append(" [✔LM](color=dark_aqua format=italic ")
                            .append("show_text=Modified by LevelledMobs)")
                    } else {
                        sb.append(" [❌LM](color=dark_gray format=italic ")
                            .append("show_text=Not modified by LevelledMobs)")
                    }

                    messenger.accept(sb.toString())
                }

                messenger.accept("&8└ ... (end of information) ...")
            }
        }.runTaskLater(LevelledMobs.lmInstance, 1)
    }

    private fun handleShieldBreaker(
        event: EntityDamageByEntityEvent
    ) {
        /*
        To handle the shield breaker, the following conditions must be met:
            1. The defending entity is a Player
            2. The attacking entity is a LivingEntity
            3. The attacking entity is levelled
         */

        if (event.entity !is Player) return
        if (event.damager !is LivingEntity) return

        val enAttacker = event.damager as LivingEntity
        val plDefender = event.entity as Player
        if (!EntityDataUtil.isLevelled(enAttacker, true)) return

        val shieldSearch = PlayerUtils.findItemStackInEitherHand(plDefender) {
            itemStack -> itemStack != null && itemStack.type === Material.SHIELD
        } ?: return

        val formula = EntityDataUtil
            .getShieldBreakerMultiplierFormula(enAttacker, true) ?: return

        //TODO remove
        val multiplier = evaluateExpression(
            replacePapiAndContextPlaceholders(
                formula,
                Context(plDefender)
                //TODO also have attacker context. Context#withAttacker(enAttacker)
            )
        )

        /*
        darn ... unfortunately the bukkit API does not allow us to get the entity who caused
        the player's item to be damaged in PlayerItemDamageEvent. so shield breaking won't be
        a feature until that changes. I'll leave the code here in case it becomes possible
        in the future.

        Yes, I have thought about using a bit of hacky code to get around it, but I'd rather not
        since it could cause weird side-effects. For instance, applying a 1-tick-expiry
        temporary metadata value to `plDefender` with the `multiplier` value, and then referencing
        that in the `PlayerItemDamageEvent`.
        */
    }
}