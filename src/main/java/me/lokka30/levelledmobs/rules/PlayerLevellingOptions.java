/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.util.LinkedList;
import java.util.List;

import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.MinAndMaxHolder;
import me.lokka30.levelledmobs.result.PlayerLevelSourceResult;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public void mergeRule(final @Nullable PlayerLevellingOptions options) {
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

    public boolean getMatchPlayerLevel(){
        return this.matchPlayerLevel != null && this.matchPlayerLevel;
    }

    public boolean getEnabled(){
        // enabled is true by default unless specifically disabled
        return this.enabled == null || this.enabled;
    }

    public boolean getUsePlayerMaxLevel(){
        return this.usePlayerMaxLevel != null && this.usePlayerMaxLevel;
    }

    public boolean getRecheckPlayers(){
        return this.recheckPlayers != null && this.recheckPlayers;
    }

    public @Nullable MinAndMaxHolder getPlayerLevels(final @NotNull LivingEntityWrapper lmEntity) {
        final PlayerLevellingOptions options = lmEntity.main.rulesManager.getRulePlayerLevellingOptions(
                lmEntity);

        if (options == null || !options.getEnabled()) {
            return null;
        }

        final Player player = lmEntity.getPlayerForLevelling();
        if (player == null) {
            return null;
        }

        int levelSource;
        final String variableToUse =
                Utils.isNullOrEmpty(options.variable) ? "%level%" : options.variable;
        final double scale = options.playerLevelScale != null ? options.playerLevelScale : 1.0;
        final PlayerLevelSourceResult playerLevelSourceResult = lmEntity.main.levelManager.getPlayerLevelSourceNumber(
                lmEntity.getPlayerForLevelling(), lmEntity, variableToUse);

        final double origLevelSource =
                playerLevelSourceResult.isNumericResult ? playerLevelSourceResult.numericResult : 1;

        applyValueToPdc(lmEntity, playerLevelSourceResult);
        levelSource = Math.max((int) Math.round(origLevelSource * scale), 1);

        final MinAndMaxHolder results = new MinAndMaxHolder(1, 1);
        String tierMatched = null;
        final String capDisplay = options.levelCap == null ? "" : "cap: " + options.levelCap + ", ";

        if (options.getUsePlayerMaxLevel()) {
            results.min = levelSource;
            results.max = results.min;
        } else if (options.getMatchPlayerLevel()) {
            results.max = levelSource;
        } else {
            boolean foundMatch = false;
            for (final LevelTierMatching tier : options.levelTiers) {
                boolean meetsMin = false;
                boolean meetsMax = false;
                boolean hasStringMatch = false;


                if (tier.sourceTierName != null) {
                    hasStringMatch = playerLevelSourceResult.stringResult.equalsIgnoreCase(
                            tier.sourceTierName);
                } else if (playerLevelSourceResult.isNumericResult) {
                    meetsMin = (tier.minLevel == null || levelSource >= tier.minLevel);
                    meetsMax = (tier.maxLevel == null || levelSource <= tier.maxLevel);
                }

                if (meetsMin && meetsMax || hasStringMatch) {
                    if (tier.valueRanges[0] > 0) {
                        results.min = tier.valueRanges[0];
                    }
                    if (tier.valueRanges[1] > 0) {
                        results.max = tier.valueRanges[1];
                    }
                    tierMatched = tier.toString();
                    foundMatch = true;
                    break;
                }
            }

            if (!foundMatch) {
                if (playerLevelSourceResult.isNumericResult) {
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity, () -> String.format(
                            "mob: %s, player: %s, lvl-src: %s, lvl-scale: %s, %sno tiers matched",
                            lmEntity.getNameIfBaby(), player.getName(), origLevelSource, levelSource,
                            capDisplay));
                } else {
                    DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity, () -> String.format(
                            "mob: %s, player: %s, lvl-src: '%s', %sno tiers matched",
                            lmEntity.getNameIfBaby(), player.getName(),
                            playerLevelSourceResult.stringResult, capDisplay));
                }
                if (options.levelCap != null){
                    results.max = options.levelCap;
                    results.useMin = false;
                    return results;
                }
                else{
                    return null;
                }
            }
        }


        String varianceDebug;
        if (playerLevelSourceResult.randomVarianceResult != null){
            results.max += playerLevelSourceResult.randomVarianceResult;
            // ensure the min value is at least 1
            results.min = Math.max(results.min, 1);
            // ensure the min value is not higher than the max value
            results.min = Math.min(results.min, results.max);

            varianceDebug = String.format(", var: %s", playerLevelSourceResult.randomVarianceResult);
        } else {
            varianceDebug = "";
        }

        if (options.levelCap != null) {
            results.ensureMinAndMax(1, options.levelCap);
        }

        final String homeName = playerLevelSourceResult.homeNameUsed != null ?
                String.format(" (%s)", playerLevelSourceResult.homeNameUsed) : "";

        if (tierMatched == null) {
            DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity, () ->  String.format(
                    "mob: %s, player: %s, lvl-src: %s%s%s, lvl-scale: %s, %sresult: %s",
                    lmEntity.getNameIfBaby(), player.getName(), origLevelSource, homeName,
                    varianceDebug, levelSource, capDisplay, results));
        } else {
            final String tierMatchedFinal = tierMatched;
            if (playerLevelSourceResult.isNumericResult) {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity, () -> String.format(
                        "mob: %s, player: %s, lvl-src: %s%s%s, lvl-scale: %s, tier: %s, %sresult: %s",
                        lmEntity.getNameIfBaby(), player.getName(), origLevelSource, homeName,
                        varianceDebug, levelSource, tierMatchedFinal, capDisplay, results));
            } else {
                DebugManager.log(DebugType.PLAYER_LEVELLING, lmEntity, () -> String.format(
                        "mob: %s, player: %s, lvl-src: '%s'%s, tier: %s, %sresult: %s",
                        lmEntity.getNameIfBaby(), player.getName(),
                        playerLevelSourceResult.stringResult, varianceDebug, tierMatchedFinal,
                        capDisplay, results));
            }
        }

        if (options.getRecheckPlayers()) {
            final String numberOrString = playerLevelSourceResult.isNumericResult ?
                    String.valueOf(playerLevelSourceResult.numericResult) : playerLevelSourceResult.stringResult;
            if (numberOrString != null)
                lmEntity.getPDC().set(lmEntity.main.namespacedKeys.playerLevellingSourceNumber, PersistentDataType.STRING, numberOrString);
        }
        lmEntity.playerLevellingAllowDecrease = options.decreaseLevel;

        return results;
    }

    private void applyValueToPdc(final @NotNull LivingEntityWrapper lmEntity,
                                 final @NotNull PlayerLevelSourceResult playerLevel){
        final String value = playerLevel.isNumericResult ?
                String.valueOf(playerLevel.numericResult) : playerLevel.stringResult;

        try {
            lmEntity.getPDC().set(
                    lmEntity.main.namespacedKeys.playerLevellingValue,
                    PersistentDataType.STRING,
                    value
            );
        }
        catch (Exception ignored){}
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();

        if (!getEnabled()) {
            sb.append("(disabled)");
        }

        if (variable != null) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("var: ");
            sb.append(variable);
        }

        if (getMatchPlayerLevel()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("match-plr-lvl");
        }

        if (getUsePlayerMaxLevel()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("use-plr-max-lvl");
        }

        if (playerLevelScale != null) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("scale: ");
            sb.append(playerLevelScale);
        }

        if (levelCap != null) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("cap: ");
            sb.append(levelCap);
        }

        if (!levelTiers.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(levelTiers);
        }

        if (decreaseLevel) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("decrease-lvl");
        }

        if (getRecheckPlayers()){
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append("rechk-plr");
        }

        if (sb.isEmpty()) {
            return super.toString();
        } else {
            return sb.toString();
        }
    }
}
