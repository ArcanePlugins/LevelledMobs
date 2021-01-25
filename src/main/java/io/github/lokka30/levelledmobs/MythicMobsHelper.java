package io.github.lokka30.levelledmobs;

import io.lumine.xikage.mythicmobs.MythicMobs;
import org.bukkit.entity.LivingEntity;

public class MythicMobsHelper {
    private final LevelledMobs instance;

    public MythicMobsHelper(final LevelledMobs instance) {
        this.instance = instance;
    }

    public boolean isMythicMob(final LivingEntity livingEntity){
        return MythicMobs.inst().getAPIHelper().isMythicMob(livingEntity);
    }
}
