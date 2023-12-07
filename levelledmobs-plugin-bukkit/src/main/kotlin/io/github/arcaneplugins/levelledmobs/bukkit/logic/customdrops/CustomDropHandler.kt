package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.data.InternalEntityDataUtil
import io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler.groups
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.DropTableRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.EntityTypeRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.MobGroupRecipient
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.debug
import java.util.Collections
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.concurrent.ThreadLocalRandom


object CustomDropHandler {
    val DROP_TABLE_RECIPIENTS  = mutableSetOf<DropTableRecipient>()
    val ENTITY_TYPE_RECIPIENTS = mutableSetOf<EntityTypeRecipient>()
    val MOB_GROUP_RECIPIENTS   = mutableSetOf<MobGroupRecipient>()

    fun clearCustomDropRecipients(){
        DROP_TABLE_RECIPIENTS.clear()
        ENTITY_TYPE_RECIPIENTS.clear()
        MOB_GROUP_RECIPIENTS.clear()
    }

    fun getDefinedCustomDropsForEntity(
        entity: LivingEntity,
        context: Context
    ) : MutableList<CustomDrop>{
        debug(DebugCategory.DROPS_GENERIC) { "getDefinedCustomDropsForEntity BEGIN" }

        val cds = mutableListOf<CustomDrop>()

        if (!EntityDataUtil.isLevelled(entity, true)) return cds
        debug(DebugCategory.DROPS_GENERIC) { "Entity is levelled (OK)" }

        /*
        Stage 1
        Retrieve Entity-Type Drops
         */
        debug(DebugCategory.DROPS_GENERIC) { "Adding entity-type drops (size=${cds.size})" }
        cds.addAll(getDefinedCustomDropsForEntityType(entity.type, context))

        /*
        Stage 2
        Retrieve Drop-Table Drops
         */
        debug(DebugCategory.DROPS_GENERIC) { "Getting drop-table drops (size==${cds.size})" }
        val dropTableIds = InternalEntityDataUtil.getDropTableIds(entity)

        for (dropTableId in dropTableIds) {
            for (recip in DROP_TABLE_RECIPIENTS) {
                if (!recip.id.equals(dropTableId, ignoreCase = true)) continue
                if (doesCustomDropRecipientNotMeetConditions(recip, context)) continue
                cds.addAll(recip.drops)
            }
        }

        /*
        Stage 3
        Filtration, Part 1
         */

        /*
        Stage 3
        Filtration, Part 1
         */
        filterCustomDropsByConditions(cds, entity, context)

        /*
        Stage 4
        Filtration, Part 2
         */

        /*
        Stage 4
        Filtration, Part 2
         */
        filterCustomDropsByDropGroup(cds)

        debug(DebugCategory.DROPS_GENERIC) { "getDefinedCustomDropsForEntity DONE (size=${cds.size})" }

        return cds
    }

    private fun filterCustomDropsByConditions(
        cds: MutableList<CustomDrop>,
        entity: LivingEntity,
        context: Context
    ){
        debug(DebugCategory.DROPS_GENERIC) { "Filtration (size=${cds.size})" }

        val level: Int = EntityDataUtil.getLevel(entity, true)!!
        val wasSpawnedByMobSpawner: Boolean = EntityDataUtil.getSpawnReason(
            entity, true) == CreatureSpawnEvent.SpawnReason.SPAWNER
        val player = context.player

        debug(DebugCategory.DROPS_GENERIC) { "Entity was spawned by Mob Spawner: $wasSpawnedByMobSpawner" }
        debug(DebugCategory.DROPS_GENERIC) {
            "Any custom drop contains no-spawner: " +
                    cds.stream().anyMatch(CustomDrop::noSpawner) }

        // min and max levels
        cds.removeIf { cd: CustomDrop -> cd.entityMinLevel != null && cd.entityMinLevel!! > level }
        cds.removeIf { cd: CustomDrop -> cd.entityMaxLevel != null && cd.entityMaxLevel!! < level }

        // no-spawner
        cds.removeIf { cd: CustomDrop -> wasSpawnedByMobSpawner && cd.noSpawner }

        // required permissions
        if (player != null) {
            cds.removeIf { cd: CustomDrop ->
                cd.requiredPermissions.stream().anyMatch{perm: String -> !player.hasPermission(perm)}
            }
        }

        // formula condition
        cds.removeIf { cd: CustomDrop -> cd.formulaCondition != null &&
            LogicHandler.evaluateExpression(
                LogicHandler.replacePapiAndContextPlaceholders(
                    cd.formulaCondition,
                    context
                )
            ) != 1.0
        }
    }

    fun filterCustomDropsByDropGroup(
        cds: MutableList<CustomDrop>
    ){
        debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) { "BEGIN Filtering drops by drop group." }
        debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) { "Starting out with ${cds.size} drops." }

        // This map is used to group drops by their drop group ID.
        val dropGroupDropsMap = mutableMapOf<String, MutableList<CustomDrop>>()

        val debugDropsOverview = Runnable { debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) {
            val sb = StringBuilder("\n*** START Debug Drops Overview ***\n")

            for (dropGroupDropsEntry in dropGroupDropsMap.entries){
                val gid = dropGroupDropsEntry.key
                val gdrops = dropGroupDropsEntry.value

                sb.append("* • Group '")
                    .append(gid)
                    .append("' contains (")
                    .append(gdrops.size)
                    .append("x):\n")

                for (gdrop in gdrops){
                    sb.append("*    • ")
                        .append((gdrop as ItemCustomDrop).material)
                        .append(" \tpriority=")
                        .append(gdrop.priority)
                        .append(" \tshuffle=")
                        .append(gdrop.shuffle)
                        .append(".\n")
                }
            }

            sb.toString()
        }}

        /*
        Step 1 - Group Drops by Drop Group ID
         */

        /*
        Step 1 - Group Drops by Drop Group ID
         */
        debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) { "Grouping drops by Drop Group ID." }
        for (cd in cds) {
            val dropGroupId = cd.dropGroupId
            if (!dropGroupDropsMap.containsKey(dropGroupId)) {
                dropGroupDropsMap[dropGroupId] = mutableListOf()
            }
            dropGroupDropsMap[dropGroupId]!!.add(cd)
        }
        debugDropsOverview.run()

        /*
        Step 2 - Sort Drops by Priority
         */
        debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) { "Sorting drops by priority." }
        for (dropGroupId in dropGroupDropsMap.keys) {
            dropGroupDropsMap[dropGroupId]
                ?.sortWith(Collections.reverseOrder(Comparator.comparingInt(CustomDrop::priority)))
        }
        debugDropsOverview.run()

        /*
        Step 3 - Shuffling
         */

        debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) { "Shuffling." }
        for (drops in dropGroupDropsMap.values) {
            // store drops in a separate list for shuffling
            val shuffledDrops = mutableListOf<CustomDrop>()

            // indexes are associated with a drop priority
            val shuffledIndexes = mutableMapOf<Int, MutableList<Int>>()
            val dropsArray = drops.toTypedArray<CustomDrop>()

            for (index in 0..dropsArray.size){
                val drop = dropsArray[index]

                if (!drop.shuffle) continue

                shuffledDrops.add(drop)

                val priority = drop.priority
                if (!shuffledIndexes.containsKey(priority)) {
                    shuffledIndexes[priority] = mutableListOf()
                }

                shuffledIndexes[priority]!!.add(index)
            }

            for (indexEntry in shuffledIndexes.entries){
                indexEntry.value.shuffle()
            }

            for (shuffledDrop in shuffledDrops) {
                val indexes = shuffledIndexes[shuffledDrop.priority] ?: continue
                val index = indexes[0]
                indexes.removeAt(0)
                drops[index] = shuffledDrop
            }
        }

        debugDropsOverview.run()

        /*
        Step 4 - Max Drops per Drop Group
         */

        debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) { "Trimming drops by Max Drops per Group." }
        for (entry in dropGroupDropsMap.entries) {
            val gid = entry.key
            val drops: MutableList<CustomDrop> = entry.value

            // determine max drops in drop group
            // this uses the smallest value found of all of the drops in that group
            var maxDropsInGroup: Int? = null
            for(drop in drops) {
                val maxDropsInGroupForDrop = drop.maxDropsInGroup ?: continue

                if (maxDropsInGroup == null || maxDropsInGroupForDrop < maxDropsInGroup) {
                    maxDropsInGroup = maxDropsInGroupForDrop
                }
            }

            val maxDropsInGroupFinal = maxDropsInGroup!!
            debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) {"MaxDropsInGroup=$maxDropsInGroupFinal for GroupId='$gid'."}

            while (drops.size > maxDropsInGroup) {
                drops.removeAt(drops.size - 1)
            }
        }
        debugDropsOverview.run()

        /*
        Step 5 - Update Custom Drops List With Filtration Results
         */

        debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) { "Updating Custom Drops List with results." }
        cds.clear()
        dropGroupDropsMap.values.forEach(cds::addAll)

        debug(DebugCategory.DROPS_FILTRATION_BY_GROUP) { "Finishing with ${cds.size} drops." }
    }

    private fun doesCustomDropRecipientNotMeetConditions(
        recipient: CustomDropRecipient,
        context: Context
    ): Boolean{
        debug(DebugCategory.DROPS_GENERIC) { "doesDropTableNotApply BEGIN: " + recipient.javaClass.getSimpleName() }

        // check overall permissions

        // check overall permissions
        debug(DebugCategory.DROPS_GENERIC) { "checking overall permissions" }
        if (recipient.overallPermissions.isNotEmpty()) {
            debug(DebugCategory.DROPS_GENERIC) { "overall permissions is not empty" }
            val player = context.player
            debug(DebugCategory.DROPS_GENERIC) { "has player context: " + (player != null) }
            if (player == null) return true

            for (overallPermission in recipient.overallPermissions) {
                if (!player.hasPermission(overallPermission)) {
                    debug(DebugCategory.DROPS_GENERIC) {
                        "${player.name} doesn't have perm: $overallPermission; not applying drop table."
                    }
                    return true
                }
            }
        }
        debug(DebugCategory.DROPS_GENERIC) { "overall permissions check passed (OK)" }

        // check overall chance
        debug(DebugCategory.DROPS_GENERIC) { "checking overall chance" }
        val overallChance = recipient.overallChance
        debug(DebugCategory.DROPS_GENERIC) { "overallChance=$overallChance" }
        if (overallChance != 100f) {
            val randomChance = ThreadLocalRandom.current().nextFloat(0f, 100f)
            debug(DebugCategory.DROPS_GENERIC) { "randomChance=$randomChance" }

            val chanceUnsatisfied = overallChance < randomChance
            debug(DebugCategory.DROPS_GENERIC) { "chance satisfied: " + !chanceUnsatisfied }

            return chanceUnsatisfied
        }

        debug(DebugCategory.DROPS_GENERIC) { "overall chance check passed (OK)" }
        debug(DebugCategory.DROPS_GENERIC) { "doesDropTableNotApply: DONE (OK)" }

        return false
    }

    fun getDefinedCustomDropsForEntityType(
        entityType: EntityType,
        context: Context
    ): MutableList<CustomDrop>{
        val applicableCds = mutableListOf<CustomDrop>()

        for (recip in DROP_TABLE_RECIPIENTS) {
            if (!recip.applicableEntities.contains(entityType)) continue
            if (doesCustomDropRecipientNotMeetConditions(recip, context)) continue
            applicableCds.addAll(recip.drops)
        }

        for (recip in ENTITY_TYPE_RECIPIENTS) {
            if (recip.entityType !== entityType) continue
            if (doesCustomDropRecipientNotMeetConditions(recip, context)) continue
            applicableCds.addAll(recip.drops)
        }

        for (recip in MOB_GROUP_RECIPIENTS) {
            if (doesCustomDropRecipientNotMeetConditions(recip, context)) continue
            val groupOpt = groups.stream()
                .filter { g: Group -> g.identifier.equals(recip.mogGroupId,ignoreCase = true) }
                .findFirst()
            groupOpt.ifPresent { group: Group ->
                for (groupItem in group.items) {
                    if (groupItem.equals(entityType.name, ignoreCase = true)) {
                        applicableCds.addAll(recip.drops)
                        return@ifPresent
                    }
                }
            }
        }

        return applicableCds
    }
}