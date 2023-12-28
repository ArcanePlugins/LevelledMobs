package me.lokka30.levelledmobs.rules;

import javax.annotation.Nullable;

/**
 * Provides a common interface for various rules
 * that can be merged together
 *
 * @author stumper66
 * @since 3.12.0
 */
public interface MergableRule {
    void merge(final @Nullable MergableRule mergableRule);

    boolean doMerge();

    Object cloneItem();
}
