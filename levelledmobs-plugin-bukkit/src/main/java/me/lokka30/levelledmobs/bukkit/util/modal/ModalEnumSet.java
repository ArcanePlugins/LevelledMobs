package me.lokka30.levelledmobs.bukkit.util.modal;

import java.util.EnumSet;
import org.jetbrains.annotations.NotNull;

//TODO Unused, remove?

public class ModalEnumSet<E extends Enum<E>> extends ModalCollection<E> {

    public ModalEnumSet(
        final @NotNull EnumSet<E> items,
        final @NotNull Type type
    ) {
        super(items, type);
    }

}
