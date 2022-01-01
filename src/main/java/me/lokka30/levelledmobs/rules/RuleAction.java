/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.rules;

import me.lokka30.levelledmobs.nametag.NametagHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;

public record RuleAction(
        @NotNull Optional<String>                                           nametagFormat,
        @NotNull Optional<Float>                                            nametagVisibilityDuration,
        @NotNull Optional<HashSet<NametagHandler.NametagVisibilityMethod>>  nametagVisibilityMethods,
        @NotNull Optional<Boolean>                                          matchedSettingsBabies,
        @NotNull Optional<Boolean>                                          matchedSettingsPassengers,
        @NotNull Optional<HashSet<Executable>>                              executables
) {}
