package me.lokka30.levelledmobs.rules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * When for rules to prevent them from being merged
 * when used as a preset
 *
 * @author stumper66
 * @since 3.2.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DoNotMerge {

}
