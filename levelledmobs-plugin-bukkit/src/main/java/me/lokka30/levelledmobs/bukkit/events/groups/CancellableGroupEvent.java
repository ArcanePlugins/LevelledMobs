package me.lokka30.levelledmobs.bukkit.events.groups;

import java.util.Objects;
import me.lokka30.levelledmobs.bukkit.events.CancellableEvent;
import me.lokka30.levelledmobs.bukkit.logic.Group;
import org.jetbrains.annotations.NotNull;

public abstract class CancellableGroupEvent extends CancellableEvent {

  /* vars */

  private final Group group;

  /* constructors */

  public CancellableGroupEvent(
      final @NotNull Group group
  ) {
    this.group = Objects.requireNonNull(group, "group");
  }

  /* getters and setters */

  @NotNull
  public Group getGroup() {
    return group;
  }

}
