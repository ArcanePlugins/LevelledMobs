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
class YmlParsingHelper {
    fun getBoolean(cs: ConfigurationSection?, name: String): Boolean {
        return getBoolean(cs, name, false)
    }

    fun getBoolean(
        cs: ConfigurationSection?, name: String,
        defaultValue: Boolean
    ): Boolean {
        if (cs == null) {
            return defaultValue
        }
        val useName: String = getKeyNameFromConfig(cs, name)

        return cs.getBoolean(useName, defaultValue)
    }

    fun getBoolean2(
        cs: ConfigurationSection?, name: String,
        defaultValue: Boolean?
    ): Boolean? {
        if (cs == null) {
            return defaultValue
        }
        val useName: String = getKeyNameFromConfig(cs, name)

        return if (cs[useName] != null) {
            cs.getBoolean(useName)
        } else {
            defaultValue
        }
    }

    fun getString(cs: ConfigurationSection?, name: String): String? {
        return getString(cs, name, null)
    }

    fun getString(
        cs: ConfigurationSection?, name: String,
        defaultValue: String?
    ): String? {
        if (cs == null) {
            return defaultValue
        }
        val useName: String = getKeyNameFromConfig(cs, name)

        return cs.getString(useName, defaultValue)
    }

    fun getStringSet(cs: ConfigurationSection, name: String): Set<String> {
        val useName: String = getKeyNameFromConfig(cs, name)

        val results: MutableSet<String> = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
        // rather than use addAll we'll make sure there no empty strings
        for (item in cs.getStringList(useName)) {
            if (item.isNotEmpty()) {
                results.add(item)
            }
        }

        return results
    }

    fun getInt(cs: ConfigurationSection?, name: String): Int {
        return getInt(cs, name, 0)
    }

    fun getInt(
        cs: ConfigurationSection?, name: String,
        defaultValue: Int
    ): Int {
        if (cs == null) {
            return defaultValue
        }

        val useName: String = getKeyNameFromConfig(cs, name)
        return cs.getInt(useName, defaultValue)
    }

    fun getInt2(
        cs: ConfigurationSection?, name: String,
        defaultValue: Int?
    ): Int? {
        if (cs == null) {
            return defaultValue
        }

        val useName: String = getKeyNameFromConfig(cs, name)

        return if (cs[useName] != null) {
            cs.getInt(useName)
        } else {
            defaultValue
        }
    }

    @Suppress("unused")
    private fun getDouble(cs: ConfigurationSection?, name: String): Double {
        if (cs == null) {
            return 0.0
        }
        val useName: String = getKeyNameFromConfig(cs, name)

        return cs.getDouble(useName, 0.0)
    }

    fun getDouble2(
        cs: ConfigurationSection?, name: String,
        defaultValue: Double?
    ): Double? {
        if (cs == null) {
            return defaultValue
        }
        val useName: String = getKeyNameFromConfig(cs, name)

        return if (cs[useName] != null) {
            cs.getDouble(useName)
        } else {
            defaultValue
        }
    }

    fun getFloat(cs: ConfigurationSection?, name: String): Float {
        return getFloat(cs, name, 0.0f)
    }

    fun getFloat(
        cs: ConfigurationSection?, name: String,
        defaultValue: Float
    ): Float {
        if (cs == null) {
            return defaultValue
        }
        val useName: String = getKeyNameFromConfig(cs, name)

        return cs.getDouble(useName, defaultValue.toDouble()).toFloat()
    }

    fun getFloat2(
        cs: ConfigurationSection?, name: String,
        defaultValue: Float?
    ): Float? {
        if (cs == null) {
            return defaultValue
        }
        val useName: String = getKeyNameFromConfig(cs, name)

        return if (cs[useName] != null) {
            cs.getDouble(useName).toFloat()
        } else {
            defaultValue
        }
    }

    fun getIntTimeUnit(
        cs: ConfigurationSection?,
        name: String,
        defaultValue: Int?
    ): Int? {
        if (cs == null) {
            return defaultValue
        }
        val useName: String = getKeyNameFromConfig(cs, name)

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
        cs: ConfigurationSection?,
        name: String,
        defaultValue: Long?
    ): Long? {
        if (cs == null) {
            return defaultValue
        }
        val useName: String = getKeyNameFromConfig(cs, name)

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

    companion object {
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
        if (cs == null) {
            return results
        }

        var foundKeyName: String? = null
        for (enumeratedKey in cs.getKeys(false)) {
            if (key.equals(enumeratedKey, ignoreCase = true)) {
                foundKeyName = enumeratedKey
                break
            }
        }

        if (foundKeyName == null) {
            return results
        }

        val lst = cs.getList(foundKeyName)
        if (lst != null && lst.isNotEmpty()) {
            for (item in lst) {
                if (item.toString().isNotEmpty()) results.add(item.toString())
            }

            return results
        }

        val temp = cs.getString(foundKeyName)
        if (!temp.isNullOrEmpty()) results.add(temp)

        return results
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
        val periodSplit = (key.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray())
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

    fun objToCS(cs: ConfigurationSection?, path: String): ConfigurationSection? {
        if (cs == null) {
            return null
        }
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
}