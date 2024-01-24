@file:Suppress("DEPRECATION")

package io.github.arcaneplugins.levelledmobs.util

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.MessageUtils.colorizeAll
import io.github.arcaneplugins.levelledmobs.util.Utils.capitalize
import io.github.arcaneplugins.levelledmobs.util.Utils.colorizeAllInList
import io.github.arcaneplugins.levelledmobs.util.Utils.replaceEx
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.meta.ItemMeta

/**
 * Provides function for APIs that are deprecated in Paper but required for use in Spigot
 *
 * @author stumper66
 * @since 3.3.0
 */
object SpigotUtils {
    fun sendHyperlink(
        sender: CommandSender,
        message: String?,
        url: String?
    ) {
        val component = TextComponent(message)
        component.clickEvent =
            ClickEvent(ClickEvent.Action.OPEN_URL, url)
        component.hoverEvent =
            HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(url))
        sender.sendMessage(component)
    }

    fun updateItemMetaLore(
        meta: ItemMeta,
        lore: MutableList<String>?
    ) {
        if (lore == null) {
            meta.lore = null
        } else {
            meta.lore = colorizeAllInList(lore)
        }
    }

    fun updateItemDisplayName(
        meta: ItemMeta,
        displayName: String?
    ) {
        meta.setDisplayName(displayName)
    }

    fun getPlayerDisplayName(
        player: Player?
    ): String {
        if (player == null) {
            return ""
        }
        return player.displayName
    }

    fun getPlayersKiller(
        event: PlayerDeathEvent
    ): LivingEntityWrapper? {
        if (event.deathMessage == null) {
            return null
        }

        val entityDamageEvent = event.entity.lastDamageCause
        if (entityDamageEvent == null || entityDamageEvent.isCancelled
            || entityDamageEvent !is EntityDamageByEntityEvent
        ) {
            return null
        }

        val damager = entityDamageEvent.damager
        var killer: LivingEntity? = null

        if (damager is Projectile) {
            if (damager.shooter is LivingEntity) {
                killer = damager.shooter as LivingEntity?
            }
        } else if (damager is LivingEntity) {
            killer = damager
        }

        if (killer == null || killer.name.isEmpty() || killer is Player) {
            return null
        }

        val lmKiller = LivingEntityWrapper.getInstance(killer)
        lmKiller.associatedPlayer = event.entity
        if (!lmKiller.isLevelled) {
            return lmKiller
        }

        val nametagResult = LevelledMobs.instance.levelManager.getNametag(lmKiller,
            isDeathNametag = true,
            preserveMobName = true
        )
        var deathMessage = nametagResult.nametagNonNull
            .replace("%player%", event.entity.name)
        if (deathMessage.isEmpty() || "disabled".equals(deathMessage, ignoreCase = true)) {
            return lmKiller
        }

        if (nametagResult.hadCustomDeathMessage) {
            var nametag = nametagResult.nametagNonNull
            if (nametag.contains("{DisplayName}")) {
                nametag = nametag.replace(
                    "{DisplayName}", LevelledMobs.instance.levelManager.replaceStringPlaceholders(
                        nametagResult.customDeathMessage!!, lmKiller, false, null, false
                    )
                )
            }
            event.deathMessage = colorizeAll(nametag.replace("%player%", event.entity.name))
        } else {
            if (deathMessage.contains("{DisplayName}")) {
                deathMessage = deathMessage.replace("{DisplayName}", capitalize(lmKiller.nameIfBaby))
            }

            event.deathMessage = colorizeAll(replaceEx(event.deathMessage!!, killer.name, deathMessage))
        }

        return lmKiller
    }
}