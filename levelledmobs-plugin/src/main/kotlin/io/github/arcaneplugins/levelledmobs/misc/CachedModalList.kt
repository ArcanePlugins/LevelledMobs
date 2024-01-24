package io.github.arcaneplugins.levelledmobs.misc

import java.util.TreeSet
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper

/**
 * A standardized list used for holding various rule lists
 *
 * @author stumper66
 * @since 3.0.0
 */
class CachedModalList<T> : Cloneable {
    var allowedList = mutableSetOf<T>()
    var allowedGroups: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    var excludedList = mutableSetOf<T>()
    var excludedGroups: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    var doMerge: Boolean = false
    var allowAll: Boolean = false
    var excludeAll: Boolean = false

    constructor()

    constructor(
        allowedList: MutableSet<T>,
        excludedList: MutableSet<T>
    ){
        this.allowedList = allowedList
        this.excludedList = excludedList
    }

    fun isEnabledInList(item: T, lmEntity: LivingEntityWrapper?): Boolean {
        if (this.allowAll) {
            return true
        }
        if (this.excludeAll) {
            return false
        }
        if (this.isEmpty()) {
            return true
        }

        if (lmEntity != null) {
            for (group in lmEntity.getApplicableGroups()) {
                if (excludedGroups.contains(group)) {
                    return false
                }
            }

            if (excludedList.contains(item)) {
                return false
            }

            for (group in lmEntity.getApplicableGroups()) {
                if (allowedGroups.contains(group)) {
                    return true
                }
            }
        }

        if (excludedList.contains(item)) {
            return false
        }

        return this.isBlacklist || allowedList.contains(item)
    }

    fun isEmpty(): Boolean {
        return allowedList.isEmpty() &&
                allowedGroups.isEmpty() &&
                excludedList.isEmpty() &&
                excludedGroups.isEmpty()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (allowedList.isNotEmpty()) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("lst: ")
            sb.append(this.allowedList)
        }
        if (this.allowAll) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("all allowed")
        }

        if (allowedGroups.isNotEmpty()) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("grps: ")
            sb.append(this.allowedGroups)
        }

        if (this.excludeAll) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("all excluded")
        }

        if (excludedList.isNotEmpty()) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("ex-lst: ")
            sb.append(this.excludedList)
        }
        if (excludedGroups.isNotEmpty()) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("ex-grps: ")
            sb.append(this.excludedGroups)
        }

        return sb.toString()
    }

    val isWhitelist: Boolean
        get() {
        return (allowedList.isNotEmpty() || allowedGroups.isNotEmpty()) &&
                (excludedList.isEmpty() && excludedGroups.isEmpty())
        }

    val isBlacklist: Boolean
        get() {
        return (allowedList.isEmpty() && allowedGroups.isEmpty()) &&
                (excludedList.isNotEmpty() || excludedGroups.isNotEmpty())
        }

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): Any {
        var copy: CachedModalList<T>? = null
        try {
            copy = super.clone() as CachedModalList<T>
            copy.allowedList = ((allowedList as TreeSet<T>).clone() as TreeSet<T>)
            copy.allowedGroups = (allowedGroups as TreeSet<String>).clone() as TreeSet<String>
            copy.excludedList = ((excludedList as TreeSet<T>).clone() as TreeSet<T>)
            copy.excludedGroups = (excludedGroups as TreeSet<String>).clone() as TreeSet<String>
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }

        return copy as Any
    }

    @Suppress("UNCHECKED_CAST")
    fun mergeCachedModal(cachedModalList: CachedModalList<*>) {
        allowedList.addAll(cachedModalList.allowedList as Collection<T>)
        excludedList.addAll(cachedModalList.excludedList as Collection<T>)

        allowedGroups.addAll(cachedModalList.allowedGroups)
        excludedGroups.addAll(cachedModalList.excludedGroups)

        if (cachedModalList.allowAll) {
            this.allowAll = true
        }
        if (cachedModalList.excludeAll) {
            this.excludeAll = true
        }
    }
}