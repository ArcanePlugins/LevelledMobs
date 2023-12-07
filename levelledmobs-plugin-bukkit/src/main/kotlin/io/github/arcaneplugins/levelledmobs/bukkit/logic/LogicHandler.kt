package io.github.arcaneplugins.levelledmobs.bukkit.logic

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.config.groups.InbuiltGroup
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.placeholder.ContextPlaceholderHandler
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.CustomDropHandler
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.DropTableRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.EntityTypeRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.MobGroupRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.CommandCustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.FunctionPostParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.FunctionPreParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.ProcessPostParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.ProcessPreParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.ActionParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.ConditionParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.GroupPostParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.GroupPreParseEvent
import io.github.arcaneplugins.levelledmobs.bukkit.logic.preset.Preset
import io.github.arcaneplugins.levelledmobs.bukkit.util.EnchantTuple
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.war
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.RangedInt
import io.github.arcaneplugins.levelledmobs.bukkit.util.math.TimeUtils
import io.github.arcaneplugins.levelledmobs.bukkit.util.modal.impl.ModalEntityTypeSet
import java.util.Collections
import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemFlag
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.serialize.SerializationException
import redempt.crunch.Crunch
import redempt.crunch.functional.EvaluationEnvironment
import java.util.function.Consumer
import java.util.function.Predicate
import kotlin.math.max
import kotlin.math.min


object LogicHandler {
    val CRUNCH_EVAL_ENV = EvaluationEnvironment()

    init {
        /*
        Syntax: between(num, lower, upper)

        Returns `1` if `num` is between (inclusive) `lower` and `upper`, else, returning `0`.

        Example: between(7, 1, 5) -> 0
            (7 is not between 1 and 5 (inclusive), so 0 (false) was returned.)
         */
        CRUNCH_EVAL_ENV.addFunction("between", 3
        ) { d: DoubleArray ->
            if (d[0] >= d[1] && d[0] <= d[2]
            ) 1.0 else 0.0
        }

        /*
        Syntax: clamp(num, lower, upper)

        Returns `num` although increasing it / reducing it to be in between (inclusive) the
        `lower` and `upper` bounds.

        Example: clamp(7, 1, 5) -> 5
            (7 is larger than the upper bound of 5, so 5 was returned.)
         */
        CRUNCH_EVAL_ENV.addFunction("clamp", 3
        ) { d: DoubleArray ->
            min(
                d[2],
                max(d[0], d[1])
            )
        }
    }

    val CONTEXT_PLACEHOLDER_HANDLER = ContextPlaceholderHandler()
    val groups = mutableSetOf<Group>()
    val presets = mutableSetOf<Preset>()
    val lmFunctions = mutableListOf<LmFunction>()
    var parsedFunctionsNode: CommentedConfigurationNode? = null

    fun evaluateExpression(expression: String): Double{
        return Crunch.compileExpression(expression, CRUNCH_EVAL_ENV).evaluate()
    }

    fun load(){
        inf("Loading logic system")
        CONTEXT_PLACEHOLDER_HANDLER.load()
        parseGroups()
        parsePresets()
        parseCustomDrops()
        parseFunctions()
        //PacketInterceptorUtil.registerInterceptor()
        //LevelledMobs.lmInstance.getLibLabelHandler().registerListeners()
    }

    fun unload(){
        //inf("Unregistering EntityLabelLib packet interceptor")

        //PacketInterceptorUtil.unregisterInterceptor()
        //LevelledMobs.lmInstance.getLibLabelHandler().unregisterListeners()

//        try {
//            LevelledMobs.lmInstance.getLibLabelHandler().unregisterListeners()
//        } catch (ignored: NullPointerException) { }
    }

    /**
     * Checks all available functions if they have any of the given triggers, and any applicable
     * functions will be ran using the given context.
     *
     * @param context the context given for the function to run with
     * @param triggers a list of trigger IDs, which a function requires at least one match to apply
     */
    fun runFunctionsWithTriggers(
        context: Context,
        triggers: MutableList<String>
    ) {
        debug(DebugCategory.FUNCTIONS_GENERIC) {"Running functions with triggers: $triggers"}

        lmFunctions.forEach { it.exiting = false }

        lmFunctions
            .filter { it.hasAnyTriggers(triggers) }
            .forEach { it.run(context, false) }
    }

    private fun parseGroups(){
        groups.clear()

        val groupMaps = LevelledMobs.lmInstance
            .configHandler.groupsCfg
            .root!!.node("groups")
            .childrenMap()

        for (groupEntry in groupMaps.entries){
            // let's make sure that the entry's key is a string (identifiers must be strings)
            if (groupEntry.key !is String){
                throw IllegalArgumentException(
                    "Found group with an invalid identifier, '{groupEntry.key}'" +
                            ", group identifiers must be strings (text)."
                )
            }

            val group = Group(groupEntry.key as String)

            // let's make sure that all groups have unique identifiers
            if (groups.stream().anyMatch { otherGroup: Group ->
                otherGroup.identifier.equals(group.identifier, ignoreCase = true)
            }) {
                throw java.lang.IllegalArgumentException("Found group with a duplicate identifier, '{group.identifier}'" +
                        ", group identifiers must be unique.")
            }

            // let's call the pre-parse event
            val preParseEvent = GroupPreParseEvent(group)
            Bukkit.getPluginManager().callEvent(preParseEvent)
            if (preParseEvent.isCancelled) continue

            // we've got the green light to parse it now
            if (groupEntry.value.isList){
                // group has a list key
                try{

                }
                catch (ex: SerializationException){
                    war(
                        "Unable to parse group '${group.identifier}': it is " +
                                "highly likely that the user has made a syntax error in the " +
                                "'groups.yml' file. A stack trace will be printed below for " +
                                "debugging purposes."
                    )
                    throw RuntimeException(ex)
                }
            }
            else{
                throw java.lang.IllegalArgumentException(
                    "Unable to parse group '${group.identifier}' as it does " +
                            "not contain a valid list of items."
                )
            }

            // let's add the group to the set of groups
            groups.add(group)

            // let's call the post-parse event
            Bukkit.getPluginManager().callEvent(GroupPostParseEvent(group))

            // add inbuilt groups unless they already exist (and are thus overridden).
            // LM won't call any events for these because they are already parsed.
            mutableListOf(
                InbuiltGroup.PASSIVE_MOBS,
                InbuiltGroup.NEUTRAL_MOBS,
                InbuiltGroup.HOSTILE_MOBS,
                InbuiltGroup.BOSS_MOBS
            ).forEach { inbuiltGroup ->
                run {
                    if (groups.stream().noneMatch { group ->
                            group.identifier == inbuiltGroup.identifier
                        }) {
                        groups.add(inbuiltGroup)
                    }
                }
            }
        }
    }

    private fun parsePresets(){
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

        for (actualPresetNode in LevelledMobs.lmInstance
            .configHandler.presetsCfg
            .root!!.node("presets")
            .childrenList()){

            val identifier = actualPresetNode.node("preset").string

            /* assert valid identifier */
            if (identifier.isNullOrEmpty()){
                throw IllegalArgumentException(
                    "There is a preset in presets.yml with an unknown/invalid identifier, " +
                            "so LevelledMobs is unable to parse it."
                )
            }
            if (lmFunctions.stream().anyMatch { otherFunction ->
                otherFunction.identifier == identifier
                }){
                    throw java.lang.IllegalArgumentException(
                    "There are two or more presets in presets.yml which share the " +
                            "same identifier, '${identifier}'. Presets must have unique " +
                            "identifiers.")
            }

            /*
            parse preset node

            removes the preset identifier key
             */
            val parsedPresetNode: CommentedConfigurationNode = actualPresetNode.copy()
            parsedPresetNode.removeChild("preset")

            /* register parsed preset */
            presets.add(
                Preset(
                    identifier,
                    parsedPresetNode,
                    actualPresetNode
                )
            )
        }
    }

    private fun parseCustomDrops(){
        CustomDropHandler.clearCustomDropRecipients()

        parseDropTableCustomDrops()
        parseEntityTypeCustomDrops()
        parseMobGroupCustomDrops()
    }

    private fun parseDropTableCustomDrops(){
        for (dropTableNode in LevelledMobs.lmInstance
            .configHandler.customDropsCfg
            .root!!.node("drop-tables").childrenList()
        ) {
            var recipient: DropTableRecipient

            try{
                recipient = DropTableRecipient(
//                    Objects.requireNonNull(
//                        dropTableNode.node("drop-table").string!!,
//                        "Drop table at path '${dropTableNode.path()}' does not specify a drop table ID."
//                    )
                    dropTableNode.node("drop-table").string!!,
                    mutableListOf(),
                    dropTableNode.node("overall-chance").getFloat(100.0f),
                    dropTableNode.node("overall-permissions")
                        .getList(String::class.java, Collections.emptyList()),
                    ModalEntityTypeSet.parseNode(dropTableNode.node("entities"))
                )
            }
            catch (ex: SerializationException){
                throw java.lang.RuntimeException(ex)
            }

            recipient.drops.addAll(
                LogicHandler.parseCustomDropsAtSection(dropTableNode.node("drops"), recipient)
            )

            CustomDropHandler.DROP_TABLE_RECIPIENTS.add(recipient)
        }
    }

    private fun parseEntityTypeCustomDrops(){
        for (entityTypeNode in LevelledMobs.lmInstance
            .configHandler.customDropsCfg
            .root!!.node("entities").childrenList()
        ) {
            var recipient: EntityTypeRecipient

            try{
                val entityNodeType = entityTypeNode.node("entity").string
                require(entityTypeNode != null) {
                    "Entity drops list at path '${entityTypeNode.path()}' does not specify an entity type."
                }

                recipient = EntityTypeRecipient(
                    mutableListOf(),
                    entityTypeNode.node("overall-chance").getFloat(100.0f),
                    entityTypeNode.node("overall-permissions")
                        .getList<String>(String::class.java, emptyList<String>()),
                    EntityType.valueOf(entityNodeType!!.uppercase())
                )
            }
            catch (ex: SerializationException){
                throw java.lang.RuntimeException(ex)
            }

            recipient.drops.addAll(
                parseCustomDropsAtSection(entityTypeNode.node("drops"), recipient)
            )

            CustomDropHandler.ENTITY_TYPE_RECIPIENTS.add(recipient)
        }
    }

    private fun parseMobGroupCustomDrops(){
        for (mobGroupNode in LevelledMobs.lmInstance
        .configHandler.customDropsCfg
            .root!!.node("mob-groups").childrenList()
        ) {
            var recipient: MobGroupRecipient

            try{
                val mobGroupName = mobGroupNode.node("mob-group").string
                require(mobGroupName != null){
                    "CustomDrop MobGroup list at path '${mobGroupNode.path()}' does not specify a mob group ID."
                }

                recipient = MobGroupRecipient(
                    mutableListOf(),
                    mobGroupNode.node("overall-chance").getFloat(100.0f),
                    mobGroupNode.node("overall-permissions")
                        .getList(String::class.java, emptyList()),
                    mobGroupName
                )
            }
            catch (ex: SerializationException){
                throw java.lang.RuntimeException(ex)
            }

            recipient.drops.addAll(
                parseCustomDropsAtSection(mobGroupNode.node("drops"), recipient)
            )

            for (cd in parseCustomDropsAtSection(
                mobGroupNode.node("drops"), recipient
            )) {
                recipient.drops.add(cd)
            }

            CustomDropHandler.MOB_GROUP_RECIPIENTS.add(recipient)
        }
    }

    private fun parseCustomDropsAtSection(
        dropsNode: CommentedConfigurationNode,
        recipient: CustomDropRecipient
    ): MutableList<CustomDrop>{
        val customDrops = mutableListOf<CustomDrop>()
        dropsNode.childrenList()
            .forEach { dropNode -> customDrops.add(parseCustomDropAtSection(dropNode, recipient)) }

        return customDrops
    }

    private fun parseCustomDropAtSection(
        dropNode: CommentedConfigurationNode,
        recipient: CustomDropRecipient
    ): CustomDrop{
        val parseCommonAttribs: Consumer<CustomDrop> = Consumer { cd: CustomDrop ->
            run {
                if (dropNode.hasChild("chance"))
                    cd.withChance(
                        max(
                            0.0f, min(
                                100.0f,
                                dropNode.node("chance").float
                            )
                        )
                    )

                if (dropNode.hasChild("min-level"))
                    cd.withEntityMinLevel(dropNode.node("min-level").int)

                if (dropNode.hasChild("max-level"))
                    cd.withEntityMaxLevel(dropNode.node("max-level").int)

                if (dropNode.hasChild("no-spawner"))
                    cd.withNoSpawner(dropNode.node("no-spawner").boolean)

                if (dropNode.hasChild("priority"))
                    cd.withPriority(dropNode.node("priority").int)

                if (dropNode.hasChild("max-drops-in-group"))
                    cd.withMaxDropsInGroup(dropNode.node("max-drops-in-group").int)

                if (dropNode.hasChild("chunk-kill-limited"))
                    cd.withChunkKillLimited(dropNode.node("chunk-kill-limited").boolean)

                if (dropNode.hasChild("drop-group-id"))
                    cd.withDropGroupId(dropNode.node("drop-group-id").string!!)

                if (dropNode.hasChild("overrides-vanilla-drops"))
                    cd.withOverridesVanillaDrops(dropNode.node("overrides-vanilla-drops").boolean)

                if (dropNode.hasChild("overrides-non-vanilla-drops"))
                    cd.withOverridesNonVanillaDrops(dropNode.node("overrides-non-vanilla-drops").boolean)

                if (dropNode.hasChild("formula-condition"))
                    cd.withFormulaCondition(dropNode.node("formula-condition").string)

                if (dropNode.hasChild("shuffle"))
                    cd.withShuffling(dropNode.node("shuffle").boolean)

                try {
                    if (dropNode.hasChild("permissions"))
                        cd.withRequiredPermissions(
                            dropNode.node("permissions")
                                .getList(String::class.java, emptyList())
                        )
                    if (dropNode.hasChild("cause-of-death"))
                        cd.withDeathCauses(
                            dropNode.node("cause-of-death")
                                .getList(String::class.java, emptyList())
                        )
                } catch (ex: SerializationException) {
                    throw java.lang.RuntimeException(ex)
                }
            }
        }

        if(dropNode.hasChild("material")) {
            val icd = ItemCustomDrop(
                Material.valueOf(dropNode.node("material").getString("AIR").uppercase()),
                recipient
            )

            parseCommonAttribs.accept(icd)

            if (dropNode.hasChild("name"))
                icd.withName(dropNode.node("name").string)

            if (dropNode.hasChild("amount")) icd.withAmount(
                RangedInt(dropNode.node("amount").getString("0"))
            )

            if (dropNode.hasChild("custom-model-data"))
                icd.withCustomModelData(dropNode.node("custom-model-data").int)

            if (dropNode.hasChild("no-multiplier"))
                icd.withNoMultiplier(dropNode.node("no-multiplier").boolean)

            if (dropNode.hasChild("durability-loss"))
                icd.withDurabilityLoss(dropNode.node("durability-loss").int)

            if (dropNode.hasChild("only-drop-if-equipped"))
                icd.withOnlyDropIfEquipped(dropNode.node("only-drop-if-equipped").boolean
            )

            // lists are parsed here in the try catch block
            try {
                icd.withItemFlags(
                    dropNode.node("item-flags")
                        .getList(ItemFlag::class.java, emptyList())
                )
            } catch (ex: SerializationException) {
                throw java.lang.RuntimeException(ex)
            }

            debug(DebugCategory.DROPS_GENERIC) { "START parsing enchant tuples" }
            val enchantTuples = mutableListOf<EnchantTuple>()
            for(enchTupleNode in dropNode.node("enchantments").childrenList()
            ) {
                debug(DebugCategory.DROPS_GENERIC) { "Parsing enchant tuple at path " + enchTupleNode.path() }
                val enchantmentId = enchTupleNode.node("enchantment").string
                require(enchantmentId != null) {
                    "No enchantment ID specified at node '${enchTupleNode.node("enchantment").path()}'"
                }
                debug(DebugCategory.DROPS_GENERIC) { "enchantment ID = $enchantmentId" }
                val enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentId.lowercase()))
                require(enchantment != null) { "Invalid enchantment '${enchTupleNode.path()}'." }

                enchantTuples.add(
                    EnchantTuple(
                        enchantment,
                        enchTupleNode.node("chance").getFloat(100.0f),
                        enchTupleNode.node("strength").getInt(1)
                    )
                )
                icd.withEnchantments(enchantTuples)
                debug(DebugCategory.DROPS_GENERIC) { "Enchantment tuples parsed: " + icd.enchantments.size }
                debug(DebugCategory.DROPS_GENERIC) { "DONE parsing enchant tuples" }
            }

            return icd
        }
        else if(dropNode.hasChild("commands")) {
            val commands = mutableListOf<String>()
            val commandsNode = dropNode.node("commands")

            if (commandsNode.isList) {
                try {
                    commands.addAll(
                        commandsNode.getList(String::class.java, emptyList())
                    )
                } catch (ex: ConfigurateException) {
                    throw java.lang.RuntimeException(ex)
                }
            } else {
                val command = commandsNode.string
                require(command != null){"commandsNode"}
                commands.add(command)
            }

            // replace the leading slash in the commands
            for (i in commands.indices) {
                val original = commands[i]
                require(original.startsWith("/")) { "Custom drop commands must begin with a slash: $original" }
                commands[i] = original.substring(1)
            }

            val ccd = CommandCustomDrop(commands, recipient)
            parseCommonAttribs.accept(ccd)

            val runNode = dropNode.node("run")
            if (runNode.isList) {
                try {
                    ccd.withCommandRunEvents(
                        runNode.getList(String::class.java, emptyList())
                    )
                } catch (ex: ConfigurateException) {
                    throw java.lang.RuntimeException(ex)
                }
            } else {
                ccd.withCommandRunEvent(
                    runNode.string!!
                )
            }

            val delayNode = dropNode.node("delay")

            if (!delayNode.virtual()) {
                try {
                    val temp = dropNode.node("delay").get(Any::class.java)
                    ccd.withCommandDelay(TimeUtils.parseTimeToTicks(temp!!))
                } catch (ex: SerializationException) {
                    throw java.lang.RuntimeException(ex)
                }
            }

            return ccd
        } else {
            throw java.lang.IllegalArgumentException(
                """
                Custom drop at node '${dropNode.path()}' does not define a material or list of commands.
                """.trimIndent()
            )
        }
    }

    private fun parseFunctions(){
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
        lmFunctions.clear()
        /*
        Initialise "GodsEye" Presets System

        Recursively walks through the functions node and its child nodes, replaces all 'use-presets'
        declarations.

        Uses a clone/copy of the actual file-synced object so that the presets parser does not
        mess with the user's file.

        [start preset parsing]
         */

        parsedFunctionsNode =
            LevelledMobs.lmInstance
                .configHandler.settingsCfg
                .root!!.node("functions")
                .copy()

        walkNodePresetParse(parsedFunctionsNode!!)

        val functionNodes = parsedFunctionsNode!!.childrenList()
        for(functionNode in functionNodes) {
            val identifier = functionNode.node("function").string
            val description = functionNode.node("description").getString("")

            /* assert valid identifier */

            require(!identifier.isNullOrBlank()) {
                "There is a function in settings.yml with an unknown/invalid identifier, " +
                        "so LevelledMobs is unable to parse it."
            }
            require(!lmFunctions.stream().anyMatch(Predicate<LmFunction> { otherFunction: LmFunction ->
                otherFunction.identifier.equals(identifier,ignoreCase = true)
            })) {
                "There are two or more functions in settings.yml which share the " +
                        "same identifier, '$identifier'. Functions must have unique " +
                        "identifiers."
            }

            /* create obj */
            val function = LmFunction(identifier, description, functionNode)

            /* add triggers */
            val triggersNode = functionNode.node("triggers")
            if (!triggersNode.empty()) {
                if (!triggersNode.isList) {
                    throw java.lang.IllegalArgumentException(
                        "Unable to parse triggers of function '$identifier'" +
                                ": not a valid list of triggers."
                    )
                }
                val triggersList: List<String>
                try {
                    triggersList = triggersNode.getList(String::class.java)!!
                } catch (ex: SerializationException) {
                    sev(
                        ("Unable to parse triggers of function '" + identifier +
                                "'. This is usually caused by a the user creating a syntax error in " +
                                "the settings.yml file. A stack trace will be printed below for debugging " +
                                "purposes.")
                    )
                    throw java.lang.RuntimeException(ex)
                } catch (ex: NullPointerException) {
                    sev(
                        ("Unable to parse triggers of function '" + identifier +
                                "'. This is usually caused by a the user creating a syntax error in " +
                                "the settings.yml file. A stack trace will be printed below for debugging " +
                                "purposes.")
                    )
                    throw java.lang.RuntimeException(ex)
                }

                function.triggers.addAll(triggersList)

            }

            /*
            [done preset parsing]
             */

            /* call pre parse fun */

            val functionPreParseEvent = FunctionPreParseEvent(function)
            Bukkit.getPluginManager().callEvent(functionPreParseEvent)
            if (functionPreParseEvent.cancelled) continue

            /* parse processes */
            parseProcesses(function)

            /* call post parse fun */
            Bukkit.getPluginManager().callEvent(FunctionPostParseEvent(function))

            lmFunctions.add(function)
        }
    }

    private fun walkNodePresetParse(
        node: CommentedConfigurationNode
    ){
        if (node.hasChild("use-presets")) {
            val presetIds: List<String>?
            try {
                presetIds = node.node("use-presets").getList(String::class.java)
                if (presetIds == null) {
                    throw java.lang.NullPointerException("presetIds is null")
                }
            } catch (e: SerializationException) {
                throw java.lang.RuntimeException(e)
            }
            node.removeChild("use-presets")
            for (presetId in presetIds) {
                for (preset in presets) {
                    if (preset.identifier != presetId) continue
                    node.mergeFrom(preset.parsedNode)
                }
            }
        }

        node.childrenList().forEach(Consumer { childNode: CommentedConfigurationNode? ->
            walkNodePresetParse(childNode!!)
        })
        node.childrenMap().values.forEach(Consumer { childNode: CommentedConfigurationNode? ->
            walkNodePresetParse(childNode!!)
        })
    }

    private fun parseProcesses(
        function: LmFunction
    ){
        function.processes.clear()

        val processNodes = function
            .node.node("processes").childrenList()

        for(processNode in processNodes) {
            val identifier = processNode.node("process").string
            val description = processNode.node("description").getString("")
            val delay = max(processNode.node("delay").getInt(0).toDouble(), 0.0).toLong()

            /* assert valid identifier */

            require(!identifier.isNullOrBlank()) {
                ("There is a process in settings.yml with an unknown/invalid " +
                        "identifier in the function '${function.identifier}', so "
                        + "LevelledMobs is unable to parse it.")
            }

            require(!function.processes.stream().anyMatch { otherProcess: Process ->
                otherProcess.identifier.equals(identifier,ignoreCase = true
                )
            }) {
                "There are two or more processes in the function" +
                        " '${function.identifier}' which share the same identifier," +
                        ", '$identifier'. Processes must have unique identifiers."
            }

            /* create obj */

            val process = Process(identifier, description, processNode, function, delay)

            /* call pre-parse process event */
            val processPreParseEvent = ProcessPreParseEvent(process)
            Bukkit.getPluginManager().callEvent(processPreParseEvent)
            if (processPreParseEvent.cancelled) continue

            /* add presets */
            if(processNode.hasChild("presets")) {
                val presetsNode = processNode.node("presets")
                require(presetsNode.isList) {
                    "Unable to parse presets of process '$identifier'" +
                            ": not a valid list of presets."
                }

                var presetsIdentifiersList: List<String>? = null
                try {
                    presetsIdentifiersList = presetsNode.getList(String::class.java)
                } catch (ex: SerializationException) {
                    sev(
                        "Unable to parse presets of process '$identifier'" +
                                ". This is usually caused by a the user creating a syntax error in " +
                                "the settings.yml file. A stack trace will be printed below for debugging " +
                                "purposes."
                    )
                    throw java.lang.RuntimeException(ex)
                } catch (ex: java.lang.NullPointerException) {
                    sev(
                        ("Unable to parse presets of process '$identifier'" +
                                ". This is usually caused by a the user creating a syntax error in " +
                                "the settings.yml file. A stack trace will be printed below for debugging " +
                                "purposes.")
                    )
                    throw java.lang.RuntimeException(ex)
                }

                if (presetsIdentifiersList == null){
                    throw NullPointerException("presetsIdentifiersList was null")
                }

                presetIterator@
                for (presetIdentifier: String in presetsIdentifiersList) {
                    if (process.presets.stream()
                            .anyMatch { otherPreset: Preset -> (otherPreset.identifier == presetIdentifier) }
                    ) {
                        war(
                            "Process '" + identifier + "' has preset '" + presetIdentifier +
                                    "' listed more than once. A preset can only be used at most once per "
                                    + "process.", true
                        )
                        continue
                    }
                    for (otherPreset in presets) {
                        if ((otherPreset.identifier == presetIdentifier)) {
                            process.presets.add(otherPreset)
                            continue@presetIterator
                        }
                    }
                    throw IllegalArgumentException("Unknown preset '$presetIdentifier'")
                }
            }

            /* parse actions */

            parseActions(process)
            parseConditions(process)

            /* call post-parse process event */

            Bukkit.getPluginManager().callEvent(ProcessPostParseEvent(process))
            function.processes.add(process)
        }
    }

    private fun parseActions(
        process: Process
    ){
        process.actions.clear()
        val actionNodes = process.node.node("do").childrenList()

        for (actionNode in actionNodes){
            if(actionNode.hasChild("action")) {
                val identifier = actionNode.node("action").getString("")
                if (identifier.isBlank()) {
                    sev("Process '${process.identifier}' in function '${ process.parentFunction.identifier}' " +
                            "has an action with an invalid identifier.", true)
                    return
                }

                val actionParseEvent = ActionParseEvent(identifier, process, actionNode)
                Bukkit.getPluginManager().callEvent(actionParseEvent)

                require(actionParseEvent.claimed) {
                    "Unknown action '$identifier' at path '${actionNode.path()}'."
                }
            }
            else{
                throw IllegalArgumentException(
                    "An item was declared in the actions list which is not an action at path '${actionNode.path()}'."
                )
            }
        }
    }

    private fun parseConditions(
        process: Process
    ){
        process.conditions.clear()
        val allConditionsNode = process.node.node("if")

        if (allConditionsNode.empty()) {
            // allow processes with no conditions.
            // processes with no conditions always run when called.
            return
        }

        val conditionNodes = allConditionsNode.childrenList()

        for (conditionNode in conditionNodes){
            if(conditionNode.hasChild("condition")) {
                val identifier = conditionNode.node("condition").getString("")

                require(identifier.isNotBlank()) {
                    "Invalid condition ID specified at path '${conditionNode.path()}'."
                }

                val conditionParseEvent = ConditionParseEvent(identifier, process, conditionNode)
                Bukkit.getPluginManager().callEvent(conditionParseEvent)

                require(conditionParseEvent.claimed) {
                    "Unknown condition '$identifier' at path '${conditionNode.path()}'."
                }
            }
            else{
                throw IllegalArgumentException(
                    "An item was declared in the conditions list "
                            + "which is not a condition at path '${conditionNode.path()}'."
                )
            }
        }
    }

    fun replacePapiAndContextPlaceholders(
        str: String?,
        context: Context
    ): String{
        if (str.isNullOrBlank()) {
            return ""
        }

        val contextReplaced: String = context.replacePlaceholders(str)

        return if (contextReplaced.contains("%") && Bukkit.getPluginManager()
                .isPluginEnabled("PlaceholderAPI")
        ) {
            PlaceholderAPI.setPlaceholders(context.player, contextReplaced)
        } else {
            contextReplaced
        }
    }
}