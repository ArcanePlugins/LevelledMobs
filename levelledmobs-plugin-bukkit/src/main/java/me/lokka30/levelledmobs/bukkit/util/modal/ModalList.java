package me.lokka30.levelledmobs.bukkit.util.modal;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ModalList<T> extends ModalCollection<T> {

    public ModalList(
        final @NotNull List<T> items,
        final @NotNull Type type
    ) {
        super(items, type);
    }

}
