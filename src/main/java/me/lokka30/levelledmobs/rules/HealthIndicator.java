package me.lokka30.levelledmobs.rules;

import java.util.Map;

public class HealthIndicator {
    public String indicator;
    public String indicatorHalf;
    public Double scale;
    public Integer maxIndicators;
    public Map<Integer, String> tiers;
    public Boolean doMerge;

    public String toString(){
        final StringBuilder sb = new StringBuilder();
        if (indicator != null){
            sb.append("ind: ");
            sb.append(indicator);
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

        if (sb.length() > 0)
            return sb.toString();
        else
            return super.toString();
    }
}
