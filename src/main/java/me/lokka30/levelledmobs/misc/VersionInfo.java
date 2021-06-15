package me.lokka30.levelledmobs.misc;

import java.io.InvalidObjectException;
import java.util.LinkedList;
import java.util.List;

/**
 * A custom implementation for comparing program versions
 *
 * @author stumper66
 */
public class VersionInfo implements Comparable<VersionInfo> {
    public VersionInfo(final String version) throws InvalidObjectException {
        if (version == null) throw new NullPointerException("version can't be null");

        this.versionStr = version;
        String[] split = version.split("\\.");
        this.thisVerSplit = new LinkedList<>();
        for (final String numTemp : split){
            if (!Utils.isDouble(numTemp)) throw new InvalidObjectException("Version can only contain numbers and periods");
            int intD = Integer.parseInt(numTemp);
            thisVerSplit.add(intD);
        }

        for (int i = 4; i < thisVerSplit.size(); i++)
            thisVerSplit.add(0);
    }

    final private String versionStr;
    final List<Integer> thisVerSplit;

    @Override
    public boolean equals(final Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof VersionInfo)) return false;

        final VersionInfo otherVersion = (VersionInfo) o;
        return this.versionStr.equals(((VersionInfo) o).getVersion());
    }

    @Override
    public String toString() {
        return this.versionStr;
    }

    public String getVersion(){
        return this.versionStr;
    }

    @Override
    public int compareTo(final VersionInfo v) {
        for (int i = 0; i < 4; i++) {
            final int compareInt = v.thisVerSplit.get(i);
            final int thisInt = this.thisVerSplit.get(i);

            if (thisInt > compareInt)
                return 1;
            else if (thisInt < compareInt)
                return -1;
        }

        return 0;
    }
}
