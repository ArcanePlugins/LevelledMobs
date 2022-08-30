package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategyRequestEvent;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class SetLevelAction extends Action {

    /* vars */

    private final boolean babiesInheritLevel;
    private final String formula;
    private final Set<LevellingStrategy> strategies = new HashSet<>();
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

        // TODO Test.
        /*
        Here we want to call out for all known levelling strategies to be registered to the
        SetLevelAction.
         */
        final var strategyNames = new LinkedList<String>(); //TODO remove for testing

        // Iterate through each strategyId specified under the strategies section
        for(var strategyNode : getActionNode().node("strategies").childrenList()) {

            if(strategyNode.key() == null) {
                //TODO log error: strategy key must not be null
                new Throwable().printStackTrace();
                continue;
            }

            // note: can't use the pattern style cast as IntelliJ's analyzer has trouble if it is
            // present.
            if(!(strategyNode.key() instanceof String)) {
                //TODO log error: strategy keys must be strings
                new Throwable().printStackTrace();
                continue;
            }
            //noinspection PatternVariableCanBeUsed
            final String strategyId = (String) strategyNode.key();

            // fire LevellingStrategyRequestEvent
            //noinspection ConstantConditions
            final var stratReqEvent = new LevellingStrategyRequestEvent(strategyId, strategyNode);
            Bukkit.getPluginManager().callEvent(stratReqEvent);

            if(stratReqEvent.isCancelled()) {
                continue;
            }

            // add all strategies from the events
            getStrategies().addAll(stratReqEvent.getStrategies());

            //TODO remove for testing
            for(var strategy : stratReqEvent.getStrategies()) {
                strategyNames.add(strategy.getName());
            }
        }

        //TODO test debugging remove this
        Log.war(String.format(
            "DEBUG: The following levelling strategies are in the SetLevelAction located in the process '%s' in function '%s': %s",
            getParentProcess().getIdentifier(),
            getParentProcess().getParentFunction().getIdentifier(),
            strategyNames
        ), false);
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

        if(!(context.getEntity() instanceof final LivingEntity lent)) {
            //TODO better error message
            Log.war("set-level: entity must be livingentity", true);
            return;
        }

        //TODO handle babies level inheritance
        // ...
        if(getBabiesInheritLevel()) {
            //TODO
            Log.inf("babies inherit level logic not done yet");
        }

        //TODO handle passengers level inheritance
        // ...
        if(getPassengersInheritLevel()) {
            Log.inf("passengers inherit level logic not done yet");
        }

        final var level = processFormula(context);

        // if the formula was invalid or returned 'no-level',
        // remove level if entity has one. otherwise, job is done
        if (level == null) {
            // check if the entity is levelled or not
            if(!InternalEntityDataUtil.isLevelled(lent)) {
                return;
            }

            // Looks like the mob was previously levelled, so we need to remove most of the LM stuff
            InternalEntityDataUtil.unlevelMob(lent);
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

        var formula = context.replacePlaceholders(getFormula());
        //TODO levelling strategies in formula

        //TODO use Crunch to spit out a level. make sure to catch any exceptions

        return Math.max(getMinPossibleLevel(), 4);
    }

    /* getters and setters */

    public Set<LevellingStrategy> getStrategies() {
        return strategies;
    }

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

    public boolean getBabiesInheritLevel() {
        return babiesInheritLevel;
    }

    public boolean getPassengersInheritLevel() {
        return passengersInheritLevel;
    }
}
