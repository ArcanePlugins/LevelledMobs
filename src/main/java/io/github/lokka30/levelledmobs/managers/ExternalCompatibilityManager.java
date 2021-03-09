package io.github.lokka30.levelledmobs.managers;

import io.github.lokka30.levelledmobs.LevelledMobs;
import io.lumine.xikage.mythicmobs.MythicMobs;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;

/**
 * This class handles compatibility with other plugins such as EliteMobs and Citizens
 *
 * @author lokka30
 */
public class ExternalCompatibilityManager {

    private final LevelledMobs main;

    public ExternalCompatibilityManager(final LevelledMobs main) {
        this.main = main;
    }

    private final HashMap<ExternalCompatibility, Boolean> externalCompatibilityMap = new HashMap<>();

    public enum ExternalCompatibility {
        MYTHIC_MOBS,
        ELITE_MOBS, ELITE_MOBS_NPCS, ELITE_MOBS_SUPER_MOBS,
        INFERNAL_MOBS,
        CITIZENS,
        SHOPKEEPERS
    }

    public void load() {
        externalCompatibilityMap.clear();

        for (ExternalCompatibility externalCompatibility : ExternalCompatibility.values()) {
            externalCompatibilityMap.put(externalCompatibility, main.settingsCfg.getBoolean("external-compatibilities." + externalCompatibility.toString(), true));
        }
    }

    public boolean isExternalCompatibilityEnabled(ExternalCompatibility externalCompatibility) {
        return externalCompatibilityMap.get(externalCompatibility);
    }

    public static boolean hasProtocolLibInstalled() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
    }

    public static boolean hasMythicMobsInstalled() {
        return Bukkit.getPluginManager().getPlugin("MythicMobs") != null;
    }

    public static boolean hasWorldGuardInstalled() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public static boolean isMythicMob(final LivingEntity livingEntity) {
        return MythicMobs.inst().getAPIHelper().isMythicMob(livingEntity);
    }
}
