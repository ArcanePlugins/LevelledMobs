package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.rule.component.context.Context
import java.util.TreeSet

class CachedModalList<T : Comparable<T>> : Cloneable {
    var allowedList: Set<T>
    var allowedGroups: Set<String>
    var excludedList: Set<T>
    var excludedGroups: Set<String>
    var doMerge = false
    var allowAll = false
    var excludeAll = false

    constructor(){
        this.allowedList = TreeSet()
        this.allowedGroups = TreeSet(String.CASE_INSENSITIVE_ORDER)
        this.excludedList = TreeSet()
        this.excludedGroups = TreeSet(String.CASE_INSENSITIVE_ORDER)
    }

    constructor(allowedList: Set<T>, excludedList: Set<T>){
        this.allowedList = allowedList
        this.excludedList = excludedList
        this.allowedGroups = TreeSet(String.CASE_INSENSITIVE_ORDER)
        this.excludedGroups = TreeSet(String.CASE_INSENSITIVE_ORDER)
    }

    fun isEnabledInList(item: T, context: Context?): Boolean{
        if (this.allowAll) return true
        if (this.excludeAll) return false
        if (this.isEmpty) return false

        if (context != null){
            for (group in context.getApplicableGroups()) {
                if (excludedGroups.contains(group)) {
                    return false
                }
            }

            if (excludedList.contains(item)) {
                return false
            }

            for (group in context.getApplicableGroups()) {
                if (allowedGroups.contains(group)) {
                    return true
                }
            }
        }

        if (excludedList.contains(item)) {
            return false
        }

        return this.isBlackList || this.allowedList.contains(item)
    }

    val isEmpty: Boolean
        get() {
            return allowedList.isEmpty() &&
                    allowedGroups.isEmpty() &&
                    excludedList.isEmpty() &&
                    excludedGroups.isEmpty()
        }

    val isWhiteList: Boolean
        get() {
            return (allowedList.isNotEmpty() || allowedGroups.isNotEmpty()) &&
                    excludedList.isEmpty() && excludedGroups.isEmpty()
        }

    val isBlackList: Boolean
        get() {
            return allowedList.isEmpty() && allowedGroups.isEmpty() &&
                    (excludedList.isNotEmpty() || excludedGroups.isNotEmpty())
        }

    override fun toString(): String {
        val sb = StringBuilder()
        if (allowedList.isNotEmpty()) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("lst: ")
            sb.append(allowedList)
        }
        if (allowAll) {
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
            sb.append(allowedGroups)
        }

        if (excludeAll) {
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
            sb.append(excludedList)
        }
        if (excludedGroups.isNotEmpty()) {
            if (sb.isNotEmpty()) {
                sb.append(", ")
            }
            sb.append("ex-grps: ")
            sb.append(excludedGroups)
        }

        return sb.toString()
    }

    @Suppress("UNCHECKED_CAST")
    public override fun clone(): Any {
        val copy: CachedModalList<T> = super.clone() as CachedModalList<T>
        copy.allowedList = (this.allowedList as TreeSet<T>).clone() as TreeSet<T>
        copy.allowedGroups = (this.allowedGroups as TreeSet<String>).clone() as TreeSet<String>
        copy.excludedList = (this.excludedList as TreeSet<T>).clone() as TreeSet<T>
        copy.excludedGroups = (this.excludedGroups as TreeSet<String>).clone() as TreeSet<String>

        return copy
    }

    fun mergeCachedModal(cachedModalList: CachedModalList<T>){
        (this.allowedList as TreeSet<T>).addAll(cachedModalList.allowedList as TreeSet<T>)
        (this.excludedList as TreeSet<T>).addAll(cachedModalList.excludedList as TreeSet<T>)

        (this.allowedGroups as TreeSet<String>).addAll(cachedModalList.allowedGroups)
        (this.excludedGroups as TreeSet<String>).addAll(cachedModalList.excludedGroups)

        if (cachedModalList.allowAll) {
            allowAll = true
        }
        if (cachedModalList.excludeAll) {
            excludeAll = true
        }
    }
}