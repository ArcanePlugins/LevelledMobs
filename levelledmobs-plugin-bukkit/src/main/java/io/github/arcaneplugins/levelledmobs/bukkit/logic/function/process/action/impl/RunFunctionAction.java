package io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.impl;

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.LogicHandler;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.context.Context;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.LmFunction;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.Process;
import io.github.arcaneplugins.levelledmobs.bukkit.logic.function.process.action.Action;
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class RunFunctionAction extends Action {

    /* vars */

    private final String otherFuncId;
    private boolean sentError = false;

    /* constructors */

    public RunFunctionAction(
        @NotNull Process process,
        @NotNull CommentedConfigurationNode node
    ) {
        super(process, node);
        this.otherFuncId = Objects.requireNonNull(
            getActionNode().node("id").getString(),
            "RunFunctionAction did not specify valid ID of function to run"
        );
    }

    /* methods */

    @Override
    public void run(Context context) {
        final boolean potentiallyCircularFunction = context.getLinkedFunctions().stream()
            .anyMatch(otherFunction -> otherFunction.getIdentifier().equals(getOtherFuncId()));

        final boolean useCircularFunctionDependencyDetection = LevelledMobs.getInstance()
            .getConfigHandler().getSettingsCfg()
            .getRoot().node("advanced", "circular-function-dependency-detection")
            .getBoolean(false);

        if(useCircularFunctionDependencyDetection && potentiallyCircularFunction) {
            Log.sev(String.format(
                "Blocked potentially recursive call to run function '%s' in process '%s' (parent " +
                    "function '%s'). This protection can be disabled - be advised that recursive " +
                    "calls can result in memory leaks. LM will call 'exit-all' on the cause. " +
                    "This message will only appear once.",
                getOtherFuncId(),
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier()
            ), true);
            getParentProcess().getParentFunction().exitAll(context);
            return;
        }

        final Optional<LmFunction> functionToRunOpt = LogicHandler.getFunctions().stream()
            .filter(otherFunction -> otherFunction.getIdentifier().equals(getOtherFuncId()))
            .findFirst();

        if(functionToRunOpt.isEmpty()) {
            if(hasSentError())
                return;

            Log.sev(String.format(
                "Unable to run function '%s' from process '%s' in function '%s' as function '%s' " +
                    "does not exist.",
                getOtherFuncId(),
                getParentProcess().getIdentifier(),
                getParentProcess().getParentFunction().getIdentifier(),
                getOtherFuncId()
            ), true);

            setHasSentError(true);
        } else {
            if(!potentiallyCircularFunction)
                context.withLinkedFunction(getParentProcess().getParentFunction());

            functionToRunOpt.get().run(context, false);
        }
    }

    /* getters and setters */

    public boolean hasSentError() {
        return sentError;
    }

    public void setHasSentError(final boolean state) {
        this.sentError = state;
    }

    @Nonnull
    public String getOtherFuncId() {
        return otherFuncId;
    }
}
