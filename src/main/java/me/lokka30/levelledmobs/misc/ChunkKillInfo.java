package me.lokka30.levelledmobs.misc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Records entity deaths for use in the chunk kill max feature
 *
 * @author stumper66
 * @since 3.4.0
 */
public class ChunkKillInfo {

    public ChunkKillInfo() {
        this.entityCounts = new HashMap<>();
    }

    // timestamp of death, max cooldown time
    final public @NotNull Map<Instant, Integer> entityCounts;

    public @NotNull Set<Map.Entry<Instant, Integer>> getEntrySet() {
        return entityCounts.entrySet();
    }

    public boolean isEmpty() {
        return this.entityCounts.isEmpty();
    }

    public int getCount() {
        return this.entityCounts.size();
    }

    public String toString() {
        return entityCounts.toString();
    }
}
