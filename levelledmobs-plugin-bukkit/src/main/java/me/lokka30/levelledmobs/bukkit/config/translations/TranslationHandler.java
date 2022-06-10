package me.lokka30.levelledmobs.bukkit.config.translations;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import me.lokka30.levelledmobs.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.bukkit.util.EnumUtils;
import me.lokka30.levelledmobs.bukkit.util.Log;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

@SuppressWarnings({"FieldCanBeLocal"})
public final class TranslationHandler {

    /* vars */

    private final int latestFileVersion = 1;
    private final int updaterCutoffFileVersion = 1;

    private final InbuiltLang defaultLang = InbuiltLang.getDefault();
    private final String defaultLangStr = defaultLang.toString();

    @SuppressWarnings("FieldMayBeFinal")
    private String lang = defaultLangStr;

    private YamlConfigurationLoader loader = null;
    private CommentedConfigurationNode root = null;

    /* methods */

    /**
     * Attempts to load user's chosen translation.
     *
     * This will always return true. This is just to make it cleaner to keep the order of onEnable's
     * load calls.
     *
     * If all fails with this method, then LM will just use the defaults from {@link Message}.
     *
     * @return {@link Boolean#TRUE}
     */
    public boolean load() {
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
        Log.inf("Loading translations");

        var lang = LevelledMobs.getInstance()
            .getConfigHandler()
            .getSettingsCfg()
            .getRoot()
            .node("lang")
            .getString(defaultLangStr);

        final Function<String, Path> langToPathFun = (langInput) -> Path.of(
            LevelledMobs.getInstance().getDataFolder() + File.separator + "translations" +
                File.separator + langInput + ".yml"
        );

        while(!langToPathFun.apply(lang).toFile().exists()) {
            Log.inf("Translation not immediately available - checking");

            final @Nullable var inbuilt = InbuiltLang.of(lang);

            if(inbuilt == null) {
                // avoiding a possible stack overflow in this loop if the file doesnt save for some reason
                if(!lang.equalsIgnoreCase(defaultLangStr)) {
                    Log.sev("Countered a possible loop issue for translation selection.",
                        true);
                    return true;
                }

                // the user specified a language without providing a translation for it
                Log.sev("Lang '" + lang + "' is not an inbuilt lang, and no custom translation"
                    + " file was found for it. Falling back to '" + defaultLangStr + "' until you "
                    + "fix it.", true);

                // let's just default to en_us
                lang = defaultLangStr;

                // we continue the loop so that it can generate the file again but this time for en_us
                // noinspection UnnecessaryContinue
                continue;
            } else {
                // make sure the lang is using the correct format, ab_CD.
                // this is done by just grabbing it straight from the InbuiltLang constant.
                lang = inbuilt.toString();

                Log.inf("Saving default translation for lang '" + lang + "'");

                LevelledMobs.getInstance().saveResource(
                    "translations" + File.separator + lang + ".yml", false);

                break;
            }
        }

        this.lang = lang;
        Log.inf("Using translation '" + getLang() + "'");

        // note: should not need to check if loader is already set, as the file name is dynamic.
        // this is unlike other config files (see: Config#load())

        loader = YamlConfigurationLoader.builder().path(Path.of(
            LevelledMobs.getInstance().getDataFolder().getAbsolutePath() +
                File.separator + "translations" + File.separator + getLang() + ".yml"
        )).build();

        try {
            root = getLoader().load();
        } catch(ConfigurateException ex) {
            Log.sev(
                "Unable to load translation '" + getLang() + "'. This is usually a " +
                    "user-caused error caused from YAML syntax errors inside the file, such as an " +
                    "incorrect indent or stray symbol. We recommend that you use a YAML parser " +
                    "website - such as the one linked here - to help locate where these errors " +
                    "are appearing. --> https://www.yaml-online-parser.appspot.com/ <-- A stack " +
                    "trace will be printed below for debugging purposes.", true
            );
            ex.printStackTrace();
            return true;
        }

        /*
        update translations
         */
        update();

        /*
        load messages
         */
        for(var message : Message.values()) {
            final var node = getRoot().node((Object[]) message.getKeyPath());

            if(node.empty()) {
                Log.war("A message is missing its translation at path " +
                    Arrays.toString(message.getKeyPath()) +
                    ". Using a default value until you fix it.", true);
                continue;
            }

            try {
                if(message.isListType() && node.isList()) {
                    final var list = node.getList(String.class);
                    if(list == null) {
                        message.setDeclared(new String[]{});
                    } else {
                        message.setDeclared(list.toArray(new String[0]));
                    }
                } else {
                    message.setDeclared(new String[]{node.getString("")});
                }
            } catch(ConfigurateException ex) {
                Log.war("Unable to parse translation at path '" +
                    Arrays.toString(message.getKeyPath()) + "'. This is usually caused by the " +
                    "user accidentally creating a syntax error whilst editing the file.", true);
            }
        }

        return true;
    }

    private void update() {
        var currentFileVersion = getCurrentFileVersion();

        if(currentFileVersion == 0) {
            Log.sev("Unable to detect file version of translation '" + getLang() + "'. Was " +
                "it modified by the user?", true);
            return;
        }

        if(currentFileVersion > getLatestFileVersion()) {
            Log.war("Translation '" + getLang() + "' is somehow newer than the latest "
                + "compatible file version. How did we get here?", false);
            return;
        }

        if(currentFileVersion < getUpdaterCutoffFileVersion()) {
            final var heading = "Translation '" + getLang() + "' is too old for LevelledMobs to "
                + "update. ";

            // make a different recommendation based upon whether the translation is inbuilt
            if(InbuiltLang.of(getLang()) == null) {
                // not an inbuilt translation
                Log.sev(heading + "As this seems to be a 'custom' translation, we recommend " +
                    "you to store a backup of the translation file in a separate location, then " +
                    "remove the file from the 'plugins/LevelledMobs/translations' directory, and " +
                    "switch to an inbuilt translation such as 'en_US' for the time being. Then, " +
                    "customize the new translation file as you wish.", true);
            } else {
                // an inbuilt translation
                Log.sev(heading + "As this translation is an 'inbuilt' translation, you can " +
                    "simply remove the file and allow LevelledMobs to generate the latest one " +
                    "for you automatically. If you have made any edits to this translation file, " +
                    "remember to back it up and transfer the edits to the newly generated file.",
                    true);
            }
            return;
        }

        //noinspection ConstantConditions
        while(currentFileVersion < getLatestFileVersion()) {
            Log.inf("Upgrading translation '" + getLang() + "' from file version '" +
                currentFileVersion + "' to '" + (currentFileVersion + 1) + "'.");

            switch(currentFileVersion) {
                case Integer.MIN_VALUE -> {
                    // Example migration code
                    currentFileVersion++;
                    try {
                        getRoot().node("metadata", "version", "current").set(currentFileVersion);
                        getLoader().save(getRoot());
                    } catch(ConfigurateException ex) {
                        Log.sev("Unable to write updates to file of lang '" + getLang() + "'.",
                            true);
                        return;
                    }
                }
                default -> {
                    Log.sev("Attempted to update from file version '" + currentFileVersion +
                        "' of translation '" + getLang() + "', but no updater logic is present " +
                        "for that file version.", true);
                    return;
                }
            }
        }
    }

    public int getCurrentFileVersion() {
        return getRoot().node("metadata", "version", "current").getInt(0);
    }

    public String getEntityName(final Entity entity) {
        Objects.requireNonNull(entity, "entity");
        //TODO
        return getEntityName(entity.getType());
    }

    public String getEntityName(final @NotNull EntityType entityType) {
        Objects.requireNonNull(entityType, "entityType");
        //TODO
        return EnumUtils.formatEnumConstant(entityType);
    }

    /* var getters and setters */

    public int getLatestFileVersion() { return latestFileVersion; }

    public int getUpdaterCutoffFileVersion() { return updaterCutoffFileVersion; }

    public String getLang() { return lang; }

    public YamlConfigurationLoader getLoader() { return loader; }

    public CommentedConfigurationNode getRoot() { return root; }

}
