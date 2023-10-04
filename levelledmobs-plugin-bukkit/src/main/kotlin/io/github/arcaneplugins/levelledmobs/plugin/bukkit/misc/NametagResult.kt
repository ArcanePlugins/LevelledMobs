package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

class NametagResult(val nametag: String?) {
    val overriddenName: String? = null
    var hadCustomDeathMessage: Boolean = false

    val nametagNonNull: String
        get() {
            return nametag ?: ""
        }

    val isNullOrEmpty: Boolean
        get() {
            return nametag.isNullOrEmpty()
        }
}