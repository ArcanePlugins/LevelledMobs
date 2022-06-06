package me.lokka30.levelledmobs.bukkit.integration;

import java.util.HashSet;
import java.util.Set;
import me.lokka30.levelledmobs.bukkit.integration.internal.CitizensIntegration;
import me.lokka30.levelledmobs.bukkit.integration.internal.RtuLangApiIntegration;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class IntegrationHandler {

    /* vars */

    private final Set<Integration> integrations = new HashSet<>();

    private final Set<Integration> internalIntegrations = Set.of(
        new CitizensIntegration(),
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

}
