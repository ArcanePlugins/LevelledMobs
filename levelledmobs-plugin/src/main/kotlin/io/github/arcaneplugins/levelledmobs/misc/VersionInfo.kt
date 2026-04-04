package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.util.Log
import java.io.InvalidObjectException
import io.github.arcaneplugins.levelledmobs.util.Utils.isDouble
import java.util.logging.Logger

/**
 * A custom implementation for comparing program versions
 *
 * @author stumper66
 * @since 2.6.0
 */
class VersionInfo(
    versionInput: String
) : Comparable<VersionInfo> {
    private var thisVerSplit = mutableListOf<Int>()
    val version: String

    init {
        val buildNum = versionInput.indexOf("build") // 26.1.1.build.15
        version = if (buildNum > 0)
            versionInput.substring(0, buildNum)
        else
            versionInput

        val split = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (numTemp in split) {
            if (!isDouble(numTemp))
                throw InvalidObjectException("Version can only contain numbers and periods")

            val intD = numTemp.toInt()
            thisVerSplit.add(intD)
        }

        repeat(thisVerSplit.size - 4) {
            thisVerSplit.add(0)
        }
    }

    fun isLessThan(version: String): Boolean {
        return compareTo(VersionInfo(version)) == -1
    }

    fun isLessThanOrEquals(version: String): Boolean {
        return compareTo(VersionInfo(version)) <= 0
    }

    fun isGreaterThan(version: String): Boolean {
        return compareTo(VersionInfo(version)) == 1
    }

    fun isGreaterThanOrEqual(version: String): Boolean {
        return compareTo(VersionInfo(version)) >= 0
    }

    override fun compareTo(other: VersionInfo): Int {
        for (i in 0..3) {
            if (other.thisVerSplit.size <= i && thisVerSplit.size - 1 <= i)
                break
            else if (other.thisVerSplit.size <= i)
                return 1
            else if (thisVerSplit.size <= i)
                return -1

            val compareInt: Int = other.thisVerSplit[i]
            val thisInt = thisVerSplit[i]

            if (thisInt > compareInt)
                return 1
            else if (thisInt < compareInt)
                return -1
        }

        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other == this) return true
        if (other !is VersionInfo) return false

        return this.version == other.version
    }

    override fun hashCode(): Int {
        return this.version.hashCode()
    }

    override fun toString(): String {
        return version
    }
}