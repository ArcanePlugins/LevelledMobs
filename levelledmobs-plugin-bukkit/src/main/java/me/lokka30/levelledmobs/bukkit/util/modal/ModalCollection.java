package me.lokka30.levelledmobs.bukkit.util.modal;

import java.util.Collection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public abstract class ModalCollection<T> {

    private final Collection<T> items;
    private final Type type;

    public ModalCollection(final Collection<T> items, final Type type) {
        this.items = Objects.requireNonNull(items, "items");
        this.type = Objects.requireNonNull(type, "type");
    }

    public boolean contains(final T item) {
        return switch(getType()) {
            case INCLUSIVE -> getItems().contains(item);
            case EXCLUSIVE -> !getItems().contains(item);
        };
    }

    @NotNull
    public Collection<T> getItems() {
        return items;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    public enum Type {
        INCLUSIVE,
        EXCLUSIVE
    }

}
