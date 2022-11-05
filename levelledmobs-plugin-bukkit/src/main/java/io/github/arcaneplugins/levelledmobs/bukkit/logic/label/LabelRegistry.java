package io.github.arcaneplugins.levelledmobs.bukkit.logic.label;

import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.bukkit.entity.LivingEntity;

public class LabelRegistry {

    private static final Set<LabelHandler> labelHandlers = new HashSet<>();

    private LabelRegistry() {}

    @Nonnull
    public static Set<LabelHandler> getLabelHandlers() {
        return labelHandlers;
    }

    public static void setPrimaryLabelHandler(
        @Nonnull final LivingEntity lent,
        @Nonnull final String handlerId,
        final boolean requirePersistence
    ) {
        InternalEntityDataUtil.setPrimaryLabelHandler(lent, handlerId, requirePersistence);
    }

}
