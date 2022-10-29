package io.github.arcaneplugins.levelledmobs.bukkit.logic.group;

import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class Group {

    /* vars */

    private final String identifier;
    private final List<String> items = new LinkedList<>();

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
    public List<String> getItems() { return items; }

}
