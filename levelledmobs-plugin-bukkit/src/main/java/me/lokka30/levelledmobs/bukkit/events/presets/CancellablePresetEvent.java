package me.lokka30.levelledmobs.bukkit.events.presets;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.events.CancellableEvent;
import me.lokka30.levelledmobs.bukkit.logic.Preset;
import org.jetbrains.annotations.NotNull;

public abstract class CancellablePresetEvent extends CancellableEvent {

  /* vars */

  private final Preset preset;

  /* constructors */

  public CancellablePresetEvent(final @NotNull Preset preset) {
    this.preset = Objects.requireNonNull(preset, "preset");
  }

  /* getters and setters */

  @NotNull
  public Preset getPreset() {
    return preset;
  }

}
