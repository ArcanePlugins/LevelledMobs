package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class ApplicableRulesResult {
    public ApplicableRulesResult(){
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

    public boolean hasChanceRules(){
        return (!this.allApplicableRules_MadeChance.isEmpty() || this.allApplicableRules_DidNotMakeChance.isEmpty());
    }
}
