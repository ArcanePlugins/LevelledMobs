/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import java.util.Map;
import java.util.TreeMap;

import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.jetbrains.annotations.NotNull;


/**
 * Holds any rule information regarding the health indicator
 *
 * @author stumper66
 * @since 3.1.0
 */
public class HealthIndicator implements Cloneable {

    public String indicator;
    public String indicatorHalf;
    public Double scale;
    public Integer maxIndicators;
    public Map<Integer, String> tiers;
    Boolean doMerge;

    public HealthIndicator cloneItem() {
        HealthIndicator copy = null;
        try {
            copy = (HealthIndicator) super.clone();
            if (this.tiers != null) {
                copy.tiers = new TreeMap<>();
                copy.tiers.putAll(this.tiers);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }

    public @NotNull String formatHealthIndicator(final @NotNull LivingEntityWrapper lmEntity) {
        final double mobHealth = lmEntity.getLivingEntity().getHealth();

        if (mobHealth == 0.0) {
            return "";
        }

        final int maxIndicators = this.maxIndicators != null ? this.maxIndicators : 10;
        final String indicatorStr = this.indicator != null ? this.indicator : "▐";
        final double scale = this.scale != null ? this.scale : 5.0;

        int indicatorsToUse = scale == 0 ?
                (int) Math.ceil(mobHealth) : (int) Math.ceil(mobHealth / scale);
        final int tiersToUse = (int) Math.ceil((double) indicatorsToUse / (double) maxIndicators);
        int toRecolor = 0;
        if (tiersToUse > 0) {
            toRecolor = indicatorsToUse % maxIndicators;
        }

        String primaryColor = "";
        String secondaryColor = "";

        if (this.tiers != null) {
            if (this.tiers.containsKey(tiersToUse)) {
                primaryColor = this.tiers.get(tiersToUse);
            } else if (this.tiers.containsKey(0)) {
                primaryColor = this.tiers.get(0);
            }

            if (tiersToUse > 0 && this.tiers.containsKey(tiersToUse - 1)) {
                secondaryColor = this.tiers.get(tiersToUse - 1);
            } else if (this.tiers.containsKey(0)) {
                secondaryColor = this.tiers.get(0);
            }
        }

        final StringBuilder result = new StringBuilder();
        result.append(primaryColor);

        if (tiersToUse < 2) {
            boolean useHalf = false;
            if (this.indicatorHalf != null && indicatorsToUse < maxIndicators) {
                useHalf = scale / 2.0 <= (indicatorsToUse * scale) - mobHealth;
                if (useHalf && indicatorsToUse > 0) {
                    indicatorsToUse--;
                }
            }

            result.append(indicatorStr.repeat(indicatorsToUse));
            if (useHalf) {
                result.append(this.indicatorHalf);
            }
        } else {
            if (toRecolor == 0) {
                result.append(indicatorStr.repeat(maxIndicators));
            } else {
                result.append(indicatorStr.repeat(toRecolor));
                result.append(secondaryColor);
                result.append(indicatorStr.repeat(maxIndicators - toRecolor));
            }
        }

        return result.toString();
    }

    void mergeIndicator(final @NotNull HealthIndicator mergingIndicator) {
        if (mergingIndicator.indicator != null) {
            this.indicator = mergingIndicator.indicator;
        }
        if (mergingIndicator.indicatorHalf != null) {
            this.indicatorHalf = mergingIndicator.indicatorHalf;
        }
        if (mergingIndicator.scale != null) {
            this.scale = mergingIndicator.scale;
        }
        if (mergingIndicator.maxIndicators != null) {
            this.maxIndicators = mergingIndicator.maxIndicators;
        }

        if (mergingIndicator.tiers == null) {
            return;
        }

        if (this.tiers == null) {
            this.tiers = new TreeMap<>();
        }
        this.tiers.putAll(mergingIndicator.tiers);
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (indicator != null) {
            sb.append("ind: ");
            sb.append(indicator);
        }

        if (indicatorHalf != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("ind.5: ");
            sb.append(indicatorHalf);
        }

        if (scale != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("scl: ");
            sb.append(scale);
        }

        if (maxIndicators != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("max: ");
            sb.append(maxIndicators);
        }

        if (tiers != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(tiers);
        }

        if (doMerge != null && doMerge) {
            if (sb.length() > 0) {
                sb.append("&r, ");
            }
            sb.append("merge: true");
        }

        if (sb.length() > 0) {
            return sb.toString();
        } else {
            return super.toString();
        }
    }
}
