package io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs.BuffType;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.ModalCollection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;

public class ModalBuffTypeSet extends ModalCollection<BuffType> {

    public ModalBuffTypeSet(
        @NotNull EnumSet<BuffType> items,
        @NotNull Mode mode
    ) {
        super(items, mode);
    }

    public static ModalBuffTypeSet fromCfgSection(
        final @Nonnull CommentedConfigurationNode node
    ) {
        final ModalCollection.Mode mode;
        final EnumSet<BuffType> items;

        if(node.hasChild("in-list")) {
            mode = Mode.INCLUSIVE;
            items = EnumSet.noneOf(BuffType.class);

            try {
                for(final String buffTypeStr : node.node("in-list").getList(String.class,
                    Collections.emptyList())
                ) {
                    items.add(BuffType.valueOf(buffTypeStr.toUpperCase(Locale.ROOT)));
                }
            } catch(ConfigurateException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            mode = Mode.EXCLUSIVE;
            items = EnumSet.allOf(BuffType.class);

            try {
                for(final String buffTypeStr : node.node("not-in-list").getList(String.class,
                    Collections.emptyList())
                ) {
                    items.remove(BuffType.valueOf(buffTypeStr.toUpperCase(Locale.ROOT)));
                }
            } catch(ConfigurateException ex) {
                throw new RuntimeException(ex);
            }
        }

        return new ModalBuffTypeSet(items, mode);
    }
}
