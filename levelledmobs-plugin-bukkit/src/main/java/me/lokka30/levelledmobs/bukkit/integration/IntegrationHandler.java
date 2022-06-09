package me.lokka30.levelledmobs.bukkit.integration;

import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.integration.internal.CitizensIntegration;
import me.lokka30.levelledmobs.bukkit.integration.internal.NbtApiIntegration;
import me.lokka30.levelledmobs.bukkit.integration.internal.RtuLangApiIntegration;
import me.lokka30.levelledmobs.bukkit.integration.nbthandler.NbtProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public final class IntegrationHandler {

    /* vars */

    private final Set<Integration> integrations = new HashSet<>();

    private final Set<Integration> internalIntegrations = Set.of(
        new CitizensIntegration(),
        new NbtApiIntegration(),
        new RtuLangApiIntegration()
    );

    /* constructors */

    public IntegrationHandler() {
        integrations.addAll(internalIntegrations);
    }

    /* methods */

    /* getters and setters */

    @NotNull
    public Set<Integration> getIntegrations() { return integrations; }

    @NotNull
    public Set<Integration> getInternalIntegrations() { return internalIntegrations; }

    @Nullable
    public NbtProvider getPrimaryNbtProvider() {
        Integration selected = null;

        for(var integration : getIntegrations()) {
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

}
