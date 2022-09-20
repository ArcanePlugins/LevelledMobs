package me.lokka30.levelledmobs.bukkit.logic.function.process.action.impl.setlevel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.api.data.EntityDataUtil;
import me.lokka30.levelledmobs.bukkit.data.InternalEntityDataUtil;
import me.lokka30.levelledmobs.bukkit.logic.context.Context;
import me.lokka30.levelledmobs.bukkit.logic.function.process.Process;
import me.lokka30.levelledmobs.bukkit.logic.function.process.action.Action;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategy;
import me.lokka30.levelledmobs.bukkit.logic.levelling.strategy.LevellingStrategyRequestEvent;
import me.lokka30.levelledmobs.bukkit.util.Log;
import me.lokka30.levelledmobs.bukkit.util.TriLevel;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import redempt.crunch.Crunch;

public class SetLevelAction extends Action {

    /* vars */

    private final String formula;
    private final Set<LevellingStrategy> strategies = new HashSet<>();

    private final boolean useInheritanceIfAvailable;
    private final String inheritanceBreedingFormula;
    private final String inheritanceTransformationFormula;

    /* constructors */

    public SetLevelAction(
        @NotNull Process parentProcess,
        @NotNull CommentedConfigurationNode actionNode
    ) {
        super(parentProcess, actionNode);
        Log.tmpdebug("Found set level action at path: " + getActionNode().path().toString());

        this.formula = getActionNode().node("formula")
            .getString("no-level");

        this.useInheritanceIfAvailable = getActionNode()
            .node("inheritance", "use-if-available")
            .getBoolean(false);

        this.inheritanceBreedingFormula = getActionNode()
            .node("inheritance", "breeding", "formula")
            .getString("(%father-level% + %mother-level%) / 2");

        this.inheritanceTransformationFormula = getActionNode()
            .node("inheritance", "transformation", "formula")
            .getString("%mother-level%");

        /*
        Here we want to call out for all known levelling strategies to be registered to the
        SetLevelAction.
         */
        // Iterate through each strategyId specified under the strategies section
        for(var strategyNodeEntry : getActionNode()
            .node("strategies")
            .childrenMap().entrySet()
        ) {
            final CommentedConfigurationNode strategyNode = strategyNodeEntry.getValue();

            if(strategyNodeEntry.getKey() == null) {
                //TODO log error: strategy key must not be null
                new Throwable().printStackTrace();
                continue;
            }

            if(!(strategyNodeEntry.getKey() instanceof String strategyId)) {
                //TODO log error: strategy keys must be strings
                new Throwable().printStackTrace();
                continue;
            }

            // fire LevellingStrategyRequestEvent
            final var stratReqEvent = new LevellingStrategyRequestEvent(strategyId, strategyNode);
            Bukkit.getPluginManager().callEvent(stratReqEvent);

            if(stratReqEvent.isCancelled()) {
                continue;
            }

            // add all strategies from the events
            getStrategies().addAll(stratReqEvent.getStrategies());
        }
    }

    /* methods */

    @Override
    public void run(Context context) {
        Objects.requireNonNull(context, "context");

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

        TriLevel result;

        result = generateInheritedLevels(context);
        if(result == null) result = generateStandardLevels(context);

        // if the formula was invalid or returned 'no-level',
        // remove level if entity has one. otherwise, job is done
        if (result == null) {
            // check if the entity is levelled or not
            if(!InternalEntityDataUtil.isLevelled(lent, false)) {
                return;
            }

            // Looks like the mob was previously levelled, so we need to remove most of the LM stuff
            InternalEntityDataUtil.unlevelMob(lent);
            return;
        }

        InternalEntityDataUtil.setMinLevel(lent, result.getMinLevel(), true);
        InternalEntityDataUtil.setLevel(lent, result.getLevel(), true);
        InternalEntityDataUtil.setMaxLevel(lent, result.getMaxLevel(), true);

        //TODO apply inheritance formulas to (parent) entity.

        Log.tmpdebug("Finished levelling a %s. Lvl=%s, MinLvl=%s, MaxLvl=%s".formatted(
            lent.getType(), result.getLevel(), result.getMinLevel(), result.getMaxLevel()
        ));
    }

    private TriLevel generateStandardLevels(Context context) {
        return processFormula(context);
    }

    private TriLevel generateInheritedLevels(Context context) {
        if(!useInheritanceIfAvailable()) return null;

        final LivingEntity lent = Objects.requireNonNull(
            (LivingEntity) context.getEntity(),
            "LivingEntity"
        );

        final @Nullable LivingEntity father = EntityDataUtil.getFather(lent, false);
        final @Nullable LivingEntity mother = EntityDataUtil.getMother(lent, false);

        if(Boolean.TRUE.equals(EntityDataUtil.wasBred(lent, true))) {

            if(father == null || mother == null) return null;

            context
                .withFather(father)
                .withMother(mother);

            final String fatherFormula = EntityDataUtil.getInheritanceBreedingFormula(father, true);
            final String motherFormula = EntityDataUtil.getInheritanceBreedingFormula(mother, true);

            //TODO do the rest of the calculate breed level pseudocode

        } else if(Boolean.TRUE.equals(EntityDataUtil.wasTransformed(lent, true))) {

            // during transformation, mother == father. we only check for one.
            if(mother == null) return null;

            // yes: it is intentional the father is the same as the mother during transformation.
            context
                .withFather(mother)
                .withMother(mother);

            final String formula = EntityDataUtil.getInheritanceTransformationFormula(mother, true);

            //TODO do the rest of the calculate transform level pseudocode

        }

        return null;
    }

    /**
     * TODO document.
     *
     * @param context TODO doc
     * @return TODO doc
     */
    @Nullable
    public TriLevel processFormula(final @NotNull Context context) {
        Objects.requireNonNull(context, "context");

        // check if the mob should have no level
        if(getFormula().equalsIgnoreCase("no-level")) {
            // null = no level
            return null;
        }

        // replace context placeholders in the formula
        String formula = context.replacePlaceholders(getFormula());

        int minLevel = getMinPossibleLevel();
        int maxLevel = getMinPossibleLevel();

        // replace levelling strategy placeholders in the formula
        for(final LevellingStrategy strategy : getStrategies()) {
            formula = strategy.replaceInFormula(formula, context);

            minLevel = Math.min(minLevel, strategy.getMinLevel());
            maxLevel = Math.max(maxLevel, strategy.getMaxLevel());
        }

        // evaluate the formula with Crunch, don't allow values below the min possible level
        final int level = Math.max(
            (int) Math.round(Crunch.evaluateExpression(formula)),
            getMinPossibleLevel()
        );

        return new TriLevel(minLevel, level, maxLevel);
    }

    /* getters and setters */

    public Set<LevellingStrategy> getStrategies() {
        return strategies;
    }

    public int getMinPossibleLevel() {
        // we don't want negative values as they create undefined game behaviour
        return Math.max(0, LevelledMobs.getInstance()
            .getConfigHandler().getSettingsCfg()
            .getRoot().node("advanced", "minimum-level").getInt(1)
        );
    }

    @NotNull
    public String getFormula() {
        return formula;
    }

    public boolean useInheritanceIfAvailable() {
        return useInheritanceIfAvailable;
    }

    public String getInheritanceTransformationFormula() {
        return inheritanceTransformationFormula;
    }

    public String getInheritanceBreedingFormula() {
        return inheritanceBreedingFormula;
    }
}
