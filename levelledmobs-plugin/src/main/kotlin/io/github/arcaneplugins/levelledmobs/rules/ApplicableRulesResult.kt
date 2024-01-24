package io.github.arcaneplugins.levelledmobs.rules

/**
 * Holds results information when applying custom drops
 *
 * @author stumper66
 * @since 3.1.2
 */
class ApplicableRulesResult {
    val allApplicableRules = mutableListOf<RuleInfo>()
    val allApplicableRules_MadeChance = mutableListOf<RuleInfo>()
    val allApplicableRules_DidNotMakeChance = mutableListOf<RuleInfo>()
}