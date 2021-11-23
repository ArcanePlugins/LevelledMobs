package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Holds results information when applying custom drops
 *
 * @author stumper66
 * @since 3.1.2
 */
public class ApplicableRulesResult {
    ApplicableRulesResult(){
        this.allApplicableRules = new LinkedList<>();
        this.allApplicableRules_MadeChance = new LinkedList<>();
        this.allApplicableRules_DidNotMakeChance = new LinkedList<>();
    }

    @NotNull
    public final List<RuleInfo> allApplicableRules;
    @NotNull
    public final List<RuleInfo> allApplicableRules_MadeChance;
    @NotNull
    public final List<RuleInfo> allApplicableRules_DidNotMakeChance;
}
