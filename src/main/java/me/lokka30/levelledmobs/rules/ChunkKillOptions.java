package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.Nullable;

public class ChunkKillOptions implements MergableRule, Cloneable{
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

    public boolean getDisableVanillaDrops(){
        return this.disableVanillaDrops != null && this.disableVanillaDrops;
    }

    public boolean getDisableItemBoost(){
        return this.disableItemBoost != null && this.disableItemBoost;
    }

    public boolean getDisableXpDrops(){
        return this.disableXpDrops != null && this.disableXpDrops;
    }

    public void merge(final @Nullable MergableRule mergableRule){
        if (!(mergableRule instanceof final ChunkKillOptions chunkKillOptions)){
            return;
        }

        if (chunkKillOptions.isDefault()) return;

        if (chunkKillOptions.disableVanillaDrops != null)
            this.disableVanillaDrops = chunkKillOptions.disableVanillaDrops;
        if (chunkKillOptions.disableItemBoost != null)
            this.disableItemBoost = chunkKillOptions.disableItemBoost;
        if (chunkKillOptions.disableXpDrops != null)
            this.disableXpDrops = chunkKillOptions.disableXpDrops;
    }

    public boolean doMerge(){
        return true;
    }

    public ChunkKillOptions cloneItem() {
        ChunkKillOptions copy = null;
        try {
            copy = (ChunkKillOptions) super.clone();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return copy;
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
