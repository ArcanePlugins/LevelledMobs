package me.lokka30.levelledmobs.compatibility;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class Compat1_19 {

    public static HashSet<EntityType> getPassiveMobs() {
        return Stream.of(
            EntityType.valueOf("ALLAY"),
            EntityType.valueOf("TADPOLE"),
            EntityType.valueOf("FROG")
        ).collect(Collectors.toCollection(HashSet::new));
    }

    public static HashSet<EntityType> getHostileMobs() {
        return Stream.of(
            EntityType.valueOf("WARDEN")
        ).collect(Collectors.toCollection(HashSet::new));
    }

    public static HashSet<EntityType> getAquaticMobs() {
        return Stream.of(
            EntityType.valueOf("TADPOLE")
        ).collect(Collectors.toCollection(HashSet::new));
    }

    public static @NotNull Set<String> all19Mobs() {
        final Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(List.of("ALLAY", "TADPOLE", "FROG", "WARDEN"));
        return names;
    }
}
