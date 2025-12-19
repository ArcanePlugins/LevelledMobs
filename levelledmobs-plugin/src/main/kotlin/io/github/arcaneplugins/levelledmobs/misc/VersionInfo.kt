package io.github.arcaneplugins.levelledmobs.misc

import java.io.InvalidObjectException
import io.github.arcaneplugins.levelledmobs.util.Utils.isDouble

/**
 * A custom implementation for comparing program versions
 *
 * @author stumper66
 * @since 2.6.0
 */
class VersionInfo(
    val version: String
) : Comparable<VersionInfo> {
    private val versionStr: String? = null
    private var thisVerSplit = mutableListOf<Int>()

    init {
        val split = version.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (numTemp in split) {
            if (!isDouble(numTemp)) {
                throw InvalidObjectException("Version can only contain numbers and periods")
            }
            val intD = numTemp.toInt()
            thisVerSplit.add(intD)
        }

        repeat(thisVerSplit.size - 4) {
            thisVerSplit.add(0)
        }
    }

    override fun compareTo(other: VersionInfo): Int {
        for (i in 0..3) {
            if (other.thisVerSplit.size <= i && thisVerSplit.size - 1 <= i) {
                break
            } else if (other.thisVerSplit.size <= i) {
                return 1
            } else if (thisVerSplit.size <= i) {
                return -1
            }

            val compareInt: Int = other.thisVerSplit[i]
            val thisInt = thisVerSplit[i]

            if (thisInt > compareInt) {
                return 1
            } else if (thisInt < compareInt) {
                return -1
            }
        }

        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other == this) {
            return true
        }
        if (other !is VersionInfo) {
            return false
        }

        return this.versionStr == other.version
    }

    override fun hashCode(): Int {
        return this.versionStr.hashCode()
    }
}