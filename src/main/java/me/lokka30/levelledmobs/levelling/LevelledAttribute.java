/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

/**
 * @author lokka30
 * @see NamespacedKeys
 * @see org.bukkit.attribute.Attribute
 * @since v4.0.0
 * All Levellable attributes are listed here.
 * Descriptions for 'VANILLA' levellable attributes
 * are copied from the Spigot javadocs here:
 * https://hub.spigotmc.org/javadocs/spigot/org/bukkit/attribute/Attribute.html
 * Vanilla attribute javadocs last updated: 23 August 2021 (MC 1.17.1)
 * Only VANILLA Levelled attributes are actually Attributes on mobs,
 * the rest are stored on the mob via NamespacedKeys and analysed in
 * various listeners.
 */
public enum LevelledAttribute {

    /**
     * @since v4.0.0
     * Armor bonus of an Entity.
     */
    VANILLA_GENERIC_ARMOR,

    /**
     * @since v4.0.0
     * Armor durability bonus of an Entity.
     */
    VANILLA_GENERIC_ARMOR_TOUGHNESS,

    /**
     * @since v4.0.0
     * Attack damage of an Entity.
     */
    VANILLA_GENERIC_ATTACK_DAMAGE,

    /**
     * @since v4.0.0
     * Attack knockback of an Entity.
     */
    VANILLA_GENERIC_ATTACK_KNOCKBACK,

    /**
     * @since v4.0.0
     * Attack speed of an Entity.
     */
    VANILLA_GENERIC_ATTACK_SPEED,

    /**
     * @since v4.0.0
     * Flying speed of an Entity.
     */
    VANILLA_GENERIC_FLYING_SPEED,

    /**
     * @since v4.0.0
     * Range at which an Entity will follow others.
     */
    VANILLA_GENERIC_FOLLOW_RANGE,

    /**
     * @since v4.0.0
     * Resistance of an Entity to knockback.
     */
    VANILLA_GENERIC_KNOCKBACK_RESISTANCE,

    /**
     * @since v4.0.0
     * Luck bonus of an Entity.
     */
    VANILLA_GENERIC_LUCK,

    /**
     * @since v4.0.0
     * Maximum health of an Entity.
     */
    VANILLA_GENERIC_MAX_HEALTH,

    /**
     * @since v4.0.0
     * Movement speed of an Entity.
     */
    VANILLA_GENERIC_MOVEMENT_SPEED,

    /**
     * @since v4.0.0
     * Strength with which a horse will jump.
     */
    VANILLA_HORSE_JUMP_STRENGTH,

    /**
     * @since v4.0.0
     * Chance of a zombie to spawn reinforcements.
     */
    VANILLA_ZOMBIE_SPAWN_REINFORCEMENTS,

    /**
     * @since v4.0.0
     * The ranged attack damage multiplier the Entity has
     * (e.g. Skeleton arrows)
     */
    CUSTOM_RANGED_ATTACK_DAMAGE,

    /**
     * @since v4.0.0
     * The item drop multiplier the Entity has
     * (The Entity's item drops will be multiplied upon its death)
     */
    CUSTOM_ITEM_DROP,

    /**
     * @since v4.0.0
     * The experience drop multiplier the Entity has
     * (The Entity's XP drops will be multiplied upon its death)
     */
    CUSTOM_EXPERIENCE_DROP,

    /**
     * @since v4.0.0
     * The creeper blast damage value the Entity (Creeper) has
     */
    CUSTOM_CREEPER_BLAST_DAMAGE
}