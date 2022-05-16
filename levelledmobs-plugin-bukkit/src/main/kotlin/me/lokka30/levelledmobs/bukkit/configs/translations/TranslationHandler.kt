package me.lokka30.levelledmobs.bukkit.configs.translations

import me.lokka30.levelledmobs.bukkit.LevelledMobs
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logInf
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logSev
import me.lokka30.levelledmobs.bukkit.LevelledMobs.Companion.logWar
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

/*
FIXME comment
 */
class TranslationHandler {

    companion object {
        /*
        FIXME comment
         */
        val inbuiltTranslations = setOf(
            "de_DE",
            "en_AU",
            "en_GB",
            "en_US",
            "es_ES",
            "fr_FR"
        )

        /*
        FIXME comment
         */
        const val latestFileVersion = 1

        /*
        FIXME comment
         */
        const val updaterCutoffFileVersion = 1
    }

    /*
    FIXME comment
     */
    var lang: String = "en_US"
        private set

    /*
    FIXME comment
     */
    var loader: YamlConfigurationLoader? = null
        private set

    /*
    FIXME comment
     */
    var root: CommentedConfigurationNode? = null
        private set

    /*
    Load translations
     */
    fun load() {
        logInf("Loading translations...")

        // if translations directory does not exist, create it
        val translationsPathStr =
            "${LevelledMobs.instance!!.dataFolder.absolutePath}${File.separator}translations"
        val translationsPath = Path.of(translationsPathStr)
        if (!translationsPath.exists())
            translationsPath.toFile().mkdir()

        // lang = lang specified in settings.yml
        lang = LevelledMobs
            .instance!!
            .configHandler
            .settingsCfg
            .root!!
            .node("lang")
            .getString("en_US")


        /*
        if the specified language path does not exist on the file system
            if the specified language is not an inbuilt language
                fall back to en_US and warn console
            save resource
         */
        var langPath = Path.of("${translationsPathStr}${File.separator}${lang}.yml")
        if (!langPath.exists()) {
            val inbuilt = inbuiltTranslations.firstOrNull { it.equals(lang, ignoreCase = true) }
            if (inbuilt == null) {
                logWar("Translation for language '${lang}' does not exist; using en_US.")
                lang = "en_US"
                langPath = Path.of("${translationsPathStr}${File.separator}${lang}.yml")
            }

            // copy resource
            LevelledMobs.instance!!
                .saveResource("translations${File.separator}${lang}.yml", false)

            logInf("Saved default translation for '${lang}'.")
        }

        /*
        create loader object
        get root config section
         */
        logInf("Loading contents of translation '${lang}'...")
        loader = YamlConfigurationLoader.builder()
            .path(langPath)
            .build()

        try {
            root = loader!!.load()
        } catch (ex: ConfigurateException) {
            logSev(
                "Unable to load translation '${lang}'. This is usually a " +
                        "user-caused error caused from YAML syntax errors inside the file, such as an " +
                        "incorrect indent or stray symbol. We recommend that you use a YAML parser " +
                        "website - such as the one linked here - to help locate where these errors " +
                        "are appearing. --> https://www.yaml-online-parser.appspot.com/ <-- A stack trace " +
                        "will be printed below for debugging purposes."
            )
            ex.printStackTrace()
            return
        }

        update()
    }

    /*
    FIXME comment
     */
    private fun update() {
        var currentFileVersion = root!!.node("metadata", "version", "current").getInt(0)

        if (currentFileVersion == 0) {
            logSev("Unable to detect file version of translation '${lang}'. Was it modified?")
            return
        }

        if (currentFileVersion > latestFileVersion) {
            logWar(
                "Translation '${lang}' is somehow newer than the latest compatible file " +
                        "version. How did we get here?"
            )
            return
        }


        while (currentFileVersion < latestFileVersion) {
            logInf(
                "Updating translation '${lang}' from file version '${currentFileVersion}' " +
                        "to '${currentFileVersion + 1}'..."
            )

            when (currentFileVersion) {
                12345 -> {
                    // some migration code  here ...
                    currentFileVersion++
                    root!!.node("metadata", "version", "current").set(currentFileVersion)
                    loader!!.save(root)
                }
                else -> {
                    logSev(
                        "Attempted to update from file version '${currentFileVersion}' of translation " +
                                "'${lang}', but no updater logic is present for that file " +
                                "version. Please inform LM support, as this should be impossible."
                    )
                    return
                }
            }
        }
    }

}