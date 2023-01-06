package io.github.arcaneplugins.levelledmobs.bukkit.integration;

import io.github.arcaneplugins.levelledmobs.bukkit.integration.impl.CitizensIntegration;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.impl.NbtApiIntegration;
import io.github.arcaneplugins.levelledmobs.bukkit.integration.type.nbt.NbtProvider;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class IntegrationHandler {

    private IntegrationHandler() throws IllegalAccessException {
        throw new IllegalAccessException("Illegal instantiation of utility class");
    }

    /* vars */

    private static final Set<Integration> INTEGRATIONS = new HashSet<>();

    /* methods */

    public static boolean load() {
        // TODO add check if these integrations are disabled.
        // TODO check why this returns a boolean

        if(hasRequiredPlugins("Citizens"))
            getIntegrations().add(new CitizensIntegration());

        if(hasRequiredPlugins("NBTAPI"))
            getIntegrations().add(new NbtApiIntegration());

        return true;
    }

    public static boolean hasRequiredPlugins(final String... plugins) {
        for(var plugin : plugins) {
            if(!Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static NbtProvider getPrimaryNbtProvider() {
        Integration selected = null;

        for(var integration : getIntegrations()) {
            if(!integration.isEnabled())
                continue;

            if(integration instanceof NbtProvider) {
                if(selected == null) {
                    selected = integration;
                } else if(integration.getPriority().ordinal() > selected.getPriority().ordinal()) {
                    selected = integration;
                }
            }
        }

        return (NbtProvider) selected;
    }

    /* getters and setters */

    @NotNull
    public static Set<Integration> getIntegrations() { return INTEGRATIONS; }

}
