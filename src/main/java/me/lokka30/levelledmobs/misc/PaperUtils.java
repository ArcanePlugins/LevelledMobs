package me.lokka30.levelledmobs.misc;

import me.lokka30.microlib.other.VersionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides function for APIs that are used in Paper but not present in Spigot
 *
 * @author stumper66
 * @since 3.3.0
 */
public class PaperUtils {
    public static void sendHyperlink(final @NotNull CommandSender sender, final String message, final String url){
        final Component newCom = Component.text().content(message).build()
                .clickEvent(ClickEvent.openUrl(url));
        sender.sendMessage(newCom);
    }

    public static void updateItemMetaLore(final @NotNull ItemMeta meta, final @Nullable List<String> lore){
        if (lore == null) return;
        final List<Component> newLore = new ArrayList<>(lore.size());

        for (final String loreLine : lore)
            newLore.add(Component.text().decoration(TextDecoration.ITALIC, false).append(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(loreLine)).build());

        meta.lore(newLore);
    }

    public static void updateItemDisplayName(final @NotNull ItemMeta meta, final @Nullable String displayName){
        if (displayName == null) return;

        meta.displayName(Component.text().decoration(TextDecoration.ITALIC, false).append(
                LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)).build());
    }

    @NotNull
    public static String getPlayerDisplayName(final @Nullable Player player){
        if (player == null) return "";
        final Component comp = player.displayName();
        if (comp instanceof TextComponent) {
            if (VersionUtils.isOneSeventeen()) {
                // this is needed because PlainTextComponentSerializer is available in 1.16.5
                return Paper117Utils.serializeTextComponent((TextComponent) comp);
            }
            else
                return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(comp);
        }
        else
            return comp.toString(); // this is never happen but just in case.  it will return a bunch of garbage
    }
}
