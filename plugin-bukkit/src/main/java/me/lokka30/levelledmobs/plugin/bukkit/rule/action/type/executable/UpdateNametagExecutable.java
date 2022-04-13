package me.lokka30.levelledmobs.plugin.bukkit.rule.action.type.executable;

import java.util.LinkedList;
import me.lokka30.levelledmobs.plugin.bukkit.rule.action.type.ExecuteAction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public record UpdateNametagExecutable(
    @NotNull LinkedList<String> args // ignored on this Executable.
) implements ExecuteAction.Executable {

    @Override
    @NotNull
    public String id() {
        return ExecuteAction.DefaultExecutableType.UPDATE_NAMETAG.id();
    }

    @Override
    public void run(@NotNull LivingEntity livingEntity) {
        if(livingEntity instanceof Player) {
            updateNametagsAroundPlayer((Player) livingEntity);
        } else {
            updateNametagForMob(livingEntity);
        }
    }

    void updateNametagsAroundPlayer(final @NotNull Player player) {
        //TODO
    }

    void updateNametagForMob(final @NotNull LivingEntity livingEntity) {
        //TODO
    }
}