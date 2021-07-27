package me.lokka30.levelledmobs.managers;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * This class handles compatibility with other plugins such as EliteMobs and Citizens
 *
 * @author lokka30, stumper66
 */
public class ExternalCompatibilityManager {

    public enum ExternalCompatibility {
        NOT_APPLICABLE,
        DANGEROUS_CAVES,
        MYTHIC_MOBS,
        ELITE_MOBS, ELITE_MOBS_NPCS, ELITE_MOBS_SUPER_MOBS,
        INFERNAL_MOBS,
        CITIZENS,
        SHOPKEEPERS,
        PLACEHOLDER_API
    }

    public static boolean isExternalCompatibilityEnabled(final ExternalCompatibility externalCompatibility, @NotNull final LivingEntityWrapper lmEntity) {
        if (lmEntity.getApplicableRules().isEmpty())
            return false;

        final Map<ExternalCompatibility, Boolean> list = lmEntity.getMainInstance().rulesManager.getRule_ExternalCompatibility(lmEntity);
        return isExternalCompatibilityEnabled(externalCompatibility, list);
    }

    public static boolean isExternalCompatibilityEnabled(final ExternalCompatibility externalCompatibility, final Map<ExternalCompatibility, Boolean> list) {
        // if not defined default to true
        return  (!list.containsKey(externalCompatibility) || list.get(externalCompatibility) != null && list.get(externalCompatibility));
    }

    public static boolean hasProtocolLibInstalled() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
    }

    public static boolean hasMythicMobsInstalled() {
        final Plugin p = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (p != null && p.getDescription().getVersion().startsWith("5"))
            return false;
        else
            return p != null;
    }

    public static boolean hasWorldGuardInstalled() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public static boolean isMythicMob(@NotNull final LivingEntityWrapper lmEntity) {

        return MythicMobs.inst().getMobManager().isActiveMob(io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter.adapt(lmEntity.getLivingEntity()));
    }

    @NotNull
    public static String getMythicMobInternalName(@NotNull final LivingEntityWrapper lmEntity){
        if (!isMythicMob(lmEntity)) return "";

        final ActiveMob mm = MythicMobs.inst().getMobManager().getMythicMobInstance(lmEntity.getLivingEntity());
        return mm.getType().getInternalName();
    }

    /**
     * @param lmEntity mob to check
     * @return if Dangerous Caves compatibility enabled and entity is from DangerousCaves
     */
    public static boolean checkDangerousCaves(final LivingEntityWrapper lmEntity) {
        final boolean isExternalType = ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.DANGEROUS_CAVES, lmEntity)
                && lmEntity.getLivingEntity().hasMetadata("DangerousCaves");

        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.DANGEROUS_CAVES);

        return isExternalType;
    }

    /**
     * @param lmEntity mob to check
     * @return if MythicMobs compatibility enabled and entity is from MythicMobs
     */
    public static boolean checkMythicMobs(final LivingEntityWrapper lmEntity) {
        if (!ExternalCompatibilityManager.hasMythicMobsInstalled()) return false;
        if (lmEntity.getMobExternalType().equals(ExternalCompatibility.MYTHIC_MOBS)) return true;

        final boolean isExternalType = isMythicMob(lmEntity);
        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.MYTHIC_MOBS);

        return isExternalType;
    }

    /**
     * @param lmEntity mob to check
     * @return if EliteMobs compatibility enabled and entity is from EliteMobs
     */
    public static boolean checkEliteMobs(final LivingEntityWrapper lmEntity) {
        final boolean isExternalType1 =
                lmEntity.getLivingEntity().hasMetadata("Elitemob");
        final boolean isExternalType2 =
                lmEntity.getLivingEntity().hasMetadata("Elitemobs_NPC");
        final boolean isExternalType3 =
                lmEntity.getLivingEntity().hasMetadata("Supermob");

        if (isExternalType1) lmEntity.setMobExternalType(ExternalCompatibility.ELITE_MOBS);
        else if (isExternalType2) lmEntity.setMobExternalType(ExternalCompatibility.ELITE_MOBS_NPCS);
        else if (isExternalType3) lmEntity.setMobExternalType(ExternalCompatibility.ELITE_MOBS_SUPER_MOBS);

        return (isExternalType1 || isExternalType2 || isExternalType3);
    }

    /**
     * @param lmEntity mob to check
     * @return if InfernalMobs compatibility enabled and entity is from InfernalMobs
     */
    public static boolean checkInfernalMobs(final LivingEntityWrapper lmEntity) {
        final boolean isExternalType = lmEntity.getLivingEntity().hasMetadata("infernalMetadata");

        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.INFERNAL_MOBS);

        return isExternalType;
    }

    /**
     * @param lmEntity mob to check
     * @return if Citizens compatibility enabled and entity is from Citizens
     */
    public static boolean checkCitizens(final LivingEntityWrapper lmEntity) {
        final boolean isExternalType = lmEntity.getLivingEntity().hasMetadata("NPC");

        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.CITIZENS);

        return isExternalType;
    }

    /**
     * @param lmEntity mob to check
     * @return if Shopkeepers compatibility enabled and entity is from Shopkeepers
     */
    public static boolean checkShopkeepers(final LivingEntityWrapper lmEntity) {
        final boolean isExternalType = lmEntity.getLivingEntity().hasMetadata("shopkeeper");

        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.SHOPKEEPERS);

        return isExternalType;
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

    @Nullable
    public static List<String> getWGRegionsAtLocation(@NotNull final LivingEntityInterface lmInterface){
        if (!ExternalCompatibilityManager.hasWorldGuardInstalled()) return null;

        return WorldGuardManager.getWorldGuardRegionsForLocation(lmInterface);
    }
}