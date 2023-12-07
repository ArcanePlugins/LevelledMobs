package io.github.arcaneplugins.levelledmobs.bukkit.config.translations

import io.github.arcaneplugins.levelledmobs.bukkit.LevelledMobs
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.inf
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.war
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.jetbrains.annotations.NotNull
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.Path
import java.util.function.Function

class TranslationHandler {

    val latestFileVersion = 1;
    val updaterCutoffFileVersion = 1

    private val defaultLang = InbuiltLang.getDefault()
    private val defaultLangStr = defaultLang.toString()
    private var lang = defaultLangStr

    private var loader: YamlConfigurationLoader? = null
    private var root: CommentedConfigurationNode? = null

    /**
     * Attempts to load user's chosen translation.
     * This will always return true. This is just to make it cleaner to keep the order of onEnable's
     * load calls.
     * If all fails with this method, then LM will just use the defaults from {@link Message}.
     *
     * @return {@link Boolean#TRUE}
     */
    fun load(): Boolean{
        /*
        [pseudocode]
        # this code loads translation specified by the user

        set lang = the user's configured language
        set langToPathFun = function of <String input, Path output>, where input is 'langInput', and output is: "%dataFolder%/translations/%langInput%.yml"

        loop while file at path %langToPathFun.apply(lang)% does not exist
            if internal translation available for lang
                save internal translation resource to data folder
                break loop
            else
                # avoiding a possible stack overflow in this loop if the file doesnt save for some reason
                assert lang != en_us

                # the user specified a language without providing a translation for it
                log [severe] language %lang% specified, but no translation file found. defaulting to en_us

                # let's just default to en_us
                set lang = en_us

                # we continue the loop so that it can generate the file again but this time for en_us
                continue loop

        load translation config from file
         */

        /*
        [pseudocode]
        # this code loads translation specified by the user

        set lang = the user's configured language
        set langToPathFun = function of <String input, Path output>, where input is 'langInput', and output is: "%dataFolder%/translations/%langInput%.yml"

        loop while file at path %langToPathFun.apply(lang)% does not exist
            if internal translation available for lang
                save internal translation resource to data folder
                break loop
            else
                # avoiding a possible stack overflow in this loop if the file doesnt save for some reason
                assert lang != en_us

                # the user specified a language without providing a translation for it
                log [severe] language %lang% specified, but no translation file found. defaulting to en_us

                # let's just default to en_us
                set lang = en_us

                # we continue the loop so that it can generate the file again but this time for en_us
                continue loop

        load translation config from file
         */
        var lang: String = LevelledMobs.lmInstance
            .configHandler
            .settingsCfg
            .root!!
            .node("lang")
            .getString(defaultLangStr)


        val langToPathFun =
            Function<String, Path> { langInput: String ->
                Path.of(
                    "${LevelledMobs.lmInstance.dataFolder}${File.separator}translations" +
                            "${File.separator}$langInput.yml"
                )
            }

        while(!langToPathFun.apply(lang).toFile().exists()) {
            inf("Translation not immediately available - checking")

            val inbuilt = InbuiltLang.of(lang)
            if (inbuilt == null) {
                // avoiding a possible stack overflow in this loop if the file doesnt save for some reason
                if (!lang.equals(defaultLangStr, ignoreCase = true)) {
                    sev(
                        "Countered a possible loop issue for translation selection.",
                        true
                    )
                    return true
                }

                // the user specified a language without providing a translation for it
                sev(
                    "Lang '$lang' is not an inbuilt lang, and no custom translation"
                            + " file was found for it. Falling back to '$defaultLangStr' until you "
                            + "fix it.", true
                )

                // let's just default to en_us
                lang = defaultLangStr

                // we continue the loop so that it can generate
                // the file again but this time for en_us
                // noinspection UnnecessaryContinue
                continue
            } else {
                // make sure the lang is using the correct format, xx_YY.
                // this is done by just grabbing it straight from the InbuiltLang constant.
                lang = inbuilt.toString()
                inf("Saving default translation for lang '$lang'")
                LevelledMobs.lmInstance.saveResource(
                    "translations${File.separator}$lang.yml", false
                )
                break
            }
        }

        this.lang = lang

        // note: should not need to check if loader is already set, as the file name is dynamic.
        // this is unlike other config files (see: Config#load())
        loader = YamlConfigurationLoader.builder().path(
            Path.of(
                LevelledMobs.lmInstance.dataFolder.absolutePath +
                        File.separator + "translations${File.separator}$lang.yml"
            )
        ).build()

        try {
            root = (loader as YamlConfigurationLoader).load()
        } catch (ex: ConfigurateException) {
            sev(
                "Unable to load translation '$lang'. This is usually a " +
                        "user-caused error caused from YAML syntax errors inside the file, such as an " +
                        "incorrect indent or stray symbol. We recommend that you use a YAML parser " +
                        "website - such as the one linked here - to help locate where these errors " +
                        "are appearing. --> https://www.yaml-online-parser.appspot.com/ <-- A stack " +
                        "trace will be printed below for debugging purposes.", true
            )
            ex.printStackTrace()
            return true
        }

        /*
        update translations
         */

        /*
        update translations
         */update()

        /*
        load messages
         */
        for (message in Message.entries) {
            val node: CommentedConfigurationNode = root!!.node(message.keyPath)

            if (node.empty()) {
                war(
                    "A message is missing its translation at path " +
                            message.keyPath +
                            ". Using a default value until you fix it.", true
                )
                continue
            }

            try {
                if (message.isListType && node.isList) {
                    val list = node.getList(String::class.java)
                    if (list == null) {
                        message.declared = mutableListOf()
                    } else {
                        message.declared = list
                    }
                } else {
                    message.declared = mutableListOf("")
                }
            } catch (ex: ConfigurateException) {
                war(
                    "Unable to parse translation at path '${message.keyPath}'. " +
                            "This is usually caused by the " +
                            "user accidentally creating a syntax error whilst editing the file.", true
                )
            }
        }

        return true
    }

    fun update(){
        var currentFileVersion = currentFileVersion

        if (currentFileVersion == 0) {
            sev(
                "Unable to detect file version of translation '$lang'. Was " +
                        "it modified by the user?", true
            )
            return
        }

        if (currentFileVersion > latestFileVersion) {
            war(
                "Translation '$lang' is somehow newer than the latest "
                        + "compatible file version. How did we get here?", false
            )
            return
        }

        if (currentFileVersion < updaterCutoffFileVersion) {
            val heading = ("Translation '$lang' is too old for LevelledMobs to "
                    + "update. ")

            // make a different recommendation based upon whether the translation is inbuilt
            if (InbuiltLang.of(lang) == null) {
                // not an inbuilt translation
                sev(
                    heading + "As this seems to be a 'custom' translation, we recommend " +
                            "you to store a backup of the translation file in a separate location, then " +
                            "remove the file from the 'plugins/LevelledMobs/translations' directory, and " +
                            "switch to an inbuilt translation such as 'en_US' for the time being. Then, " +
                            "customize the new translation file as you wish.", true
                )
            } else {
                // an inbuilt translation
                sev(
                    (heading + "As this translation is an 'inbuilt' translation, you can " +
                            "simply remove the file and allow LevelledMobs to generate the latest one " +
                            "for you automatically. If you have made any edits to this translation file, " +
                            "remember to back it up and transfer the edits to the newly generated file."),
                    true
                )
            }
            return
        }

        while (currentFileVersion < latestFileVersion) {
            inf(
                "Upgrading translation '$lang' from file version '" +
                        currentFileVersion + "' to '" + (currentFileVersion + 1) + "'."
            )
            when (currentFileVersion) {
                Int.MIN_VALUE -> {
                    // Example migration code
                    currentFileVersion++
                    try {
                        root?.node("metadata", "version", "current")?.set(currentFileVersion)
                        loader?.save(root)
                    } catch (ex: ConfigurateException) {
                        sev(
                            "Unable to write updates to file of lang '$lang'.",
                            true
                        )
                        return
                    }
                }

                else -> {
                    sev(
                        ("Attempted to update from file version '" + currentFileVersion +
                                "' of translation '$lang', but no updater logic is present " +
                                "for that file version."), true
                    )
                    return
                }
            }
        }
    }

    val currentFileVersion: Int
        get() {
            return root?.node("metadata", "version", "current")?.getInt(0) ?: 0
        }

    fun getEntityName(entity: Entity) : String{
        return entity.customName?: entity.type.name
    }

    fun getEntityName(entityType: EntityType): String{
        return entityType.translationKey()
    }


}