/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Holds any rules relating to player levelling
 *
 * @author stumper66
 * @since 3.1.0
 */
public class PlayerLevellingOptions implements Cloneable {

    PlayerLevellingOptions() {
        this.levelTiers = new LinkedList<>();
        this.decreaseLevel = true;
    }

    @NotNull
    final public List<LevelTierMatching> levelTiers;
    public Boolean matchPlayerLevel;
    public Boolean enabled;
    public Boolean usePlayerMaxLevel;
    public Boolean recheckPlayers;
    public Integer levelCap;
    public Long preserveEntityTime;
    public Double playerLevelScale;
    public String variable;
    public boolean decreaseLevel;
    public boolean doMerge;

    public void mergeRule(final PlayerLevellingOptions options) {
        if (options == null) {
            return;
        }

        this.levelTiers.addAll(options.levelTiers);
        if (options.matchPlayerLevel != null) {
            this.matchPlayerLevel = options.matchPlayerLevel;
        }
        if (options.usePlayerMaxLevel != null) {
            this.usePlayerMaxLevel = options.usePlayerMaxLevel;
        }
        if (options.playerLevelScale != null) {
            this.playerLevelScale = options.playerLevelScale;
        }
        if (options.levelCap != null) {
            this.levelCap = options.levelCap;
        }
        if (variable != null) {
            this.variable = options.variable;
        }
        if (options.enabled != null) {
            this.enabled = options.enabled;
        }
        if (options.recheckPlayers != null) {
            this.recheckPlayers = options.recheckPlayers;
        }
    }

    public PlayerLevellingOptions cloneItem() {
        PlayerLevellingOptions copy = null;
        try {
            copy = (PlayerLevellingOptions) super.clone();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();

        if (enabled != null && !enabled) {
            sb.append("(disabled)");
        }

        if (variable != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("var: ");
            sb.append(variable);
        }

        if (matchPlayerLevel != null && matchPlayerLevel) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("match-plr-lvl");
        }

        if (usePlayerMaxLevel != null && usePlayerMaxLevel) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("use-plr-max-lvl");
        }

        if (playerLevelScale != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("scale: ");
            sb.append(playerLevelScale);
        }

        if (levelCap != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("cap: ");
            sb.append(levelCap);
        }

        if (!levelTiers.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(levelTiers);
        }

        if (decreaseLevel) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("decrease-lvl");
        }

        if (recheckPlayers != null && recheckPlayers){
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("rechk-plr");
        }

        if (sb.length() == 0) {
            return super.toString();
        } else {
            return sb.toString();
        }
    }
}
