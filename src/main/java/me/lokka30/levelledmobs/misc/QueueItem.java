package me.lokka30.levelledmobs.misc;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public class QueueItem {
    public QueueItem(final LivingEntityWrapper lmEntity, final Event event){
        this.lmEntity = lmEntity;
        this.event = event;
        this.isMobProcessQueue = true;
    }

    public QueueItem(final LivingEntityWrapper lmEntity, final String nametag, final List<Player> players){
        this.lmEntity = lmEntity;
        this.nametag = nametag;
        this.isMobProcessQueue = false;
        this.players = players;
    }

    public final LivingEntityWrapper lmEntity;
    public Event event;
    public List<Player> players;
    public String nametag;
    public final boolean isMobProcessQueue;
}
