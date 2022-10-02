package me.lokka30.levelledmobs.bukkit.util.modal;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;

public abstract class ModalCollection<T> {

    private final Collection<T> items;
    private Mode mode;

    public ModalCollection(final Collection<T> items, final Mode mode) {
        this.items = Objects.requireNonNull(items, "items");
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public boolean contains(final T item) {
        return switch(getMode()) {
            case INCLUSIVE -> getItems().contains(item);
            case EXCLUSIVE -> !getItems().contains(item);
        };
    }

    @NotNull
    public Collection<T> getItems() {
        return items;
    }

    @NotNull
    public ModalCollection.Mode getMode() {
        return mode;
    }

    public void setMode(final @Nonnull Mode mode) {
        this.mode = mode;
    }

    public enum Mode {
        INCLUSIVE,
        EXCLUSIVE;

        public Mode inverse() {
            return this == INCLUSIVE ? EXCLUSIVE : INCLUSIVE;
        }
    }

}
