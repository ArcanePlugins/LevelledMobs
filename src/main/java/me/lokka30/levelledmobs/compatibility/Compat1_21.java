package me.lokka30.levelledmobs.compatibility;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds lists of entity types that are only present in minecraft 1.21 and newer.  Must be a
 * separate class to maintain compatibility with older versions
 *
 * @author stumper66
 * @since 3.14.0
 */
public class Compat1_21 {
    public static HashSet<EntityType> getPassiveMobs() {
        return Stream.of(
                EntityType.valueOf("ARMADILLO")
        ).collect(Collectors.toCollection(HashSet::new));
    }

    public static @NotNull Set<String> all21Mobs() {
        final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.add("ARMADILLO");
        return names;
    }
}
