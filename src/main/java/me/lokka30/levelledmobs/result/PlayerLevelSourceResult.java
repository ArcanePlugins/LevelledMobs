package me.lokka30.levelledmobs.result;

/**
 * Used for PlayerLevelling options
 *
 * @author stumper66
 * @since 3.3.0
 */
public class PlayerLevelSourceResult {

    public PlayerLevelSourceResult(final int numericResult) {
        this.numericResult = numericResult;
        this.isNumericResult = true;
    }

    public PlayerLevelSourceResult(final String stringResult) {
        this.stringResult = stringResult != null ? stringResult : "";
        this.isNumericResult = false;
    }

    public int numericResult;
    public Integer randomVarianceResult;
    public String stringResult;
    public final boolean isNumericResult;
    public String homeNameUsed;
}
