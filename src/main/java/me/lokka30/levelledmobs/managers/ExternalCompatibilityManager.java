package me.lokka30.levelledmobs.managers;

import io.lumine.xikage.mythicmobs.MythicMobs;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;

/**
 * This class handles compatibility with other plugins such as EliteMobs and Citizens
 *
 * @author lokka30
 */
public class ExternalCompatibilityManager {

    private static final HashMap<ExternalCompatibility, Boolean> externalCompatibilityMap = new HashMap<>();

    public enum ExternalCompatibility {
        DANGEROUS_CAVES,
        MYTHIC_MOBS,
        ELITE_MOBS, ELITE_MOBS_NPCS, ELITE_MOBS_SUPER_MOBS,
        INFERNAL_MOBS,
        CITIZENS,
        SHOPKEEPERS
    }

    public static void load(final LevelledMobs main) {
        externalCompatibilityMap.clear();

        for (ExternalCompatibility externalCompatibility : ExternalCompatibility.values()) {
            externalCompatibilityMap.put(externalCompatibility, main.settingsCfg.getBoolean("external-compatibilities." + externalCompatibility.toString(), true));
        }
    }

    public static boolean isExternalCompatibilityEnabled(ExternalCompatibility externalCompatibility) {
        return ExternalCompatibilityManager.externalCompatibilityMap.get(externalCompatibility);
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

    public static boolean isMythicMob(final LivingEntityWrapper lmEntity) {
        //return MythicMobs.inst().getAPIHelper().isMythicMob(livingEntity);
        return MythicMobs.inst().getMobManager().isActiveMob(io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter.adapt(lmEntity.getLivingEntity()));
    }

    /**
     * @param lmEntity mob to check
     * @return if Dangerous Caves compatibility enabled and entity is from DangerousCaves
     */
    public static boolean checkDangerousCaves(final LivingEntityWrapper lmEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.DANGEROUS_CAVES)
                && lmEntity.getLivingEntity().hasMetadata("DangerousCaves");
    }

    /**
     * @param lmEntity mob to check
     * @return if MythicMobs compatibility enabled and entity is from MythicMobs
     */
    public static boolean checkMythicMobs(final LivingEntityWrapper lmEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.MYTHIC_MOBS)
                && isMythicMob(lmEntity);
    }

    /**
     * @param lmEntity mob to check
     * @return if EliteMobs compatibility enabled and entity is from EliteMobs
     */
    public static boolean checkEliteMobs(final LivingEntityWrapper lmEntity) {
        return
                (ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS) && (lmEntity.getLivingEntity().hasMetadata("Elitemob")))
                        || (ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS_NPCS) && (lmEntity.getLivingEntity().hasMetadata("Elitemobs_NPC")))
                        || (ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS_SUPER_MOBS) && (lmEntity.getLivingEntity().hasMetadata("Supermob")));
    }

    /**
     * @param lmEntity mob to check
     * @return if InfernalMobs compatibility enabled and entity is from InfernalMobs
     */
    public static boolean checkInfernalMobs(final LivingEntityWrapper lmEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.INFERNAL_MOBS)
                && lmEntity.getLivingEntity().hasMetadata("infernalMetadata");
    }

    /**
     * @param lmEntity mob to check
     * @return if Citizens compatibility enabled and entity is from Citizens
     */
    public static boolean checkCitizens(final LivingEntityWrapper lmEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.CITIZENS)
                && lmEntity.getLivingEntity().hasMetadata("NPC");
    }

    /**
     * @param lmEntity mob to check
     * @return if Shopkeepers compatibility enabled and entity is from Shopkeepers
     */
    public static boolean checkShopkeepers(final LivingEntityWrapper lmEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.SHOPKEEPERS)
                && lmEntity.getLivingEntity().hasMetadata("shopkeeper");
    }

    /**
     * @param location location to check regions of
     * @param main the main LevelledMobs instance
     * @return if WorldGuard is installed and region of entity blocks levelling (flag derived)
     */
    public static boolean checkWorldGuard(final Location location, final LevelledMobs main) {
        return ExternalCompatibilityManager.hasWorldGuardInstalled()
                && !main.worldGuardManager.regionAllowsLevelling(location);
    }
}
