package me.lokka30.levelledmobs.compatibility;

import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Compat1_20 {
    public static HashSet<EntityType> getPassiveMobs() {
        return Stream.of(
                EntityType.valueOf("CAMEL"),
                EntityType.valueOf("SNIFFER")
        ).collect(Collectors.toCollection(HashSet::new));
    }

    public static @NotNull Set<String> all20Mobs() {
        final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(List.of("CAMEL", "SNIFFER"));
        return names;
    }
}
