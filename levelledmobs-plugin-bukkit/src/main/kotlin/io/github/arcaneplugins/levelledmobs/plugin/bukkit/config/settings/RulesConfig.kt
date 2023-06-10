package io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.settings

import io.github.arcaneplugins.levelledmobs.plugin.bukkit.config.YamlConfig

class RulesConfig : YamlConfig(
    fileName = "rules",
    isResourceFile = true,
    latestFileVersion = 4,
) {
    override fun migrateToNextVersion() {
        TODO("Not yet implemented")
    }
}