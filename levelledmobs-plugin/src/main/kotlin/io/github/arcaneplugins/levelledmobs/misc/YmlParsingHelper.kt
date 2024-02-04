package io.github.arcaneplugins.levelledmobs.misc

import java.util.TreeSet
import io.github.arcaneplugins.levelledmobs.util.Utils
import io.github.arcaneplugins.levelledmobs.util.Utils.parseTimeUnit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemoryConfiguration

/**
 * @author stumper66
 * @since 3.1.0
 */
class YmlParsingHelper(
    var cs: ConfigurationSection
) {

    fun getBoolean(
        name: String
    ): Boolean {
        return getBoolean(name, false)
    }

    fun getBoolean(
        name: String,
        defaultValue: Boolean
    ): Boolean {
        return getBoolean(cs, name, defaultValue)
    }

    fun getBoolean2(
        name: String,
        defaultValue: Boolean?
    ): Boolean? {
        val useName = getKeyNameFromConfig(cs, name)

        return if (cs[useName] != null) {
            cs.getBoolean(useName)
        } else {
            defaultValue
        }
    }

    fun getString(
        name: String
    ): String? {
        return getString(name, null)
    }

    fun getString(
        name: String,
        defaultValue: String?
    ): String? {
        return getString(cs, name, defaultValue)
    }

    fun getStringSet(
        name: String
    ): MutableSet<String> {
        return getStringSet(cs, name)
    }

    fun getInt(
        name: String
    ): Int {
        return getInt(name, 0)
    }

    fun getInt(
        name: String,
        defaultValue: Int
    ): Int {
        val useName = getKeyNameFromConfig(cs, name)
        return cs.getInt(useName, defaultValue)
    }

    fun getInt2(
        name: String,
        defaultValue: Int?
    ): Int? {
        val useName = getKeyNameFromConfig(cs, name)

        return if (cs[useName] != null) {
            cs.getInt(useName)
        } else {
            defaultValue
        }
    }

    private fun getDouble(
        name: String
    ): Double {
        val useName = getKeyNameFromConfig(cs, name)
        return cs.getDouble(useName, 0.0)
    }

    fun getDouble2(
        name: String,
        defaultValue: Double?
    ): Double? {
        return getDouble2(cs, name, defaultValue)
    }

    fun getFloat(
        name: String
    ): Float {
        return getFloat(name, 0.0f)
    }

    fun getFloat(
        name: String,
        defaultValue: Float
    ): Float {
        val useName = getKeyNameFromConfig(cs, name)
        return cs.getDouble(useName, defaultValue.toDouble()).toFloat()
    }

    fun getFloat2(
        name: String,
        defaultValue: Float?
    ): Float? {
        return getFloat2(cs, name, defaultValue)
    }

    fun getIntTimeUnit(
        name: String,
        defaultValue: Int?
    ): Int? {
        val useName = getKeyNameFromConfig(cs, name)

        if (cs[useName] != null) {
            if (cs.getInt(useName) > 0) {
                return cs.getInt(useName)
            }

            val temp = cs.getString(useName)
            val useDefaultValue: Long? = defaultValue?.toLong()
            val result = parseTimeUnit(temp, useDefaultValue, false, null)
            return if (result != null)
                Math.toIntExact(result)
            else
                defaultValue
        } else {
            return defaultValue
        }
    }

    fun getIntTimeUnitMS(
        name: String,
        defaultValue: Long?
    ): Long? {
        val useName = getKeyNameFromConfig(cs, name)

        if (cs[useName] != null) {
            if (cs.getLong(useName) > 0) {
                return cs.getLong(useName)
            }
            val temp = cs.getString(useName)
            return parseTimeUnit(temp, defaultValue, true, null) ?: defaultValue
        } else {
            return defaultValue
        }
    }

    fun getStringOrList(
        key: String
    ): MutableList<String> {
        return getStringOrList(cs, key)
    }

    fun getStringList(
        key: String
    ): MutableList<String>{
        val useKey = getKeyNameFromConfig(cs, key)
        return cs.getStringList(useKey)
    }

    fun getListFromConfigItem(
        key: String
    ): MutableList<String> {
        return getListFromConfigItem(cs, key)
    }

    fun getKeyNameFromConfig(
        key: String
    ): String {
        return Companion.getKeyNameFromConfig(cs, key)
    }

    fun objToCS(
        path: String
    ): ConfigurationSection? {
        return objToCS(cs, path)
    }

    companion object {
        fun getFloat2(
            cs: ConfigurationSection?,
            name: String,
            defaultValue: Float?
        ): Float? {
            if (cs == null) return defaultValue
            val useName = getKeyNameFromConfig(cs, name)

            return if (cs[useName] != null) {
                cs.getDouble(useName).toFloat()
            } else {
                defaultValue
            }
        }

        fun getStringSet(
            cs: ConfigurationSection?,
            name: String
        ): MutableSet<String> {
            if (cs == null) return mutableSetOf()

            val useName = getKeyNameFromConfig(cs, name)
            val results: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
            // rather than use addAll we'll make sure there no empty strings
            for (item in cs.getStringList(useName)) {
                if (item.isNotEmpty()) {
                    results.add(item)
                }
            }

            return results
        }

        fun getBoolean(
            cs: ConfigurationSection?,
            name: String
        ): Boolean {
            return getBoolean(cs, name, false)
        }

        fun getBoolean(
            cs: ConfigurationSection?,
            name: String,
            defaultValue: Boolean
        ): Boolean {
            if (cs == null) return defaultValue

            val useName = getKeyNameFromConfig(cs, name)
            return cs.getBoolean(useName, defaultValue)
        }

        fun getDouble2(
            cs: ConfigurationSection?,
            name: String,
            defaultValue: Double?
        ): Double? {
            if (cs == null) return defaultValue

            val useName = getKeyNameFromConfig(cs, name)

            return if (cs[useName] != null) {
                cs.getDouble(useName)
            } else {
                defaultValue
            }
        }

        fun getString(
            cs: ConfigurationSection?,
            name: String
        ): String? {
            return getString(cs, name, null)
        }

        fun getString(
            cs: ConfigurationSection?,
            name: String,
            defaultValue: String?
        ): String? {
            if (cs == null) return defaultValue

            val useName = getKeyNameFromConfig(cs, name)
            return cs.getString(useName, defaultValue)
        }

        fun objToCS(
            cs: ConfigurationSection,
            path: String
        ): ConfigurationSection? {
            val useKey = getKeyNameFromConfig(cs, path)
            val obj = cs[useKey] ?: return null

            when (obj) {
                is ConfigurationSection -> {
                    return obj
                }

                is Map<*, *> -> {
                    val result = MemoryConfiguration()
                    result.addDefaults((obj as MutableMap<String, Any>))
                    return result.defaultSection
                }

                else -> {
                    val currentPath = if (cs.currentPath.isNullOrEmpty()) path else cs.currentPath + "." + path
                    Utils.logger.warning(
                        "$currentPath: couldn't parse Config of type: " + obj.javaClass
                            .simpleName + ", value: " + obj
                    )
                    return null
                }
            }
        }

        fun getKeyNameFromConfig(
            cs: ConfigurationSection,
            key: String
        ): String {
            if (!key.contains(".")) {
                for (enumeratedKey in cs.getKeys(false)) {
                    if (key.equals(enumeratedKey, ignoreCase = true)) {
                        return enumeratedKey
                    }
                }

                return key
            }

            // key contains one or more periods
            val periodSplit = (key.split("\\."))
            val sb = StringBuilder(periodSplit.size)
            var keysFound = 0

            for (thisKey in periodSplit) {
                var foundKey = false
                val checkKeyName = if (sb.isEmpty()) thisKey else sb.toString()
                val useCS = if (keysFound == 0) cs else objToCS(cs, checkKeyName)

                if (useCS == null) {
                    break
                }

                for (enumeratedKey in useCS.getKeys(false)) {
                    if (thisKey.equals(enumeratedKey, ignoreCase = true)) {
                        if (sb.isNotEmpty()) {
                            sb.append(".")
                        }
                        sb.append(enumeratedKey)
                        foundKey = true
                        keysFound++
                        break
                    }
                }
                if (!foundKey) {
                    break
                }
            }

            // if only some of the keys were found then add the remaining ones
            for (i in keysFound until periodSplit.size) {
                if (sb.isNotEmpty()) {
                    sb.append(".")
                }
                sb.append(periodSplit[i])
            }

            return sb.toString()
        }

        fun getListFromConfigItem(
            cs: ConfigurationSection,
            key: String
        ): MutableList<String> {
            var foundKeyName: String? = null
            for (enumeratedKey in cs.getKeys(false)) {
                if (key.equals(enumeratedKey, ignoreCase = true)) {
                    foundKeyName = enumeratedKey
                    break
                }
            }

            if (foundKeyName == null) {
                return mutableListOf()
            }

            val result = cs.getStringList(foundKeyName)
            if (result.isEmpty() && cs.getString(foundKeyName) != null && "" != cs.getString(foundKeyName)) {
                result.add(cs.getString(foundKeyName))
            }

            return result
        }
    }

    fun getStringOrList(
        cs: ConfigurationSection?,
        key: String
    ): MutableList<String> {
        val results = mutableListOf<String>()
        if (cs == null) return results

        val foundKeyName = getKeyNameFromConfig(cs, key)
        val lst = cs.getStringList(foundKeyName)

        if (lst.isNotEmpty()) {
            for (item in lst) {
                if (item.toString().isNotEmpty()) results.add(item.toString())
            }

            return results
        }

        val temp = cs.getString(foundKeyName)
        if (!temp.isNullOrEmpty()) {
            results.addAll(temp.split(","))
        }

        return results
    }
}