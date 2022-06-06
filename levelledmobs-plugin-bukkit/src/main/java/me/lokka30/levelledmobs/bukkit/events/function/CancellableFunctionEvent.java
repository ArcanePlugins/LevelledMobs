package me.lokka30.levelledmobs.bukkit.events.function;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.events.CancellableEvent;
import me.lokka30.levelledmobs.bukkit.logic.LmFunction;
import org.jetbrains.annotations.NotNull;

public abstract class CancellableFunctionEvent extends CancellableEvent {

  /* vars */

  private final LmFunction function;

  /* constructors */

  public CancellableFunctionEvent(
      final @NotNull LmFunction function
  ) {
    this.function = Objects.requireNonNull(function, "function");
  }

  /* getters and setters */

  @NotNull
  public LmFunction getFunction() {
    return function;
  }

}
