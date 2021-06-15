package me.lokka30.levelledmobs.compatibility;

import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds lists of entity types that are only present in
 * minecraft 1.17 and newer.  Must be a separate class
 * to maintain compatibility with older versions
 *
 * @author stumper66
 */
public class MC1_17_Compat {
    public static boolean isServer1_17OrNewer() {
        boolean result = true;
        try{
            EntityType.valueOf("AXOLOTL");
        }
        catch (IllegalArgumentException ignored) {
            result = false;
        }

        return result;
    }

    public static HashSet<EntityType> getPassiveMobs() {
        return Stream.of(
                EntityType.AXOLOTL,
                EntityType.GLOW_SQUID,
                EntityType.GOAT
        ).collect(Collectors.toCollection(HashSet::new));
    }
}
