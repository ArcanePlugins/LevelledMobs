package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.DROPS_FILTRATION_BY_GROUP;
import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.DROPS_GENERIC;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.DropTableRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.EntityTypeRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.MobGroupRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.type.ItemCustomDrop;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.NotNull;

public class CustomDropHandler {

    private CustomDropHandler() throws IllegalAccessException {
        throw new IllegalAccessException("Illegal instantiation of utility class");
    }

    public static final Collection<DropTableRecipient> DROP_TABLE_RECIPIENTS = new HashSet<>();
    public static final Collection<EntityTypeRecipient> ENTITY_TYPE_RECIPIENTS = new HashSet<>();
    public static final Collection<MobGroupRecipient> MOB_GROUP_RECIPIENTS = new HashSet<>();

    public static void clearCustomDropRecipients() {
        DROP_TABLE_RECIPIENTS.clear();
        ENTITY_TYPE_RECIPIENTS.clear();
        MOB_GROUP_RECIPIENTS.clear();
    }

    public static @Nonnull List<CustomDrop> getDefinedCustomDropsForEntity(
        final @Nonnull LivingEntity entity,
        final @Nonnull Context context
    ) {
        Objects.requireNonNull(entity, "entity");

        Log.debug(DROPS_GENERIC, () -> "getDefinedCustomDropsForEntity BEGIN");

        final List<CustomDrop> cds = new LinkedList<>();

        if(!EntityDataUtil.isLevelled(entity, true)) return cds;
        Log.debug(DROPS_GENERIC, () -> "Entity is levelled (OK)");

        /*
        Stage 1
        Retrieve Entity-Type Drops
         */
        Log.debug(DROPS_GENERIC, () -> "Adding entity-type drops (size=" + cds.size() + ")");
        cds.addAll(getDefinedCustomDropsForEntityType(entity.getType(), context));

        /*
        Stage 2
        Retrieve Drop-Table Drops
         */
        Log.debug(DROPS_GENERIC, () -> "Getting drop-table drops (size=" + cds.size() + ")");
        //TODO get the drop tables applied to the entity thru a LmFunction
        //TODO in lm3 that's called 'usedroptableid'

        /*
        Stage 3
        Filtration, Part 1
         */
        filterCustomDropsByConditions(cds, entity, context);

        /*
        Stage 4
        Filtration, Part 2
         */
        filterCustomDropsByDropGroup(cds);

        Log.debug(DROPS_GENERIC, () -> "getDefinedCustomDropsForEntity DONE (size=" + cds.size() + ")");

        return cds;
    }

    private static void filterCustomDropsByConditions(
        final @NotNull List<CustomDrop> cds,
        final @NotNull LivingEntity entity,
        final @NotNull Context context
    ) {
        Objects.requireNonNull(cds, "customDrops");

        Log.debug(DROPS_GENERIC, () -> "Filtration (size=" + cds.size() + ")" + Log.DEBUG_I_AM_BLIND_SUFFIX);

        final int level = Objects.requireNonNull(
            EntityDataUtil.getLevel(entity, true),
            "level"
        );

        final boolean wasSpawnedByMobSpawner = Objects.requireNonNullElse(
            EntityDataUtil.getSpawnReason(entity, true),
            SpawnReason.NATURAL
        ) == SpawnReason.SPAWNER;

        final @Nullable Player player = context.getPlayer();

        Log.debug(DROPS_GENERIC, () -> "Entity was spawned by Mob Spawner: " + wasSpawnedByMobSpawner);
        Log.debug(DROPS_GENERIC, () -> "Any custom drop contains no-spawner: " +
            cds.stream().anyMatch(CustomDrop::requiresNoSpawner));

        // min and max levels
        cds.removeIf(cd -> cd.getEntityMinLevel() != null && cd.getEntityMinLevel() > level);
        cds.removeIf(cd -> cd.getEntityMaxLevel() != null && cd.getEntityMaxLevel() < level);

        // no-spawner
        cds.removeIf(cd -> wasSpawnedByMobSpawner && cd.requiresNoSpawner());

        // required permissions
        if(player != null) {
            cds.removeIf(cd ->
                cd.getRequiredPermissions().stream().anyMatch(perm -> !player.hasPermission(perm))
            );
        }

        // formula condition
        cds.removeIf(cd -> cd.getFormulaCondition() != null &&
            LogicHandler.evaluateExpression(
                LogicHandler.replacePapiAndContextPlaceholders(
                    cd.getFormulaCondition(),
                    context
                )
            ) != 1.0d
        );
    }

    private static void filterCustomDropsByDropGroup(
        final @NotNull List<CustomDrop> cds
    ) {
        Objects.requireNonNull(cds, "customDrops");

        Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "BEGIN Filtering drops by drop group.");
        Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "Starting out with " + cds.size() + " drops.");

        // This map is used to group drops by their drop group ID.
        final Map<String, List<CustomDrop>> dropGroupDropsMap = new HashMap<>();

        final Runnable debugDropsOverview = () -> Log.debug(DROPS_FILTRATION_BY_GROUP, () -> {
            final StringBuilder sb = new StringBuilder("\n*** START Debug Drops Overview ***\n");

            for(final Entry<String, List<CustomDrop>> dropGroupDropsEntry
                : dropGroupDropsMap.entrySet()
            ) {
                final String gid = dropGroupDropsEntry.getKey();
                final List<CustomDrop> gdrops = dropGroupDropsEntry.getValue();

                sb.append("* • Group '")
                    .append(gid)
                    .append("' contains (")
                    .append(gdrops.size())
                    .append("x):\n");

                for(final CustomDrop gdrop : gdrops) {
                    sb.append("*    • ")
                        .append(((ItemCustomDrop) gdrop).getMaterial())
                        .append(" \tpriority=")
                        .append(gdrop.getPriority())
                        .append(" \tshuffle=")
                        .append(gdrop.shouldShuffle())
                        .append(".\n");
                }
            }

            sb.append("*** DONE Debug Drops Overview ***");

            return sb.toString();
        });

        /*
        Step 1 - Group Drops by Drop Group ID
         */
        Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "Grouping drops by Drop Group ID.");
        for(final CustomDrop cd : cds) {
            final String dropGroupId = cd.getDropGroupId();

            if(!dropGroupDropsMap.containsKey(dropGroupId)) {
                dropGroupDropsMap.put(dropGroupId, new LinkedList<>());
            }

            dropGroupDropsMap.get(dropGroupId).add(cd);
        }
        debugDropsOverview.run();

        /*
        Step 2 - Sort Drops by Priority
         */
        Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "Sorting drops by priority.");
        for(final String dropGroupId : dropGroupDropsMap.keySet()) {
            dropGroupDropsMap.get(dropGroupId)
                .sort(Collections.reverseOrder(Comparator.comparingInt(CustomDrop::getPriority)));
        }
        debugDropsOverview.run();

        /*
        Step 3 - Shuffling
         */
        Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "Shuffling.");
        for(final List<CustomDrop> drops : dropGroupDropsMap.values()) {
            // store drops in a separate list for shuffling
            final List<CustomDrop> shuffledDrops = new LinkedList<>();

            // indexes are associated with a drop priority
            final Map<Integer, List<Integer>> shuffledIndexes = new HashMap<>();

            final CustomDrop[] dropsArray = drops.toArray(new CustomDrop[0]);
            for(int index = 0; index < dropsArray.length; index++) {
                final CustomDrop drop = dropsArray[index];

                if(!drop.shouldShuffle()) continue;

                shuffledDrops.add(drop);

                final int priority = drop.getPriority();
                if(!shuffledIndexes.containsKey(priority)) {
                    shuffledIndexes.put(priority, new LinkedList<>());
                }

                shuffledIndexes.get(priority).add(index);
            }

            for(final Entry<Integer, List<Integer>> indexEntry: shuffledIndexes.entrySet())
                Collections.shuffle(indexEntry.getValue());

            for(final CustomDrop shuffledDrop : shuffledDrops) {
                final List<Integer> indexes = shuffledIndexes.get(shuffledDrop.getPriority());
                final int index = indexes.get(0);
                indexes.remove(0);
                drops.set(index, shuffledDrop);
            }
        }
        debugDropsOverview.run();

        /*
        Step 4 - Max Drops per Drop Group
         */
        Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "Trimming drops by Max Drops per Group.");
        for(final Entry<String, List<CustomDrop>> entry : dropGroupDropsMap.entrySet()) {
            final String gid = entry.getKey();
            final List<CustomDrop> drops = entry.getValue();

            // determine max drops in drop group
            // this uses the smallest value found of all of the drops in that group
            Integer maxDropsInGroup = null;
            for(final CustomDrop drop : drops) {
                final Integer maxDropsInGroupForDrop = drop.getMaxDropsInGroup();
                if(maxDropsInGroupForDrop == null) continue;

                if(maxDropsInGroup == null || maxDropsInGroupForDrop < maxDropsInGroup)
                    maxDropsInGroup = maxDropsInGroupForDrop;
            }

            final Integer maxDropsInGroupFinal = maxDropsInGroup;
            Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "MaxDropsInGroup=" + maxDropsInGroupFinal
                + " for GroupId='" + gid + "'.");

            while(maxDropsInGroup != null && drops.size() > maxDropsInGroup) {
                drops.remove(drops.size() - 1);
            }
        }
        debugDropsOverview.run();

        /*
        Step 5 - Update Custom Drops List With Filtration Results
         */
        Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "Updating Custom Drops List with results.");
        cds.clear();
        dropGroupDropsMap.values().forEach(cds::addAll);

        Log.debug(DROPS_FILTRATION_BY_GROUP, () -> "Finishing with " + cds.size() + " drops.");
    }

    public static @Nonnull List<CustomDrop> getDefinedCustomDropsForEntityType(
        final @Nonnull EntityType entityType,
        final @Nonnull Context context
    ) {
        final List<CustomDrop> applicableCds = new LinkedList<>();

        final Function<CustomDropRecipient, Boolean> doesDropTableNotApply = recip -> {
            Log.debug(DROPS_GENERIC, () ->
                "doesDropTableNotApply BEGIN: " + recip.getClass().getSimpleName()
            );

            // check overall permissions
            Log.debug(DROPS_GENERIC, () -> "checking overall permissions");
            if(!recip.getOverallPermissions().isEmpty()) {
                Log.debug(DROPS_GENERIC, () -> "overall permissions is not empty");
                final Player player = context.getPlayer();
                Log.debug(DROPS_GENERIC, () -> "has player context: " + (player != null));
                if(player == null) return true;
                for(final String overallPermission : recip.getOverallPermissions()) {
                    if(!player.hasPermission(overallPermission)) {
                        Log.debug(DROPS_GENERIC, () -> player.getName() + " doesn't have perm: " +
                            overallPermission + "; not applying drop table.");
                        return true;
                    }
                }
            }
            Log.debug(DROPS_GENERIC, () -> "overall permissions check passed (OK)");

            // check overall chance
            Log.debug(DROPS_GENERIC, () -> "checking overall chance");
            final float overallChance = recip.getOverallChance();
            Log.debug(DROPS_GENERIC, () -> "overallChance=" + overallChance);
            if(overallChance != 100f) {
                final float randomChance =
                    ThreadLocalRandom.current().nextFloat(0, 100);

                Log.debug(DROPS_GENERIC, () -> "randomChance=" + randomChance);

                final boolean chanceUnsatisfied = overallChance < randomChance;

                Log.debug(DROPS_GENERIC, () -> "chance satisfied: " + !chanceUnsatisfied);

                return chanceUnsatisfied;
            }
            Log.debug(DROPS_GENERIC, () -> "overall chance check passed (OK)");

            Log.debug(DROPS_GENERIC, () -> "doesDropTableNotApply: DONE (OK)");

            return false;
        };

        for(final DropTableRecipient recip : DROP_TABLE_RECIPIENTS) {
            if(!recip.getApplicableEntityTypes().contains(entityType)) continue;
            if(doesDropTableNotApply.apply(recip)) continue;
            applicableCds.addAll(recip.getDrops());
        }

        for(final EntityTypeRecipient recip : ENTITY_TYPE_RECIPIENTS) {
            if(recip.getEntityType() != entityType) continue;
            if(doesDropTableNotApply.apply(recip)) continue;
            applicableCds.addAll(recip.getDrops());
        }

        for(final MobGroupRecipient recip : MOB_GROUP_RECIPIENTS) {
            if(doesDropTableNotApply.apply(recip)) continue;

            final Optional<Group> groupOpt = LogicHandler.getGroups().stream()
                .filter(g -> g.getIdentifier().equalsIgnoreCase(recip.getMobGroupId()))
                .findFirst();

            groupOpt.ifPresent(group -> {
                for(final String groupItem : group.getItems()) {
                    if(groupItem.equalsIgnoreCase(entityType.name())) {
                        applicableCds.addAll(recip.getDrops());
                        return;
                    }
                }
            });
        }

        return applicableCds;
    }

}
