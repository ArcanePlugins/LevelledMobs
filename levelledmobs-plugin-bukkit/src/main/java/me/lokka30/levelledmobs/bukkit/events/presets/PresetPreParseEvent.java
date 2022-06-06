package me.lokka30.levelledmobs.bukkit.events.presets;

import me.lokka30.levelledmobs.bukkit.logic.Preset;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class PresetPreParseEvent extends CancellablePresetEvent {

    /* vars */

    private static final HandlerList HANDLERS = new HandlerList();

    /* constructors */

    public PresetPreParseEvent(final @NotNull Preset preset) {
        super(preset);
    }

    /* getters and setters */

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
