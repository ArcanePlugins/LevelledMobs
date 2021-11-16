package me.lokka30.levelledmobs.misc;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
            newLore.add(Component.text().content(loreLine).build());

        meta.lore(newLore);
    }

    public static void updateItemDisplayName(final @NotNull ItemMeta meta, final @Nullable String displayName){
        if (displayName == null) return;

        meta.displayName(Component.text().content(displayName).build());
    }

    @NotNull
    public static String getPlayerDisplayName(final @Nullable Player player){
        if (player == null) return "";
        final Component comp = player.displayName();
        if (comp instanceof TextComponent)
            return PlainTextComponentSerializer.plainText().serialize(comp);
        else
            return comp.toString(); // this is never happen but just in case.  it will return a bunch of garbage
    }
}
