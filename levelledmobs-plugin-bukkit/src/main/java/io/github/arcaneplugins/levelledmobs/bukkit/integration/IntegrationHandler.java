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

    /* vars */

    private final Set<Integration> integrations = new HashSet<>();

    /* methods */

    public boolean load() {
        // TODO add check if these integrations are disabled.

        if(hasRequiredPlugins("Citizens"))
            getIntegrations().add(new CitizensIntegration());

        if(hasRequiredPlugins("NBTAPI"))
            getIntegrations().add(new NbtApiIntegration());

        return true;
    }

    public boolean hasRequiredPlugins(final String... plugins) {
        for(var plugin : plugins) {
            if(!Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public NbtProvider getPrimaryNbtProvider() {
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
    public Set<Integration> getIntegrations() { return integrations; }

}
