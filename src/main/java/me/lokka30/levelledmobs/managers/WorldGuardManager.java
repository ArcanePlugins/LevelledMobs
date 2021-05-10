package me.lokka30.levelledmobs.managers;

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
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * TODO Describe...
 *
 * @author Eyrian2010
 */
public class WorldGuardManager {

    /* Flags */
    public static StringFlag
            customMinLevelFlag, // This flag forces mobs to not be levelled lower than the value stated in the flag. -1 = no minimum from WorldGuard.
            customMaxLevelFlag; // This flag forces mobs to not be levelled higher than the value stated in the flag. -1 = no maximum from WorldGuard.
    public static StateFlag
            useCustomLevelsFlag, // This flag dictates if the custom min and max flags will be used or not. If false, then the min and max flags will have no effect.
            allowLevelledMobsFlag; // This flag dictates if mobs that spawn in the WorldGuard region will be levelled or not.

    public WorldGuardManager() {
        registerFlags();
    }

    public void registerFlags() {
        final FlagRegistry flagRegistry = WorldGuard.getInstance().getFlagRegistry();

        try {
            StateFlag useCustomLevelsFlag, allowLevelledMobsFlag;
            StringFlag customMinLevelFlag, customMaxLevelFlag;

            allowLevelledMobsFlag = new StateFlag("LM-AllowLevelledMobs", true);
            useCustomLevelsFlag = new StateFlag("LM-UseCustomLevels", false);
            customMinLevelFlag = new StringFlag("LM-CustomMinLevel", "-1");
            customMaxLevelFlag = new StringFlag("LM-CustomMaxLevel", "-1");

            flagRegistry.register(allowLevelledMobsFlag);
            flagRegistry.register(useCustomLevelsFlag);
            flagRegistry.register(customMinLevelFlag);
            flagRegistry.register(customMaxLevelFlag);

            WorldGuardManager.allowLevelledMobsFlag = allowLevelledMobsFlag;
            WorldGuardManager.useCustomLevelsFlag = useCustomLevelsFlag;
            WorldGuardManager.customMinLevelFlag = customMinLevelFlag;
            WorldGuardManager.customMaxLevelFlag = customMaxLevelFlag;

        } catch (FlagConflictException e) {

            final Flag<?> allowLevelledMobs = flagRegistry.get("LM-AllowLevelledMobs");
            final Flag<?> useCustomLevels = flagRegistry.get("LM-UseCustomLevels");
            final Flag<?> customMinLevel = flagRegistry.get("LM-CustomMinLevel");
            final Flag<?> customMaxLevel = flagRegistry.get("LM-CustomMaxLevel");

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
    @NotNull
    public List<ProtectedRegion> getRegionSet(final LivingEntityInterface lmInterface) {
        final List<ProtectedRegion> results = new ArrayList<>();
        final Location location = lmInterface.getLocation();

        final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(lmInterface.getWorld()));

        if (regionManager == null) return results;

        final ProtectedRegion globalRegion = regionManager.getRegion("__global__");
        if (location.getWorld() == null) {
            if (globalRegion != null) results.add(globalRegion);
            return results;
        }

        final BlockVector3 blockVector = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        if (globalRegion != null) results.add(globalRegion);
        for (final ProtectedRegion region : regionManager.getApplicableRegions(blockVector))
            results.add(region);

        return results;
    }

    // Get all regions at a location
    @NotNull
    public List<ProtectedRegion> getRegionSet(final Location location) {
        final List<ProtectedRegion> results = new ArrayList<>();
        if (location.getWorld() == null) return results;

        final RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(location.getWorld()));

        if (regionManager == null) return results;

        final ProtectedRegion globalRegion = regionManager.getRegion("__global__");
        if (location.getWorld() == null) {
            if (globalRegion != null) results.add(globalRegion);
            return results;
        }

        final BlockVector3 blockVector = BlockVector3.at(location.getX(), location.getY(), location.getZ());
        final ApplicableRegionSet regionSet = regionManager.getApplicableRegions(blockVector);
        if (globalRegion != null) results.add(globalRegion);
        for (final ProtectedRegion region : regionSet)
            results.add(region);

        return results;
    }


    //Sorts a RegionSet by priority, lowest to highest.
    @Nullable
    public ProtectedRegion[] sortRegionsByPriority(final List<ProtectedRegion> regionSet) {
        if (regionSet == null) return null;

        ProtectedRegion[] protectedRegions = new ProtectedRegion[0];

        if (regionSet.size() == 0) {
            return protectedRegions;
        } else if (regionSet.size() == 1) {
            protectedRegions = new ProtectedRegion[1];
            return regionSet.toArray(protectedRegions);
        }

        final List<ProtectedRegion> protectedRegionList = new ArrayList<>(regionSet);
        protectedRegionList.sort(Comparator.comparingInt(ProtectedRegion::getPriority));

        return protectedRegionList.toArray(protectedRegions);
    }

    //Check if region is applicable for region levelling.
    public boolean checkRegionFlags(final LivingEntityInterface lmInterface) {
        boolean minBool = false;
        boolean maxBool = false;

        if (!ExternalCompatibilityManager.hasWorldGuardInstalled()) {
            return false;
        }

        //Sorted region array, highest priority comes last.
        final ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(lmInterface));

        if (regions == null) return true;

        //Check region flags on integrity.
        for (final ProtectedRegion region : regions) {
            if (region.getFlag(WorldGuardManager.useCustomLevelsFlag) == StateFlag.State.DENY) {
                return false;
            }

            if (Utils.isInteger(region.getFlag(WorldGuardManager.customMinLevelFlag))) {
                minBool = Integer.parseInt(Objects.requireNonNull(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMinLevelFlag)))) > -1;
            }

            if (Utils.isInteger(region.getFlag(WorldGuardManager.customMaxLevelFlag))) {
                maxBool = Integer.parseInt(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMaxLevelFlag))) > -1;
            }
        }

        return minBool || maxBool;
    }


    //Generate level based on WorldGuard region flags.
    @NotNull
    public int[] getRegionLevel(final LivingEntityInterface lmInterface) {
        final int[] levels = new int[]{ -1, -1};

        final ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(lmInterface));

        if (regions == null) return levels;

        for (final ProtectedRegion region : regions) {
            if (Utils.isInteger(region.getFlag(WorldGuardManager.customMinLevelFlag))) {
                levels[0] = Integer.parseInt(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMinLevelFlag)));
            }

            if (Utils.isInteger(region.getFlag(WorldGuardManager.customMaxLevelFlag))) {
                levels[1] = Integer.parseInt(Objects.requireNonNull(region.getFlag(WorldGuardManager.customMaxLevelFlag)));
            }
        }

        return levels;
    }

    public boolean regionAllowsLevelling(final LivingEntityInterface lmInterface) {
        final ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(lmInterface));

        if (regions == null) return true;

        for (final ProtectedRegion region : regions) {
            return region.getFlag(WorldGuardManager.allowLevelledMobsFlag) != StateFlag.State.DENY;
        }

        return true;
    }

    public boolean regionAllowsLevelling(final Location location) {
        final ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(location));

        if (regions == null) return true;

        StateFlag.State state = null;

        for (final ProtectedRegion region : regions) {
            StateFlag.State foundState = region.getFlag(WorldGuardManager.allowLevelledMobsFlag);
            if (foundState != null) state = foundState;
        }

        return state == null || state == StateFlag.State.ALLOW;
    }

    @NotNull
    public static List<String> GetWorldGuardRegionsForLocation(@NotNull final LivingEntityInterface lmInterface) {
        final List<String> wg_Regions = new LinkedList<>();

        if (lmInterface.getWorld() == null) return wg_Regions;
        final com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(lmInterface.getWorld());

        final BlockVector3 position = BlockVector3.at(
                lmInterface.getLocation().getBlockX(),
                lmInterface.getLocation().getBlockY(),
                lmInterface.getLocation().getBlockZ()
        );

        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionManager regions = container.get(world);
        if (regions == null) return wg_Regions;

        final ApplicableRegionSet set = regions.getApplicableRegions(position);
        for (final ProtectedRegion region : set)
            wg_Regions.add(region.getId());

        return wg_Regions;
    }
}
