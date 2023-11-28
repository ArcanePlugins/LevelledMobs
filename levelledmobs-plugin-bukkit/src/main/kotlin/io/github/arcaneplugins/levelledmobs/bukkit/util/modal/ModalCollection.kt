package io.github.arcaneplugins.levelledmobs.bukkit.util.modal

abstract class ModalCollection<T>(
    val items: MutableCollection<T>,
    var mode: Mode
) {
    fun contains(
        item: T
    ): Boolean{
        return when (mode) {
            Mode.INCLUSIVE -> items.contains(item)
            Mode.EXCLUSIVE -> !items.contains(item)
        }
    }

    enum class Mode {
        INCLUSIVE,
        EXCLUSIVE;

        fun inverse(): Mode {
            return if (this == INCLUSIVE) EXCLUSIVE else INCLUSIVE
        }
    }

}