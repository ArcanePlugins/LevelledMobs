package me.lokka30.levelledmobs.bukkit.configs.translations;

import java.util.Locale;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public enum InbuiltLang {
    DE_DE,
    EN_AU,
    EN_GB,
    EN_US,
    ES_ES,
    FR_FR;

    @Override
    @NotNull
    public String toString() {
        final var uppercase = super.toString();
        final var split = uppercase.split("_");

        Validate.isTrue(split.length == 2,
            "Invalid language code '" + uppercase + "'!");

        return split[0].toLowerCase(Locale.ROOT) + "_" + split[1].toUpperCase(Locale.ROOT);
    }

    @Nullable
    public static InbuiltLang of(final String languageCode) {
        try {
            return InbuiltLang.valueOf(languageCode.toUpperCase(Locale.ROOT));
        } catch(IllegalArgumentException ex) {
            return null;
        }
    }
}
