package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;

public class ChunkKillOptions {
    public Boolean disableVanillaDrops;
    public Boolean disableItemBoost;
    public Boolean disableXpDrops;

    public ChunkKillOptions(){
        disableItemBoost = true;
        disableXpDrops = true;
    }

    public boolean isDefault(){
        return (disableVanillaDrops == null && disableItemBoost == null && disableXpDrops == null);
    }

    public void merge(final @NotNull ChunkKillOptions chunkKillOptions){
        if (chunkKillOptions.isDefault()) return;

        if (chunkKillOptions.disableVanillaDrops != null)
            this.disableVanillaDrops = chunkKillOptions.disableVanillaDrops;
        if (chunkKillOptions.disableItemBoost != null)
            this.disableItemBoost = chunkKillOptions.disableItemBoost;
        if (chunkKillOptions.disableXpDrops != null)
            this.disableXpDrops = chunkKillOptions.disableXpDrops;
    }

    public String toString(){
        if (this.isDefault()) return "Default";

        final StringBuilder sb = new StringBuilder();
        if (disableVanillaDrops != null && disableVanillaDrops) {
            sb.append("disableVanillaDrops");
        }
        if (disableItemBoost != null && disableItemBoost) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("disableItemBoost");
        }
        if (disableXpDrops != null && disableXpDrops) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append("disableXpDrops");
        }

        return sb.toString();
    }
}
