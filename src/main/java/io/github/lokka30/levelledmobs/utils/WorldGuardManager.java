package io.github.lokka30.levelledmobs.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.github.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class WorldGuardManager {

    private final LevelledMobs instance;

    /* Flags */
    public static StringFlag
            customMinLevelFlag, // This flag forces mobs to not be levelled lower than the value stated in the flag. -1 = no minimum from WorldGuard.
            customMaxLevelFlag; // This flag forces mobs to not be levelled higher than the value stated in the flag. -1 = no maximum from WorldGuard.
    public static StateFlag
            useCustomLevelsFlag, // This flag dictates if the custom min and max flags will be used or not. If false, then the min and max flags will have no effect.
            allowLevelledMobsFlag; // This flag dictates if mobs that spawn in the WorldGuard region will be levelled or not.

    public WorldGuardManager(final LevelledMobs instance) {
        this.instance = instance;
        registerFlags();
    }

    public void registerFlags() {
        FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();

        try {
            StateFlag useCustomLevelsFlag, allowLevelledMobsFlag;
            StringFlag customMinLevelFlag, customMaxLevelFlag;

            allowLevelledMobsFlag = new StateFlag("LM_AllowLevelledMobs", false);
            useCustomLevelsFlag = new StateFlag("LM_UseCustomLevels", false);
            customMinLevelFlag = new StringFlag("LM_CustomMinLevel", "-1");
            customMaxLevelFlag = new StringFlag("LM_CustomMaxLevel", "-1");

            flagRegistry.register(allowLevelledMobsFlag);
            flagRegistry.register(useCustomLevelsFlag);
            flagRegistry.register(customMinLevelFlag);
            flagRegistry.register(customMaxLevelFlag);

            WorldGuardManager.allowLevelledMobsFlag = allowLevelledMobsFlag;
            WorldGuardManager.useCustomLevelsFlag = useCustomLevelsFlag;
            WorldGuardManager.customMinLevelFlag = customMinLevelFlag;
            WorldGuardManager.customMaxLevelFlag = customMaxLevelFlag;

        } catch (FlagConflictException e) {

            Flag<?> allowLevelledMobs = flagRegistry.get("LM_AllowLevelledMobs");
            Flag<?> useCustomLevels = flagRegistry.get("LM_UseCustomLevels");
            Flag<?> customMinLevel = flagRegistry.get("LM_CustomMinLevel");
            Flag<?> customMaxLevel = flagRegistry.get("LM_CustomMaxLevel");

            if (allowLevelledMobs instanceof StateFlag) {
                WorldGuardManager.allowLevelledMobsFlag = (StateFlag) allowLevelledMobs;
            }

            if (customMinLevel instanceof StringFlag) {
                WorldGuardManager.customMinLevelFlag = (StringFlag) customMinLevel;
            }

            if (customMaxLevel instanceof StringFlag) {
                WorldGuardManager.customMaxLevelFlag = (StringFlag) customMaxLevel;
            }

            if (useCustomLevels instanceof StateFlag) {
                WorldGuardManager.useCustomLevelsFlag = (StateFlag) useCustomLevels;
            }
        }
    }

    //Get all regions at an Entities' location.
    public ApplicableRegionSet getRegionSet(LivingEntity livingEntity) {
        Location location = livingEntity.getLocation();

        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(livingEntity.getWorld()));

        assert regionManager != null;

        BlockVector3 blockVector = BlockVector3.at(location.getX(), location.getY(), location.getZ());

        return regionManager.getApplicableRegions(blockVector);
    }

    //Sorts a RegionSet by priority, lowest to highest.
    public ProtectedRegion[] sortRegionsByPriority(ApplicableRegionSet regionSet) {
        ProtectedRegion[] protectedRegions = new ProtectedRegion[0];
        List<ProtectedRegion> protectedRegionList = new ArrayList<>();

        if (regionSet.size() == 0) {
            return protectedRegions;
        } else if (regionSet.size() == 1) {
            protectedRegions = new ProtectedRegion[1];
            return regionSet.getRegions().toArray(protectedRegions);
        }

        for (ProtectedRegion region : regionSet) {
            protectedRegionList.add(region);
        }

        protectedRegionList.sort(Comparator.comparingInt(ProtectedRegion::getPriority));

        return protectedRegionList.toArray(protectedRegions);
    }

    //Check if region is applicable for region levelling.
    public boolean checkRegionFlags(LivingEntity ent) {
        boolean minBool = false;
        boolean maxBool = false;

        if (instance.hasWorldGuardInstalled) {
            return false;
        }

        //Sorted region array, highest priority comes last.
        ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(ent));

        //Check region flags on integrity.
        for (ProtectedRegion region : regions) {
            if (region.getFlag(WorldGuardManager.useCustomLevelsFlag) == StateFlag.State.DENY) {
                return false;
            }

            if (Utils.isInteger(region.getFlag(WorldGuardManager.customMinLevelFlag))) {
                minBool = Integer.parseInt(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMinLevelFlag))) > -1;
            }

            if (Utils.isInteger(region.getFlag(WorldGuardManager.customMaxLevelFlag))) {
                maxBool = Integer.parseInt(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMaxLevelFlag))) > Integer.parseInt(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMinLevelFlag)));
            }
        }

        return minBool && maxBool;
    }


    //Generate level based on WorldGuard region flags.
    public int[] getRegionLevel(LivingEntity livingEntity, int minLevel, int maxLevel) {
        ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(livingEntity));

        for (ProtectedRegion region : regions) {
            if (Utils.isInteger(region.getFlag(WorldGuardManager.customMinLevelFlag))) {
                minLevel = Math.max(Integer.parseInt(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMinLevelFlag))), 0);
            }

            if (Utils.isInteger(region.getFlag(WorldGuardManager.customMaxLevelFlag))) {
                maxLevel = Math.max(Integer.parseInt(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMaxLevelFlag))), 0);
            }
        }

        return new int[]{minLevel, maxLevel};
    }

    public boolean regionAllowsLevelling(LivingEntity livingEntity) {
        ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(livingEntity));

        for (ProtectedRegion region : regions) {
            return region.getFlag(WorldGuardManager.allowLevelledMobsFlag) == StateFlag.State.ALLOW;
        }

        return true;
    }
}
