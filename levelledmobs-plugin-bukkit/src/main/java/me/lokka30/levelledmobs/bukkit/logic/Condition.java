package me.lokka30.levelledmobs.bukkit.logic;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public abstract class Condition {

    /* vars */

    private final Process process;

    /* constructors */

    //todo this constructor should be like the Action one
    public Condition(final Process process) {
        this.process = Objects.requireNonNull(process, "process");
    }

    /* methods */

    public abstract boolean applies(final Context context);

    /* getters and setters */

    @NotNull
    public Process getProcess() { return process; }

}
