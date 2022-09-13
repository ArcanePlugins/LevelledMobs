/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A standardized list used for holding various rule lists
 *
 * @author stumper66
 * @since 3.0.0
 */
@SuppressWarnings("unchecked")
public class CachedModalList<T extends Comparable<T>> implements Cloneable {

    public CachedModalList() {
        this.allowedList = new TreeSet<>();
        this.allowedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.excludedList = new TreeSet<>();
        this.excludedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    public CachedModalList(@NotNull final Set<T> allowedList, @NotNull final Set<T> excludedList) {
        this.allowedList = allowedList;
        this.excludedList = excludedList;
        this.allowedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.excludedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    @NotNull public Set<T> allowedList;
    @NotNull public Set<String> allowedGroups;
    @NotNull public Set<T> excludedList;
    @NotNull public Set<String> excludedGroups;
    public boolean doMerge;
    public boolean allowAll;
    public boolean excludeAll;

    public boolean isEnabledInList(final T item, @Nullable final LivingEntityWrapper lmEntity) {
        if (this.allowAll) {
            return true;
        }
        if (this.excludeAll) {
            return false;
        }
        if (this.isEmpty()) {
            return true;
        }

        if (lmEntity != null) {
            for (final String group : lmEntity.getApplicableGroups()) {
                if (this.excludedGroups.contains(group)) {
                    return false;
                }
            }

            if (this.excludedList.contains(item)) {
                return false;
            }

            for (final String group : lmEntity.getApplicableGroups()) {
                if (this.allowedGroups.contains(group)) {
                    return true;
                }
            }
        }

        if (this.excludedList.contains(item)) {
            return false;
        }

        return this.isBlacklist() || this.allowedList.contains(item);
    }

    public boolean isEmpty() {
        return this.allowedList.isEmpty() &&
            this.allowedGroups.isEmpty() &&
            this.excludedList.isEmpty() &&
            this.excludedGroups.isEmpty();
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (!this.allowedList.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("lst: ");
            sb.append(this.allowedList);
        }
        if (this.allowAll) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("all allowed");
        }

        if (!this.allowedGroups.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("grps: ");
            sb.append(this.allowedGroups);
        }

        if (this.excludeAll) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("all excluded");
        }

        if (!this.excludedList.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("ex-lst: ");
            sb.append(this.excludedList);
        }
        if (!this.excludedGroups.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("ex-grps: ");
            sb.append(this.excludedGroups);
        }

        return sb.toString();
    }

    @SuppressWarnings("unused")
    public boolean isWhitelist() {
        return (!this.allowedList.isEmpty() || !this.allowedGroups.isEmpty()) &&
            (this.excludedList.isEmpty() && this.excludedGroups.isEmpty());
    }

    public boolean isBlacklist() {
        return (this.allowedList.isEmpty() && this.allowedGroups.isEmpty()) &&
            (!this.excludedList.isEmpty() || !this.excludedGroups.isEmpty());
    }

    public Object clone() {
        CachedModalList<T> copy = null;
        try {
            copy = (CachedModalList<T>) super.clone();
            copy.allowedList = (TreeSet<T>) ((TreeSet<T>) (this.allowedList)).clone();
            copy.allowedGroups = (TreeSet<String>) ((TreeSet<String>) (this.allowedGroups)).clone();
            copy.excludedList = (TreeSet<T>) ((TreeSet<T>) (this.excludedList)).clone();
            copy.excludedGroups = (TreeSet<String>) ((TreeSet<String>) (this.excludedGroups)).clone();
        } catch (final CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return copy;
    }

    public void mergeCachedModal(@NotNull final CachedModalList<?> cachedModalList) {
        this.allowedList.addAll((Collection<? extends T>) cachedModalList.allowedList);
        this.excludedList.addAll((Collection<? extends T>) cachedModalList.excludedList);

        this.allowedGroups.addAll(cachedModalList.allowedGroups);
        this.excludedGroups.addAll(cachedModalList.excludedGroups);

        if (cachedModalList.allowAll) {
            this.allowAll = true;
        }
        if (cachedModalList.excludeAll) {
            this.excludeAll = true;
        }
    }
}
