package me.lokka30.levelledmobs.rules;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DeathMessages {
    public DeathMessages(){
        this.messages = new LinkedList<>();
        this.isEnabled = true;
    }

    private final List<String> messages;
    public final boolean isEnabled;

    public void addEntry(final int weight, final @NotNull String message){
        final int number = Math.max(1, weight);
        for (int i = 0; i < number; i++)
            messages.add(message);
    }

    public @Nullable String getDeathMessage(){
        if (this.messages.isEmpty()) return null;

        final int useArray = ThreadLocalRandom.current().nextInt(this.messages.size());
        return this.messages.get(useArray);
    }

    public boolean isEmpty(){
        return this.messages.isEmpty();
    }

    public String toString(){
        if (!this.isEnabled)
            return "DeathMessages (disabled)";
        if (this.isEmpty())
            return "DeathMessages";

        return String.format("DeathMessages (%s defined)", this.messages.size());
    }
}
