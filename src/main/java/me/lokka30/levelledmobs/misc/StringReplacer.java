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

    public void replace(final @NotNull String replace, final double replaceWith){
        replace(replace, String.valueOf(replaceWith));
    }

    public void replace(final @NotNull String replace, final int replaceWith){
        replace(replace, String.valueOf(replaceWith));
    }

    public StringReplacer replace(final @NotNull String replace, final @Nullable String replaceWith){
        final String replaceWithText = replaceWith != null ? replaceWith : "";

        text = text.replace(replace, replaceWithText);
        return this;
    }

    public boolean isEmpty(){
        return this.text.isEmpty();
    }

    public boolean contains(final @NotNull CharSequence s){
        return this.text.contains(s);
    }
}
