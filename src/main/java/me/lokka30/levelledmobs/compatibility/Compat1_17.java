/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.compatibility;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Holds lists of entity types that are only present in minecraft 1.17 and newer.  Must be a
 * separate class to maintain compatibility with older versions
 *
 * @author stumper66
 * @since 3.0.0
 */
public class Compat1_17 {

    public static HashSet<EntityType> getPassiveMobs() {
        return Stream.of(
            EntityType.AXOLOTL,
            EntityType.GLOW_SQUID,
            EntityType.GOAT
        ).collect(Collectors.toCollection(HashSet::new));
    }

    @Contract(pure = true)
    public static @NotNull @Unmodifiable Collection<EntityType> getForceBlockedEntityType() {
        return List.of(EntityType.GLOW_ITEM_FRAME, EntityType.MARKER);
    }
}
