package me.lokka30.levelledmobs.bukkit.utils

enum class TriState {
    TRUE,
    UNKNOWN,
    FALSE;

    companion object {
        fun of(boolean: Boolean): TriState {
            return if(boolean) { TRUE } else { FALSE }
        }

        fun of(boolean: Boolean?): TriState {
            return if(boolean == null) {
                UNKNOWN
            } else if(boolean) {
                TRUE
            } else {
                FALSE
            }
        }
    }
}