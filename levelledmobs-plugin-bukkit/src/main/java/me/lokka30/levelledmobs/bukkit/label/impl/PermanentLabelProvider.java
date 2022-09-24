package me.lokka30.levelledmobs.bukkit.label.impl;

import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class PermanentLabelProvider {

    @NotNull
    public static String generateLabel(@NotNull LivingEntity lent) {
        //TODO make this configurable
        return "&8« &bLvl.%entity-level%&f %entity-prefix%&f%entity-name%&f%entity-suffix%&8 • " +
            "&7%entity-health-rounded%&8/&7%entity-max-health-rounded%&c♥ &8»";
    }

}
