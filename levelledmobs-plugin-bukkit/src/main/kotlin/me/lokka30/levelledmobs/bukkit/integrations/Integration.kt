package me.lokka30.levelledmobs.bukkit.integrations

/*
FIXME Comment
 */
abstract class Integration(
    description: String, // FIXME use this somewhere e.g. command listing all integrations
    enabledByDefault: Boolean,
    internal: Boolean
) {

    /*
    FIXME Comment
     */
    var enabled = enabledByDefault

}