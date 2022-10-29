package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.condition.Condition;
import java.util.concurrent.ThreadLocalRandom;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class ChanceCondition extends Condition {

    /* vars */

    private final float chance;

    /* constructors */

    public ChanceCondition(Process process, final CommentedConfigurationNode node) {
        super(process, node);
        this.chance = node.node("value").getFloat(100.0f);
    }

    /* methods */

    @Override
    public boolean applies(Context context) {
        final var random = ThreadLocalRandom.current().nextFloat();
        final var chance = (getChance() / 100.0f);
        return random <= chance;
    }

    /* getters and setters */

    public float getChance() { return chance; }

}
