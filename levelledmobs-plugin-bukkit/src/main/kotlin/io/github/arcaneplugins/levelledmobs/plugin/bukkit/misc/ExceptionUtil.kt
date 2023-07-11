package io.github.arcaneplugins.levelledmobs.plugin.bukkit.misc

object ExceptionUtil {

    fun printExceptionNicely(
        ex: Exception,
        context: String = "An error has occurred with LevelledMobs"
    ) {
        Log.severe("""
            
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
            Log.severe("To aid maintainers with debugging code, a stack trace is provided below:")
            ex.printStackTrace()
            Log.severe("""
                
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