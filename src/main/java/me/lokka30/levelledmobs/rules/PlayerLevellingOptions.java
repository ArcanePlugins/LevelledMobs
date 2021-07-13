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
    public Boolean usePlayerMaxLevel;
    public Double playerLevelScale;

    public String toString(){
        final StringBuilder sb = new StringBuilder();

        if (enabled != null && !enabled)
            sb.append("(disabled)");

        if (matchPlayerLevel != null && matchPlayerLevel) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("match-plr-lvl");
        }

        if (usePlayerMaxLevel != null && usePlayerMaxLevel) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("use-plr-max-lvl");
        }

        if (playerLevelScale != null){
            if (sb.length() > 0) sb.append(", ");
            sb.append("scale: ");
            sb.append(playerLevelScale);
        }

        if (!levelTiers.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(levelTiers);
        }

        if (sb.length() == 0)
            return super.toString();
        else
            return sb.toString();
    }
}
