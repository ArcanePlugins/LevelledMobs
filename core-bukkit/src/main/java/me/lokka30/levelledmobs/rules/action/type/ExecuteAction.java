package me.lokka30.levelledmobs.rules.action.type;

import de.leonhard.storage.sections.FlatFileSection;
import me.lokka30.levelledmobs.rules.Rule;
import me.lokka30.levelledmobs.rules.action.RuleAction;
import me.lokka30.levelledmobs.rules.action.RuleActionType;
import me.lokka30.levelledmobs.rules.action.type.executable.UpdateNametagExecutable;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;

public record ExecuteAction(
        @NotNull Rule parentRule,
        @NotNull HashSet<Executable> executables
) implements RuleAction {

    @Override @NotNull
    public RuleActionType type() {
        return RuleActionType.EXECUTE;
    }

    @Override
    public void run(@NotNull final LivingEntity livingEntity) {
        executables().forEach(executable -> executable.run(livingEntity));
    }

    @NotNull
    public static ExecuteAction of(final @NotNull Rule parentRule, final @NotNull FlatFileSection section) {
        final HashSet<Executable> executables = new HashSet<>();

        for(String executableStr : section.getStringList(".execute")) {
            ExecutableType executableType;

            final LinkedList<String> args = new LinkedList<>(Arrays.asList(executableStr.split(":")));

            final Optional<ExecutableType> executableTypeOptional = ExecutableType.of(args.get(0));
            if(executableTypeOptional.isPresent()) {
                executableType = executableTypeOptional.get();
            } else {
                Utils.LOGGER.error("The execute list at path '&b" + section.getPathPrefix() + "&7' has an invalid executable" +
                        " type specified: '&b" + args.get(0) + "&7'. Fix this ASAP.");
                continue;
            }
            args.remove(0);

            if(executables.stream().anyMatch(other -> other.type() == executableType)) {
                Utils.LOGGER.error("The execute list at path '&b" + section.getPathPrefix() + "&7' has the executable type " +
                        "'&b" + executableType + "&7' specified more than once. This will harm the intended behaviour of your" +
                        " configuration. Fix this ASAP.");
                continue;
            }

            switch(executableType) {
                case UPDATE_NAMETAG: executables.add(new UpdateNametagExecutable(args)); break;
                default: throw new IllegalStateException("Unexpected executable type: " + executableType);
            }
        }

        return new ExecuteAction(parentRule, executables);
    }

    public enum ExecutableType {
        UPDATE_NAMETAG("update-nametag");

        private final String id;
        ExecutableType(final @NotNull String id) {
            this.id = id;
        }

        @NotNull
        public String id() { return id; }

        public static Optional<ExecutableType> of(final @NotNull String id) {
            for(ExecutableType type : values()) {
                if(type.id().equalsIgnoreCase(id)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    public interface Executable {

        @NotNull
        ExecutableType type();

        @NotNull
        LinkedList<String> args();

        void run(@NotNull final LivingEntity livingEntity);

    }
}
