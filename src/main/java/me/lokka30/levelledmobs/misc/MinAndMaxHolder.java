package me.lokka30.levelledmobs.misc;

/**
 * Holds values used when a min and max value is needed
 *
 * @author stumper66
 * @since 3.12.2
 */
public class MinAndMaxHolder {
    public MinAndMaxHolder(final int min, final int max){
        this.min = min;
        this.max = max;
    }

    public int min;
    public int max;
    public boolean useMin = true;

    public void ensureMinAndMax(final int min, final int max){
        this.min = Math.max(this.min, min);
        this.max = Math.min(this.max, max);
    }

    public String toString(){
        return String.format("%s, %s", min, max);
    }
}
