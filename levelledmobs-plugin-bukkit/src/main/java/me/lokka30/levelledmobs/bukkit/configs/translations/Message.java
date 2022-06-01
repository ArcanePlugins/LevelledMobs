package me.lokka30.levelledmobs.bukkit.configs.translations;

import de.themoep.minedown.MineDown;
import java.util.Arrays;
import java.util.Collections;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.CommandSender;
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

        new String[]{"command", "common", "access-not-player"},

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

        "[LevelledMobs 4](color=#00ffff-#a9ffff,format=bold) &8| &7The &fUltimate&7 RPG Mob "
            + "Levelling Solution",
        "&8 • &7For a list of available commands, run '&b&n[/lm help](run_command=/lm help)&7'.",
        "&8 • &7To learn more about the plugin, run '&b&n[/lm about](run_command=/lm about)&7'."
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_ABOUT_INVALID_USAGE(
        true,

        new String[]{"command", "levelledmobs", "subcommand", "about", "invalid-usage"},

        "%prefix-severe% Invalid usage; try '&b/lm about&7'."
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_ABOUT_SUCCESS(
        true,

        new String[]{"command", "levelledmobs", "subcommand", "about", "success"},

        "&8&m+-------------»",
        "&7Running [LevelledMobs](color=#00ffff-#a9ffff,format=bold) &bv%version%&7,",
        "&7maintained by &b%maintainers%&7.",
        " ",
        "&7Links: &b&n[Resource Page](open_url=https://spigotmc.org/resources/74304)&r   " +
            "&3&n[Wiki](open_url=https://github.com/lokka30/LevelledMobs/Wiki)&r   " +
            "&9&n[Credits](open_url=https://github.com/lokka30/LevelledMobs/wiki/Credits)&r   " +
            "&1&n[Source](open_url=https://github.com/lokka30/LevelledMobs)",
        " ",
        "&8&m+-------------»"
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_SUMMON_NOT_SUMMONABLE(
        true,

        new String[]{"command", "levelledmobs", "subcommand", "summon", "summoning"},

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
     *
     * The name of the constant should be akin to the position it has in the translation file
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
    public BaseComponent[][] formatMd(final String... replacements) {
        final var length = getDeclared().length;
        final var to = new BaseComponent[length][];
        for(int i = 0; i < length; i++)
            to[i] = MineDown.parse(getDeclared()[i], replacements);
        return to;
    }

    /*
    public void sendTo_v1(final CommandSender sender, final String... replacements) {
        final var modifiedReplacements = new String[replacements.length + 6];

        modifiedReplacements[0] = "%prefix-info%";
        modifiedReplacements[1] = Message.GENERIC_PREFIX_INFO.getDeclared()[0];
        modifiedReplacements[2] = "%prefix-warning%";
        modifiedReplacements[3] = Message.GENERIC_PREFIX_WARNING.getDeclared()[0];
        modifiedReplacements[4] = "%prefix-severe%";
        modifiedReplacements[5] = Message.GENERIC_PREFIX_SEVERE.getDeclared()[0];
        System.arraycopy(replacements, 0, modifiedReplacements, 3, replacements.length);

        for(var components : formatMd(modifiedReplacements))
            sender.spigot().sendMessage(components);
    }

    @SuppressWarnings("unused")
    public void sendToVersion_v2(final CommandSender sender, final String... replacements) {
        final var newReplacements = new ArrayList<String>();

        newReplacements.add("%prefix-info%");
        newReplacements.add(Message.GENERIC_PREFIX_INFO.getDeclared()[0]);
        newReplacements.add("%prefix-warning%");
        newReplacements.add(Message.GENERIC_PREFIX_WARNING.getDeclared()[0]);
        newReplacements.add("%prefix-severe%");
        newReplacements.add(Message.GENERIC_PREFIX_SEVERE.getDeclared()[0]);
        Collections.addAll(newReplacements, replacements);

        for(var components : formatMd(newReplacements.toArray(new String[0])))
            sender.spigot().sendMessage(components);
    }
    TODO delete these if sendTo works
     */

    public void sendTo(final CommandSender sender, final String... replacements) {
        // ... firstly, let's add the prefix placeholders in the replacements array ...

        // let's start out with a list of the prefix placeholder replacement pairs [6 list items]
        final var newReplacements = Arrays.asList(
            "%prefix-info%", Message.GENERIC_PREFIX_INFO.getDeclared()[0],
            "%prefix-warning%", Message.GENERIC_PREFIX_WARNING.getDeclared()[0],
            "%prefix-severe%", Message.GENERIC_PREFIX_SEVERE.getDeclared()[0]
        );

        // now let's merge the replacements into the newReplacements list so they're combined
        Collections.addAll(newReplacements, replacements);

        // ... cool, now let's send these beautiful messages ...

        for(var components : formatMd(newReplacements.toArray(new String[0])))
            sender.spigot().sendMessage(components);
    }

    public static String joinDelimited(final Iterable<? extends String> args) {
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
     *
     * {@code false}: is a one-line string, NOT multi line capable, e.g., list delimiter: "&7, &b"
     *
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
