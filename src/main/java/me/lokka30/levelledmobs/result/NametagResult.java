package me.lokka30.levelledmobs.result;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to hold the result of getting or updating nametags
 *
 * @author stumper66
 * @since 3.7.0
 */
public class NametagResult {
    public NametagResult(final @Nullable String nametag){
        this.nametag = nametag;
    }

    private @Nullable String nametag;
    public @Nullable String overriddenName;
    private boolean _hadCustomDeathMessage;
    private String _customDeathMessage;
    public LivingEntity killerMob;

    public @Nullable String getNametag(){
        return this.nametag;
    }

    public @NotNull String getNametagNonNull(){
        return this.nametag == null ?
                "" :
                this.nametag;
    }

    public void setNametag(final @Nullable String nametag){
        this.nametag = nametag;
    }

    public boolean isNullOrEmpty(){
        return this.nametag == null || this.nametag.isEmpty();
    }

    public void setDeathMessage(final @Nullable String customDeathMessage){
        this._customDeathMessage = customDeathMessage;
        this._hadCustomDeathMessage = customDeathMessage != null;
    }

    public boolean hadCustomDeathMessage(){
        return this._hadCustomDeathMessage;
    }

    public String getcustomDeathMessage(){
        return this._customDeathMessage;
    }
}
