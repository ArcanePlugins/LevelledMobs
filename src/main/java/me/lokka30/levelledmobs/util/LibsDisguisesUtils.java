package me.lokka30.levelledmobs.util;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.Disguise;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.wrappers.SchedulerWrapper;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class LibsDisguisesUtils {
    static {
        hasLibsDisguises = ExternalCompatibilityManager.hasLibsDisguisesInstalled();
    }

    private static final boolean hasLibsDisguises;

    public static boolean isMobUsingLibsDisguises(final @NotNull LivingEntityWrapper lmEntity){
        if (!hasLibsDisguises) return false;

        Disguise disguise;
        if (lmEntity.libsDisguiseCache != null){
            disguise = (Disguise) lmEntity.libsDisguiseCache;
        }
        else{
            disguise = DisguiseAPI.getDisguise(lmEntity.getLivingEntity());
            lmEntity.libsDisguiseCache = disguise;
        }

        return disguise != null && disguise.isDisguiseInUse();
    }

    public static void updateLibsDisguiseNametag(final @NotNull LivingEntityWrapper lmEntity, final String nametag){
        if (!isMobUsingLibsDisguises(lmEntity)) return;

        final Disguise disguise = (Disguise) lmEntity.libsDisguiseCache;
        final LivingEntity le = lmEntity.getLivingEntity();

        SchedulerWrapper wrapper = new SchedulerWrapper(le, () ->
                disguise.getWatcher().setCustomName(nametag));

        wrapper.run();
    }
}
