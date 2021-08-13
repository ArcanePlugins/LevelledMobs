/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Holds the configuration and logic for applying a variable low number bias
 * to the levelling systems
 *
 * @author stumper66
 * @since 3.0.0
 */
public class LevelNumbersWithBias {

    public LevelNumbersWithBias(final int minLevel, final int maxLevel, final int factor){
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.factor = factor;
        this.numberList = new LinkedList<>();
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

    public boolean isEmpty(){
        return this.numberList.isEmpty();
    }

    public void populateData(){
        if (!this.numberList.isEmpty()) this.numberList.clear();

        int useFavor = maxLevel + 10 - factor;
        for (int i = minLevel; i <= maxLevel; i++) {
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
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;

        LevelNumbersWithBias levelNumbersWithBias = (LevelNumbersWithBias) obj;
        return this.minLevel == levelNumbersWithBias.minLevel && this.maxLevel == levelNumbersWithBias.maxLevel && this.factor == levelNumbersWithBias.factor;
    }
}
