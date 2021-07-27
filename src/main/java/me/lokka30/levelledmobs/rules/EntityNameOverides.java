package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EntityNameOverides {
    public EntityNameOverides(final @NotNull String mobNameOrLevelRange) {
        this.mobNameOrLevelRange = mobNameOrLevelRange;
    }

    @NotNull
    final public String mobNameOrLevelRange;
    public List<LevelTierMatching> entityNames;
    public boolean isLevelRange;

}
