package me.lokka30.levelledmobs.misc;

import org.bukkit.event.entity.EntitySpawnEvent;

public class QueueItem {
    public QueueItem(final LivingEntityWrapper lmEntity, final EntitySpawnEvent event){
        this.lmEntity = lmEntity;
        this.event = event;
    }

    public final LivingEntityWrapper lmEntity;
    public final EntitySpawnEvent event;
}
