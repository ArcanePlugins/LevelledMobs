package io.github.arcaneplugins.levelledmobs.rules

/**
 * Provides a list of strings that can be optionally
 * merged together
 *
 * @author stumper66
 * @since 3.3.0
 */
class MergeableStringList {
    val items = mutableListOf<String>()
    var doMerge: Boolean = false

    constructor()

    constructor(item: String?){
        if (item != null)
            this.items.add(item)
    }

    constructor(item: String?, doMerge: Boolean){
        if (item == null) return

        items.add(item)
        this.doMerge = doMerge
    }

    fun setItemFromString(input: String?) {
        if (input == null) return

        items.add(input)
    }

    fun setItemFromList(input: Collection<String>?) {
        if (input == null) return

        items.addAll(input)
    }

    fun mergeFromList(input: Collection<String>?) {
        if (input == null) return

        items.addAll(input)
    }

    val isEmpty: Boolean
        get() = items.isEmpty()

    val isNotEmpty: Boolean
        get() = !this.isEmpty

    override fun toString(): String {
        if (items.isEmpty()) return super.toString()

        val sb = StringBuilder()
        if (items.size == 1)
            sb.append(items[0])
        else
            sb.append(items)

        if (doMerge) {
            if (sb.isNotEmpty()) sb.append(" ")

            sb.append("(merge)")
        }

        return sb.toString()
    }
}