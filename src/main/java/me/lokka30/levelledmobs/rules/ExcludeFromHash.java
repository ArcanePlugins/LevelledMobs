package me.lokka30.levelledmobs.rules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used on various rules to exclude them from
 * the mob hash feature
 *
 * @author stumper66
 * @since 3.12.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcludeFromHash {
}
