package me.lokka30.levelledmobs.customdrops;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;

public class CustomCommand extends CustomDropBase {
    public CustomCommand(@NotNull final CustomDropsDefaults defaults){
        super(defaults);
        this.rangedEntries = new TreeMap<>();
    }

    public String commandName;
    public String command;
    @NotNull
    public Map<String, String> rangedEntries;

    public CustomCommand cloneItem() {
        CustomCommand copy = null;
        try {
            copy = (CustomCommand) super.clone();
        } catch (Exception ignored) {}

        return copy;
    }
}
