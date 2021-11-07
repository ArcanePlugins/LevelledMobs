package me.lokka30.levelledmobs.misc;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("deprecation")
public class SpigotUtils {
    public static void sendHyperlink(final @NotNull CommandSender sender, final String message, final String url){

        final TextComponent component = new TextComponent(message);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(url)));
        sender.sendMessage(component);
    }

    public static void updateItemMetaLore(final @NotNull ItemMeta meta, final @Nullable List<String> lore){
        meta.setLore(lore);
    }

    public static void updateItemDisplayName(final @NotNull ItemMeta meta, final @Nullable String displayName){
        meta.setDisplayName(displayName);
    }

    @NotNull
    public static String getPlayerDisplayName(final @Nullable Player player){
        if (player == null) return "";
        return player.getDisplayName();
    }
}
