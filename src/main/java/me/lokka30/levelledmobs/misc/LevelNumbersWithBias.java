package me.lokka30.levelledmobs.misc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public class LevelNumbersWithBias {

    public LevelNumbersWithBias(int minLevel, int maxLevel, int factor){
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.factor = factor;
        this.numberList = new ArrayList<>();
        this.createdTime = LocalDateTime.now();
    }

    final private int minLevel;
    final private int maxLevel;
    final private int factor;
    final private List<Integer> numberList;
    final LocalDateTime createdTime;

    public int getNumberWithinLimits(){
        return this.numberList.get(ThreadLocalRandom.current().nextInt(0, numberList.size() - 1));
    }

    public int getMinLevel(){
        return minLevel;
    }

    public int getMaxLevel(){
        return maxLevel;
    }

    public int getFactor(){
        return factor;
    }

    public void populateData(){
        if (!this.numberList.isEmpty()) this.numberList.clear();

        int useFavor = maxLevel + 10 - factor;
        for (int i = minLevel; i < maxLevel; i++) {
            for (int t = 0; t < useFavor; t++) {
                this.numberList.add(i);
            }
            useFavor--;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(minLevel, maxLevel, factor);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;

        LevelNumbersWithBias levelNumbersWithBias = (LevelNumbersWithBias) obj;
        return this.minLevel == levelNumbersWithBias.minLevel && this.maxLevel == levelNumbersWithBias.maxLevel && this.factor == levelNumbersWithBias.factor;
    }
}
