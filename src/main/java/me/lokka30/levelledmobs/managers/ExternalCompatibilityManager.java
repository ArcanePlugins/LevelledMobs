package me.lokka30.levelledmobs.managers;

import io.lumine.xikage.mythicmobs.MythicMobs;
import me.lokka30.levelledmobs.LevelledMobs;
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

    public static boolean isMythicMob(final LivingEntity livingEntity) {
        //return MythicMobs.inst().getAPIHelper().isMythicMob(livingEntity);
        return MythicMobs.inst().getMobManager().isActiveMob(io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter.adapt(livingEntity));
    }

    /**
     * @param livingEntity mob to check
     * @return if Dangerous Caves compatibility enabled & entity is from DangerousCaves
     */
    public static boolean checkDangerousCaves(final LivingEntity livingEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.DANGEROUS_CAVES)
                && livingEntity.hasMetadata("DangerousCaves");
    }

    /**
     * @param livingEntity mob to check
     * @return if MythicMobs compatibility enabled & entity is from MythicMobs
     */
    public static boolean checkMythicMobs(final LivingEntity livingEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.MYTHIC_MOBS)
                && isMythicMob(livingEntity);
    }

    /**
     * @param livingEntity mob to check
     * @return if EliteMobs compatibility enabled & entity is from EliteMobs
     */
    public static boolean checkEliteMobs(final LivingEntity livingEntity) {
        return
                (ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS) && (livingEntity.hasMetadata("Elitemob")))
                        || (ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS_NPCS) && (livingEntity.hasMetadata("Elitemobs_NPC")))
                        || (ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS_SUPER_MOBS) && (livingEntity.hasMetadata("Supermob")));
    }

    /**
     * @param livingEntity mob to check
     * @return if InfernalMobs compatibility enabled & entity is from InfernalMobs
     */
    public static boolean checkInfernalMobs(final LivingEntity livingEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.INFERNAL_MOBS)
                && livingEntity.hasMetadata("infernalMetadata");
    }

    /**
     * @param livingEntity mob to check
     * @return if Citizens compatibility enabled & entity is from Citizens
     */
    public static boolean checkCitizens(final LivingEntity livingEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.CITIZENS)
                && livingEntity.hasMetadata("NPC");
    }

    /**
     * @param livingEntity mob to check
     * @return if Shopkeepers compatibility enabled & entity is from Shopkeepers
     */
    public static boolean checkShopkeepers(final LivingEntity livingEntity) {
        return ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.SHOPKEEPERS)
                && livingEntity.hasMetadata("shopkeeper");
    }

    /**
     * @param location location to check regions of
     * @return if WorldGuard is installed & region of entity blocks levelling (flag derived)
     */
    public static boolean checkWorldGuard(final Location location, final LevelledMobs main) {
        return ExternalCompatibilityManager.hasWorldGuardInstalled()
                && !main.worldGuardManager.regionAllowsLevelling(location);
    }
}
