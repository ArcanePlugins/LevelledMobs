package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl.setbuffs;

import static io.github.arcaneplugins.levelledmobs.bukkit.debug.DebugCategory.BUFFS;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

/*
TODO Javadoc
 */
public class SetBuffsAction extends Action {

    /*
    TODO: Add buffs
        - CUSTOM_RANGED_ATTACK_DAMAGE
        - CUSTOM_CREEPER_BLAST_DAMAGE
        - CUSTOM_ITEM_DROP
        - CUSTOM_XP_DROP
     */

    @Nonnull
    private final Set<Buff> buffs = new HashSet<>();
    private final boolean enabled;


    /*
    TODO Javadoc
     */
    public SetBuffsAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        Log.debug(BUFFS, () -> "Initialized SetBuffsAction @ " + getActionNode().path());

        this.enabled = getActionNode().node("enabled").getBoolean(true);
        if(!isEnabled()) return;

        for(final CommentedConfigurationNode buffNode : getActionNode()
            .node("buffs").childrenList()
        ) {
            Log.debug(BUFFS, () -> "Parsing buff @ " + buffNode.path());
            getBuffs().add(new Buff(buffNode));
        }

        Log.debug(BUFFS, () -> "Parsed " + getBuffs().size() + " buffs");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(Context context) {
        if(!isEnabled()) { return; }

        if(context.getEntity() == null) {
            throw new IllegalStateException(
                "SetBuffsAction at path '" + getActionNode().path() + "' attempted to run " +
                    "without an entity context."
            );
        }
        if(!(context.getEntity() instanceof final LivingEntity entity)) {
            throw new IllegalStateException(
                "SetBuffsAction at path '" + getActionNode().path() + "' attempted to run " +
                    "without a (living) entity context."
            );
        }

        getBuffs().forEach(buff -> buff.apply(context, entity));
    }

    @Nonnull
    public Set<Buff> getBuffs() {
        return buffs;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
