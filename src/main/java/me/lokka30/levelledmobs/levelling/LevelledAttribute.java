/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

/**
 * All Levellable attributes are listed here.
 * Descriptions for 'VANILLA' levellable attributes
 * are copied from the Spigot javadocs here:
 * https://hub.spigotmc.org/javadocs/spigot/org/bukkit/attribute/Attribute.html
 * Vanilla attribute javadocs last updated: 23 August 2021 (MC 1.17.1)
 * Only VANILLA Levelled attributes are actually Attributes on mobs,
 * the rest are stored on the mob via NamespacedKeys and analysed in
 * various listeners.
 *
 * @author lokka30
 * @see LevelledNamespacedKeys
 * @see org.bukkit.attribute.Attribute
 * @since v4.0.0
 */
public enum LevelledAttribute {

    /**
     * Armor bonus of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_ARMOR,

    /**
     * Armor durability bonus of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_ARMOR_TOUGHNESS,

    /**
     * Attack damage of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_ATTACK_DAMAGE,

    /**
     * Attack knockback of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_ATTACK_KNOCKBACK,

    /**
     * Attack speed of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_ATTACK_SPEED,

    /**
     * Flying speed of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_FLYING_SPEED,

    /**
     * Range at which an Entity will follow others.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_FOLLOW_RANGE,

    /**
     * Resistance of an Entity to knockback.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_KNOCKBACK_RESISTANCE,

    /**
     * Luck bonus of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_LUCK,

    /**
     * Maximum health of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_MAX_HEALTH,

    /**
     * Movement speed of an Entity.
     *
     * @since v4.0.0
     */
    VANILLA_GENERIC_MOVEMENT_SPEED,

    /**
     * Strength with which a horse will jump.
     *
     * @since v4.0.0
     */
    VANILLA_HORSE_JUMP_STRENGTH,

    /**
     * Chance of a zombie to spawn reinforcements.
     *
     * @since v4.0.0
     */
    VANILLA_ZOMBIE_SPAWN_REINFORCEMENTS,

    /**
     * The ranged attack damage multiplier the Entity has
     * (e.g. Skeleton arrows)
     *
     * @since v4.0.0
     */
    CUSTOM_RANGED_ATTACK_DAMAGE,

    /**
     * The item drop multiplier the Entity has
     * (The Entity's item drops will be multiplied upon its death)
     *
     * @since v4.0.0
     */
    CUSTOM_ITEM_DROP,

    /**
     * The experience drop multiplier the Entity has
     * (The Entity's XP drops will be multiplied upon its death)
     *
     * @since v4.0.0
     */
    CUSTOM_EXPERIENCE_DROP,

    /**
     * The creeper blast damage value the Entity (Creeper) has
     *
     * @since v4.0.0
     */
    CUSTOM_CREEPER_BLAST_DAMAGE
}