package io.github.arcaneplugins.levelledmobs.util

import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * Provides function for APIs that are used in Paper 1.17+ but not present in 1.16
 *
 * @author stumper66
 * @since 3.3.0
 */
object Paper117Utils {
    fun serializeTextComponent(
        textComponent: TextComponent
    ): String {
        return PlainTextComponentSerializer.plainText().serialize(textComponent)
    }
}