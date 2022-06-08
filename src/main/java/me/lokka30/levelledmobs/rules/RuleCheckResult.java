package me.lokka30.levelledmobs.rules;

/**
 * Holds rule results in order to preserve the chance rule state
 *
 * @author stumper66
 * @since 3.1.2
 */
class RuleCheckResult {
    RuleCheckResult(final boolean useResult){
        this.useResult = useResult;
    }

    RuleCheckResult(final boolean useResult, final Boolean ruleMadeChance){
        this.useResult = useResult;
        this.ruleMadeChance = ruleMadeChance;
    }

    final boolean useResult;
    Boolean ruleMadeChance;
}
