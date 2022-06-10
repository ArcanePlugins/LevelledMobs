package me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal;

import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholder;
import org.jetbrains.annotations.NotNull;

/*
    Placeholder   |      Description      |      Example
------------------+-----------------------+------------------
    %entity-type% | formatted entity type | 'Wither Skeleton'
%entity-type-raw% |    raw entity type    | 'WITHER_SKELETON'
 */
public class PlayerNamePlaceholder implements ContextPlaceholder {

    @Override
    public @NotNull String replace(String from, Context context) {
        if(context.getPlayer() == null) {
            return from;
        }

        /*
        Wither Skeleton
         */
        if(from.contains("%player-name%")) {
            return from.replace(
                "%player-name%",
                context.getPlayer().getName()
            );
        }

        /*
        WITHER_SKELETON
         */
        if(from.contains("%player-displayname%")) {
            return from.replace(
                "%player-displayname%",
                context.getPlayer().getDisplayName()
            );
        }

        return from;
    }
}
