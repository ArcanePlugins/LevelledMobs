package me.lokka30.levelledmobs.bukkit.logic.function.process.condition.impl;

import java.util.Locale;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.ConditionParseEvent;
import me.lokka30.levelledmobs.bukkit.listener.ListenerWrapper;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.Condition;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@SuppressWarnings("RedundantLabeledSwitchRuleCodeBlock")
public class ConditionParseListener extends ListenerWrapper {

    /* constructor */

    public ConditionParseListener() {
        super(true);
    }

    /* methods */

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onConditionParse(final ConditionParseEvent event) {
        final var process = event.getProcess();
        final var node = event.getNode();

        switch(event.getIdentifier().toLowerCase(Locale.ROOT)) {
            case "chance" -> {
                addCondition(event, new ChanceCondition(process, node));
            }
            case "entity-biome", "mob-biome" -> {
                addCondition(event, new EntityBiomeCondition(process, node));
            }
            case "entity-custom-name-contains", "mob-custom-name-contains" -> {
                addCondition(event, new EntityCustomNameContains(process, node));
            }
            case "entity-type", "mob-type" -> {
                addCondition(event, new EntityTypeCondition(process, node));
            }
            case "entity-world", "mob-world" -> {
                addCondition(event, new EntityWorldCondition(process, node));
            }
            case "player-world" -> {
                addCondition(event, new PlayerWorldCondition(process, node));
            }
        }
    }

    private void addCondition(final ConditionParseEvent event, final Condition condition) {
        event.getProcess().getConditions().add(condition);
        event.setClaimed(true);
    }
}
