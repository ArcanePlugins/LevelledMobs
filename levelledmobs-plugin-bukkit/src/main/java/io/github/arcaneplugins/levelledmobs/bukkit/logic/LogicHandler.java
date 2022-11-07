package io.github.arcaneplugins.levelledmobs.bukkit.logic;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholderHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.FunctionPostParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.FunctionPreParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.ProcessPostParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.ProcessPreParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.ActionParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.ConditionParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.GroupPostParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.GroupPreParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.preset.Preset;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public final class LogicHandler {

    /* vars */

    private final ContextPlaceholderHandler contextPlaceholderHandler = new ContextPlaceholderHandler();
    private final HashSet<Group> groups = new HashSet<>();
    private final HashSet<Preset> presets = new HashSet<>();
    private final LinkedHashSet<LmFunction> functions = new LinkedHashSet<>();
    private CommentedConfigurationNode parsedFunctionsNode;

    /* methods */

    /**
    Initialisation - parse functions, presets, groups, etc.
     */
    public boolean load() {
        Log.inf("Loading logic system");
        getContextPlaceholderHandler().load();
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
        final Runnable clearExitStatus = () -> getFunctions().forEach(lmf -> lmf.setExiting(false));

        clearExitStatus.run();

        for(LmFunction function : getFunctions())
            if(function.hasAnyTriggers(triggers))
                function.run(context, false);

        clearExitStatus.run();
    }

    private boolean parseGroups() {
        Log.inf("Parsing groups");

        getGroups().clear();

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
                    group.getItems().addAll(
                        Objects.requireNonNull(
                            groupEntry.getValue().getList(String.class), "groupEntry"
                        )
                    );
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

        getPresets().clear();

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

        for(final CommentedConfigurationNode actualPresetNode : LevelledMobs.getInstance()
            .getConfigHandler().getPresetsCfg()
            .getRoot().node("presets")
            .childrenList()
        ) {
            final String identifier = actualPresetNode.node("preset").getString();

            /* assert valid identifier */
            if(identifier == null || identifier.isBlank()) {
                Log.sev("There is a preset in presets.yml with an unknown/invalid identifier, " +
                    "so LevelledMobs is unable to parse it.", true);
                return false;
            }
            if(getFunctions().stream().anyMatch(otherFunction ->
                otherFunction.getIdentifier().equalsIgnoreCase(identifier))
            ) {
                Log.sev("There are two or more presets in presets.yml which share the " +
                    "same identifier, '" + identifier + "'. Presets must have unique " +
                    "identifiers.", true);
                return false;
            }

            /*
            parse preset node

            removes the preset identifier key
             */
            final CommentedConfigurationNode parsedPresetNode = actualPresetNode.copy();
            parsedPresetNode.removeChild("preset");

            /* register parsed preset */
            getPresets().add(new Preset(
                identifier,
                parsedPresetNode,
                actualPresetNode
            ));
        }

        Log.inf("Successfully parsed " + getPresets().size() + " preset(s)");
        return true;
    }

    private boolean parseCustomDrops() {
        Log.inf("Parsing custom drops");

        //TODO getCustomDrops().clear();

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

        getFunctions().clear();

        /*
        Initialise "GodsEye" Presets System

        Recursively walks through the functions node and its child nodes, replaces all 'use-presets'
        declarations.

        Uses a clone/copy of the actual file-synced object so that the presets parser does not
        mess with the user's file.

        [start preset parsing]
         */
        setParsedFunctionsNode(LevelledMobs.getInstance()
            .getConfigHandler().getSettingsCfg()
            .getRoot().node("functions")
            .copy()
        );
        walkNodePresetParse(getParsedFunctionsNode());

        final List<CommentedConfigurationNode> functionNodes = getParsedFunctionsNode().childrenList();

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
            if(!triggersNode.empty()) {
                if(!triggersNode.isList()) {
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
            }

            /*
            [done preset parsing]
             */

            /* call pre parse fun */

            final var functionPreParseEvent = new FunctionPreParseEvent(function);
            Bukkit.getPluginManager().callEvent(functionPreParseEvent);
            if(functionPreParseEvent.isCancelled()) continue;

            /* parse processes */
            if(!(parseProcesses(function)))
                return false;

            /* call post parse fun */
            Bukkit.getPluginManager().callEvent(new FunctionPostParseEvent(function));

            getFunctions().add(function);
        }

        Log.inf("Successfully parsed " + getFunctions().size() + " function(s).");
        return true;
    }

    private void walkNodePresetParse(final @NotNull CommentedConfigurationNode node) {
        if(node.hasChild("use-presets")) {
            final List<String> presetIds;
            try {
                presetIds = node.node("use-presets").getList(String.class);
                if(presetIds == null) {
                    throw new NullPointerException("presetIds is null");
                }
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
            node.removeChild("use-presets");

            for(final String presetId : presetIds) {
                for(final Preset preset : getPresets()) {
                    if(!preset.getIdentifier().equals(presetId)) continue;

                    node.mergeFrom(preset.getParsedNode());
                }
            }
        }

        node.childrenList().forEach(this::walkNodePresetParse);
        node.childrenMap().values().forEach(this::walkNodePresetParse);
    }

    private boolean parseProcesses(final @NotNull LmFunction function) {
        Objects.requireNonNull(function, "function");

        function.getProcesses().clear();

        final List<CommentedConfigurationNode> processNodes = function
            .getNode().node("processes").childrenList();

        for(var processNode : processNodes) {
            final String identifier = processNode.node("process").getString();
            final String description = processNode.node("description").getString("");
            final long delay = Math.max(processNode.node("delay").getInt(0), 0);

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
            final Process process =
                new Process(identifier, description, processNode, function, delay);

            /* call pre-parse process event */
            final ProcessPreParseEvent processPreParseEvent = new ProcessPreParseEvent(process);
            Bukkit.getPluginManager().callEvent(processPreParseEvent);
            if(processPreParseEvent.isCancelled()) continue;

            /* add presets */
            if(processNode.hasChild("presets")) {
                final CommentedConfigurationNode presetsNode = processNode.node("presets");
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
                for(final String presetIdentifier : presetsIdentifiersList) {
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

                    throw new IllegalArgumentException("Unknown preset '" + presetIdentifier + "'");
                }
            }

            /* parse actions */
            parseActions(process);
            parseConditions(process);

            /* call post-parse process event */
            Bukkit.getPluginManager().callEvent(new ProcessPostParseEvent(process));
            function.getProcesses().add(process);
        }

        return true;
    }

    private void parseActions(final @NotNull Process process) {
        Objects.requireNonNull(process, "process");

        process.getActions().clear();

        final List<CommentedConfigurationNode> actionNodes = process.getNode().node("do").childrenList();

        for(var actionNode : actionNodes) {
            if(actionNode.hasChild("action")) {
                final String identifier = actionNode.node("action").getString("");

                if(identifier.isBlank()) {
                    Log.sev(String.format(
                        "Process '%s' in function '%s' has an action with an invalid identifier.",
                        process.getIdentifier(), process.getParentFunction().getIdentifier()
                    ), true);
                    return;
                }

                final ActionParseEvent actionParseEvent =
                    new ActionParseEvent(identifier, process, actionNode);

                Bukkit.getPluginManager().callEvent(actionParseEvent);

                if(!actionParseEvent.isClaimed()) {
                    throw new IllegalArgumentException(
                        "Unknown action '%s' at path '%s'.".formatted(identifier, actionNode.path())
                    );
                }
            } else {
                throw new IllegalArgumentException(
                    "An item was declared in the actions list which is not an action at path '%s'."
                        .formatted(actionNode.path())
                );
            }
        }

    }

    private void parseConditions(final @NotNull Process process) {
        Objects.requireNonNull(process, "process");

        process.getConditions().clear();

        final var allConditionsNode = process.getNode().node("if");

        if(allConditionsNode.empty()) {
            // allow processes with no conditions.
            // processes with no conditions always run when called.
            return;
        }

        final List<CommentedConfigurationNode> conditionNodes = allConditionsNode.childrenList();

        for(var conditionNode : conditionNodes) {
            if(conditionNode.hasChild("condition")) {
                final String identifier = conditionNode.node("condition").getString("");

                if(identifier.isBlank()) {
                    throw new IllegalArgumentException(
                        "Invalid condition ID specified at path '%s'.".formatted(
                            conditionNode.path()
                        )
                    );
                }

                final ConditionParseEvent conditionParseEvent =
                    new ConditionParseEvent(identifier, process, conditionNode);

                Bukkit.getPluginManager().callEvent(conditionParseEvent);

                if(!conditionParseEvent.isClaimed()) {
                    throw new IllegalArgumentException(
                        "Unknown condition '%s' at path '%s'.".formatted(
                            identifier, conditionNode.path()
                        )
                    );
                }
            } else {
                throw new IllegalArgumentException(
                    "An item was declared in the conditions list "
                        + "which is not a condition at path '%s'.".formatted(
                            conditionNode.path()
                    )
                );
            }
        }

    }

    @Nonnull
    public static String replacePapiAndContextPlaceholders(
        final @Nullable String str,
        final @Nonnull Context context
    ) {
        Objects.requireNonNull(context, "context");

        if(str == null || str.isBlank()) {
            return "";
        }

        final String contextReplaced = context.replacePlaceholders(str);

        if(contextReplaced.contains("%") && Bukkit.getPluginManager()
            .isPluginEnabled("PlaceholderAPI")
        ) {
            return PlaceholderAPI.setPlaceholders(context.getPlayer(), contextReplaced);
        } else {
            return contextReplaced;
        }
    }

    /* getters and setters */

    @NotNull
    public ContextPlaceholderHandler getContextPlaceholderHandler() {
        return contextPlaceholderHandler;
    }

    @NotNull
    public HashSet<Group> getGroups() { return groups; }

    @NotNull
    public HashSet<Preset> getPresets() { return presets; }

    @NotNull
    public LinkedHashSet<LmFunction> getFunctions() { return functions; }

    @NotNull
    public CommentedConfigurationNode getParsedFunctionsNode() {
        return this.parsedFunctionsNode;
    }

    private void setParsedFunctionsNode(final @NotNull CommentedConfigurationNode node) {
        this.parsedFunctionsNode = node;
    }
}
