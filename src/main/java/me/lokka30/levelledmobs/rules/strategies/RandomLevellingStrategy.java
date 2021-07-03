package me.lokka30.levelledmobs.rules.strategies;

import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class RandomLevellingStrategy implements LevellingStrategy, Cloneable {

    public Map<String, Integer> weightedRandom;
    public boolean doMerge;
    private int[] randomArray;
    private int minLevel;
    private int maxLevel;

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasWeightedRandom(){
        return this.weightedRandom != null && !this.weightedRandom.isEmpty();
    }

    public int generateLevel(final int minLevel, final int maxLevel) {
        return generateLevel(null, minLevel, maxLevel);
    }

    @Override
    public int generateLevel(final LivingEntityWrapper lmEntity, final int minLevel, final int maxLevel) {
        // this function only has lmEmtity to satify the interface requirement
        if (!this.hasWeightedRandom())
            return getRandomLevel(minLevel, maxLevel);

        if (this.randomArray == null || minLevel != this.minLevel || maxLevel != this.maxLevel)
            populateWeightedRandom(minLevel, maxLevel);

        Utils.logger.info(Arrays.toString(this.randomArray));

        final int useArrayNum = ThreadLocalRandom.current().nextInt(0, this.randomArray.length);
        return this.randomArray[useArrayNum];
    }

    public void populateWeightedRandom(final int minLevel, final int maxLevel){
        if (!this.hasWeightedRandom()) return;

        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        int count = 0;
        final List<int[]> numbers = new LinkedList<>();
        final List<Integer> values = new LinkedList<>();
        final List<Integer> orig_OverallNumberRange = new LinkedList<>();

        for (int i = minLevel; i <= maxLevel; i++)
            orig_OverallNumberRange.add(i);

        final List<Integer> overallNumberRange = new LinkedList<>(orig_OverallNumberRange);

        // first loop parses the number range string and counts totals
        // so we know how big to size the array
        for (final String range : this.weightedRandom.keySet()){
            if (Utils.isNullOrEmpty(range)) continue;
            final int value = this.weightedRandom.get(range);

            final int[] numRange = parseNumberRange(range);
            if (numRange[0] == -1 && numRange[1] == -1){
                Utils.logger.warning("Invalid number range for weighted random: " + range);
                continue;
            }

            int start = numRange[0] < 0 ? numRange[1] : numRange[0];
            int end = numRange[1] < 0 ? numRange[0] : numRange[1];
            numbers.add(new int[]{start, end});
            values.add(value);

            for (int i = start; i <= end; i++){
                if (!orig_OverallNumberRange.contains(i)) continue;

                overallNumberRange.remove(Integer.valueOf(i));
                count += value;
            }
        }

        for (final int number : overallNumberRange)
            count++;

        this.randomArray = new int[count];
        int newCount = 0;
        int valuesCount = 0;

        // now we actually populate the array
        for (final int[] nums : numbers){
            for (int i = nums[0]; i <= nums[1]; i++){
                if (!orig_OverallNumberRange.contains(i)) continue;
                for (int t = 0; t < values.get(valuesCount); t++) {
                    this.randomArray[newCount] = i;
                    newCount++;
                }
            }
            valuesCount++;
        }

        for (final int number : overallNumberRange){
            this.randomArray[newCount] = number;
            newCount++;
        }
    }

    private int[] parseNumberRange(final String range){
        final int[] results = new int[] {-1, -1};

        if (!range.contains("-")){
            if (!Utils.isInteger(range)) return results;

            results[0] = Integer.parseInt(range);
            results[1] = results[0];
            return results;
        }

        String[] nums = range.split("-");
        if (nums.length < 2) return results;

        nums[0] = nums[0].trim();
        nums[1] = nums[1].trim();

        if (!nums[0].isEmpty() && Utils.isInteger(nums[0]))
            results[0] = Integer.parseInt(nums[0]);

        if (!nums[1].isEmpty() && Utils.isInteger(nums[1]))
            results[1] = Integer.parseInt(nums[1]);

        return results;
    }

    private int getRandomLevel(final int minLevel, final int maxLevel){
        return ThreadLocalRandom.current().nextInt(minLevel, maxLevel + 1);
    }

    @Override
    public void mergeRule(final LevellingStrategy levellingStrategy) {
        if (!(levellingStrategy instanceof RandomLevellingStrategy)) return;

        final RandomLevellingStrategy randomLevelling = (RandomLevellingStrategy) levellingStrategy;
        if (this.weightedRandom == null || randomLevelling.doMerge)
            this.weightedRandom = randomLevelling.weightedRandom;
        else if (randomLevelling.weightedRandom != null)
            this.weightedRandom.putAll(randomLevelling.weightedRandom);
    }

    @Override
    public RandomLevellingStrategy cloneItem() {
        RandomLevellingStrategy copy = null;
        try {
            copy = (RandomLevellingStrategy) super.clone();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return copy;
    }

    public String toString(){
        if (!this.hasWeightedRandom())
            return "RandomLevellingStrategy";

        if (minLevel == 0)
            return this.weightedRandom.toString();

        return String.format("%s-%s: %s", this.minLevel, this.maxLevel, this.weightedRandom);
    }
}
