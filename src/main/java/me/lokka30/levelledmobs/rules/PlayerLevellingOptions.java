package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class PlayerLevellingOptions implements Cloneable {
    public PlayerLevellingOptions(){
        this.levelTiers = new LinkedList<>();
    }

    @NotNull
    final public List<LevelTierMatching> levelTiers;
    public Boolean matchPlayerLevel;
    public Boolean enabled;
    public Boolean usePlayerMaxLevel;
    public Double playerLevelScale;
    public String variable;

    public void mergeRule(final PlayerLevellingOptions options) {
        if (options == null) return;

        this.levelTiers.addAll(options.levelTiers);
        if (options.matchPlayerLevel != null) this.matchPlayerLevel = options.matchPlayerLevel;
        if (options.usePlayerMaxLevel != null) this.usePlayerMaxLevel = options.usePlayerMaxLevel;
        if (options.playerLevelScale != null) this.playerLevelScale = options.playerLevelScale;
        if (variable != null) this.variable = options.variable;
    }

    public PlayerLevellingOptions cloneItem() {
        PlayerLevellingOptions copy = null;
        try {
            copy = (PlayerLevellingOptions) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return copy;
    }

    public String toString(){
        final StringBuilder sb = new StringBuilder();

        if (enabled != null && !enabled)
            sb.append("(disabled)");

        if (variable != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("var: ");
            sb.append(variable);
        }

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
