package io.github.arcaneplugins.levelledmobs.misc

import java.util.TreeSet
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.generator.structure.Structure

/**
 * A standardized list used for holding various rule lists
 *
 * @author stumper66
 * @since 3.0.0
 */
class CachedModalList<T> : Cloneable {
    var includedList = mutableSetOf<T>()
    var includedGroups: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    var excludedList = mutableSetOf<T>()
    var excludedGroups: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)
    var doMerge: Boolean = false
    var includeAll: Boolean = false
    var excludeAll: Boolean = false

    constructor()

    constructor(
        includedList: MutableSet<T>,
        excludedList: MutableSet<T>
    ){
        this.includedList = includedList
        this.excludedList = excludedList
    }

    fun isIncludedInList(item: T, lmEntity: LivingEntityWrapper?): Boolean {
        if (this.includeAll) return true
        if (this.excludeAll) return false
        if (this.isEmpty()) return true

        if (lmEntity != null) {
            for (group in lmEntity.getApplicableGroups()) {
                if (excludedGroups.contains(group)) return false
            }

            if (excludedList.contains(item)) return false

            for (group in lmEntity.getApplicableGroups()) {
                if (includedGroups.contains(group)) return true
            }
        }

        if (excludedList.contains(item)) return false

        return this.isBlacklist || includedList.contains(item)
    }

    private fun applySpecialFormatting(
        input: MutableSet<T>,
        sb: StringBuilder
    ){
        // if certain types need special handling to format the output
        // visually then define them here

        var isFirst = true
        for (item in input){
            if (!isFirst) sb.append(", ")

            if (item is Structure) {
                // TODO: make this not use a deprecated method
                @Suppress("removal")
                sb.append(item.key().value())
            }
            else
                sb.append(item)

            isFirst = false
        }
    }

    fun isEmpty(): Boolean {
        return includedList.isEmpty() &&
                includedGroups.isEmpty() &&
                excludedList.isEmpty() &&
                excludedGroups.isEmpty()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (includedList.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("lst: ")
            applySpecialFormatting(this.includedList, sb)
        }
        if (this.includeAll) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("all included")
        }

        if (includedGroups.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("grps: ")
            sb.append(this.includedGroups)
        }

        if (this.excludeAll) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("all excluded")
        }

        if (excludedList.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("ex-lst: ")
            applySpecialFormatting(this.excludedList, sb)
        }
        if (excludedGroups.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append(", ")

            sb.append("ex-grps: ")
            sb.append(this.excludedGroups)
        }

        return sb.toString()
    }

    val isWhitelist: Boolean
        get() {
        return (includedList.isNotEmpty() || includedGroups.isNotEmpty()) &&
                (excludedList.isEmpty() && excludedGroups.isEmpty())
        }

    val isBlacklist: Boolean
        get() {
        return (includedList.isEmpty() && includedGroups.isEmpty()) &&
                (excludedList.isNotEmpty() || excludedGroups.isNotEmpty())
        }

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): Any {
        var copy: CachedModalList<T>? = null
        try {
            copy = super.clone() as CachedModalList<T>
            copy.includedList = ((includedList as TreeSet<T>).clone() as TreeSet<T>)
            copy.includedGroups = (includedGroups as TreeSet<String>).clone() as TreeSet<String>
            copy.excludedList = ((excludedList as TreeSet<T>).clone() as TreeSet<T>)
            copy.excludedGroups = (excludedGroups as TreeSet<String>).clone() as TreeSet<String>
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }

        return copy as Any
    }

    @Suppress("UNCHECKED_CAST")
    fun mergeCachedModal(cachedModalList: CachedModalList<*>) {
        includedList.addAll(cachedModalList.includedList as Collection<T>)
        excludedList.addAll(cachedModalList.excludedList as Collection<T>)

        includedGroups.addAll(cachedModalList.includedGroups)
        excludedGroups.addAll(cachedModalList.excludedGroups)

        if (cachedModalList.includeAll) this.includeAll = true
        if (cachedModalList.excludeAll) this.excludeAll = true
    }
}