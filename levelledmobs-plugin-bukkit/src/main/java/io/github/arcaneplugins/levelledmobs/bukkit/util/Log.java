package io.github.arcaneplugins.levelledmobs.bukkit.util;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory;
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugHandler;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public final class Log {

    /*
    When testing, sometimes it is useful to include this suffix on particular debug logs to
    make it more clear to see amongst the rest of the mess.
     */
    @SuppressWarnings("unused")
    public static final String DEBUG_I_AM_BLIND_SUFFIX = " " + ChatColor.AQUA + "<------ notice me";

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

    public static void debug(
        @Nonnull final DebugCategory cat,
        @Nonnull final Supplier<String> msgSupplier
    ) {
        if(!DebugHandler.isCategoryEnabled(cat)) return;

        final String msg = msgSupplier.get();

        if(DebugHandler.isCategoryEnabled(DebugCategory.BROADCAST_TO_OPS)) {
            for(final Player player : Bukkit.getOnlinePlayers()) {
                if(!player.isOp()) continue;

                player.sendMessage(
                    ChatColor.DARK_GRAY + "[LM DEBUG : " + cat.name() + "] " + msg
                );
            }
        }

        inf("[DEBUG : " + cat.name() + "] " + msg);
    }

}
