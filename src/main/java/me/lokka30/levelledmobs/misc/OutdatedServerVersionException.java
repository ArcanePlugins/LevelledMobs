package me.lokka30.levelledmobs.misc;

/**
 * This exception should be thrown when a feature in a plugin
 * requires a certain server version, but the server running
 * the plugin isn't running a recent enough Minecraft version to do so.
 *
 * @author lokka30
 * @since 2.0.0
 */
@SuppressWarnings("unused")
public class OutdatedServerVersionException extends RuntimeException {

    public OutdatedServerVersionException(String errorMsg) {
        super(errorMsg);
    }
}

