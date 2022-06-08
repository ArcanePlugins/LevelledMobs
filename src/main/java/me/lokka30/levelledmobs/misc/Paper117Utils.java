package me.lokka30.levelledmobs.misc;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Provides function for APIs that are used in Paper 1.17+ but not present in 1.16
 *
 * @author stumper66
 * @since 3.3.0
 */
class Paper117Utils {
    @NotNull
    static String serializeTextComponent(final @NotNull TextComponent textComponent){
        return PlainTextComponentSerializer.plainText().serialize(textComponent);
    }
}
