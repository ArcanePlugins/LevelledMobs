/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.misc.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import simplepets.brainsynder.api.plugin.SimplePets;

import java.util.Collections;
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
        PLACEHOLDER_API,

        SIMPLE_PETS
    }

    /* Store any external namespaced keys with null values by default */
    private static NamespacedKey dangerousCavesMobTypeKey = null;
    private static NamespacedKey ecoBossesKey = null;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isExternalCompatibilityEnabled(final ExternalCompatibility externalCompatibility, final @NotNull Map<ExternalCompatibility, Boolean> list) {
        // if not defined default to true
        return  (!list.containsKey(externalCompatibility) || list.get(externalCompatibility) != null && list.get(externalCompatibility));
    }

    static boolean hasPAPI_Installed(){ return (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null); }

    public static boolean hasNBTAPI_Installed(){
        return Bukkit.getPluginManager().getPlugin("NBTAPI") != null;
    }

    @NotNull
    static String getPAPI_Placeholder(final Player player, final String placeholder){
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);

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

    private static boolean isMobOfSimplePets(@NotNull final LivingEntityWrapper lmEntity){
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("SimplePets");
        if (plugin == null) return false;

        // version 5 uses the API, older versions we'll check for metadata
        if (plugin.getDescription().getVersion().startsWith("4")) {
            for (final MetadataValue meta : lmEntity.getLivingEntity().getMetadata("pet")) {
                if (!meta.asString().isEmpty()) return true;
            }

            return false;
        }
        else
            return SimplePets.isPetEntity(lmEntity.getLivingEntity());
    }

    public static boolean isMythicMob(@NotNull final LivingEntityWrapper lmEntity) {
        final Plugin p = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (p == null) return false;

        if (!p.getDescription().getVersion().startsWith("4.12") && !p.getDescription().getVersion().startsWith("5.")) {
            final NamespacedKey mmKey = new NamespacedKey(p, "type");
            synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                return lmEntity.getPDC().has(mmKey, PersistentDataType.STRING);
            }
        }

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
        if (!isMythicMob(lmEntity)) return "";

        final Plugin p = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (p == null) return "";

        if (!p.getDescription().getVersion().startsWith("4.12") && !p.getDescription().getVersion().startsWith("5.")) {
            // MM version 5 must use this method for internal name detection
            final NamespacedKey mmKey = new NamespacedKey(p, "type");
            synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                if (lmEntity.getPDC().has(mmKey, PersistentDataType.STRING)) {
                    final String type = lmEntity.getPDC().get(mmKey, PersistentDataType.STRING);
                    return type == null ? "" : type;
                } else
                    return "";
            }
        }

        // MM version 4 detection below:

        if (!lmEntity.getLivingEntity().hasMetadata("mobname"))
            return "";

        final List<MetadataValue> metadatas = lmEntity.getLivingEntity().getMetadata("mobname");
        for (final MetadataValue md : metadatas){
            if ("true".equalsIgnoreCase(md.asString()))
                return md.asString();
        }

        return "";
    }

    static LevellableState checkAllExternalCompats(final LivingEntityWrapper lmEntity, final @NotNull LevelledMobs main){
        final Map<ExternalCompatibilityManager.ExternalCompatibility, Boolean> compatRules = main.rulesManager.getRule_ExternalCompatibility(lmEntity);

        LevellableState result = LevellableState.ALLOWED;

        if (isMobOfDangerousCaves(lmEntity) && !isExternalCompatibilityEnabled(ExternalCompatibility.DANGEROUS_CAVES, compatRules))
            result = LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_DANGEROUS_CAVES;

        if (isMobOfEcoBosses(lmEntity) && !isExternalCompatibilityEnabled(ExternalCompatibility.ECO_BOSSES, compatRules) &&
                result == LevellableState.ALLOWED)
            result = LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ECO_BOSSES;

        if (isMobOfMythicMobs(lmEntity) && !isExternalCompatibilityEnabled(ExternalCompatibility.MYTHIC_MOBS, compatRules) &&
                result == LevellableState.ALLOWED)
            result = LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_MYTHIC_MOBS;

        if (isMobOfEliteMobs(lmEntity) && !isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS, compatRules) &&
                result == LevellableState.ALLOWED)
            result = LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_MOBS;

        if (isMobOfInfernalMobs(lmEntity) && !isExternalCompatibilityEnabled(ExternalCompatibility.INFERNAL_MOBS, compatRules) &&
                result == LevellableState.ALLOWED)
            result = LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_INFERNAL_MOBS;

        if (isMobOfCitizens(lmEntity) && !isExternalCompatibilityEnabled(ExternalCompatibility.CITIZENS, compatRules) &&
                result == LevellableState.ALLOWED)
            result = LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_CITIZENS;

        if (isMobOfShopkeepers(lmEntity) && !isExternalCompatibilityEnabled(ExternalCompatibility.SHOPKEEPERS, compatRules) &&
                result == LevellableState.ALLOWED)
            result = LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SHOPKEEPERS;

        if (isMobOfSimplePets(lmEntity) && !isExternalCompatibilityEnabled(ExternalCompatibility.SIMPLE_PETS, compatRules) &&
                result == LevellableState.ALLOWED)
            result = LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SIMPLEPETS;

        return result;
    }

    /**
     * NOTE: Works on DC2 but not DC1.
     *
     * @param lmEntity mob to check
     * @return if Dangerous Caves compatibility enabled and entity is from DangerousCaves
     * @author lokka30, stumper66, imDaniX (author of DC2 - provided part of this method)
     */
    private static boolean isMobOfDangerousCaves(final LivingEntityWrapper lmEntity) {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("DangerousCaves");
        if (plugin == null) return false;

        if (dangerousCavesMobTypeKey == null)
            dangerousCavesMobTypeKey = new NamespacedKey(plugin, "mob-type");

        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            if (!lmEntity.getPDC().has(dangerousCavesMobTypeKey, PersistentDataType.STRING))
                return false;
        }

        lmEntity.setMobExternalType(ExternalCompatibility.DANGEROUS_CAVES);
        return true;
    }

    /**
     * @param lmEntity mob to check
     * @return if the compat is enabled and if the mob belongs to EcoBosses
     * @author lokka30, Auxilor (author of EcoBosses - provided part of this method)
     */
    private static boolean isMobOfEcoBosses(final LivingEntityWrapper lmEntity) {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("EcoBosses");
        if (plugin == null) return false;

        if (ecoBossesKey == null)
            ecoBossesKey = new NamespacedKey(plugin, "boss");

        synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
            if (!lmEntity.getPDC().has(ecoBossesKey, PersistentDataType.STRING))
                return false;
        }

        lmEntity.setMobExternalType(ExternalCompatibility.ECO_BOSSES);
        return true;
    }

    /**
     * @param lmEntity mob to check
     * @return if MythicMobs compatibility enabled and entity is from MythicMobs
     */
    private static boolean isMobOfMythicMobs(final LivingEntityWrapper lmEntity) {
        if (!ExternalCompatibilityManager.hasMythicMobsInstalled()) return false;
        if (lmEntity.isMobOfExternalType(ExternalCompatibility.MYTHIC_MOBS)) return true;

        final boolean isExternalType = isMythicMob(lmEntity);
        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.MYTHIC_MOBS);

        return isExternalType;
    }

    /**
     * @param lmEntity mob to check
     * @return if EliteMobs compatibility enabled and entity is from EliteMobs
     */
    private static boolean isMobOfEliteMobs(final LivingEntityWrapper lmEntity) {
        final Plugin p = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (p != null){
            final boolean isEliteMob;
            synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                isEliteMob = lmEntity.getPDC().has(new NamespacedKey(p, "EliteMobsCullable"), PersistentDataType.STRING);
            }

            if (isEliteMob){
                lmEntity.setMobExternalType(ExternalCompatibility.ELITE_MOBS);
                return true;
            }
        }

        return false;
    }

    /**
     * @param lmEntity mob to check
     * @return if InfernalMobs compatibility enabled and entity is from InfernalMobs
     */
    private static boolean isMobOfInfernalMobs(final @NotNull LivingEntityWrapper lmEntity) {
        final boolean isExternalType = lmEntity.getLivingEntity().hasMetadata("infernalMetadata");

        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.INFERNAL_MOBS);

        return isExternalType;
    }

    /**
     * @param lmEntity mob to check
     * @return if Citizens compatibility enabled and entity is from Citizens
     */
    private static boolean isMobOfCitizens(final @NotNull LivingEntityWrapper lmEntity) {
        final boolean isExternalType = isMobOfCitizens(lmEntity.getLivingEntity());

        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.CITIZENS);

        return isExternalType;
    }

    public static boolean isMobOfCitizens(final @NotNull LivingEntity livingEntity) {
        return livingEntity.hasMetadata("NPC");
    }

    /**
     * @param lmEntity mob to check
     * @return if Shopkeepers compatibility enabled and entity is from Shopkeepers
     */
    private static boolean isMobOfShopkeepers(final @NotNull LivingEntityWrapper lmEntity) {
        final boolean isExternalType = lmEntity.getLivingEntity().hasMetadata("shopkeeper");

        if (isExternalType) lmEntity.setMobExternalType(ExternalCompatibility.SHOPKEEPERS);

        return isExternalType;
    }

    @NotNull
    public static List<String> getWGRegionsAtLocation(@NotNull final LivingEntityInterface lmInterface){
        if (!ExternalCompatibilityManager.hasWorldGuardInstalled()) return Collections.emptyList();

        return WorldGuardIntegration.getWorldGuardRegionsForLocation(lmInterface);
    }

    @NotNull
    public static PlayerHomeCheckResult getPlayerHomeLocation(final @NotNull LevelledMobs main, final @NotNull Player player, final boolean allowBed){
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("essentials");
        if (plugin == null)
            return new PlayerHomeCheckResult("Unable to get player home, Essentials is not installed", null);

        if (allowBed && player.getWorld().getEnvironment() != World.Environment.NETHER) {
            final Location bedLocation = player.getBedSpawnLocation();
            if (bedLocation != null)
                return new PlayerHomeCheckResult(null, bedLocation, "bed");
        }

        final com.earth2me.essentials.Essentials essentials = (com.earth2me.essentials.Essentials) plugin;
        final com.earth2me.essentials.User user = essentials.getUser(player);
        if (user == null)
            return new PlayerHomeCheckResult("Unable to locate player information in essentials");

        if (user.getHomes() == null || user.getHomes().isEmpty()) {
            PlayerNetherOrWorldSpawnResult result = Utils.getNetherPortalOrWorldSpawn(main, player);
            final String whichSource = result.isNetherPortalLocation ? "nether portal" : "spawn";
            return new PlayerHomeCheckResult("Player has no homes set, using " + whichSource + " location", result.location);
        }

        return new PlayerHomeCheckResult(null, user.getHome(user.getHomes().get(0)), user.getHomes().get(0));
    }
}
