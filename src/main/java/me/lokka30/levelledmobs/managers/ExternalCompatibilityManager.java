/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * This class handles compatibility with other plugins such as EliteMobs and Citizens
 *
 * @author lokka30, stumper66
 * @since 2.4.0
 */
public class ExternalCompatibilityManager {

    public enum ExternalCompatibility {
        NOT_APPLICABLE,

        // DangerousCaves plugin
        DANGEROUS_CAVES,

        // EcoBosses plugin
        ECO_BOSSES,

        // MythicMobs plugin
        MYTHIC_MOBS,

        // EliteMobs plugin
        ELITE_MOBS, ELITE_MOBS_NPCS, ELITE_MOBS_SUPER_MOBS,

        // InfernalMobs plugin
        INFERNAL_MOBS,

        // Citizens plugin
        CITIZENS,

        // Shopkeepers plugin
        SHOPKEEPERS,

        // PlaceholderAPI plugin
        PLACEHOLDER_API
    }

    /* Store any external namespaced keys with null values by default */
    public static NamespacedKey dangerousCavesMobTypeKey = null;
    public static NamespacedKey ecoBossesKey = null;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

    public static boolean hasPAPI_Installed(){ return (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null); }

    public static boolean hasNBTAPI_Installed(){
        return Bukkit.getPluginManager().getPlugin("NBTAPI") != null;
    }

    public static boolean hasMCMMO_CoreInsatlled(){
        return Bukkit.getPluginManager().getPlugin("MMOCore") != null;
    }

    @NotNull
    public static String getPAPI_Placeholder(final Player player, final String placeholder){
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
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
        if (lmEntity.getLivingEntity().hasMetadata("mythicmob")){
            final List<MetadataValue> metadatas = lmEntity.getLivingEntity().getMetadata("mythicmob");
            for (final MetadataValue md : metadatas){
                if (md.asBoolean()) return true;
            }
        }

        return false;
    }

    @NotNull
    public static String getMythicMobInternalName(@NotNull final LivingEntityWrapper lmEntity){
        if (!isMythicMob(lmEntity) || !lmEntity.getLivingEntity().hasMetadata("mobname")) return "";

        final List<MetadataValue> metadatas = lmEntity.getLivingEntity().getMetadata("mobname");
        for (final MetadataValue md : metadatas){
            if ("true".equalsIgnoreCase(md.asString()))
                return md.asString();
        }

        return "";
    }

    /**
     * NOTE: Works on DC2 but not DC1.
     *
     * @param lmEntity mob to check
     * @return if Dangerous Caves compatibility enabled and entity is from DangerousCaves
     * @author lokka30, stumper66, imDaniX (author of DC2 - provided part of this method)
     */
    public static boolean checkDangerousCaves(final LivingEntityWrapper lmEntity) {
        if (!ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.DANGEROUS_CAVES, lmEntity))
            return false;

        if (!Bukkit.getPluginManager().isPluginEnabled("DangerousCaves")) return false;

        if (dangerousCavesMobTypeKey == null) {
            //noinspection ConstantConditions
            dangerousCavesMobTypeKey = new NamespacedKey(Bukkit.getPluginManager().getPlugin("DangerousCaves"), "mob-type");
        }

        if (!lmEntity.getLivingEntity().getPersistentDataContainer().has(dangerousCavesMobTypeKey, PersistentDataType.STRING))
            return false;

        lmEntity.setMobExternalType(ExternalCompatibility.DANGEROUS_CAVES);
        return true;
    }

    /**
     * @param lmEntity mob to check
     * @return if the compat is enabled and if the mob belongs to EcoBosses
     * @author lokka30, Auxilor (author of EcoBosses - provided part of this method)
     */
    public static boolean checkEcoBosses(final LivingEntityWrapper lmEntity) {
        if (!ExternalCompatibilityManager.isExternalCompatibilityEnabled(ExternalCompatibility.ECO_BOSSES, lmEntity))
            return false;

        if (!Bukkit.getPluginManager().isPluginEnabled("EcoBosses")) return false;

        if (ecoBossesKey == null) {
            //noinspection ConstantConditions
            ecoBossesKey = new NamespacedKey(Bukkit.getPluginManager().getPlugin("ecobosses"), "boss");
        }

        if (!lmEntity.getLivingEntity().getPersistentDataContainer().has(ecoBossesKey, PersistentDataType.STRING))
            return false;

        lmEntity.setMobExternalType(ExternalCompatibility.ECO_BOSSES);
        return true;
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
                && !main.worldGuardIntegration.regionAllowsLevelling(location);
    }

    @Nullable
    public static List<String> getWGRegionsAtLocation(@NotNull final LivingEntityInterface lmInterface){
        if (!ExternalCompatibilityManager.hasWorldGuardInstalled()) return null;

        return WorldGuardIntegration.getWorldGuardRegionsForLocation(lmInterface);
    }
}
