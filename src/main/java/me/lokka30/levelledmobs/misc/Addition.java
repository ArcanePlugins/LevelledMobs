/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

/**
 * Holds the attributes that can have multipliers applied
 *
 * @author lokka30, stumper66
 * @see org.bukkit.attribute.Attribute
 * @since 2.6.0
 */
public enum Addition {
    // Prefix of ATTRIBUTE if it is a Minecraft vanilla attribute like GENERIC_MOVEMENT_SPEED
    ATTRIBUTE_MOVEMENT_SPEED,
    ATTRIBUTE_ATTACK_DAMAGE,
    ATTRIBUTE_MAX_HEALTH,

    // Prefix of CUSTOM if it is a custom value used in listeners
    CUSTOM_RANGED_ATTACK_DAMAGE,
    CUSTOM_ITEM_DROP,
    CUSTOM_XP_DROP,

    CREEPER_BLAST_DAMAGE
}
