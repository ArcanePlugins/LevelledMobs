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
package io.github.arcaneplugins.levelledmobs.bukkit.util

/**
 * [DescriptiveException] can be thrown when a highly descriptive [message] is provided to describe
 * why this exception was thrown.
 *
 * [DescriptiveException] extends [RuntimeException], though importantly, it disables stack traces,
 * as the [message] is assumed to contain all details required to diagnose an issue. Disabling stack
 * traces can make it easier for non-technical users to read the exception message without the
 * bloat of a stack trace (which can immediately look too daunting to read around).
 *
 * As a side-note, the fact that [DescriptiveException] disables stack traces also has the
 * positive side effect of improving performance, though using [DescriptiveException] for that
 * exact purpose should be avoided where possible as stack traces can make user-reported edge
 * cases much easier to diagnose especially if the provided [message] misses an important detail.
 *
 * Properties [message] and [cause] are inherited from [RuntimeException].
 *
 * @author Lachlan Adamson (lokka30)
 * @see RuntimeException
 */
open class DescriptiveException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {

    init {
        //todo doc
        clearRecursiveCause()
    }

    /**
     * We don't want this exception to have a stack trace.
     */
    override fun fillInStackTrace(): Throwable {
        return this
    }

    /**
     * When they're printed, make them look nicer.
     */
    override fun toString(): String {
        return message!!
    }

    /**
     * Recursively clear the stackTrace array of each [cause].
     */
    private fun clearRecursiveCause() {
        stackTrace = arrayOf()

        var recursiveCause: Throwable? = cause
        while(recursiveCause != null) {
            recursiveCause.stackTrace = arrayOf()
            recursiveCause = recursiveCause.cause
        }
    }

}