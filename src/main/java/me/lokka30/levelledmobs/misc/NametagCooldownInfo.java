package me.lokka30.levelledmobs.misc;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Comparator;
// implements Comparator<NametagCooldownInfo> {
public class NametagCooldownInfo {
    public Instant timestamp;
    public WeakReference<LivingEntity> livingEntity;
    public int duration;

    public NametagCooldownInfo(){ }

    public NametagCooldownInfo(final LivingEntity livingEntity, final int duration){
        this.livingEntity = new WeakReference<>(livingEntity);
        this.timestamp = Instant.now();
        this.duration = duration;
    }

//    public int compare(final @NotNull NametagCooldownInfo a, final @NotNull NametagCooldownInfo b) {
//        return a.timestamp.compareTo(b.timestamp);
//    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof NametagCooldownInfo)) return false;

        final NametagCooldownInfo nametagCooldownInfo = (NametagCooldownInfo) o;
        return nametagCooldownInfo.livingEntity.get() != null && nametagCooldownInfo.livingEntity.get() == this.livingEntity.get();
    }

    public String toString(){
        return String.format("%s: %s, %s",
                livingEntity.get(), timestamp, duration);
    }
}
