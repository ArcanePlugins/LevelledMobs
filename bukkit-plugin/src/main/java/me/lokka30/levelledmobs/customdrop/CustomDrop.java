/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.customdrop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.EnumSet;
import java.util.Optional;

public record CustomDrop(
        // TODO unsure about having optionals here, need to re-think this out.
        Optional<Material>          material, // Material can only be optional for the 'default' section's drop object.
        Optional<Float>             chanceRequirement,
        Optional<Float>             overallChanceRequirement,
        Optional<Integer>           minLevelRequirement,
        Optional<Integer>           maxLevelRequirement,
        Optional<Integer>           amount,
        Optional<Float>             equippedChance,
        Optional<Integer>           customModelData,
        Optional<EnumSet<ItemFlag>> itemFlags,
        Optional<String>            nbtData,
        Optional<Boolean>           removeVanillaDrops,
        Optional<Boolean>           noMultiplier,
        Optional<Boolean>           onlyDropItemsIfEquipped,
        Optional<Integer>           groupId,
        Optional<Integer>           maxDropGroup,
        Optional<Integer>           priority
) {}
