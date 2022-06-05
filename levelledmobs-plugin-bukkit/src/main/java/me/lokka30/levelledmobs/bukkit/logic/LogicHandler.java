package me.lokka30.levelledmobs.bukkit.logic;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.utils.Log;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

public final class LogicHandler {

    /* vars */

    private final HashSet<Group> groups = new HashSet<>();
    private final HashSet<Preset> presets = new HashSet<>();
    private final LinkedHashSet<LmFunction> functions = new LinkedHashSet<>();

    /* methods */

    /**
    Initialisation - parse functions, presets, groups, etc.
     */
    public boolean load() {
        Log.inf("Loading logic system.");
        return parseGroups() && parsePresets() && parseCustomDrops() && parseFunctions();
    }

    /**
     * Checks all available functions if they have any of the given triggers, and any applicable
     * functions will be ran using the given context.
     *
     * @param context the context given for the function to run with
     * @param triggers a list of trigger IDs, which a function requires at least one match to apply
     */
    public void runFunctionsWithTriggers(
        final @NotNull RunContext context,
        final @NotNull String... triggers
    ) {
        for(var function : getFunctions())
            if(function.hasAnyTriggers(triggers))
                function.run(context);
    }

    private boolean parseGroups() {
        Log.inf("Parsing groups.");

        //todo let's not use forEach here..

        LevelledMobs.getInstance()
            .getConfigHandler().getGroupsCfg()
            .getRoot().node("groups")
            .childrenMap().forEach((key, section) -> {

                if(key instanceof String identifier) {
                    // firstly, we don't want any duplicate groups
                    if(getGroups()
                        .stream()
                        .anyMatch(group -> group.getIdentifier().equalsIgnoreCase(identifier))
                    ) {
                        // darn.. it's a duplicate. skip it and parse the next group
                        Log.war("Skipping duplicate group '" + identifier +
                            "'. Ensure every group uses unique names.",
                            true);
                    } else {
                        // group is not a duplicate
                        if(section.isList()) {
                            // group has a list key
                            try {
                                getGroups().add(new Group(
                                    identifier,
                                    Objects.requireNonNull(section.getList(String.class))
                                ));
                            } catch(SerializationException ex) {
                                Log.sev("Unable to parse group '" + identifier + "' - it is " +
                                    "highly likely that the user has made a syntax error in the " +
                                    "'groups.yml' file. A stack trace will be printed below for " +
                                    "debugging purposes.", true);
                                ex.printStackTrace();
                            }
                        }
                    }
                } else {
                    Log.sev("Skipping group '" + key.toString() + "' as its identifier is " +
                        "not a String (text).", true);
                }
            });

        Log.inf("Successfully parsed " + getGroups().size() + " groups.");
        return true;
    }

    private boolean parsePresets() {
        Log.inf("Parsing presets.");

        //TODO
        Log.inf("Successfully parsed " + getPresets().size() + " presets.");
        return true;
    }

    private boolean parseCustomDrops() {
        Log.inf("Parsing custom drops.");

        //TODO
        Log.inf("Successfully parsed " + "?" + " custom drops.");
        return true;
    }

    private boolean parseFunctions() {
        Log.inf("Parsing functions.");

        //TODO
        Log.inf("Successfully parsed " + getFunctions().size() + " functions.");
        return true;
    }

    /* getters and setters */

    @NotNull
    public HashSet<Group> getGroups() { return groups; }

    @NotNull
    public HashSet<Preset> getPresets() { return presets; }

    @NotNull
    public LinkedHashSet<LmFunction> getFunctions() { return functions; }

}
