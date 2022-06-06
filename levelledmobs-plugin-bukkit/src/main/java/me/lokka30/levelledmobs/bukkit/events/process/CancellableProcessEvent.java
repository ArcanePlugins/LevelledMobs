package me.lokka30.levelledmobs.bukkit.events.process;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.events.CancellableEvent;
import me.lokka30.levelledmobs.bukkit.logic.Process;
import org.jetbrains.annotations.NotNull;

public abstract class CancellableProcessEvent extends CancellableEvent {

  /* vars */

  private final Process process;

  /* constructors */

  public CancellableProcessEvent(final @NotNull Process process) {
    this.process = Objects.requireNonNull(process, "process");
  }

  /* getters and setters */

  @NotNull
  public Process getProcess() {
    return process;
  }

}
