/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.levelling;

/**
 * All utilized non-vanilla 'attribute-like' multipliables
 * are listed here.
 * Note that these are not actually 'attributes'
 * applied to mobs, these are instead stored as NBT
 * data on the mob and utilized whenever they are
 * required e.g. on the entity's death.
 *
 * @author lokka30
 * @see LevelledNamespacedKeys
 * @see org.bukkit.attribute.Attribute
 * @since v4.0.0
 */
public enum CustomMultiplier {

    /**
     * The ranged attack damage multiplier the Entity has.
     * (e.g. Skeleton arrows)
     *
     * @since v4.0.0
     */
    RANGED_ATTACK_DAMAGE,

    /**
     * The item drop multiplier the Entity has.
     * (The Entity's item drops will be multiplied upon its death)
     *
     * @since v4.0.0
     */
    ITEM_DROP,

    /**
     * The experience drop multiplier the Entity has.
     * (The Entity's XP drops will be multiplied upon its death)
     *
     * @since v4.0.0
     */
    EXPERIENCE_DROP,

    /**
     * The creeper blast damage value the Entity (Creeper) has.
     *
     * @since v4.0.0
     */
    CREEPER_BLAST_DAMAGE
}