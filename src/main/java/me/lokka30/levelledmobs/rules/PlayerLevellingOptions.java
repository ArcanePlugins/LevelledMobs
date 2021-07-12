package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class PlayerLevellingOptions {
    public PlayerLevellingOptions(){
        this.levelTiers = new LinkedList<>();
    }

    @NotNull
    final public List<LevelTierMatching> levelTiers;
    public Boolean matchPlayerLevel;
    public Boolean enabled;
    public Double playerLevelScale;
}
