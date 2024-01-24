package io.github.arcaneplugins.levelledmobs.rules

/**
 * A duplicate of the built-in SpawnReason from spigot
 *
 * @author stumper66
 * @since 3.1.2
 */
enum class LevelledMobSpawnReason {
    NATURAL,
    JOCKEY,

    @Deprecated("")
    CHUNK_GEN,
    LM_SPAWNER,
    SPAWNER,
    EGG,
    SPAWNER_EGG,
    LIGHTNING,
    BUILD_SNOWMAN,
    BUILD_IRONGOLEM,
    BUILD_WITHER,
    VILLAGE_DEFENSE,
    VILLAGE_INVASION,
    BREEDING,
    SLIME_SPLIT,
    REINFORCEMENTS,
    METAMORPHOSIS,
    NETHER_PORTAL,
    DISPENSE_EGG,
    INFECTION,
    CURED,
    OCELOT_BABY,
    SILVERFISH_BLOCK,
    MOUNT,
    TRAP,
    ENDER_PEARL,
    SHOULDER_ENTITY,
    DROWNED,
    SHEARED,
    EXPLOSION,
    RAID,
    PATROL,
    BEEHIVE,
    PIGLIN_ZOMBIFIED,
    FROZEN,
    COMMAND,
    CUSTOM,
    SPELL,
    LM_SUMMON,
    DEFAULT,
    DUPLICATION
}