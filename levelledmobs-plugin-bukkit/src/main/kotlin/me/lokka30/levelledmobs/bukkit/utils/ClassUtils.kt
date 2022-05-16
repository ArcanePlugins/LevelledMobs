package me.lokka30.levelledmobs.bukkit.utils

object ClassUtils {

    private val classExistsResults: MutableMap<String, Boolean> = mutableMapOf()

    fun classExists(classpath: String): Boolean {
        return if (classExistsResults.containsKey(classpath)) {
            classExistsResults[classpath]!!
        } else {
            try {
                Class.forName(classpath)
                classExistsResults[classpath] = true
                true
            } catch (ex: ClassNotFoundException) {
                classExistsResults[classpath] = false
                false
            }
        }
    }

}