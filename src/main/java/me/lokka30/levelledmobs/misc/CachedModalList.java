package me.lokka30.levelledmobs.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TreeSet;
import java.util.Set;
import java.util.Collection;

public class CachedModalList<T extends Comparable<T>> implements Cloneable {
    public CachedModalList(){
        this.allowedList = new TreeSet<>();
        this.allowedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.excludedList = new TreeSet<>();
        this.excludedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    public CachedModalList(@NotNull final Set<T> allowedList, @NotNull final Set<T> excludedList){
        this.allowedList = allowedList;
        this.excludedList = excludedList;
        this.allowedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.excludedGroups = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    @NotNull
    public Set<T> allowedList;
    @NotNull
    public Set<String> allowedGroups;
    @NotNull
    public Set<T> excludedList;
    @NotNull
    public Set<String> excludedGroups;
    public boolean doMerge;

    public boolean isEnabledInList(final T item, @Nullable final LivingEntityWrapper lmEntity) {
        if (this.isEmpty()) return true;

        if (lmEntity != null) {
            for (final String group : lmEntity.getApplicableGroups()) {
                if (this.excludedGroups.contains(group)) return false;
            }
            for (final String group : lmEntity.getApplicableGroups()) {
                if (this.allowedGroups.contains(group)) return true;
            }
        }

        if (this.excludedList.contains(item))
            return false;

        else return this.allowedList.contains(item);
    }

    public boolean isEmpty(){
        return this.allowedList.isEmpty() &&
                this.allowedGroups.isEmpty() &&
                this.excludedList.isEmpty() &&
                this.excludedGroups.isEmpty();
    }

    public String toString(){
        final StringBuilder sb = new StringBuilder();
        if (!this.allowedList.isEmpty()){
            if (sb.length() > 0) sb.append(", ");
            sb.append(this.allowedList);
        }
        if (!this.allowedGroups.isEmpty()){
            if (sb.length() > 0) sb.append(", ");
            sb.append(this.allowedGroups);
        }
        if (!this.excludedList.isEmpty()){
            if (sb.length() > 0) sb.append(", ");
            sb.append(this.excludedList);
        }
        if (!this.excludedGroups.isEmpty()){
            if (sb.length() > 0) sb.append(", ");
            sb.append(this.excludedGroups);
        }

        return sb.toString();
    }

    public boolean isWhitelist(){
        return (!this.allowedList.isEmpty() || !this.allowedGroups.isEmpty()) &&
                (this.excludedList.isEmpty() && this.excludedGroups.isEmpty());
    }

    public boolean isBlacklist(){
        return (this.allowedList.isEmpty() && this.allowedGroups.isEmpty()) &&
                (!this.excludedList.isEmpty() || !this.excludedGroups.isEmpty());
    }

    public Object clone(){
        CachedModalList<T> copy = null;
        try {
            copy = (CachedModalList<T>) super.clone();
            copy.allowedList = (TreeSet<T>) ((TreeSet<T>) (this.allowedList)).clone();
            copy.allowedGroups = (TreeSet<String>) ((TreeSet<String>) (this.allowedGroups)).clone();
            copy.excludedList = (TreeSet<T>) ((TreeSet<T>) (this.excludedList)).clone();
            copy.excludedGroups = (TreeSet<String>) ((TreeSet<String>) (this.excludedGroups)).clone();
        }
        catch (CloneNotSupportedException ignored) {}

        return copy;
    }

    public void mergeCachedModal(@NotNull CachedModalList<?> cachedModalList){
        this.allowedList.addAll((Collection<? extends T>) cachedModalList.allowedList);
        this.excludedList.addAll((Collection<? extends T>) cachedModalList.excludedList);

        this.allowedGroups.addAll(cachedModalList.allowedGroups);
        this.excludedGroups.addAll(cachedModalList.excludedGroups);
    }
}
