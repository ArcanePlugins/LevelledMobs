package me.lokka30.levelledmobs.customdrops;

import org.jetbrains.annotations.NotNull;

public class CustomCommand extends CustomDropBase {
    public CustomCommand(@NotNull final CustomDropsDefaults defaults){
        super(defaults);
    }

    public String commandName;
    public String command;
    public String ranged;

    public CustomCommand cloneItem() {
        CustomCommand copy = null;
        try {
            copy = (CustomCommand) super.clone();
        } catch (Exception ignored) {}

        return copy;
    }
}
