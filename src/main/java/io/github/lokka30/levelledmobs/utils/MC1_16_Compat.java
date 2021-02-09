package io.github.lokka30.levelledmobs.utils;

import org.bukkit.entity.EntityType;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MC1_16_Compat {
    public MC1_16_Compat(){}

    public HashSet<EntityType> getHostileMobs(){
        return Stream.of(
                EntityType.HOGLIN
        ).collect(Collectors.toCollection(HashSet::new));
    }

    public HashSet<EntityType> getPassiveMobs(){
        return Stream.of(
                EntityType.STRIDER,
                EntityType.ZOMBIFIED_PIGLIN
        ).collect(Collectors.toCollection(HashSet::new));
    }
}
