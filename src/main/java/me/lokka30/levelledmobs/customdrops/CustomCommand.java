package me.lokka30.levelledmobs.customdrops;

public class CustomCommand extends CustomDropBase {
    public String commandName;
    public String command;

    public CustomCommand cloneItem() {
        CustomCommand copy = null;
        try {
            copy = (CustomCommand) super.clone();
        } catch (Exception ignored) {}

        return copy;
    }
}
