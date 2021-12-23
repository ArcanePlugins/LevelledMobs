/*
 * Source:      <https://gamedev.stackexchange.com/a/162987>.
 * Author:      <https://gamedev.stackexchange.com/users/21890/philipp>
 * License:     Attribution-ShareAlike 4.0 International (CC BY-SA 4.0) license: <https://creativecommons.org/licenses/by-sa/4.0/legalcode>
 * Alterations:
 *  - A constructor to the Entry sub-class was added. (by: lokka30)
 */

package me.lokka30.levelledmobs.util.math;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandom<T> {

    private class Entry {
        double accumulatedWeight;
        T object;

        public Entry(double accumulatedWeight, T object) {
            this.accumulatedWeight = accumulatedWeight;
            this.object = object;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private double accumulatedWeight;

    public void addEntry(T object, double weight) {
        accumulatedWeight += weight;
        Entry e = new Entry(accumulatedWeight, object);
        entries.add(e);
    }

    public T getRandom() {
        final double r = ThreadLocalRandom.current().nextDouble() * accumulatedWeight;

        for (Entry entry: entries) {
            if (entry.accumulatedWeight >= r) {
                return entry.object;
            }
        }

        throw new NullPointerException("No entries specified in weighted random list");
    }
}