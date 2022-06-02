package me.lokka30.levelledmobs.bukkit.logic.functions.triggers;

import org.jetbrains.annotations.NotNull;

public abstract class Trigger {

    /* vars */

    private final String id;

    /* constructors */

    public Trigger(
        final @NotNull String id
    ) {
        this.id = id;
    }

}
