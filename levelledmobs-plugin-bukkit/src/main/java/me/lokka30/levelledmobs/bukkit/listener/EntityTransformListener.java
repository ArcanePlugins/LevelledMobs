package me.lokka30.levelledmobs.bukkit.listener;

import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTransformEvent;

public class EntityTransformListener extends ListenerWrapper {

    public EntityTransformListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void handle(final EntityTransformEvent event) {
        final Entity parent = event.getEntity();
        if(!(parent instanceof LivingEntity lparent)) return;

        event.getTransformedEntities().stream()
            .filter(child -> child instanceof LivingEntity)
            .map(child -> (LivingEntity) child)
            .forEach(child -> {
                InternalEntityDataUtil.setFather(child, lparent, true);
                InternalEntityDataUtil.setMother(child, lparent, true);
            });
    }
}
