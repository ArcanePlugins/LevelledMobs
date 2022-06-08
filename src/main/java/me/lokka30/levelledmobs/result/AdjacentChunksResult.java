package me.lokka30.levelledmobs.result;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

/**
 * Used in conjunction with the chunk kill count feature
 *
 * @author stumper66
 * @since 3.4.0
 */

public class AdjacentChunksResult {
    public AdjacentChunksResult(){
        this.chunkKeys = new LinkedList<>();
    }

    public int entities;
    public final @NotNull List<Long> chunkKeys;
}
