package io.github.arcaneplugins.levelledmobs.misc

import java.io.InvalidObjectException
import io.github.arcaneplugins.levelledmobs.util.Utils

/**
 * A custom implementation for comparing program versions
 *
 * @author stumper66
 * @since 2.6.0
 */
class VersionInfo(
    versionInput: String
) {
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
            if (!Utils.isDouble(numTemp))
                throw InvalidObjectException("Version can only contain numbers and periods")

            val intD = numTemp.toInt()
            thisVerSplit.add(intD)
        }

        repeat(4 - thisVerSplit.size) {
            thisVerSplit.add(0)
        }
    }

    fun isLessThan(version: String): Boolean {
        return isLessThan(VersionInfo(version))
    }

    fun isLessThan(version: VersionInfo): Boolean {
        return compare(version) == CompareResult.LESS_THAN
    }

    fun isLessThanOrEquals(version: String): Boolean {
        return isLessThanOrEquals(VersionInfo(version))
    }

    fun isLessThanOrEquals(version: VersionInfo): Boolean {
        val result = compare(version)
        return result == CompareResult.LESS_THAN ||
                result == CompareResult.EQUAL
    }

    fun isGreaterThan(version: String): Boolean {
        return isGreaterThan(VersionInfo(version))
    }

    fun isGreaterThan(version: VersionInfo): Boolean {
        return compare(version) == CompareResult.GREATER_THAN
    }

    fun isGreaterThanOrEqual(version: String): Boolean {
        return isGreaterThanOrEqual(VersionInfo(version))
    }

    fun isGreaterThanOrEqual(version: VersionInfo): Boolean {
        val result = compare(version)
        return result == CompareResult.GREATER_THAN ||
                result == CompareResult.EQUAL
    }

    private fun compare(other: VersionInfo) : CompareResult {
        for (i in 0..3) {
            if (other.thisVerSplit.size <= i && thisVerSplit.size - 1 <= i)
                break

            else if (other.thisVerSplit.size <= i)
                return CompareResult.GREATER_THAN
            else if (thisVerSplit.size <= i)
                return CompareResult.LESS_THAN

            val compareInt: Int = other.thisVerSplit[i]
            val thisInt = thisVerSplit[i]

            if (thisInt > compareInt)
                return CompareResult.GREATER_THAN
            else if (thisInt < compareInt)
                return CompareResult.LESS_THAN
        }

        return CompareResult.EQUAL
    }

    enum class CompareResult{
        LESS_THAN,
        EQUAL,
        GREATER_THAN
    }

    override fun toString(): String {
        return version
    }
}