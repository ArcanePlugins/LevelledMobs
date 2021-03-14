package me.lokka30.levelledmobs.compatibility;

import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author stumper66
 */
public class MC1_16_Compat {

    public static HashSet<EntityType> getHostileMobs() {
        return Stream.of(
                EntityType.HOGLIN
        ).collect(Collectors.toCollection(HashSet::new));
    }

    public static HashSet<EntityType> getPassiveMobs() {
        return Stream.of(
                EntityType.STRIDER,
                EntityType.ZOMBIFIED_PIGLIN
        ).collect(Collectors.toCollection(HashSet::new));
    }
}
