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

    @Override
    public @NotNull String replace(String from, Context context) {
        if(context.getEntity() != null) {
            if(context.getEntity() instanceof LivingEntity entity) {
                if(!EntityDataUtil.isLevelled(entity)) {
                    Log.war("Unable to replace entity level placeholder in message '" + from + "': "
                        + "entity is not levelled", true);
                    return from;
                }

                return from
                    .replace("%entity-level%", Integer.toString(EntityDataUtil.getLevel(entity)))
                    .replace("%entity-min-level%", Integer.toString(EntityDataUtil.getMinLevel(entity)))
                    .replace("%entity-max-level%", Integer.toString(EntityDataUtil.getMaxLevel(entity)))
                    .replace("%entity-level-ratio%", Float.toString(
                        EntityDataUtil.getMinLevel(entity) / (EntityDataUtil.getMaxLevel(entity) * 1.0f)
                    ));
            } else {
                Log.war("Unable to replace entity level placeholder in message '" + from + "': "
                    + "entity is not a LivingEntity", true);
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
