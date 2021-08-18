package me.lokka30.levelledmobs.rules;

/**
 * Holds rule results in order to preserve the chance rule state
 *
 * @author stumper66
 * @since 3.1.2
 */
public class RuleCheckResult {
    public RuleCheckResult(boolean useResult){
        this.useResult = useResult;
    }

    public RuleCheckResult(boolean useResult, Boolean ruleMadeChance){
        this.useResult = useResult;
        this.ruleMadeChance = ruleMadeChance;
    }

    public final boolean useResult;
    public Boolean ruleMadeChance;
}
