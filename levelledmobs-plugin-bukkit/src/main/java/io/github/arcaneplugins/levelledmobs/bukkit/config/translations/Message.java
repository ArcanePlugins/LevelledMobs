package io.github.arcaneplugins.levelledmobs.bukkit.config.translations;

import de.themoep.minedown.adventure.MineDown;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum Message {

    /* constants */

    COMMAND_COMMON_NO_ACCESS_MISSING_PERMISSION(
        true,

        new String[]{"command", "common", "no-access-missing-permission"},

        "%prefix-severe% You don't have access to that; it requires the permission " +
            "'&b%permission%&7'."
    ),
    COMMAND_COMMON_ACCESS_NOT_PLAYER(
        true,

        new String[]{"command", "common", "no-access-not-player"},

        "%prefix-severe% You don't have access to that; it is only accessible to players."
    ),
    COMMAND_LEVELLEDMOBS_INVALID_SUBCOMMAND(
        true,

        new String[]{"command", "levelledmobs", "invalid-subcommand"},

        "%prefix-severe% Invalid subcommand '&b%subcommand%&7'.",
        "%prefix-info% Run '&b&n[/lm help](run_command=/lm help)&7' for a list of available commands."
    ),
    COMMAND_LEVELLEDMOBS_MAIN(
        true,

        new String[]{"command", "levelledmobs", "main"},

        " ",
        "&8┌ [LevelledMobs](color=aqua-blue format=bold) &8v%version%",
        "&8│",
        "&8│ &7The &oUltimate&7 RPG Mob Levelling Solution",
        "&8│ &7Maintained by &b%maintainers%",
        "&8│",
        "&8│ &7Quick Links: &8«&9&n[Resource Page](open_url=https://spigotmc.org/resources/74304)&8» " +
            "«&9&n[Wiki](open_url=https://github.com/lokka30/LevelledMobs/Wiki)&8» " +
            "«&9&n[Credits](open_url=https://github.com/lokka30/LevelledMobs/wiki/Credits)&8» " +
            "«&9&n[Source](open_url=https://github.com/lokka30/LevelledMobs)&8»",
        "&8│",
        "&8└ &7Run &b[/lm help](run_command=/lm help) &7to list available commands.",
        " "
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_START(
        true,

        new String[]{"command", "levelledmobs", "subcommand", "reload", "start"},

        "%prefix-info% Reloading configuration..."
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_COMPLETE_SUCCESS(
        true,

        new String[]{"command", "levelledmobs", "subcommand", "reload", "complete-success"},

        "%prefix-info% Reload complete."
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_COMPLETE_FAILURE(
        true,

        new String[]{"command", "levelledmobs", "subcommand", "reload", "complete-failure"},

        "%prefix-severe% Reload failed; check console for more information."
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_SUMMON_NOT_SUMMONABLE(
        true,

        new String[]{"command", "levelledmobs", "subcommand", "summon", "not-summonable"},

        "%prefix-severe% The entity type '&b%entity-type%&7' is not summonable."
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_SUMMON_SUMMONING(
        true,

        new String[]{"command", "levelledmobs", "subcommand", "summon", "summoning"},

        "%prefix-info% Summoning a levelled '&b%entity-name%&7'."
    ),
    GENERIC_LIST_DELIMITER(
        false,

        new String[]{"generic", "list-delimiter"},

        "&7, &b"
    ),
    GENERIC_PREFIX_INFO(
        false,

        new String[]{"generic", "prefix", "info"},

        "&f&lLM:&7"
    ),
    GENERIC_PREFIX_SEVERE(
        false,

        new String[]{"generic", "prefix", "severe"},

        "&c&lLM:&7"
    ),
    GENERIC_PREFIX_WARNING(
        false,

        new String[]{"generic", "prefix", "warning"},

        "&e&lLM:&7"
    );

    /* vars */

    /*
    This array is set when the translation is loaded.
     */
    private String[] declared = null;

    /*
    This array contains the default messages, which are used if the translation file does not
    specify the translated message for whatever reason.
    Default values should be identical to those present in the default 'en_US' translation.
     */
    private final String[] def;

    /*
    Whether the particular message is of the 'list' type.

    See the getter method for more information.
     */
    private final boolean isListType;

    /*
    Path to the message's key in the translation file.
     */
    private final String[] keyPath;

    /* constructors */

    /**
     * Create a new Message
     * <p>
     * The name of the constant should be akin to the path it has in the translation file
     *
     * @param def the default messages to use if translation has not specified it, should be
     *            matching exactly what is used in the 'en_US' translation.
     */
    Message(
        final boolean isListType,
        final @NotNull String[] keyPath,
        final @NotNull String... def
    ) {
        this.isListType = isListType;
        this.keyPath = keyPath;
        this.def = def;
    }

    /* methods */

    /*
    Format a message using MineDown
     */

    public static Component formatMd(final String[] messageStr, final String... replacements) {
        Component component = Component.empty();

        for(int line = 0; line < messageStr.length; line++) {
            String toParse = messageStr[line];

            if(toParse.isBlank()) {
                component = component.append(Component.newline());
                continue;
            }

            toParse = toParse
                    .replace("%prefix-info%",
                        Message.GENERIC_PREFIX_INFO.getDeclared()[0])
                    .replace("%prefix-warning%",
                        Message.GENERIC_PREFIX_WARNING.getDeclared()[0])
                    .replace("%prefix-severe%",
                        Message.GENERIC_PREFIX_SEVERE.getDeclared()[0]);

            if(replacements.length % 2 == 0) {
                for(int j = 0; j < replacements.length; j += 2)
                    toParse = toParse.replace(replacements[j], replacements[j + 1]);
            } else {
                Log.sev("Skipping placeholder replacement in message '" + Arrays.toString(
                    messageStr) + "' as an odd number of placeholder parameters were entered.",
                    true
                );
            }

            component = component.append(MineDown.parse(toParse));

            if(line > 0) {
                component = component.append(Component.newline());
            }
        }

        return component;
    }

    public void sendTo(final @NotNull CommandSender sender, final String... replacements) {
        sender.sendMessage(formatMd(getDeclared(), replacements));
    }

    @Contract("_ -> new")
    public static @NotNull String joinDelimited(final Iterable<? extends String> args) {
        return String.join(Message.GENERIC_LIST_DELIMITER.getDeclared()[0], args);
    }

    /* getters and setters */

    /**
     * @return default messages (en_US)
     */
    @NotNull
    public String[] getDef() {
        return def;
    }

    /**
     * @return declared messages (from user's chosen translation)
     */
    @NotNull
    public String[] getDeclared() {
        return declared == null ? getDef() : declared;
    }

    /**
     * Sets the declared messages. This is only intended to be used by the translation handler,
     * for when a translation is loaded.
     *
     * @param declared the array of declared messages
     */
    public void setDeclared(final String[] declared) {
        this.declared = declared;
    }

    /**
     * Returns whether the message is a list-type or string-type
     * <p>
     * {@code false}: is a one-line string, NOT multi line capable, e.g., list delimiter: "&7, &b"
     * <p>
     * {@code true}:  is in the string-list format, e.g., no perms message: ["No permission!",
     * "yell at admin"]
     *
     * @return list-type ({@code true}) or string-type ({@code false})
     */
    public boolean isListType() { return isListType; }

    /**
     * @return path to the key in the translation file which holds the message's declared value
     */
    public String[] getKeyPath() { return keyPath; }


}
