package me.lokka30.levelledmobs.rules;

public class RuleCheckResult {
    public RuleCheckResult(boolean useResult){
        this.useResult = useResult;
    }

    public RuleCheckResult(boolean useResult, Boolean ruleMadeChance){
        this.useResult = useResult;
        this.ruleMadeChance = ruleMadeChance;
    }

    public boolean useResult;
    public Boolean ruleMadeChance;
}
