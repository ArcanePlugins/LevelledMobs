package me.lokka30.levelledmobs.bukkit.listener.condition;

import java.util.concurrent.ThreadLocalRandom;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.condition.Condition;
import me.lokka30.levelledmobs.bukkit.util.Log;
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
        Log.inf("DEBUG: chance: " + chance + ", random: " + random);
        return random <= chance;
    }

    /* getters and setters */

    public float getChance() { return chance; }

}
