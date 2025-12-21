package io.github.arcaneplugins.levelledmobs.util

import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.util.Paper117Utils.serializeTextComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.ItemMeta

/**
 * Provides function for APIs that are used in Paper but not present in Spigot
 *
 * @author stumper66
 * @since 3.3.0
 */
object PaperUtils {
    fun sendHyperlink(
        sender: CommandSender,
        message: String,
        url: String
    ) {
        val newCom: Component = Component.text().content(message).build()
            .clickEvent(ClickEvent.openUrl(url))
        sender.sendMessage(newCom)
    }

    fun updateItemMetaLore(
        meta: ItemMeta,
        lore: List<String>?
    ) {
        if (lore == null) return

        val newLore = mutableListOf<Component>()

        for (loreLine in lore) {
            newLore.add(
                Component.text().decoration(TextDecoration.ITALIC, false).append(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(loreLine)
                ).build()
            )
        }

        meta.lore(newLore)
    }

    fun updateItemDisplayName(
        meta: ItemMeta,
        displayName: String?
    ) {
        if (displayName == null) return

        meta.displayName(
            Component.text().decoration(TextDecoration.ITALIC, false).append(
                LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
            ).build()
        )
    }

    fun getPlayerDisplayName(
        player: Player?
    ): String {
        if (player == null) return ""

        val comp = player.displayName()
        return if (comp is TextComponent) {
            if (LevelledMobs.instance.ver.minecraftVersion >= 1.17) {
                // this is needed because PlainTextComponentSerializer is available in 1.17+
                serializeTextComponent(comp)
            } else {
                LegacyComponentSerializer.legacySection()
                    .serialize(comp)
            }
        }
        else
            comp.toString() // this is never happen but just in case.  it will return a bunch of garbage
    }
}