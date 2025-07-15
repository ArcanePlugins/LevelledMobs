package io.github.arcaneplugins.levelledmobs.listeners

import java.util.UUID
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.enums.AttributeNames
import io.github.arcaneplugins.levelledmobs.misc.Cooldown
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Creeper
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * This class is used for debugging the plugin. When an entity is punched, a player with permission
 * will receive a bunch of data about the mob.
 *
 * @author lokka30
 * @since 2.4.0
 */
class EntityDamageDebugListener : Listener {
    private val cooldownMap = mutableMapOf<UUID, Cooldown>()

    //This class is used to debug levellable mobs. It simply displays their current attributes, current health and current level.
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        // Make sure debug entity damage is enabled
        if (!LevelledMobs.instance.debugManager.damageDebugOutputIsEnabled)
            return

        // Make sure the mob is a LivingEntity and the attacker is a Player
        if (event.entity !is LivingEntity || event.damager !is Player)
            return

        val lmEntity = LivingEntityWrapper.getInstance(event.entity as LivingEntity)

        checkEntity(event.damager as Player, lmEntity)
        lmEntity.free()
    }

    @Suppress("DEPRECATION")
    private fun checkEntity(
        player: Player,
        lmEntity: LivingEntityWrapper
    ) {
        // Make sure the mob is levelled
        if (!lmEntity.isLevelled) return

        // Make sure the player has debug perm
        if (!player.hasPermission("levelledmobs.debug")) return


        // Don't spam the player's chat
        val entityId = lmEntity.livingEntity.entityId.toString()
        if (cooldownMap.containsKey(player.uniqueId)) {
            val cooldown = cooldownMap[player.uniqueId]

            if (cooldown!!.doesCooldownBelongToIdentifier(entityId)) {
                if (!cooldown.hasCooldownExpired(2))
                    return
            }

            cooldownMap.remove(player.uniqueId)
        }

        /* Now send them the debug message! :) */
        send(
            player,
            "&8&m+---+&r Debug information for &b" + lmEntity.typeName + "&r &8&m+---+&r"
        )

        // Print non-attributes
        send(player, "&f&nGlobal Values:", false)
        send(player, "&8&m->&b Level: &7" + lmEntity.getMobLevel)
        send(
            player,
            "&8&m->&b Current Health: &7" + Utils.round(lmEntity.livingEntity.health),
            false
        )
        if (lmEntity.livingEntity.customName != null) {
            send(
                player, "&8&m->&b Nametag: &7" + lmEntity.livingEntity.customName,
                false
            )
        }

        // Print attributes
        player.sendMessage(" ")
        send(player, "&f&nAttribute Values:", false)
        for (attributeName in AttributeNames.entries) {
            val attribute = Utils.getAttribute(attributeName) ?: continue
            val attributeInstance = lmEntity.livingEntity.getAttribute(attribute)

            if (attributeInstance == null) continue
            if (Utils.round(attributeInstance.value) == 0.0) continue

            val sb = StringBuilder("&8&m->&b ")
            sb.append(attribute.toString().replace("GENERIC_", ""))
                .append(": &7")
                .append(Utils.round(attributeInstance.value))

            var hadItems = false
            for (mod in attributeInstance.modifiers) {
                if (!hadItems)
                    sb.append(" (")
                else
                    sb.append(", ")

                if (mod.operation == AttributeModifier.Operation.MULTIPLY_SCALAR_1)
                    sb.append("* ")
                else
                    sb.append("+ ")

                sb.append(Utils.round(mod.amount, 5))

                hadItems = true
            }

            if (hadItems) {
                sb.append("), base: ")
                val remainingDigitsStr = attributeInstance.baseValue.toString()
                val remainingDigits = remainingDigitsStr.substring(
                    remainingDigitsStr.indexOf('.')
                ).length - 1
                if (remainingDigits > 1)
                    sb.append(Utils.round(attributeInstance.baseValue), 3)
                else
                    sb.append(attributeInstance.baseValue)
            }
            send(player, sb.toString(), false)
        }

        if (lmEntity.livingEntity is Creeper) {
            // Print unique values (per-mob)
            player.sendMessage(" ")
            send(player, "&f&nUnique Values:", false)

            send(player, "&8&m->&b Creeper Blast Radius: &7" +
                    (lmEntity.livingEntity as Creeper).explosionRadius, false)
        }

        send(player, "&8(End of information.)", false)

        // Add them to a delay, and remove them after 2 seconds (40 ticks)
        cooldownMap[player.uniqueId] = Cooldown(System.currentTimeMillis(), entityId)
    }

    private fun send(
        player: Player,
        message: String
    ) {
        send(player, message, true)
    }

    private fun send(
        player: Player,
        message: String,
        usePrefix: Boolean
    ) {
        if (usePrefix) {
            player.sendMessage(
                colorizeAll(LevelledMobs.instance.configUtils.prefix + "&7 " + message)
            )
        } else
            player.sendMessage(colorizeAll(message))
    }
}