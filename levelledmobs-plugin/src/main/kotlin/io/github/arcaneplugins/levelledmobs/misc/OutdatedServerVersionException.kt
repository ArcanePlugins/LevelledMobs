package io.github.arcaneplugins.levelledmobs.misc

/**
 * This exception should be thrown when a feature in a plugin
 * requires a certain server version, but the server running
 * the plugin isn't running a recent enough Minecraft version to do so.
 *
 * @author lokka30
 * @since 2.0.0
 */
class OutdatedServerVersionException(
    errorMsg: String?
) : RuntimeException(errorMsg)