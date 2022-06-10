package me.lokka30.levelledmobs.bukkit.listener.action;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class SetLevelAction extends Action {

    /* vars */

    private final String formula;
    private final boolean babiesInheritLevel;
    private final boolean passengersInheritLevel;

    /* constructors */

    public SetLevelAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);

        this.formula = getActionNode().node("formula")
            .getString("no-level");

        this.babiesInheritLevel = getActionNode().node("babies-inherit-level")
            .getBoolean(true);

        this.passengersInheritLevel = getActionNode().node("passengers-inherit-level")
            .getBoolean(true);
    }

    /* methods */

    @Override
    public void run(Context context) {
        Objects.requireNonNull(context, "context");

        //TODO Remove debug
        Log.inf("DEBUG: Running 'set-level' action");

        if(context.getEntity() == null) {
            //TODO better error message
            Log.war("set-level: function triggered without entity context", true);
            return;
        }

        if(!(context.getEntity() instanceof final LivingEntity livingEntity)) {
            //TODO better error message
            Log.war("set-level: entity must be livingentity", true);
            return;
        }

        //TODO handle babies level inheritance
        // ...
        if(babiesInheritLevel()) {
            //TODO
            Log.inf("babies inherit level logic not done yet");
        }

        //TODO handle passengers level inheritance
        // ...
        if(passengersInheritLevel()) {
            Log.inf("passengers inherit level logic not done yet");
        }

        final var level = processFormula(context);

        // if the formula was invalid or returned 'no-level',
        // remove level if entity has one. otherwise, job is done
        if (level == null) {
            // check if the entity is levelled or not
            if(!InternalEntityDataUtil.isLevelled(livingEntity)) {
                return;
            }

            // Looks like the mob was previously levelled, so we need to remove most of the LM stuff
            // TODO ...
            return;
        }

        //TODO
        InternalEntityDataUtil.setLevel((LivingEntity) context.getEntity(), level);
    }

    @Nullable
    public Integer processFormula(final @NotNull Context context) {
        Objects.requireNonNull(context, "context");

        if(getFormula().equalsIgnoreCase("no-level")) {
            return null;
        }

        final var formula = context.replacePlaceholders(getFormula());
        //TODO levelling strategies in formula

        //TODO use Crunch to spit out a level. make sure to catch any exceptions

        return Math.max(getMinPossibleLevel(), 4);
    }

    /* getters and setters */

    public int getMinPossibleLevel() {
        return Math.max(0, LevelledMobs.getInstance()
            .getConfigHandler().getSettingsCfg()
            .getRoot().node("advanced", "minimum-level").getInt(1)
        );
    }

    @NotNull
    public String getFormula() {
        return formula;
    }

    public boolean babiesInheritLevel() {
        return babiesInheritLevel;
    }

    public boolean passengersInheritLevel() {
        return passengersInheritLevel;
    }
}
