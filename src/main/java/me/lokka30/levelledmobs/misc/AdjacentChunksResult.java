package me.lokka30.levelledmobs.misc;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class AdjacentChunksResult {
    public AdjacentChunksResult(){
        this.chunkKeys = new LinkedList<>();
    }

    public int entities;
    public final @NotNull List<Long> chunkKeys;
}
