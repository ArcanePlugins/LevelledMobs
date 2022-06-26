package me.lokka30.levelledmobs.bukkit.logic.function.process.condition;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class ConditionSocket extends Condition {

    /* vars */

    /*
    The identifier of the socket (i.e., it is looking for an action to replace it under an
    identical identifier).
     */
    private final String identifier;

    /*
    This socket should only warn console once if it hasn't been replaced.
     */
    private boolean hasAlreadyWarned = false;

    /* constructors */

    public ConditionSocket(
        final @NotNull Process process,
        final @NotNull CommentedConfigurationNode node,
        final @NotNull String identifier
    ) {
        super(process, node);
        this.identifier = Objects.requireNonNull(identifier, "identifier");
    }

    /* methods */

    @Override
    public boolean applies(Context context) {
        if(hasAlreadyWarned)
            return false;

        Log.sev("Process '" + getParentProcess().getIdentifier() + "' contains a socket for the " +
            "action '" + identifier + "', but no action was found in any presets to fulfil the " +
            "socket.", true);

        hasAlreadyWarned = true;
        return false;
    }
}
