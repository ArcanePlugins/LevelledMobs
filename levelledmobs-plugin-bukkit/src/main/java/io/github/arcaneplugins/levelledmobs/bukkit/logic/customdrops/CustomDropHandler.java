package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.DROPS;
import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.UNKNOWN;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.CustomDropRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.DropTableRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.EntityTypeRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.MobGroupRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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

        Log.debug(DROPS, () -> "getDefinedCustomDropsForEntity BEGIN");

        final List<CustomDrop> cds = new LinkedList<>();

        if(!EntityDataUtil.isLevelled(entity, true)) return cds;
        Log.debug(DROPS, () -> "Entity is levelled (OK)");

        // stage 1: add entity-type drops
        Log.debug(DROPS, () -> "Adding entity-type drops (size=" + cds.size() + ")");
        cds.addAll(getDefinedCustomDropsForEntityType(entity.getType(), context));

        // stage 2: get drop-table drops
        Log.debug(DROPS, () -> "Getting drop-table drops (size=" + cds.size() + ")");
        //TODO get the drop tables applied to the entity thru a LmFunction
        //TODO in lm3 that's called 'usedroptableid'

        /*
        Stage 3
        Filtration

        If this area becomes too large, best it is moved to a separate method.
         */
        Log.debug(DROPS, () -> "Filtration (size=" + cds.size() + ")" + Log.DEBUG_I_AM_BLIND_SUFFIX);

        final int level = Objects.requireNonNull(
            EntityDataUtil.getLevel(entity, true),
            "level"
        );

        final boolean wasSpawnedByMobSpawner = Objects.requireNonNullElse(
            EntityDataUtil.getSpawnReason(entity, true),
            UNKNOWN
        ) == SpawnReason.SPAWNER;

        final @Nullable Player player = context.getPlayer();

        Log.debug(DROPS, () -> "Entity was spawned by Mob Spawner: " + wasSpawnedByMobSpawner);
        Log.debug(DROPS, () -> "Any custom drop contains no-spawner: " +
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

        cds.forEach(cd -> {
            final String formula = cd.getFormulaCondition();
            if(formula == null) return;
            Log.debug(DROPS, () -> "--- Formula Condition ---");
            Log.debug(DROPS, () -> "Formula: " + formula);
            Log.debug(DROPS, () -> "Evaluation: " +
                LogicHandler.evaluateExpression(
                    LogicHandler.replacePapiAndContextPlaceholders(
                        formula,
                        context
                    )
                )
            );
            Log.debug(DROPS, () -> "--- done ---");
        });

        // formula condition
        cds.removeIf(cd -> cd.getFormulaCondition() != null &&
            LogicHandler.evaluateExpression(
                LogicHandler.replacePapiAndContextPlaceholders(
                    cd.getFormulaCondition(),
                    context
                )
            ) != 1.0d
        );

        Log.debug(DROPS, () -> "getDefinedCustomDropsForEntity DONE (size=" + cds.size() + ")");

        return cds;
    }

    public static @Nonnull Collection<CustomDrop> getDefinedCustomDropsForEntityType(
        final @Nonnull EntityType entityType,
        final @Nonnull Context context
    ) {
        final Collection<CustomDrop> applicableCds = new LinkedList<>();

        final Function<CustomDropRecipient, Boolean> doesDropTableNotApply = recip -> {
            Log.debug(DROPS, () ->
                "doesDropTableNotApply BEGIN: " + recip.getClass().getSimpleName()
            );

            // check overall permissions
            Log.debug(DROPS, () -> "checking overall permissions");
            if(!recip.getOverallPermissions().isEmpty()) {
                Log.debug(DROPS, () -> "overall permissions is not empty");
                final Player player = context.getPlayer();
                Log.debug(DROPS, () -> "has player context: " + (player != null));
                if(player == null) return true;
                for(final String overallPermission : recip.getOverallPermissions()) {
                    if(!player.hasPermission(overallPermission)) {
                        Log.debug(DROPS, () -> player.getName() + " doesn't have perm: " +
                            overallPermission + "; not applying drop table.");
                        return true;
                    }
                }
            }
            Log.debug(DROPS, () -> "overall permissions check passed (OK)");

            // check overall chance
            Log.debug(DROPS, () -> "checking overall chance");
            final float overallChance = recip.getOverallChance();
            Log.debug(DROPS, () -> "overallChance=" + overallChance);
            if(overallChance != 100f) {
                final float randomChance =
                    ThreadLocalRandom.current().nextFloat(0, 100);

                Log.debug(DROPS, () -> "randomChance=" + randomChance);

                final boolean chanceUnsatisfied = overallChance < randomChance;

                Log.debug(DROPS, () -> "chance satisfied: " + !chanceUnsatisfied);

                return chanceUnsatisfied;
            }
            Log.debug(DROPS, () -> "overall chance check passed (OK)");

            Log.debug(DROPS, () -> "doesDropTableNotApply: DONE (OK)");

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
