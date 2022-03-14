package me.lokka30.levelledmobs.rule.listener;

/**
 * This enum lists all of the 'listenable' events that LevelledMobs has. A Listenable Event is a
 * Bukkit event that LevelledMobs allows use of in the Listeners part of the Rules System.
 *
 * @author lokka30
 * @version 1
 * @see org.bukkit.event
 * @since 4.0.0
 */
public enum ListenableEvent {

    /**
     * Represents {@code ChunkLoadEvent}.
     *
     * @see org.bukkit.event.world.ChunkLoadEvent
     * @since 4.0.0
     */
    CHUNK_LOAD,

    /**
     * Represents {@code EntityCombustEvent}.
     *
     * @see org.bukkit.event.entity.EntityCombustEvent
     * @since 4.0.0
     */
    ENTITY_COMBUST,

    /**
     * Represents {@code EntityDamageEvent}.
     *
     * @see org.bukkit.event.entity.EntityDamageEvent
     * @since 4.0.0
     */
    ENTITY_DAMAGE,

    /**
     * Represents {@code EntityDeathEvent}.
     *
     * @see org.bukkit.event.entity.EntityDeathEvent
     * @since 4.0.0
     */
    ENTITY_DEATH,

    /**
     * Represents {@code EntityRegainHealthEvent}.
     *
     * @see org.bukkit.event.entity.EntityRegainHealthEvent
     * @since 4.0.0
     */
    ENTITY_REGAIN_HEALTH,

    /**
     * Represents {@code EntitySpawnEvent}.
     *
     * @see org.bukkit.event.entity.EntitySpawnEvent
     * @since 4.0.0
     */
    ENTITY_SPAWN,

    /**
     * Represents {@code EntityTameEvent}.
     *
     * @see org.bukkit.event.entity.EntityTameEvent
     * @since 4.0.0
     */
    ENTITY_TAME,

    /**
     * Represents {@code EntityTransformEvent}.
     *
     * @see org.bukkit.event.entity.EntityTransformEvent
     * @since 4.0.0
     */
    ENTITY_TRANSFORM,

    /**
     * Represents {@code PlayerJoinEvent}.
     *
     * @see org.bukkit.event.player.PlayerJoinEvent
     * @since 4.0.0
     */
    PLAYER_JOIN,

    /**
     * Represents {@code PlayerPortalEvent}.
     *
     * @see org.bukkit.event.player.PlayerPortalEvent
     * @since 4.0.0
     */
    PLAYER_PORTAL
}
