package me.lokka30.levelledmobs.bukkit.logic.context.placeholder.internal;

import me.lokka30.levelledmobs.bukkit.api.data.EntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholder;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/*
%entity-name%: translated entity name e.g. 'Brain Eater'
 */
public class EntityLevelPlaceholder implements ContextPlaceholder {

    // TODO transfer to EntityPlaceholders class and use replaceIfExists instead

    @Override
    @SuppressWarnings("ConstantConditions")
    public @NotNull String replace(String from, Context context) {
        if(context.getEntity() != null) {
            if(context.getEntity() instanceof LivingEntity entity) {
                if(!EntityDataUtil.isLevelled(entity, false)) {
                    return from;
                }

                final int level = EntityDataUtil.getLevel(entity, false);
                final int minLevel = EntityDataUtil.getMinLevel(entity, false);
                final int maxLevel = EntityDataUtil.getMaxLevel(entity, false);
                final float levelRatio = ((level - minLevel) * 1.0f / (maxLevel - minLevel));

                return from
                    .replace("%entity-level%", Integer.toString(EntityDataUtil.getLevel(entity, false)))
                    .replace("%entity-min-level%", Integer.toString(EntityDataUtil.getMinLevel(entity, false)))
                    .replace("%entity-max-level%", Integer.toString(EntityDataUtil.getMaxLevel(entity, false)))
                    .replace("%entity-level-ratio%", Float.toString(levelRatio));
            } else {
                return from;
            }
        } else {
            // TODO error
            Log.war("Unable to replace entity level placeholder in message '" + from + "': "
                + "no entity context", true);
            return from;
        }
    }
}
