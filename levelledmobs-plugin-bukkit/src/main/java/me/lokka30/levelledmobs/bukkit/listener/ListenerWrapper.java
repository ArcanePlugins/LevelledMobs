package me.lokka30.levelledmobs.bukkit.listener;

import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public abstract class ListenerWrapper implements Listener {

    /* vars */

    private final boolean imperative;

    /* constructors */

    /**
     * Create a new listener.
     * <p>
     * 'Imperative' listeners are required by LevelledMobs in order for the plugin to operate
     * correctly, OR, they are considered very stable events which should not see breaking
     * changes (that affect LevelledMobs).
     * Imperative listeners must also be operational on all
     * versions of Minecraft which LevelledMobs is considered 'compatible'.
     * Almost every possible listener usable by LevelledMobs would be considered imperative due to
     * the brilliant (sometimes, frustrating) stability of the Bukkit API.
     *
     * @param imperative whether the listener is imperative
     */
    public ListenerWrapper(final boolean imperative) {
        this.imperative = imperative;
    }

    /* methods */

    /**
     * Attempt to register this listener
     *
     * @return {@code false} if registration failed, AND the listener is considered 'imperative'.
     */
    public boolean register() {
        try {
            Bukkit.getPluginManager().registerEvents(this, LevelledMobs.getInstance());
        } catch(Exception ex) {
            if(isImperative()) {
                Log.sev("Unable to register listener '" + getClass().getSimpleName() + "'. " +
                    "A stack trace will be printed below for debugging purposes.",
                    true);
                ex.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /* getters and setters */

    public boolean isImperative() { return imperative; }

}
