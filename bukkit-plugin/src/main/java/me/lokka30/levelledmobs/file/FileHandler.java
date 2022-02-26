/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file;

import me.lokka30.levelledmobs.customdrop.CustomDropParser;
import me.lokka30.levelledmobs.file.external.customdrops.CustomDropsFile;
import me.lokka30.levelledmobs.file.external.groups.GroupsFile;
import me.lokka30.levelledmobs.file.external.listeners.ListenersFile;
import me.lokka30.levelledmobs.file.external.misc.license.LicenseFile;
import me.lokka30.levelledmobs.file.external.presets.PresetsFile;
import me.lokka30.levelledmobs.file.external.readme.ReadmeFile;
import me.lokka30.levelledmobs.file.external.settings.SettingsFile;
import me.lokka30.levelledmobs.file.external.translations.constants.ConstantsFile;
import me.lokka30.levelledmobs.file.external.translations.messages.MessagesFile;
import me.lokka30.levelledmobs.file.internal.playerHeadTextures.PlayerHeadTexturesFile;
import me.lokka30.levelledmobs.file.internal.unlevellables.UnlevellablesFile;
import me.lokka30.levelledmobs.rule.parsing.RuleParser;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FileHandler {

    public FileHandler() {
        /* external files */
        this.customDropsFile = new CustomDropsFile();
        this.groupsFile = new GroupsFile();
        this.listenersFile = new ListenersFile();
        this.licenseFile = new LicenseFile();
        this.presetsFile = new PresetsFile();
        this.readmeFile = new ReadmeFile();
        this.settingsFile = new SettingsFile();
        this.constantsFile = new ConstantsFile();
        this.messagesFile = new MessagesFile();

        /* internal files */
        this.playerHeadTexturesFile = new PlayerHeadTexturesFile();
        this.unlevellablesFile = new UnlevellablesFile();

        /* parsers */
        this.ruleParser = new RuleParser();
        this.customDropParser = new CustomDropParser();
    }

    /* External Files */

    private final @NotNull CustomDropsFile customDropsFile;
    public @NotNull CustomDropsFile getCustomDropsFile() { return customDropsFile; }

    private final @NotNull GroupsFile groupsFile;
    public @NotNull GroupsFile getGroupsFile() { return groupsFile; }

    private final @NotNull ListenersFile listenersFile;
    public @NotNull ListenersFile getListenersFile() { return listenersFile; }

    private final @NotNull LicenseFile licenseFile;
    public @NotNull LicenseFile getLicenseFile() { return licenseFile; }

    private final @NotNull PresetsFile presetsFile;
    public @NotNull PresetsFile getPresetsFile() { return presetsFile; }

    private final @NotNull ReadmeFile readmeFile;
    public @NotNull ReadmeFile getReadmeFile() { return readmeFile; }

    private final @NotNull SettingsFile settingsFile;
    public @NotNull SettingsFile getSettingsFile() { return settingsFile; }

    private final @NotNull ConstantsFile constantsFile;
    public @NotNull ConstantsFile getConstantsFile() { return constantsFile; }

    private final @NotNull MessagesFile messagesFile;
    public @NotNull MessagesFile getMessagesFile() { return messagesFile; }

    /* Internal Files */

    private final @NotNull PlayerHeadTexturesFile playerHeadTexturesFile;
    public @NotNull PlayerHeadTexturesFile getPlayerHeadTexturesFile() { return playerHeadTexturesFile; }

    private final @NotNull UnlevellablesFile unlevellablesFile;
    public @NotNull UnlevellablesFile getUnlevellablesFile() { return unlevellablesFile; }

    /* Parsers */

    private final @NotNull RuleParser ruleParser;
    public @NotNull RuleParser getRuleParser() { return ruleParser; }

    private final @NotNull CustomDropParser customDropParser;
    public @NotNull CustomDropParser getCustomDropParser() { return customDropParser; }

    /* Methods */

    public void loadAll(final boolean fromReload) {
        loadInternalFiles(fromReload);
        loadExternalFiles(fromReload);
        loadParsers();
    }

    /**
     * @author lokka30
     * @since 4.0.0
     * Load all internal files.
     * Unnecessary to run on reload - improve performance by
     * not running this method on reload. Thus it should only
     * be ran on start-up (onEnable).
     */
    public void loadInternalFiles(final boolean fromReload) {
        Utils.LOGGER.info("Started loading internal files...");

        List.of(
                playerHeadTexturesFile,
                unlevellablesFile
        ).forEach(file -> file.load(fromReload));

        Utils.LOGGER.info("All internal files have been loaded.");
    }

    /**
     * @author lokka30
     * @since 4.0.0
     * (Re)load all external files.
     * This must be called on start-up and also on reload.
     */
    public void loadExternalFiles(final boolean fromReload) {
        Utils.LOGGER.info("Started loading external files...");

        List.of(
                customDropsFile,
                groupsFile,
                listenersFile,
                licenseFile,
                presetsFile,
                readmeFile,
                settingsFile,
                constantsFile,
                messagesFile
        ).forEach(file -> file.load(fromReload));

        Utils.LOGGER.info("All external files have been loaded.");
    }

    public void loadParsers() {
        ruleParser.parse();
        customDropParser.parse();
    }

}
