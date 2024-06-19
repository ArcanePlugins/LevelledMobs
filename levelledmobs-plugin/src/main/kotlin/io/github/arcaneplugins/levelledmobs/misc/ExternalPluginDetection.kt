package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.enums.ExternalCompatibility
import io.github.arcaneplugins.levelledmobs.util.Log
import io.github.arcaneplugins.levelledmobs.wrappers.LivingEntityWrapper
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class ExternalPluginDetection(
    private val pluginName: String,
    var friendlyName: String,
    private val keyName: String,
    private val requirement: RequirementTypes,
    private val keyType: KeyTypes
) {
    var requirementValue: String? = null
    var externalCompatibility: ExternalCompatibility? = null
    var placeholderName: String? = null
    var keyValueType: String? = "string"
        set(value) {
            field = value?.lowercase()
        }
    private var cachedPlugin: Plugin? = null
    private var cachedMamespaceKey: NamespacedKey? = null

    val isBuiltIn: Boolean
        get() = this.externalCompatibility != null

    enum class KeyTypes{
        METADATA,
        PDC
    }

    enum class RequirementTypes{
        EXISTS,
        NOT_EXISTS,
        CONTAINS,
        NOT_CONTAINS
    }

    fun isMobOfType(
        lmEntity: LivingEntityWrapper
    ): Boolean{
        if (this.isBuiltIn)
            throw Exception("External plugin definition for $friendlyName is built-in. " +
                    "Use it's corresponding internal methods")

        var plugin = cachedPlugin
        if (plugin == null){
            plugin = Bukkit.getPluginManager().getPlugin(pluginName)
            if (plugin == null || !plugin.isEnabled) return false

            cachedPlugin = plugin
        }

        val result = if (keyType == KeyTypes.PDC)
            checkPDCkey(lmEntity)
        else
            checkMetadataKey(lmEntity)

        if (result)
            lmEntity.setMobExternalType(friendlyName)

        return result
    }

    private fun checkPDCkey(
        lmEntity: LivingEntityWrapper
    ): Boolean{
        val keyExists: Boolean
        var namespaceKey = cachedMamespaceKey
        if (namespaceKey == null){
            namespaceKey = NamespacedKey(cachedPlugin!!, keyName)
            cachedMamespaceKey = namespaceKey
        }

        when (keyValueType){
            null, "" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.STRING) }
            "double" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.DOUBLE) }
            "integer", "int" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.INTEGER) }
            "byte_array" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.BYTE_ARRAY) }
            "byte" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.BYTE) }
            "boolean", "bool" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.BOOLEAN) }
            "float" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.FLOAT) }
            "integer_array" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.INTEGER_ARRAY) }
            "long" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.LONG) }
            "long_array" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.LONG_ARRAY) }
            "short" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.SHORT) }
            "tag_container" -> { keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.TAG_CONTAINER) }
            else -> {
                Log.sev("Invalid key-valuetype: $keyValueType")
                keyExists = lmEntity.pdc.has(namespaceKey, PersistentDataType.STRING)
            }
        }

        when (requirement){
            RequirementTypes.EXISTS -> { return keyExists }
            RequirementTypes.NOT_EXISTS -> { return !keyExists }
            RequirementTypes.CONTAINS, RequirementTypes.NOT_CONTAINS -> {
                if (this.requirementValue.isNullOrEmpty()) return false
                if (!keyExists) return false
                val keyValue: String?

                when (keyValueType){
                    null, "" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.STRING) }
                    "double" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.DOUBLE).toString() }
                    "integer", "int" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.INTEGER).toString() }
                    "byte_array" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.BYTE_ARRAY).toString() }
                    "byte" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.BYTE).toString() }
                    "boolean", "bool" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.BOOLEAN).toString() }
                    "float" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.FLOAT).toString() }
                    "integer_array" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.FLOAT).toString() }
                    "long" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.LONG).toString() }
                    "long_array" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.LONG_ARRAY).toString() }
                    "short" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.SHORT).toString() }
                    "tag_container" -> { keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.TAG_CONTAINER).toString() }
                    else -> {
                        keyValue = lmEntity.pdc.get(namespaceKey, PersistentDataType.STRING)
                    }
                }

                if (keyValue.isNullOrEmpty()){
                    return requirement != RequirementTypes.CONTAINS
                }

                val itContains = keyValue.contains(this.requirementValue!!, ignoreCase = true)
                return if (requirement == RequirementTypes.CONTAINS)
                    itContains
                else
                    !itContains
            }
        }
    }

    private fun checkMetadataKey(lmEntity: LivingEntityWrapper): Boolean{
        val keyExists = lmEntity.livingEntity.hasMetadata(keyName)

        when (requirement){
            RequirementTypes.EXISTS -> { return keyExists }
            RequirementTypes.NOT_EXISTS -> { return !keyExists }
            RequirementTypes.CONTAINS, RequirementTypes.NOT_CONTAINS -> {
                if (this.requirementValue.isNullOrEmpty()) return false
                if (!keyExists) return false
                val keyValues = lmEntity.livingEntity.getMetadata(keyName)

                if (keyValues.isEmpty()){
                    return requirement != RequirementTypes.CONTAINS
                }

                for (value in keyValues){
                    if (value == null) continue
                    val itContains = value.value().toString().contains(this.requirementValue!!, ignoreCase = true)
                    if (itContains && requirement == RequirementTypes.CONTAINS) return true
                }

                return requirement != RequirementTypes.CONTAINS
            }
        }
    }

    fun getPlaceholder(
        lmEntity: LivingEntityWrapper
    ): String{
        if (this.placeholderName.isNullOrEmpty()) return ""

        if (keyType == KeyTypes.PDC){
            var plugin = cachedPlugin
            if (plugin == null){
                plugin = Bukkit.getPluginManager().getPlugin(pluginName)
                if (plugin == null || !plugin.isEnabled) return ""

                cachedPlugin = plugin
            }

            var namespaceKey = cachedMamespaceKey
            if (namespaceKey == null){
                namespaceKey = NamespacedKey(cachedPlugin!!, keyName)
                cachedMamespaceKey = namespaceKey

            }

            if (!lmEntity.pdc.has(namespaceKey, PersistentDataType.STRING)) return ""
            val result = lmEntity.pdc.get(namespaceKey, PersistentDataType.STRING)
            return result ?: ""
        }
        else{
            if (!lmEntity.livingEntity.hasMetadata(keyName)) return ""
            val result = lmEntity.livingEntity.getMetadata(keyName)
            if (result.isEmpty()) return ""
            return result.first().value().toString()
        }
    }

    fun clearDetectionCache(){
        this.cachedPlugin = null
        this.cachedMamespaceKey = null
    }

    override fun toString(): String {
        var msg = "${friendlyName}: $keyName, keytype: ${requirement.toString().lowercase()}"
        if (!placeholderName.isNullOrEmpty())
            msg += " placeholder: $placeholderName"

        return msg
    }
}