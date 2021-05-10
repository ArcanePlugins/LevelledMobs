package me.lokka30.levelledmobs;

import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface LivingEntityInterface {

    @NotNull
    EntityType getEntityType();
    Location getLocation();
    World getWorld();
    @NotNull
    String getTypeName();
    @NotNull
    List<RuleInfo> getApplicableRules();
    @NotNull
    LevelledMobs getMainInstance();
}
