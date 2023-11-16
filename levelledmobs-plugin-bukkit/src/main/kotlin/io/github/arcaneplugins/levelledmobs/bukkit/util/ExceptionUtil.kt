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

object ExceptionUtil {

    fun printExceptionNicely(
        ex: Exception,
        context: String = "An error has occurred with LevelledMobs"
    ) {
        Log.sev("""
            
            ${context}.
            The following error description was provided:
            
            =======================
            ${ex.message ?: "... no description provided ..."}
            =======================
            
            Please address this issue as soon as possible to avoid unintentional behaviour.
            If you are unsure how to resolve this issue - even after checking our Wiki - feel free to contact LevelledMobs support on our Discord for assistance.
            Wiki: https://github.com/ArcanePlugins/LevelledMobs/wiki/
            
        """.trimIndent())

        if(ex !is DescriptiveException) {
            Log.sev("To aid maintainers with debugging code, a stack trace is provided below:")
            ex.printStackTrace()
            Log.sev("""
                
                =======================
                        Hey You!
                =======================
                
                You may have just scrolled up to see this large stack trace in your console - yes, we know it looks daunting.
                However, **take a minute to read the error description just above it**, since it may contain the information you need to resolve this issue.
                Wiki: https://github.com/ArcanePlugins/LevelledMobs/wiki/
                
            """.trimIndent())
        }
    }
}