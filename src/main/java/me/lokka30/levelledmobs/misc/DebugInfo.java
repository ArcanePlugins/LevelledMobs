package me.lokka30.levelledmobs.misc;

/**
 * @author stumper66
 */
public class DebugInfo {
    public int minLevel;
    public int maxLevel;
    public MobProcessReason rule;

    public DebugInfo(){
        this.rule = MobProcessReason.NONE;
    }
}
