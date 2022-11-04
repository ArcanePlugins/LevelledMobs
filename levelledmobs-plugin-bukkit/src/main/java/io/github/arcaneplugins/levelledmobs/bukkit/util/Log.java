package io.github.arcaneplugins.levelledmobs.bukkit.util;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;

@SuppressWarnings("unused")
public final class Log {

    private Log() throws IllegalAccessException {
        throw new IllegalAccessException("Attempted instantiation of utility class");
    }

    public static void inf(final String msg) {
        LevelledMobs.getInstance().getLogger().info(msg);
    }

    public static void war(final String msg) {
        war(msg, false);
    }

    public static void war(final String msg, final boolean suggestSupport) {
        LevelledMobs.getInstance().getLogger().warning(msg + (!suggestSupport ? "" :
            " If (despite multiple attempts) you are unable to fix this issue, feel free to " +
                "contact our support team for assistance."
        ));
    }

    public static void sev(final String msg) {
        sev(msg, false);
    }

    public static void sev(final String msg, final boolean suggestSupport) {
        LevelledMobs.getInstance().getLogger().severe(msg + (!suggestSupport ? "" :
            " Feel free to contact our support team if assistance is required."
        ));
    }

    //TODO remove
    public static void tmpdebug(final String msg) {
        war("[DEBUG] " + msg);
    }

}
