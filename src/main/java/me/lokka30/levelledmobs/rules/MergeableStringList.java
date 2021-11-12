package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MergeableStringList {
    public MergeableStringList() {}

    public MergeableStringList(final @Nullable String item){
        if (item == null) return;

        this.items = new LinkedList<>();
        this.items.add(item);
    }

    public MergeableStringList(final @Nullable String item, final boolean doMerge){
        if (item == null) return;

        this.items = new LinkedList<>();
        this.items.add(item);
        this.doMerge = doMerge;
    }

    public List<String> items;
    public boolean doMerge;

    public void setItemFromString(final @Nullable String input){
        if (input == null) return;

        this.items = new ArrayList<>(1);
        this.items.add(input);
    }

    public void setItemFromList(final @Nullable Collection<String> input){
        if (input == null) return;

        this.items = new LinkedList<>();
        this.items.addAll(input);
    }

    public void mergeFromList(final @Nullable Collection<String> input){
        if (input == null) return;

        if (this.items == null) this.items = new LinkedList<>();
        this.items.addAll(input);
    }

    public boolean isEmpty(){
        return this.items.isEmpty();
    }

    public String toString(){
        if (items == null || items.isEmpty())
            return super.toString();

        final StringBuilder sb = new StringBuilder();
        if (items.size() == 1)
            sb.append(items.get(0));
        else
            sb.append(items);

        if (doMerge){
            if (sb.length() > 0)
                sb.append(" ");
            sb.append("(merge)");
        }

        return sb.toString();
    }
}
