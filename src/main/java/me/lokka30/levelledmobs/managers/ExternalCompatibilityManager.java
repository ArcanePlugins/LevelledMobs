/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.misc.LevellableState;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.PlayerHomeCheckResult;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.misc.VersionInfo;
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
import simplepets.brainsynder.api.plugin.SimplePets;

import java.io.InvalidObjectException;
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
    private static Boolean useNewerEliteMobsKey = null;

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

        SIMPLE_PETS, //SimplePets plugin

        ELITE_BOSSES, //EliteBosses plugin

        BLOOD_NIGHT // Blood Night plugin
    }

    /* Store any external namespaced keys with null values by default */
    private static NamespacedKey dangerousCavesMobTypeKey = null;
    private static NamespacedKey ecoBossesKey = null;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isExternalCompatibilityEnabled(
        final ExternalCompatibility externalCompatibility,
        final @NotNull Map<ExternalCompatibility, Boolean> list
    ) {
        // if not defined default to true
        return  (!list.containsKey(externalCompatibility) || list.get(externalCompatibility) != null && list.get(externalCompatibility));
    }

    private static boolean checkIfPluginIsInstalledAndEnabled(final @NotNull String pluginName){
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    public static boolean hasPapiInstalled() {
        return checkIfPluginIsInstalledAndEnabled("PlaceholderAPI");
    }

    public static boolean hasNbtApiInstalled(){
        return checkIfPluginIsInstalledAndEnabled("NBTAPI");
    }

    @NotNull
    public static String getPapiPlaceholder(final Player player, final String placeholder){
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, placeholder);
    }

    public static boolean hasProtocolLibInstalled() {
        return checkIfPluginIsInstalledAndEnabled("ProtocolLib");
    }

    public static boolean hasMythicMobsInstalled() {
        return checkIfPluginIsInstalledAndEnabled("MythicMobs");
    }

    public static boolean hasWorldGuardInstalled() { return checkIfPluginIsInstalledAndEnabled("WorldGuard"); }

    private static boolean isMobOfSimplePets(@NotNull final LivingEntityWrapper lmEntity){
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("SimplePets");
        if (plugin == null || !plugin.isEnabled()) return false;

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

    private static boolean isMobOfEliteBosses(@NotNull final LivingEntityWrapper lmEntity){
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("EliteBosses");
        if (plugin == null || !plugin.isEnabled()) return false;

        for (final MetadataValue meta : lmEntity.getLivingEntity().getMetadata("EliteBosses")) {
            if (meta.asInt() > 0) return true;
        }

        return false;
    }

    private static boolean isMobOfBloodNight(@NotNull final LivingEntityWrapper lmEntity){
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("BloodNight");
        if (plugin == null || !plugin.isEnabled()) return false;

        return lmEntity.getPDC().has(new NamespacedKey(plugin, "mobtype"), PersistentDataType.STRING);
    }

    public static boolean isMythicMob(@NotNull final LivingEntityWrapper lmEntity) {
        final Plugin p = Bukkit.getPluginManager().getPlugin("MythicMobs");
        if (p == null || !p.isEnabled()) return false;

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
        if (p == null || !p.isEnabled()) return "";

        final boolean useNamespaceKey =
                p.getDescription().getVersion().startsWith("5.") &&
                !p.getDescription().getVersion().startsWith("5.01") &&
                !p.getDescription().getVersion().startsWith("5.00");

        if (useNamespaceKey) {
            final NamespacedKey mmKey = new NamespacedKey(p, "type");
            synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                if (lmEntity.getPDC().has(mmKey, PersistentDataType.STRING)) {
                    final String type = lmEntity.getPDC().get(mmKey, PersistentDataType.STRING);
                    return type == null ? "" : type;
                } else
                    return "";
            }
        }

        // MM version 4, 5.00 and 5.01 detection below:

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

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.DANGEROUS_CAVES, compatRules)) {
            if(isMobOfDangerousCaves(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_DANGEROUS_CAVES;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.ECO_BOSSES, compatRules)) {
            if(isMobOfEcoBosses(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ECO_BOSSES;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.MYTHIC_MOBS, compatRules)) {
            if(isMobOfMythicMobs(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_MYTHIC_MOBS;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_MOBS, compatRules)) {
            if(isMobOfEliteMobs(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_MOBS;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.INFERNAL_MOBS, compatRules)) {
            if(isMobOfInfernalMobs(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_INFERNAL_MOBS;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.CITIZENS, compatRules)) {
            if(isMobOfCitizens(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_CITIZENS;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.SHOPKEEPERS, compatRules)) {
            if(isMobOfShopkeepers(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SHOPKEEPERS;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.SIMPLE_PETS, compatRules)) {
            if(isMobOfSimplePets(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_SIMPLEPETS;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.ELITE_BOSSES, compatRules)) {
            if(isMobOfEliteBosses(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_ELITE_BOSSES;
            }
        }

        if(!isExternalCompatibilityEnabled(ExternalCompatibility.BLOOD_NIGHT, compatRules)) {
            if(isMobOfBloodNight(lmEntity)) {
                return LevellableState.DENIED_CONFIGURATION_COMPATIBILITY_BLOOD_NIGHT;
            }
        }

        return LevellableState.ALLOWED;
    }

    /**
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
            // 7.3.12 and newer uses a different namespaced key
            if (useNewerEliteMobsKey == null){
                final int theDash = p.getDescription().getVersion().indexOf('-');
                final String version = theDash > 3 ?
                        p.getDescription().getVersion().substring(0, theDash) : p.getDescription().getVersion();
                try {
                    VersionInfo pluginVer = new VersionInfo(version);
                    VersionInfo cutoverVersion = new VersionInfo("7.3.12");
                    useNewerEliteMobsKey = pluginVer.compareTo(cutoverVersion) >= 0;
                }
                catch (InvalidObjectException e){
                    Utils.logger.warning("Got error comparing EliteMob versions: " + e.getMessage());
                    // default to newer version on error
                    useNewerEliteMobsKey = true;
                }
            }

            final String checkKey = useNewerEliteMobsKey ?
                    "eliteentity" : "EliteMobsCullable";
            final boolean isEliteMob;
            synchronized (lmEntity.getLivingEntity().getPersistentDataContainer()) {
                isEliteMob = lmEntity.getPDC().has(new NamespacedKey(p, checkKey), PersistentDataType.STRING);
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
    public static PlayerHomeCheckResult getPlayerHomeLocation(final @NotNull Player player, final boolean allowBed){
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

        if (user.getHomes() == null || user.getHomes().isEmpty())
            return new PlayerHomeCheckResult(null, null);

        return new PlayerHomeCheckResult(null, user.getHome(user.getHomes().get(0)), user.getHomes().get(0));
    }
}
