package me.lokka30.levelledmobs.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.Nullable;


@SuppressWarnings("unused")
public class MergeableStringList {

    MergeableStringList() {
    }

    MergeableStringList(final @Nullable String item) {
        if (item == null) {
            return;
        }

        this.items = new LinkedList<>();
        this.items.add(item);
    }

    MergeableStringList(final @Nullable String item, final boolean doMerge) {
        if (item == null) {
            return;
        }

        this.items = new LinkedList<>();
        this.items.add(item);
        this.doMerge = doMerge;
    }

    public List<String> items;
    boolean doMerge;

    public void setItemFromString(final @Nullable String input) {
        if (input == null) {
            return;
        }

        this.items = new ArrayList<>(1);
        this.items.add(input);
    }

    void setItemFromList(final @Nullable Collection<String> input) {
        if (input == null) {
            return;
        }

        this.items = new LinkedList<>();
        this.items.addAll(input);
    }

    public void mergeFromList(final @Nullable Collection<String> input) {
        if (input == null) {
            return;
        }

        if (this.items == null) {
            this.items = new LinkedList<>();
        }
        this.items.addAll(input);
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public boolean isNotEmpty(){
        return !this.isEmpty();
    }

    public String toString() {
        if (items == null || items.isEmpty()) {
            return super.toString();
        }

        final StringBuilder sb = new StringBuilder();
        if (items.size() == 1) {
            sb.append(items.get(0));
        } else {
            sb.append(items);
        }

        if (doMerge) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append("(merge)");
        }

        return sb.toString();
    }
}
