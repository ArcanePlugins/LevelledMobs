package io.github.arcaneplugins.levelledmobs.bukkit.logic.label;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

public class LabelRegistry {

    private static final Set<LabelHandler> labelHandlers = new HashSet<>();

    private LabelRegistry() {}

    @Nonnull
    public static Set<LabelHandler> getLabelHandlers() {
        return labelHandlers;
    }

}
