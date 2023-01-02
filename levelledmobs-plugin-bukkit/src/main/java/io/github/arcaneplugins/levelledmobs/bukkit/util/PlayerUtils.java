package io.github.arcaneplugins.levelledmobs.bukkit.util;

import java.util.function.Predicate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerUtils {

    private PlayerUtils() throws IllegalAccessException {
        throw new IllegalAccessException("Illegal instantiation of utility class");
    }


    public static @Nullable FoundItemInHandResult findItemStackInEitherHand(
        final @NotNull Player player,
        final @NotNull Predicate<@Nullable ItemStack> predicate
    ) {
        final PlayerInventory inventory = player.getInventory();

        if(predicate.test(inventory.getItemInMainHand())) {
            return new FoundItemInHandResult(inventory.getItemInMainHand(), true);
        } else if(predicate.test(inventory.getItemInOffHand())) {
            return new FoundItemInHandResult(inventory.getItemInOffHand(), false);
        }

        return null;
    }

    /**
     * Object returned by {@link PlayerUtils#findItemStackInEitherHand(Player, Predicate)}.
     *
     * @param itemStack  The {@link ItemStack} that was found in the search.
     * @param inMainHand {@code true} if the item was found in the main hand, {@code false} if
     *                   the item was found in the off-hand.
     * @since 4.0.0
     */
    public record FoundItemInHandResult(
        @NotNull ItemStack itemStack,
        boolean inMainHand
    )  {}

}
