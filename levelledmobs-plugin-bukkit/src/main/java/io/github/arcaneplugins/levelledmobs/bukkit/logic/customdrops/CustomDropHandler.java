package io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops;

import io.github.arcaneplugins.levelledmobs.bukkit.api.data.EntityDataUtil;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.DropTableRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.EntityTypeRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.customdrops.recipient.MobGroupRecipient;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.group.Group;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

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
        final @Nonnull LivingEntity entity
    ) {
        Objects.requireNonNull(entity, "entity");

        final List<CustomDrop> cds = new LinkedList<>();

        if(!EntityDataUtil.isLevelled(entity, true)) return cds;

        // stage 1: add entity-type drops
        cds.addAll(getDefinedCustomDropsForEntityType(entity.getType()));

        // stage 2: get drop-table drops
        //TODO get the drop tables applied to the entity thru a LmFunction
        //TODO in lm3 that's called 'usedroptableid'

        return cds;
    }

    public static @Nonnull Collection<CustomDrop> getDefinedCustomDropsForEntityType(
        final @Nonnull EntityType entityType
    ) {
        final Collection<CustomDrop> applicableCds = new LinkedList<>();

        DROP_TABLE_RECIPIENTS.forEach((recip) -> {
            if(recip.getApplicableEntityTypes().contains(entityType)) {
                applicableCds.addAll(recip.getDrops());
            }
        });

        ENTITY_TYPE_RECIPIENTS.forEach((recip) -> {
            if(recip.getEntityType() == entityType) {
                applicableCds.addAll(recip.getDrops());
            }
        });

        MOB_GROUP_RECIPIENTS.forEach((recip) -> {
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
        });

        return applicableCds;
    }

}
