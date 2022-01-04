package me.lokka30.levelledmobs.rules.condition;

import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.jetbrains.annotations.NotNull;

/*
TODO:
    - Add javadoc comment.
 */
public interface RuleCondition {

    /*
    TODO:
        - Add javadoc comment.
     */
    boolean appliesTo(final @NotNull LevelledMob levelledMob);
}
