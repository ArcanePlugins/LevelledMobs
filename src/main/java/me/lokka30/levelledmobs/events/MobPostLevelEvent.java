package me.lokka30.levelledmobs.events;

import me.lokka30.levelledmobs.LevelInterface;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

/**
 * TODO Describe...
 *
 * @author lokka30
 */
public class MobPostLevelEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * When a mob is levelled, the following enum is used to
     * allow plugins to find the cause of the mob being
     * levelled.
     * <p>
     * NORMAL: Spawned naturally, by a spawn egg, etc.
     * CHANGED_LEVEL: When an existing levelled mob has its level changed.
     */
    public enum LevelCause {
        NORMAL,
        CHANGED_LEVEL,
        SUMMONED
    }

    private final LivingEntity entity;
    private int level;
    private final LevelCause levelCause;
    private final HashSet<LevelInterface.AdditionalLevelInformation> additionalInformation;

    public MobPostLevelEvent(@NotNull LivingEntity entity, int level, @NotNull LevelCause levelCause, @Nullable HashSet<LevelInterface.AdditionalLevelInformation> additionalInformation) {
        this.entity = entity;
        this.level = level;
        this.levelCause = levelCause;
        this.additionalInformation = additionalInformation;
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

    public HashSet<LevelInterface.AdditionalLevelInformation> getAdditionalInformation() {
        return additionalInformation;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
