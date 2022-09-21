package me.lokka30.levelledmobs.bukkit.listener;

import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;

public class EntityBreedListener extends ListenerWrapper {

    public EntityBreedListener() {
        super(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void handle(final EntityBreedEvent event) {
        final LivingEntity child = event.getEntity();
        final LivingEntity father = event.getFather();
        final LivingEntity mother = event.getMother();

        InternalEntityDataUtil.setFather(child, father, true);
        InternalEntityDataUtil.setMother(child, mother, true);
        InternalEntityDataUtil.setWasBred(child, true, true);
    }
}
