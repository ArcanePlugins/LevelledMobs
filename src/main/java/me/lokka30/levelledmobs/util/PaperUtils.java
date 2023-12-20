package me.lokka30.levelledmobs.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides function for APIs that are used in Paper but not present in Spigot
 *
 * @author stumper66
 * @since 3.3.0
 */
public class PaperUtils {

    public static void sendHyperlink(final @NotNull CommandSender sender, final String message,
        final String url) {
        final Component newCom = Component.text().content(message).build()
            .clickEvent(ClickEvent.openUrl(url));
        sender.sendMessage(newCom);
    }

    public static void updateItemMetaLore(final @NotNull ItemMeta meta,
        final @Nullable List<String> lore) {
        if (lore == null) {
            return;
        }
        final List<Component> newLore = new ArrayList<>(lore.size());

        for (final String loreLine : lore) {
            newLore.add(Component.text().decoration(TextDecoration.ITALIC, false).append(
                LegacyComponentSerializer.legacyAmpersand().deserialize(loreLine)).build());
        }

        meta.lore(newLore);
    }

    public static void updateItemDisplayName(final @NotNull ItemMeta meta,
        final @Nullable String displayName) {
        if (displayName == null) {
            return;
        }

        meta.displayName(Component.text().decoration(TextDecoration.ITALIC, false).append(
            LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)).build());
    }

    public @NotNull static String getPlayerDisplayName(final @Nullable Player player) {
        if (player == null) {
            return "";
        }
        final Component comp = player.displayName();
        if (comp instanceof TextComponent) {
            if (LevelledMobs.getInstance().getVerInfo().getMinecraftVersion() >= 1.17) {
                // this is needed because PlainTextComponentSerializer is available in 1.17+
                return Paper117Utils.serializeTextComponent((TextComponent) comp);
            } else {
                return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                    .serialize(comp);
            }
        } else {
            return comp.toString(); // this is never happen but just in case.  it will return a bunch of garbage
        }
    }

    public static @Nullable List<ItemStack> getMobPickedUpItems(final @NotNull LivingEntityWrapper lmEntity){
        final NamespacedKey pickedUpItems = LevelledMobs.getInstance().namespacedKeys.pickedUpItems;
        if (!lmEntity.getPDC().has(pickedUpItems)) return null;

        final String allItems = lmEntity.getPDC().get(pickedUpItems, PersistentDataType.STRING);
        if (allItems == null || allItems.isEmpty()) return null;

        final List<ItemStack> results = new LinkedList<>();

        for (final String serializedItem : allItems.split(";")){
            if (serializedItem.length() % 2 != 0){
                Utils.logger.info("Unable to deserialize picked up item, invalid length: " + serializedItem.length());
                continue;
            }

            final byte[] bytes = hexToByte(serializedItem);
            try{
                results.add(ItemStack.deserializeBytes(bytes));
            }
            catch (Exception e){
                Utils.logger.info("Unable to deserialize itemstack: " + e.getMessage());
            }
        }

        return results.isEmpty() ?
                null : results;
    }

    // this code was originally from https://www.baeldung.com/java-byte-arrays-hex-strings
    private static byte @NotNull [] hexToByte(final @NotNull String hexString) {
        byte[] result = new byte[hexString.length() / 2];

        int currentIndex = 0;
        for (int i = 0; i < hexString.length(); i+= 2){
            final int firstDigit = toDigit(hexString.charAt(i));
            final int secondDigit = toDigit(hexString.charAt(i + 1));
            result[currentIndex++] = (byte) ((firstDigit << 4) + secondDigit);
        }

        return result;
    }

    private static int toDigit(final char hexChar) {
        final int digit = Character.digit(hexChar, 16);
        if(digit == -1) {
            throw new IllegalArgumentException(
                    "Invalid Hexadecimal Character: "+ hexChar);
        }
        return digit;
    }
}
