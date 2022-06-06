package me.lokka30.levelledmobs.bukkit.logic;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class Group {

    /* vars */

    private final String identifier;
    private final ArrayList<String> items = new ArrayList<>();

    /* constructors */

    public Group(final @NotNull String identifier) {
        this.identifier = identifier;
    }

    public Group(
        final @NotNull String identifier,
        final @NotNull List<String> items
    ) {
        this.identifier = identifier;
        this.items.addAll(items);
    }

    /* getters and setters */

    @NotNull
    public String getIdentifier() { return identifier; }

    @NotNull
    public ArrayList<String> getItems() { return items; }

}
