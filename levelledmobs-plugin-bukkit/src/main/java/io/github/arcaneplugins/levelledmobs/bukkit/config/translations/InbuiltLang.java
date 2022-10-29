package io.github.arcaneplugins.levelledmobs.bukkit.config.translations;

import java.util.Locale;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public enum InbuiltLang {

    /**
     * German
     *
     * @since 4.0.0
     */
    DE_DE,

    /**
     * English (Australia)
     * Joke translation, not intended for actual use.
     *
     * @since 4.0.0
     */
    EN_AU,

    /**
     * English (Great Britain)
     *
     * @since 4.0.0
     */
    EN_GB,

    /**
     * English (United States of America)
     *
     * @since 4.0.0
     */
    EN_US,

    /**
     * Spanish
     *
     * @since 4.0.0
     */
    ES_ES,

    /**
     * French
     *
     * @since 4.0.0
     */
    FR_FR;

    /**
     * @return language code, formatted in the IEEE-standard manner (e.g. 'en_GB', 'fr_FR').
     */
    @Override
    @NotNull
    public String toString() {
        final var uppercase = super.toString();
        final var split = uppercase.split("_");

        Validate.isTrue(split.length == 2,
            "Invalid language code '" + uppercase + "'!");

        return split[0].toLowerCase(Locale.ROOT) + "_" + split[1].toUpperCase(Locale.ROOT);
    }

    /**
     * A variant of {@link InbuiltLang#valueOf(String)} which returns a nullable result, rather than
     * throwing an exception. Makes for cleaner code since exception handling gets messy.
     *
     * @param languageCode language code to try retrieve
     * @return inbuilt language constant if matched, otherwise, null
     */
    @Nullable
    public static InbuiltLang of(final String languageCode) {
        try {
            return InbuiltLang.valueOf(languageCode.toUpperCase(Locale.ROOT));
        } catch(IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * @return the default lang, 'en_US'.
     */
    @NotNull
    public static InbuiltLang getDefault() {
        return EN_US;
    }
}
