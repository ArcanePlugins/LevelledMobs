package me.lokka30.levelledmobs.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MobLevelEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    private boolean cancelled = false;

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * When a mob is levelled, the following enum is used to
     * allow plugins to find the cause of the mob being
     * levelled.
     * <p>
     * NORMAL: Spawned naturally, by a spawn egg, etc.
     * SUMMON: Spawned using the '/lm summon' command.
     * RE_LEVEL: When an existing levelled mob has its level changed.
     */
    public enum LevelCause {
        NORMAL, SUMMON, RE_LEVEL
    }

    private final LivingEntity entity;
    private int level;
    private final LevelCause levelCause;

    public MobLevelEvent(LivingEntity entity, int level, LevelCause levelCause) {
        this.entity = entity;
        this.level = level;
        this.levelCause = levelCause;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public int getLevel() {
        return level;
    }

    public LevelCause getCause() {
        return levelCause;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
