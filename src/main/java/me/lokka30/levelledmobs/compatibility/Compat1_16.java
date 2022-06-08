/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds lists of entity types that are only present in
 * minecraft 1.16 and newer.  Must be a separate class
 * to maintain compatibility with older versions
 *
 * @author stumper66
 * @since 2.4.0
 */
public class Compat1_16 {

    public static HashSet<EntityType> getHostileMobs() {
        if (shouldIncludePiglinBrutes()) {
            return Stream.of(
                    EntityType.HOGLIN,
                    EntityType.PIGLIN,
                    EntityType.PIGLIN_BRUTE
            ).collect(Collectors.toCollection(HashSet::new));
        } else {
            return Stream.of(
                    EntityType.HOGLIN,
                    EntityType.PIGLIN
            ).collect(Collectors.toCollection(HashSet::new));
        }
    }

    public static HashSet<EntityType> getPassiveMobs() {
        return Stream.of(
                EntityType.STRIDER,
                EntityType.ZOMBIFIED_PIGLIN
        ).collect(Collectors.toCollection(HashSet::new));
    }

    private static boolean shouldIncludePiglinBrutes(){
        final String ver = Bukkit.getBukkitVersion();
        // 1.17.1-R0.1-SNAPSHOT
        final int dash = ver.indexOf('-');
        if (dash < 1) return false;

        final String verCorrected = ver.substring(0, dash);
        return (!verCorrected.equals("1.16") && !verCorrected.equals("1.16.1"));
    }
}
