package io.github.lokka30.levelledmobs.enums;

public class DebugInfo {
    public int minLevel;
    public int maxLevel;
    public RuleUsed rule;

    public DebugInfo(){
        this.rule = RuleUsed.None;
    }

    public enum RuleUsed{
        None,
        Entity,
        Slime_Split,
        World,
        World_Guard
    }
}
