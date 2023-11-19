package me.lokka30.levelledmobs.misc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class StringReplacer {
    public StringReplacer(final @NotNull String text){
        this.text = text;
    }

    public @NotNull String text;

    public void replaceIfExists(final @NotNull String target, final @Nullable Supplier<String> operation){
        final String newText = operation != null ? operation.get() : null;

        if (text.contains(target)){
            replace(target, newText);
        }
    }

    public StringReplacer replace(final @NotNull String replace, final double replaceWith){
        return replace(replace, String.valueOf(replaceWith));
    }

    public StringReplacer replace(final @NotNull String replace, final int replaceWith){
        return replace(replace, String.valueOf(replaceWith));
    }

    public StringReplacer replace(final @NotNull String replace, final @Nullable String replaceWith){
        final String replaceWithText = replaceWith != null ? replaceWith : "";

        text = text.replace(replace, replaceWithText);
        return this;
    }
}
