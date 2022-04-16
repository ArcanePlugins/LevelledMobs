package me.lokka30.levelledmobs.plugin.old.rule.action.type;

import de.leonhard.storage.sections.FlatFileSection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import me.lokka30.levelledmobs.plugin.old.LevelledMobs;
import me.lokka30.levelledmobs.plugin.old.rule.Rule;
import me.lokka30.levelledmobs.plugin.old.rule.action.DefaultRuleActionType;
import me.lokka30.levelledmobs.plugin.old.rule.action.RuleAction;
import me.lokka30.levelledmobs.plugin.old.rule.action.type.executable.UpdateNametagExecutable;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public record ExecuteAction(
    @NotNull Rule parentRule,
    @NotNull HashSet<Executable> executables
) implements RuleAction {

    @Override
    @NotNull
    public String id() {
        return DefaultRuleActionType.EXECUTE.id();
    }

    @Override
    public void run(@NotNull final LivingEntity livingEntity) {
        executables().forEach(executable -> executable.run(livingEntity));
    }

    @Override
    public void merge(@NotNull RuleAction other) {
        final ExecuteAction otherAction = (ExecuteAction) other;
        HashSet<Executable> toAdd = new HashSet<>();
        otherAction.executables().forEach(otherExecutable -> {
            if(executables().stream()
                .noneMatch(executable -> executable.id().equals(otherExecutable.id()))) {
                toAdd.add(otherExecutable);
            }
        });

        executables().addAll(toAdd);
    }

    @NotNull
    public static ExecuteAction of(final @NotNull Rule parentRule,
        final @NotNull FlatFileSection section) {
        final HashSet<Executable> executables = new HashSet<>();

        for(String executableStr : section.getStringList(".execute")) {
            DefaultExecutableType defaultExecutableType;

            final LinkedList<String> args = new LinkedList<>(
                Arrays.asList(executableStr.split(":")));

            final Optional<DefaultExecutableType> executableTypeOptional = DefaultExecutableType.of(
                args.get(0));
            if(executableTypeOptional.isPresent()) {
                defaultExecutableType = executableTypeOptional.get();
            } else {
                LevelledMobs.logger()
                    .severe("The execute list at path '&b" + section.getPathPrefix()
                        + "&7' has an invalid executable" +
                        " type specified: '&b" + args.get(0) + "&7'. Fix this ASAP.");
                continue;
            }
            args.remove(0);

            if(executables.stream()
                .anyMatch(other -> other.id().equals(defaultExecutableType.id()))) {
                LevelledMobs.logger()
                    .severe("The execute list at path '&b" + section.getPathPrefix()
                        + "&7' has the executable type " +
                        "'&b" + defaultExecutableType
                        + "&7' specified more than once. This will harm the intended behaviour of your"
                        +
                        " configuration. Fix this ASAP.");
                continue;
            }

            switch(defaultExecutableType) {
                case UPDATE_NAMETAG:
                    executables.add(new UpdateNametagExecutable(args));
                    break;
                default:
                    throw new IllegalStateException(
                        "Unexpected executable type: " + defaultExecutableType);
            }
        }

        return new ExecuteAction(parentRule, executables);
    }

    public enum DefaultExecutableType {
        UPDATE_NAMETAG("update-nametag");

        private final String id;

        DefaultExecutableType(final @NotNull String id) {
            this.id = id;
        }

        @NotNull
        public String id() {
            return id;
        }

        public static Optional<DefaultExecutableType> of(final @NotNull String id) {
            for(DefaultExecutableType type : values()) {
                if(type.id().equalsIgnoreCase(id)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    public interface Executable {

        @NotNull
        String id();

        @NotNull
        LinkedList<String> args();

        void run(@NotNull final LivingEntity livingEntity);

    }
}
