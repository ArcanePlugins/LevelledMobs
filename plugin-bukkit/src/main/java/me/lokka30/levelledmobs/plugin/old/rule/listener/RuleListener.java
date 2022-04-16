/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.old.rule.listener;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Optional;
import me.lokka30.levelledmobs.plugin.old.rule.RuleEntry;
import org.jetbrains.annotations.NotNull;

public record RuleListener(
    @NotNull String listenerId,
    @NotNull Optional<String> description,
    @NotNull EnumSet<ListenableEvent> listenFor,
    boolean ignoreCancelled,
    @NotNull LinkedList<RuleEntry> entries
) {

}
