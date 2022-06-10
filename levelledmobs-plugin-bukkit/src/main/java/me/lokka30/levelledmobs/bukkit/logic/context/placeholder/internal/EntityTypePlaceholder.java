package me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal;

import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholder;
import me.lokka30.levelledmobs.bukkit.util.EnumUtils;
import org.jetbrains.annotations.NotNull;

/*
    Placeholder   |      Description      |      Example
------------------+-----------------------+------------------
    %entity-type% | formatted entity type | 'Wither Skeleton'
%entity-type-raw% |    raw entity type    | 'WITHER_SKELETON'
 */
public class EntityTypePlaceholder implements ContextPlaceholder {

    @Override
    public @NotNull String replace(String from, Context context) {
        if(context.getEntityType() == null) {
            return from;
        }

        /*
        Wither Skeleton
         */
        if(from.contains("%entity-type%")) {
            return from.replace(
                "%entity-type%",
                EnumUtils.formatEnumConstant(context.getEntityType())
            );
        }

        /*
        WITHER_SKELETON
         */
        if(from.contains("%entity-type-raw%")) {
            return from.replace(
                "%entity-type-raw%",
                context.getEntityType().toString()
            );
        }

        return from;
    }
}
