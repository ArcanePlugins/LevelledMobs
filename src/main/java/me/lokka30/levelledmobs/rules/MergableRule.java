package me.lokka30.levelledmobs.rules;

import javax.annotation.Nullable;

public interface MergableRule {
    void merge(final @Nullable MergableRule mergableRule);

    boolean doMerge();

    Object cloneItem();
}
