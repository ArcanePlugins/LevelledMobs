package io.github.arcaneplugins.levelledmobs.misc

class MobPluginDetection(
    val pluginName: String,
    val keyName: String,
    val requirement: RequirementTypes
) {
    var requirementValue: String? = null

    enum class RequirementTypes{
        EXISTS,
        NOT_EXISTS,
        CONTAINS,
        NOT_CONTAINS
    }
}