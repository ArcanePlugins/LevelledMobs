package io.github.arcaneplugins.levelledmobs.bukkit.listener.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.listener.ListenerWrapper;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.ConditionParseEvent;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.ChanceCondition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityBiomeCondition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityCustomNameContains;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityLevelCondition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityOwnerCondition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityTypeCondition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.EntityWorldCondition;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl.PlayerWorldCondition;
import java.util.Locale;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class ConditionParseListener extends ListenerWrapper {

    /* constructor */

    public ConditionParseListener() {
        super(true);
    }

    /* methods */

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConditionParse(final ConditionParseEvent event) {
        final Process process = event.getProcess();
        final CommentedConfigurationNode node = event.getNode();

        switch(event.getIdentifier().toLowerCase(Locale.ROOT)) {
            case "chance" ->
                addCondition(event, new ChanceCondition(process, node));
            case "entity-biome" ->
                addCondition(event, new EntityBiomeCondition(process, node));
            case "entity-custom-name-contains" ->
                addCondition(event, new EntityCustomNameContains(process, node));
            case "entity-level" ->
                addCondition(event, new EntityLevelCondition(process, node));
            case "entity-owner" ->
                addCondition(event, new EntityOwnerCondition(process, node));
            case "entity-type" ->
                addCondition(event, new EntityTypeCondition(process, node));
            case "entity-world" ->
                addCondition(event, new EntityWorldCondition(process, node));
            case "player-world" ->
                addCondition(event, new PlayerWorldCondition(process, node));
        }
    }

    private void addCondition(final ConditionParseEvent event, final Condition condition) {
        event.getProcess().getConditions().add(condition);
        event.setClaimed(true);
    }
}
