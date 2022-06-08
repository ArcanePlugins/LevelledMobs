package me.lokka30.levelledmobs.bukkit.listener.condition;

import java.util.Locale;
import me.lokka30.levelledmobs.bukkit.event.condition.ConditionParseEvent;
import me.lokka30.levelledmobs.bukkit.listener.ListenerWrapper;
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
        switch(event.getIdentifier().toLowerCase(Locale.ROOT)) {
            case "chance" -> {
                event.getProcess().getConditions().add(new ChanceCondition(
                    event.getProcess(), event.getNode()
                ));
            }
            default -> {
                return;
            }
        }

        event.setClaimed(true);
    }
}
