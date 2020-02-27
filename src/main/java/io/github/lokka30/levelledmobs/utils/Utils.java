package io.github.lokka30.levelledmobs.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
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

import static io.github.lokka30.levelledmobs.LevelledMobs.getInstance;

public class Utils {
    public static String getRecommendedServerVersion() {
        return "1.15";
    }

    public static int getRecommendedSettingsVersion() {
        return 13;
    }

    //This is a method created by Jonik & Mustapha Hadid at StackOverflow.
    //It simply grabs 'value', being a double, and rounds it, leaving 'places' decimal places intact.
    //Created by Jonik & Mustapha Hadid @ stackoverflow
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    //Creates a weighted array where the values contain the sum of itself and all preceding values
    public static double[] createWeightedArray(double[] inputarray) {
        double[] outputarray = new double[inputarray.length];

        outputarray[0] = inputarray[0];
        for (int i = 1; i < inputarray.length; i++) {
            outputarray[i] = inputarray[i] + outputarray[i - 1];
        }

        return outputarray;
    }

    //Binomial distribution function
    public static double binomialDistribution(int n, int k, double p) {
        return ((double)factorial(n)) / ((double)(factorial(k)) * ((double)factorial(n - k))) * Math.pow(p, k) * Math.pow(1 - p, n - k);
    }

    //Factorial function
    public static long factorial(int num) {
        long result = 1;
        for (int i = num; i > 1; i--)
            result *= i;
        return result;
    }

    //Integer check
    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    //Integer check
    public static boolean isInteger(String s, int radix) {
        if(s == null || s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }

    //Get all regions at an Entities' location.
    public static ApplicableRegionSet getRegionSet(LivingEntity ent){
        Location loc = ent.getLocation();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(ent.getWorld()));

        return Objects.requireNonNull(regions).getApplicableRegions(BlockVector3.at(loc.getX(),loc.getY(),loc.getZ()));
    }

    //Sorts a RegionSet by priority, lowest to highest.
    public static ProtectedRegion[] sortRegionsByPriority(ApplicableRegionSet regset){
        ProtectedRegion[] regionarray = new ProtectedRegion[0];
        List<ProtectedRegion> regionList = new ArrayList<>();

        if(regset.size() == 0)
            return regionarray;
        else if (regset.size() == 1) {
            regionarray = new ProtectedRegion[1];
            return regset.getRegions().toArray(regionarray);
        }

        for(ProtectedRegion r : regset){
            regionList.add(r);
        }

        regionList.sort(Comparator.comparingInt(ProtectedRegion::getPriority));

        return regionList.toArray(regionarray);
    }

    //Check if region is applicable for region levelling.
    public static boolean checkRegionFlags(LivingEntity ent) {
        boolean minbool = false;
        boolean maxbool = false;
        boolean customlevelbool = false;

        //Check if WorldGuard-plugin exists
        if(!getInstance().worldguard)
            return false;

        //Sorted region array, highest priority comes last.
        ProtectedRegion[] regions = sortRegionsByPriority(getRegionSet(ent));

        //Check region flags on integrity.
        for (ProtectedRegion region : regions) {
            if (isInteger(region.getFlag(LevelledMobs.minlevelflag)))
                if (Integer.parseInt(Objects.requireNonNull(region.getFlag(LevelledMobs.minlevelflag))) > -1)
                    minbool = true;
            if (isInteger(region.getFlag(LevelledMobs.maxlevelflag)))
                if (Integer.parseInt(Objects.requireNonNull(region.getFlag(LevelledMobs.maxlevelflag))) > Integer.parseInt(Objects.requireNonNull(region.getFlag(LevelledMobs.minlevelflag))))
                    maxbool = true;
            if (region.getFlag(LevelledMobs.allowlevelflag) == StateFlag.State.ALLOW)
                customlevelbool = true;
            else if (region.getFlag(LevelledMobs.allowlevelflag) == StateFlag.State.DENY)
                customlevelbool = false;
        }

        return minbool && maxbool && customlevelbool;
    }
}
