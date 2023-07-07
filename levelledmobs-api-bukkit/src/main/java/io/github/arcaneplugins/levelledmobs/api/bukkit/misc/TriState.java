/*
This program is/was a part of the LevelledMobs project's source code.
Copyright (C) 2023  Lachlan Adamson (aka lokka30)
Copyright (C) 2023  LevelledMobs Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.arcaneplugins.levelledmobs.api.bukkit.misc;


//TODO doc
public enum TriState {

    TRUE,
    UNSPECIFIED,
    FALSE;

    //TODO doc
    public static TriState of(
        final boolean bool
    ) {
        return bool ? TRUE : FALSE;
    }

    //TODO doc
    public static TriState of(
        final Boolean bool
    ) {
        return bool == null ? UNSPECIFIED : of(bool);
    }

    //TODO doc
    // 'falsy': treats only TRUE as Boolean.TRUE
    public boolean toFalsyBoolean() {
        return this == TRUE;
    }

}
