package me.lokka30.levelledmobs.bukkit.logic;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.event.action.ActionParseEvent;
import me.lokka30.levelledmobs.bukkit.event.condition.ConditionParseEvent;
import me.lokka30.levelledmobs.bukkit.event.function.FunctionPostParseEvent;
import me.lokka30.levelledmobs.bukkit.event.function.FunctionPreParseEvent;
import me.lokka30.levelledmobs.bukkit.event.group.GroupPostParseEvent;
import me.lokka30.levelledmobs.bukkit.event.group.GroupPreParseEvent;
import me.lokka30.levelledmobs.bukkit.event.process.ProcessPostParseEvent;
import me.lokka30.levelledmobs.bukkit.event.process.ProcessPreParseEvent;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.LmFunction;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.ActionSocket;
import me.lokka30.levelledmobs.bukkit.logic.group.Group;
import me.lokka30.levelledmobs.bukkit.logic.preset.Preset;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
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
        Log.inf("Loading logic system");
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
        final @NotNull Context context,
        final @NotNull String... triggers
    ) {
        for(var function : getFunctions())
            if(function.hasAnyTriggers(triggers))
                function.run(context, false);
    }

    private boolean parseGroups() {
        Log.inf("Parsing groups");

        final var groupsMap = LevelledMobs.getInstance()
                .getConfigHandler().getGroupsCfg()
                .getRoot().node("groups")
                .childrenMap();

        // enumerate through groups map
        for(var groupEntry : groupsMap.entrySet()) {

            // let's make sure that the entry's key is a string (identifiers must be strings)
            if(!(groupEntry.getKey() instanceof String)) {
                Log.sev("Found group with an invalid identifier, '" + groupEntry.getKey() +
                    "', group identifiers must be strings (text).", true);
                // TODO make LM automatically fix these after the updater runs for groups.yml
                return false;
            }

            final var group = new Group((String) groupEntry.getKey());

            // let's make sure that all groups have unique identifiers
            if(getGroups().stream().anyMatch(otherGroup ->
                otherGroup.getIdentifier().equalsIgnoreCase(group.getIdentifier()))
            ) {
                Log.war("Found group with a duplicate identifier, '" + group.getIdentifier() +
                    "', group identifiers must be unique.", true);
                continue;
            }

            // let's call the pre-parse event
            final var preParseEvent = new GroupPreParseEvent(group);
            Bukkit.getPluginManager().callEvent(preParseEvent);
            if(preParseEvent.isCancelled()) continue;

            // we've got the green light to parse it now
            if(groupEntry.getValue().isList()) {
                // group has a list key
                try {
                    group.getItems().addAll(groupEntry.getValue().getList(String.class));
                } catch(SerializationException ex) {
                    Log.sev("Unable to parse group '" + group.getIdentifier() + "': it is " +
                        "highly likely that the user has made a syntax error in the " +
                        "'groups.yml' file. A stack trace will be printed below for " +
                        "debugging purposes.", true);
                    ex.printStackTrace();
                    return false;
                }
            } else {
                Log.sev("Unable to parse group '" + group.getIdentifier() + "' as it does " +
                    "not contain a valid list of items.", true);
                return false;
            }

            // let's add the group to the set of groups
            getGroups().add(group);

            // let's call the post-parse event
            Bukkit.getPluginManager().callEvent(new GroupPostParseEvent(group));
        }

        Log.inf("Successfully parsed " + getGroups().size() + " group(s)");
        return true;
    }

    private boolean parsePresets() {
        Log.inf("Parsing presets");

        /*
        summary:

        for each preset in config
            assert key is a string
            set identifier = (string) key
            set section = <config section of preset>
            assert section is a node and not a key-value pair
            create preset object
            call pre-parse event
            if pre-parse event cancelled, continue with next preset
            call post-parse event
         */

        //TODO
        Log.inf("Successfully parsed " + getPresets().size() + " preset(s)");
        return true;
    }

    private boolean parseCustomDrops() {
        Log.inf("Parsing custom drops");

        //TODO
        Log.inf("Successfully parsed " + "?" + " custom drop(s).");
        return true;
    }

    private boolean parseFunctions() {
        /*
        [pseudocode]
        for each function
            call pre-parse function event

            for each process
                call pre-parse process event

                for each condition
                    call condition parse event
                    if event was cancelled, skip
                    if a plugin made a claim to the condition's identifier
                        add condition object from event to process's conditions list
                    else
                        error
                for each action
                    call action parse event
                    if event was cancelled, skip
                    if a plugin made a claim to the action's identifier
                        add action object from event to process's actions list
                    else
                        error

                call post-parse process event

            call post-parse function event
         */

        Log.inf("Parsing functions.");

        // don't use `var` here - type is not easily inferred without IDE
        final List<CommentedConfigurationNode> functionNodes = LevelledMobs.getInstance()
            .getConfigHandler().getSettingsCfg()
            .getRoot().node("functions").childrenList();

        for(var functionNode : functionNodes) {
            final var identifier = functionNode.node("function").getString();
            final var description = functionNode.node("description").getString("");

            /* assert valid identifier */
            if(identifier == null || identifier.isBlank()) {
                Log.sev("There is a function in settings.yml with an unknown/invalid identifier, " +
                    "so LevelledMobs is unable to parse it.", true);
                return false;
            }
            if(getFunctions().stream().anyMatch(otherFunction ->
                otherFunction.getIdentifier().equalsIgnoreCase(identifier))
            ) {
                Log.sev("There are two or more functions in settings.yml which share the " +
                    "same identifier, '" + identifier + "'. Functions must have unique " +
                    "identifiers.", true);
                return false;
            }

            /* create obj */
            final var function = new LmFunction(identifier, description, functionNode);

            /* add triggers */
            final var triggersNode = functionNode.node("triggers");
            if(triggersNode.empty() || !triggersNode.isList()) {
                Log.sev("Unable to parse triggers of function '" + identifier +
                    "': not a valid list of triggers.", true);
                return false;
            }
            final List<String> triggersList;
            try {
                triggersList = Objects.requireNonNull(triggersNode.getList(String.class), "list");
            } catch (SerializationException | NullPointerException ex) {
                Log.sev("Unable to parse triggers of function '" + identifier +
                    "'. This is usually caused by a the user creating a syntax error in " +
                    "the settings.yml file. A stack trace will be printed below for debugging " +
                    "purposes.", true);
                ex.printStackTrace();
                return false;
            }
            function.getTriggers().addAll(triggersList);

            /* call pre parse fun */

            final var functionPreParseEvent = new FunctionPreParseEvent(function);
            Bukkit.getPluginManager().callEvent(functionPreParseEvent);
            if(functionPreParseEvent.isCancelled()) continue;

            /* parse processes */
            if(!(parseProcesses(function)))
                return false;

            /* call post parse fun */
            Bukkit.getPluginManager().callEvent(new FunctionPostParseEvent(function));
        }

        for(var function : getFunctions()) {
            Log.inf(" ");
            Log.inf(" ");
            Log.inf("DEBUG: " + function.toString());
            Log.inf(" ");
            Log.inf(" ");
        }

        Log.inf("Successfully parsed " + getFunctions().size() + " function(s).");
        return true;
    }

    private boolean parseProcesses(final @NotNull LmFunction function) {
        Objects.requireNonNull(function, "function");

        final List<CommentedConfigurationNode> processNodes = function
            .getNode().node("processes").childrenList();

        for(var processNode : processNodes) {
            final var identifier = processNode.node("process").getString();
            final var description = processNode.node("description").getString("");

            /* assert valid identifier */
            if(identifier == null || identifier.isBlank()) {
                Log.sev("There is a process in settings.yml with an unknown/invalid " +
                    "identifier in the function '" + function.getIdentifier() + "', so "
                    + "LevelledMobs is unable to parse it.", true);
                return false;
            }
            if(function.getProcesses().stream().anyMatch(otherProcess ->
                otherProcess.getIdentifier().equalsIgnoreCase(identifier))
            ) {
                Log.sev("There are two or more processes in the function '" +
                    function.getIdentifier() + "' which share the same identifier, '" +
                    identifier + "'. Processes must have unique identifiers.", true);
                return false;
            }

            /* create obj */
            final var process = new Process(identifier, description, processNode, function);

            /* call pre-parse process event */
            final var processPreParseEvent = new ProcessPreParseEvent(process);
            Bukkit.getPluginManager().callEvent(processPreParseEvent);
            if(processPreParseEvent.isCancelled()) continue;

            /* add presets */
            if(processNode.hasChild("presets")) {
                final var presetsNode = processNode.node("presets");
                if(!presetsNode.isList()) {
                    Log.sev("Unable to parse presets of process '" + identifier +
                        "': not a valid list of presets.", true);
                    return false;
                }
                final List<String> presetsIdentifiersList;
                try {
                    presetsIdentifiersList = Objects.requireNonNull(presetsNode.getList(String.class), "presets");
                } catch (SerializationException | NullPointerException ex) {
                    Log.sev("Unable to parse presets of process '" + identifier +
                        "'. This is usually caused by a the user creating a syntax error in " +
                        "the settings.yml file. A stack trace will be printed below for debugging " +
                        "purposes.", true);
                    ex.printStackTrace();
                    return false;
                }

                presetIterator:
                for(var presetIdentifier : presetsIdentifiersList) {
                    if(process.getPresets().stream().anyMatch(otherPreset ->
                        otherPreset.getIdentifier().equals(presetIdentifier))
                    ) {
                        Log.war("Process '" + identifier + "' has preset '" + presetIdentifier +
                            "' listed more than once. A preset can only be used at most once per "
                            + "process.", true);
                        continue;
                    }

                    for(var otherPreset : getPresets()) {
                        if(otherPreset.getIdentifier().equals(presetIdentifier)) {
                            process.getPresets().add(otherPreset);
                            continue presetIterator;
                        }
                    }

                    Log.sev("Process '" + identifier + "' specifies an unknown preset, '" +
                        presetIdentifier + "'.", true);
                    return false;
                }
            }

            /* parse actions */
            //TODO make preset parsing happen after function parsing
            //TODO make preset parasing factor in sockets
            //TODO future - may want to consider multiple sockets behaviour
            if(!(parseActions(process) && parseConditions(process)))
                return false;

            /* call post-parse process event */
            Bukkit.getPluginManager().callEvent(new ProcessPostParseEvent(process));
            function.getProcesses().add(process);
        }

        return true;
    }

    //TODO test
    private boolean parseActions(final @NotNull Process process) {
        Objects.requireNonNull(process, "process");

        final List<CommentedConfigurationNode> actionNodes = process.getNode().node("do").childrenList();

        for(var actionNode : actionNodes) {
            if(actionNode.hasChild("action")) {
                final String identifier = actionNode.node("action").getString("");

                if(identifier.isBlank()) {
                    Log.sev(String.format(
                        "Process '%s' in function '%s' has an action with an invalid identifier.",
                        process.getIdentifier(), process.getFunction().getIdentifier()
                    ), true);
                    return false;
                }

                final var actionParseEvent = new ActionParseEvent(identifier, process, actionNode);
                Bukkit.getPluginManager().callEvent(actionParseEvent);
                if(!actionParseEvent.isClaimed()) {
                    Log.sev(String.format(
                        "Action '%s' in process '%s' in function '%s' is not known to " +
                            "LevelledMobs or any of it external integrations. Verify the " +
                            "spelling is correct.",
                        identifier, process.getIdentifier(), process.getFunction().getIdentifier()
                    ), true);
                    return false;
                }

            } else if(actionNode.hasChild("socket")) {
                process.getActions().add(new ActionSocket(
                    process,
                    actionNode,
                    actionNode.node("socket").getString(null)
                ));
            } else {
                Log.sev(String.format(
                    "Process '%s' in function '%s' contains an item in the actions list which " +
                        "does not identify as an action or socket.",
                    process.getIdentifier(), process.getFunction().getIdentifier()
                ), true);
                return false;
            }
        }

        return true;
    }

    // TODO Test
    private boolean parseConditions(final @NotNull Process process) {
        Objects.requireNonNull(process, "process");

        final List<CommentedConfigurationNode> conditionNodes = process.getNode().node("if").childrenList();

        for(var conditionNode : conditionNodes) {
            if(conditionNode.hasChild("condition")) {
                final String identifier = Objects.requireNonNull(
                    conditionNode.node("condition").getString(null),
                    "identifier"
                );

                final var conditionParseEvent = new ConditionParseEvent(identifier, process, conditionNode);
                Bukkit.getPluginManager().callEvent(conditionParseEvent);
                if(!conditionParseEvent.isClaimed()) {
                    Log.sev(String.format(
                        "Condition '%s' in process '%s' in function '%s' is not known to " +
                            "LevelledMobs or any of it external integrations. Verify the " +
                            "spelling is correct.",
                        identifier, process.getIdentifier(), process.getFunction().getIdentifier()
                    ), true);
                    return false;
                }

            } else if(conditionNode.hasChild("socket")) {
                process.getActions().add(new ActionSocket(
                    process,
                    conditionNode,
                    conditionNode.node("socket").getString(null)
                ));
            } else {
                Log.sev(String.format(
                    "Process '%s' in function '%s' contains an item in the conditions list which " +
                        "does not identify as an action or socket.",
                    process.getIdentifier(), process.getFunction().getIdentifier()
                ), true);
                return false;
            }
        }

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
