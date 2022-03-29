/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.file;

import java.util.List;
import me.lokka30.levelledmobs.plugin.bukkit.customdrop.CustomDropParser;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.customdrops.CustomDropsFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.groups.GroupsFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.listeners.ListenersFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.misc.license.LicenseFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.presets.PresetsFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.readme.ReadmeFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.settings.SettingsFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.translations.constants.ConstantsFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.external.translations.messages.MessagesFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.internal.playerHeadTextures.PlayerHeadTexturesFile;
import me.lokka30.levelledmobs.plugin.bukkit.file.internal.unlevellables.UnlevellablesFile;
import me.lokka30.levelledmobs.plugin.bukkit.rule.parsing.RuleParser;
import me.lokka30.levelledmobs.plugin.bukkit.util.Utils;

public class FileHandler {

    /* External Files */

    public final CustomDropsFile customDropsFile = new CustomDropsFile();
    public final GroupsFile groupsFile = new GroupsFile();
    public final ListenersFile listenersFile = new ListenersFile();
    public final LicenseFile licenseFile = new LicenseFile();
    public final PresetsFile presetsFile = new PresetsFile();
    public final ReadmeFile readmeFile = new ReadmeFile();
    public final SettingsFile settingsFile = new SettingsFile();
    public final ConstantsFile constantsFile = new ConstantsFile();
    public final MessagesFile messagesFile = new MessagesFile();

    /* Internal Files */

    public final PlayerHeadTexturesFile playerHeadTexturesFile = new PlayerHeadTexturesFile();
    public final UnlevellablesFile unlevellablesFile = new UnlevellablesFile();

    /* Parsers */

    public final RuleParser ruleParser = new RuleParser();
    public final CustomDropParser customDropParser = new CustomDropParser();

    /* Methods */

    public void loadAll(final boolean fromReload) {
        loadInternalFiles(fromReload);
        loadExternalFiles(fromReload);
        loadParsers();
    }

    /**
     * @author lokka30
     * @since 4.0.0 Load all internal files. Unnecessary to run on reload - improve performance by
     * not running this method on reload. Thus it should only be ran on start-up (onEnable).
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
     * @since 4.0.0 (Re)load all external files. This must be called on start-up and also on reload.
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
