package io.github.arcaneplugins.levelledmobs.bukkit.logic;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.DROPS;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholderHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.DropTableRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.EntityTypeRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.MobGroupRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.CommandCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.FunctionPostParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.FunctionPreParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.ProcessPostParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.ProcessPreParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.ActionParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setpacketlabel.PacketInterceptorUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.ConditionParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.GroupPostParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.GroupPreParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.preset.Preset;
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnchantTuple;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt;
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.TimeUtils;
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;

public final class LogicHandler {

    private LogicHandler() throws IllegalAccessException {
        throw new IllegalAccessException("Illegal instantiation of utility class");
    }

    /* vars */

    public static final EvaluationEnvironment CRUNCH_EVAL_ENV = new EvaluationEnvironment();

    static {
        /*
        Syntax: between(num, lower, upper)

        Returns `1` if `num` is between (inclusive) `lower` and `upper`, else, returning `0`.

        Example: between(7, 1, 5) -> 0
            (7 is not between 1 and 5 (inclusive), so 0 (false) was returned.)
         */
        CRUNCH_EVAL_ENV.addFunction("between", 3,
            (d) -> (d[0] >= d[1] && d[0] <= d[2]) ? 1.0d : 0.0d);

        /*
        Syntax: clamp(num, lower, upper)

        Returns `num` although increasing it / reducing it to be in between (inclusive) the
        `lower` and `upper` bounds.

        Example: clamp(7, 1, 5) -> 5
            (7 is larger than the upper bound of 5, so 5 was returned.)
         */
        CRUNCH_EVAL_ENV.addFunction("clamp", 3,
            (d) -> Math.min(d[2], Math.max(d[0], d[1])));
    }

    private static final ContextPlaceholderHandler CONTEXT_PLACEHOLDER_HANDLER = new ContextPlaceholderHandler();
    private static final HashSet<Group> GROUPS = new HashSet<>();
    private static final HashSet<Preset> PRESETS = new HashSet<>();
    private static final LinkedHashSet<LmFunction> LM_FUNCTIONS = new LinkedHashSet<>();
    private static CommentedConfigurationNode parsedFunctionsNode;

    /* methods */

    public static double evaluateExpression(final String expression) {
        return Crunch.compileExpression(expression, CRUNCH_EVAL_ENV).evaluate();
    }

    /**
    Initialisation - parse functions, presets, groups, etc.
     */
    public static void load() {
        Log.inf("Loading logic system");
        getContextPlaceholderHandler().load();
        parseGroups();
        parsePresets();
        parseCustomDrops();
        parseFunctions();
        PacketInterceptorUtil.registerInterceptor();
        LevelledMobs.getInstance().getLibLabelHandler().registerListeners();
    }

    public static void unload() {
        Log.inf("Unregistering packet interceptor");
        PacketInterceptorUtil.unregisterInterceptor();
        LevelledMobs.getInstance().getLibLabelHandler().unregisterListeners();
    }

    /**
     * Checks all available functions if they have any of the given triggers, and any applicable
     * functions will be ran using the given context.
     *
     * @param context the context given for the function to run with
     * @param triggers a list of trigger IDs, which a function requires at least one match to apply
     */
    public static void runFunctionsWithTriggers(
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

    private static void parseGroups() {
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
                throw new IllegalArgumentException(
                    "Found group with an invalid identifier, '" + groupEntry.getKey() +
                    "', group identifiers must be strings (text).");
            }

            final var group = new Group((String) groupEntry.getKey());

            // let's make sure that all groups have unique identifiers
            if(getGroups().stream().anyMatch(otherGroup ->
                otherGroup.getIdentifier().equalsIgnoreCase(group.getIdentifier()))
            ) {
                throw new IllegalArgumentException(
                    "Found group with a duplicate identifier, '" + group.getIdentifier() +
                    "', group identifiers must be unique.");
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
                    Log.war("Unable to parse group '" + group.getIdentifier() + "': it is " +
                        "highly likely that the user has made a syntax error in the " +
                        "'groups.yml' file. A stack trace will be printed below for " +
                        "debugging purposes.");
                    throw new RuntimeException(ex);
                }
            } else {
                throw new IllegalArgumentException(
                    "Unable to parse group '" + group.getIdentifier() + "' as it does " +
                    "not contain a valid list of items.");
            }

            // let's add the group to the set of groups
            getGroups().add(group);

            // let's call the post-parse event
            Bukkit.getPluginManager().callEvent(new GroupPostParseEvent(group));
        }

        Log.inf("Successfully parsed " + getGroups().size() + " group(s)");
    }

    private static void parsePresets() {
        Log.inf("Parsing presets.");

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
                throw new IllegalArgumentException(
                    "There is a preset in presets.yml with an unknown/invalid identifier, " +
                    "so LevelledMobs is unable to parse it.");
            }
            if(getFunctions().stream().anyMatch(otherFunction ->
                otherFunction.getIdentifier().equalsIgnoreCase(identifier))
            ) {
                throw new IllegalArgumentException(
                    "There are two or more presets in presets.yml which share the " +
                    "same identifier, '" + identifier + "'. Presets must have unique " +
                    "identifiers.");
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

        Log.inf("Successfully parsed " + getPresets().size() + " preset(s).");
    }

    private static void parseCustomDrops() {
        Log.inf("Parsing custom drops.");

        CustomDropHandler.clearCustomDropRecipients();

        parseDropTableCustomDrops();
        parseEntityTypeCustomDrops();
        parseMobGroupCustomDrops();

        Log.inf("Successfully parsed custom drops.");
    }

    private static void parseDropTableCustomDrops() {
        Log.inf("Parsing Drop Table custom drops.");

        for(final CommentedConfigurationNode dropTableNode : LevelledMobs.getInstance()
            .getConfigHandler().getCustomDropsCfg()
            .getRoot().node("drop-tables").childrenList()
        ) {
            final DropTableRecipient recipient;

            try {
                recipient = new DropTableRecipient(
                    Objects.requireNonNull(
                        dropTableNode.node("drop-table").getString(),
                        "Drop table at path '%s' does not specify a drop table ID."
                            .formatted(dropTableNode.path())
                    ),
                    new LinkedList<>(),
                    dropTableNode.node("overall-chance").getFloat(100.0f),
                    dropTableNode.node("overall-permissions")
                        .getList(String.class, Collections.emptyList()),
                    ModalEntityTypeSet.parseNode(dropTableNode.node("entities"))
                );
            } catch(SerializationException ex) {
                throw new RuntimeException(ex);
            }

            recipient.getDrops().addAll(
                parseCustomDropsAtSection(dropTableNode.node("drops"), recipient)
            );

            CustomDropHandler.DROP_TABLE_RECIPIENTS.add(recipient);
        }

        Log.inf("Successfully parsed custom drops for %s drop tables.".formatted(
            CustomDropHandler.DROP_TABLE_RECIPIENTS.size()));
    }

    private static void parseEntityTypeCustomDrops() {
        Log.inf("Parsing Entity Type custom drops.");

        for(final CommentedConfigurationNode entityTypeNode : LevelledMobs.getInstance()
            .getConfigHandler().getCustomDropsCfg()
            .getRoot().node("entities").childrenList()
        ) {
            final EntityTypeRecipient recipient;

            try {
                recipient = new EntityTypeRecipient(
                    new LinkedList<>(),
                    entityTypeNode.node("overall-chance").getFloat(100.0f),
                    entityTypeNode.node("overall-permissions")
                        .getList(String.class, Collections.emptyList()),
                    EntityType.valueOf(
                        Objects.requireNonNull(
                            entityTypeNode.node("entity").getString(),
                            "Entity drops list at path '%s' does not specify an entity type."
                                .formatted(entityTypeNode.path())
                        ).toUpperCase(Locale.ROOT)
                    )
                );
            } catch(SerializationException ex) {
                throw new RuntimeException(ex);
            }

            recipient.getDrops().addAll(
                parseCustomDropsAtSection(entityTypeNode.node("drops"), recipient)
            );

            CustomDropHandler.ENTITY_TYPE_RECIPIENTS.add(recipient);
        }

        Log.inf("Successfully parsed custom drops for %s entity types.".formatted(
            CustomDropHandler.ENTITY_TYPE_RECIPIENTS.size()));
    }

    private static void parseMobGroupCustomDrops() {
        Log.inf("Parsing Mob Group custom drops.");

        for(final CommentedConfigurationNode mobGroupNode : LevelledMobs.getInstance()
            .getConfigHandler().getCustomDropsCfg()
            .getRoot().node("mob-groups").childrenList()
        ) {
            final MobGroupRecipient recipient;

            try {
                recipient = new MobGroupRecipient(
                    new LinkedList<>(),
                    mobGroupNode.node("overall-chance").getFloat(100.0f),
                    mobGroupNode.node("overall-permissions")
                        .getList(String.class, Collections.emptyList()),
                    Objects.requireNonNull(
                        mobGroupNode.node("mob-group").getString(),
                        "CustomDrop MobGroup list at path '%s' does not specify a mob group ID."
                            .formatted(mobGroupNode.path())
                    )
                );
            } catch(SerializationException ex) {
                throw new RuntimeException(ex);
            }

            recipient.getDrops().addAll(
                parseCustomDropsAtSection(mobGroupNode.node("drops"), recipient)
            );

            for(final CustomDrop cd : parseCustomDropsAtSection(
                mobGroupNode.node("drops"), recipient
            )) {
                recipient.getDrops().add(cd);
            }

            CustomDropHandler.MOB_GROUP_RECIPIENTS.add(recipient);
        }

        Log.inf("Successfully parsed custom drops for %s mob groups.".formatted(
            CustomDropHandler.MOB_GROUP_RECIPIENTS.size()));
    }

    private static @Nonnull Collection<? extends CustomDrop> parseCustomDropsAtSection(
        final @Nonnull CommentedConfigurationNode dropsNode,
        final @Nonnull CustomDropRecipient recipient
    ) {
        final Collection<CustomDrop> customDrops = new LinkedList<>();
        dropsNode.childrenList()
            .forEach(dropNode -> customDrops.add(parseCustomDropAtSection(dropNode, recipient)));
        return customDrops;
    }

    private static @Nonnull CustomDrop parseCustomDropAtSection(
        final @Nonnull CommentedConfigurationNode dropNode,
        final @Nonnull CustomDropRecipient recipient
    ) {
        final Consumer<CustomDrop> parseCommonAttribs = (cd) -> {
            cd.withChance(Math.max(0, Math.min(100, dropNode.node("chance").getFloat(100f))));
            if(dropNode.hasChild("min-level")) cd.withEntityMinLevel(dropNode.node("min-level").getInt());
            if(dropNode.hasChild("max-level")) cd.withEntityMaxLevel(dropNode.node("max-level").getInt());
            cd.withNoSpawner(dropNode.node("no-spawner").getBoolean(cd.requiresNoSpawner()));
            cd.withPriority(dropNode.node("priority").getInt(cd.getPriority()));
            if(dropNode.hasChild("max-drops-in-group")) cd.withMaxDropsInGroup(dropNode.node("max-drops-in-group").getInt());
            cd.withChunkKillLimited(dropNode.node("chunk-kill-limited").getBoolean(cd.isChunkKillLimited()));
            cd.withDropGroupId(dropNode.node("drop-group-id").getString(cd.getDropGroupId()));
            cd.withOverridesVanillaDrops(dropNode.node("overrides-vanilla-drops").getBoolean(cd.shouldOverrideVanillaDrops()));
            cd.withOverridesNonVanillaDrops(dropNode.node("overrides-non-vanilla-drops").getBoolean(cd.shouldOverrideVanillaDrops()));
            cd.withFormulaCondition(dropNode.node("formula-condition").getString());

            try {
                cd.withRequiredPermissions(dropNode.node("permissions").getList(String.class, Collections.emptyList()));
                cd.withDeathCauses(dropNode.node("cause-of-death").getList(String.class, Collections.emptyList()));
            } catch(SerializationException ex) {
                throw new RuntimeException(ex);
            }
        };

        if(dropNode.hasChild("material")) {
            final ItemCustomDrop icd = new ItemCustomDrop(
                Material.valueOf(dropNode.node("material")
                    .getString("AIR").toUpperCase(Locale.ROOT)),
                recipient
            );

            parseCommonAttribs.accept(icd);

            if(dropNode.hasChild("name"))
                icd.withName(dropNode.node("name").getString());

            if(dropNode.hasChild("amount"))
                icd.withAmount(
                    new RangedInt(
                        dropNode.node("amount").getString("0")
                    )
                );

            if(dropNode.hasChild("custom-model-data"))
                icd.withCustomModelData(dropNode.node("custom-model-data").getInt());

            if(dropNode.hasChild("no-multiplier"))
                icd.withNoMultiplier(dropNode.node("no-multiplier").getBoolean());

            if(dropNode.hasChild("durability-loss"))
                icd.withDurabilityLoss(dropNode.node("durability-loss").getInt());

            if(dropNode.hasChild("only-drop-if-equipped"))
                icd.withOnlyDropIfEquipped(
                    dropNode.node("only-drop-if-equipped").getBoolean()
                );

            // lists are parsed here in the try catch block
            try {
                icd.withItemFlags(
                    dropNode.node("item-flags")
                        .getList(ItemFlag.class, Collections.emptyList())
                );
            } catch(SerializationException ex) {
                throw new RuntimeException(ex);
            }

            Log.debug(DROPS, () -> "START parsing enchant tuples");
            final Collection<EnchantTuple> enchantTuples = new HashSet<>();
            for(final CommentedConfigurationNode enchTupleNode :
                dropNode.node("enchantments").childrenList()
            ) {
                Log.debug(DROPS, () -> "Parsing enchant tuple at path " + enchTupleNode.path());

                final String enchantmentId = Objects.requireNonNull(
                    enchTupleNode.node("enchantment").getString(),
                    "No enchantment ID specified at node '%s'"
                        .formatted(enchTupleNode.node("enchantment").path())
                );
                Log.debug(DROPS, () -> "enchantment ID = " + enchantmentId);

                enchantTuples.add(new EnchantTuple(
                    Objects.requireNonNull(
                        Enchantment.getByKey(NamespacedKey.minecraft(
                            enchantmentId.toLowerCase(Locale.ROOT)
                        )),
                        "Invalid enchantment '%s'.".formatted(enchTupleNode.path())
                    ),
                    enchTupleNode.node("chance").getFloat(100.0f),
                    enchTupleNode.node("strength").getInt(1)
                ));
            }
            icd.withEnchantments(enchantTuples);
            Log.debug(DROPS, () -> "Enchantment tuples parsed: " + icd.getEnchantments().size());
            Log.debug(DROPS, () -> "DONE parsing enchant tuples");

            return icd;
        } else if(dropNode.hasChild("command") || dropNode.hasChild("commands")) {
            final Collection<String> commands = new LinkedList<>();

            if(dropNode.hasChild("command")) {
                commands.add(Objects.requireNonNull(dropNode.getString()));
            } else {
                try {
                    commands.addAll(dropNode.getList(String.class, Collections.emptyList()));
                } catch(ConfigurateException ex) {
                    throw new RuntimeException(ex);
                }
            }

            final CommandCustomDrop ccd = new CommandCustomDrop(commands, recipient);

            parseCommonAttribs.accept(ccd);

            try {
                ccd.withCommandRunEvents(dropNode.node("run").getList(String.class, Collections.emptyList()));
            } catch(ConfigurateException ex) {
                throw new RuntimeException(ex);
            }

            try {
                ccd.withCommandDelay(TimeUtils.parseTimeToTicks(
                    dropNode.node("delay").get(Object.class, 0L)
                ));
            } catch(SerializationException ex) {
                throw new RuntimeException(ex);
            }

            return ccd;
        } else {
            throw new IllegalArgumentException("""
                Custom drop at node '%s' does not define a material or command."""
                .formatted(dropNode.path()));
        }
    }

    private static void parseFunctions() {
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
                throw new IllegalArgumentException(
                    "There is a function in settings.yml with an unknown/invalid identifier, " +
                    "so LevelledMobs is unable to parse it.");
            }
            if(getFunctions().stream().anyMatch(otherFunction ->
                otherFunction.getIdentifier().equalsIgnoreCase(identifier))
            ) {
                throw new IllegalArgumentException(
                    "There are two or more functions in settings.yml which share the " +
                    "same identifier, '" + identifier + "'. Functions must have unique " +
                    "identifiers.");
            }

            /* create obj */
            final var function = new LmFunction(identifier, description, functionNode);

            /* add triggers */
            final var triggersNode = functionNode.node("triggers");
            if(!triggersNode.empty()) {
                if(!triggersNode.isList()) {
                    throw new IllegalArgumentException(
                        "Unable to parse triggers of function '" + identifier +
                        "': not a valid list of triggers.");
                }
                final List<String> triggersList;
                try {
                    triggersList = Objects.requireNonNull(triggersNode.getList(String.class), "list");
                } catch (SerializationException | NullPointerException ex) {
                    Log.sev("Unable to parse triggers of function '" + identifier +
                        "'. This is usually caused by a the user creating a syntax error in " +
                        "the settings.yml file. A stack trace will be printed below for debugging " +
                        "purposes.");
                    throw new RuntimeException(ex);
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
            parseProcesses(function);

            /* call post parse fun */
            Bukkit.getPluginManager().callEvent(new FunctionPostParseEvent(function));

            getFunctions().add(function);
        }

        Log.inf("Successfully parsed " + getFunctions().size() + " function(s).");
    }

    private static void walkNodePresetParse(final @NotNull CommentedConfigurationNode node) {
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

        node.childrenList().forEach(LogicHandler::walkNodePresetParse);
        node.childrenMap().values().forEach(LogicHandler::walkNodePresetParse);
    }

    private static void parseProcesses(final @NotNull LmFunction function) {
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
                throw new IllegalArgumentException(
                    "There is a process in settings.yml with an unknown/invalid " +
                    "identifier in the function '" + function.getIdentifier() + "', so "
                    + "LevelledMobs is unable to parse it.");
            }
            if(function.getProcesses().stream().anyMatch(otherProcess ->
                otherProcess.getIdentifier().equalsIgnoreCase(identifier))
            ) {
                throw new IllegalArgumentException(
                    "There are two or more processes in the function '" +
                    function.getIdentifier() + "' which share the same identifier, '" +
                    identifier + "'. Processes must have unique identifiers.");
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
                    throw new IllegalArgumentException(
                        "Unable to parse presets of process '" + identifier +
                        "': not a valid list of presets.");
                }
                final List<String> presetsIdentifiersList;
                try {
                    presetsIdentifiersList = Objects.requireNonNull(presetsNode.getList(String.class), "presets");
                } catch (SerializationException | NullPointerException ex) {
                    Log.sev("Unable to parse presets of process '" + identifier +
                        "'. This is usually caused by a the user creating a syntax error in " +
                        "the settings.yml file. A stack trace will be printed below for debugging " +
                        "purposes.");
                    throw new RuntimeException(ex);
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

                    for(final Preset otherPreset : getPresets()) {
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
    }

    private static void parseActions(final @NotNull Process process) {
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

    private static void parseConditions(final @NotNull Process process) {
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
    public static ContextPlaceholderHandler getContextPlaceholderHandler() {
        return CONTEXT_PLACEHOLDER_HANDLER;
    }

    @NotNull
    public static HashSet<Group> getGroups() { return GROUPS; }

    @NotNull
    public static HashSet<Preset> getPresets() { return PRESETS; }

    @NotNull
    public static LinkedHashSet<LmFunction> getFunctions() { return LM_FUNCTIONS; }

    @NotNull
    public static CommentedConfigurationNode getParsedFunctionsNode() {
        return parsedFunctionsNode;
    }

    private static void setParsedFunctionsNode(final @NotNull CommentedConfigurationNode node) {
        parsedFunctionsNode = node;
    }
}
