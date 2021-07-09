package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class MMO_Core_Options {
    public MMO_Core_Options(){
        this.levelTiers = new LinkedList<>();
    }

    @NotNull
    final public List<LevelTierMatching> levelTiers;
    public Boolean matchPlayerLevel;
    public Boolean enabled;
    public Double playerLevelScale;
}
