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

    private LevelledMobs instance;

    public WorldGuardManager(final LevelledMobs instance) {
        this.instance = instance;
    }

    public void registerFlags() {
        FlagRegistry freg = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag allowflag;
            StringFlag minflag, maxflag;

            allowflag = new StateFlag("CustomLevelFlag", false);
            minflag = new StringFlag("MinLevelFlag", "-1");
            maxflag = new StringFlag("MaxLevelFlag", "-1");

            freg.register(allowflag);
            freg.register(minflag);
            freg.register(maxflag);

            LevelledMobs.allowlevelflag = allowflag;
            LevelledMobs.minlevelflag = minflag;
            LevelledMobs.maxlevelflag = maxflag;
        } catch (FlagConflictException e) {
            Flag<?> allow = freg.get("CustomLevelFlag");
            Flag<?> min = freg.get("MinLevelFlag");
            Flag<?> max = freg.get("MaxLevelFlag");

            if (min instanceof StringFlag) {
                LevelledMobs.minlevelflag = (StringFlag) min;
            }

            if (max instanceof StringFlag) {
                LevelledMobs.maxlevelflag = (StringFlag) max;
            }

            if (allow instanceof StateFlag) {
                LevelledMobs.allowlevelflag = (StateFlag) allow;
            }
        }
    }

    //Get all regions at an Entities' location.
    public ApplicableRegionSet getRegionSet(LivingEntity ent) {
        Location loc = ent.getLocation();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(ent.getWorld()));

        return Objects.requireNonNull(regions).getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
    }

    //Sorts a RegionSet by priority, lowest to highest.
    public ProtectedRegion[] sortRegionsByPriority(ApplicableRegionSet regset) {
        ProtectedRegion[] regionarray = new ProtectedRegion[0];
        List<ProtectedRegion> regionList = new ArrayList<>();

        if (regset.size() == 0)
            return regionarray;
        else if (regset.size() == 1) {
            regionarray = new ProtectedRegion[1];
            return regset.getRegions().toArray(regionarray);
        }

        for (ProtectedRegion r : regset) {
            regionList.add(r);
        }

        regionList.sort(Comparator.comparingInt(ProtectedRegion::getPriority));

        return regionList.toArray(regionarray);
    }

    //Check if region is applicable for region levelling.
    public boolean checkRegionFlags(LivingEntity ent) {
        boolean minbool = false;
        boolean maxbool = false;
        boolean customlevelbool = false;

        //Check if WorldGuard-plugin exists
        if (instance.hasWorldGuard)
            return false;

        //Sorted region array, highest priority comes last.
        ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(ent));

        //Check region flags on integrity.
        for (ProtectedRegion region : regions) {
            if (Utils.isInteger(region.getFlag(LevelledMobs.minlevelflag)))
                if (Integer.parseInt(Objects.requireNonNull(region.getFlag(LevelledMobs.minlevelflag))) > -1)
                    minbool = true;
            if (Utils.isInteger(region.getFlag(LevelledMobs.maxlevelflag)))
                if (Integer.parseInt(Objects.requireNonNull(region.getFlag(LevelledMobs.maxlevelflag))) > Integer.parseInt(Objects.requireNonNull(region.getFlag(LevelledMobs.minlevelflag))))
                    maxbool = true;
            if (region.getFlag(LevelledMobs.allowlevelflag) == StateFlag.State.ALLOW)
                customlevelbool = true;
            else if (region.getFlag(LevelledMobs.allowlevelflag) == StateFlag.State.DENY)
                customlevelbool = false;
        }

        return minbool && maxbool && customlevelbool;
    }


    //Generate level based on WorldGuard region flags.
    public int[] getRegionLevel(LivingEntity ent, int minlevel, int maxlevel) {
        //Sorted region array, highest priority comes last.
        ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(ent));

        //Set min. max. level to flag values
        for (ProtectedRegion region : regions) {
            if (Utils.isInteger(region.getFlag(LevelledMobs.minlevelflag)))
                minlevel = Math.max(Integer.parseInt(Objects.requireNonNull(region.getFlag(LevelledMobs.minlevelflag))), 0);
            if (Utils.isInteger(region.getFlag(LevelledMobs.maxlevelflag)))
                maxlevel = Math.max(Integer.parseInt(Objects.requireNonNull(region.getFlag(LevelledMobs.maxlevelflag))), 0);
        }

        return new int[]{minlevel, maxlevel};
    }
}
