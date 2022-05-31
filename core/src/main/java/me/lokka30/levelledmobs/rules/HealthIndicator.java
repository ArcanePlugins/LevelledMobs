/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;


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
            if (this.tiers != null){
                copy.tiers = new TreeMap<>();
                copy.tiers.putAll(this.tiers);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
    }

    void mergeIndicator(final @NotNull HealthIndicator mergingIndicator){
        if (mergingIndicator.indicator != null) this.indicator = mergingIndicator.indicator;
        if (mergingIndicator.indicatorHalf != null) this.indicatorHalf = mergingIndicator.indicatorHalf;
        if (mergingIndicator.scale != null) this.scale = mergingIndicator.scale;
        if (mergingIndicator.maxIndicators != null) this.maxIndicators = mergingIndicator.maxIndicators;

        if (mergingIndicator.tiers == null) return;

        if (this.tiers == null) this.tiers = new TreeMap<>();
        this.tiers.putAll(mergingIndicator.tiers);
    }

    public String toString(){
        final StringBuilder sb = new StringBuilder();
        if (indicator != null){
            sb.append("ind: ");
            sb.append(indicator);
        }

        if (indicatorHalf != null){
            if (sb.length() > 0) sb.append(", ");
            sb.append("ind.5: ");
            sb.append(indicatorHalf);
        }

        if (scale != null){
            if (sb.length() > 0) sb.append(", ");
            sb.append("scl: ");
            sb.append(scale);
        }

        if (maxIndicators != null){
            if (sb.length() > 0) sb.append(", ");
            sb.append("max: ");
            sb.append(maxIndicators);
        }

        if (tiers != null){
            if (sb.length() > 0) sb.append(", ");
            sb.append(tiers);
        }

        if (doMerge != null && doMerge){
            if (sb.length() > 0) sb.append("&r, ");
            sb.append("merge: true");
        }

        if (sb.length() > 0)
            return sb.toString();
        else
            return super.toString();
    }
}
