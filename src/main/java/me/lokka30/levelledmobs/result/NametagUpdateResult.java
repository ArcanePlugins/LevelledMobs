package me.lokka30.levelledmobs.result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to hold the result of getting or updating nametags
 *
 * @author stumper66
 * @since 3.7.0
 */
public class NametagUpdateResult {
    public NametagUpdateResult(final @Nullable String nametag){
        this.nametag = nametag;
    }

    private final @Nullable String nametag;
    public @Nullable String overriddenName;
    public boolean hadDeathMessage;

    public @Nullable String getNametag(){
        return this.nametag;
    }

    public @NotNull String getNametagNonNull(){
        return this.nametag == null ?
                "" :
                this.nametag;
    }
}
