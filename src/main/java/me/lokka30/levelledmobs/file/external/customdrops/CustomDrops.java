/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.file.external.customdrops;

import de.leonhard.storage.Yaml;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.file.external.VersionedFile;
import me.lokka30.levelledmobs.file.external.YamlExternalFile;
import me.lokka30.levelledmobs.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class CustomDrops implements YamlExternalFile, VersionedFile {

    private final @NotNull LevelledMobs main;
    public CustomDrops(final @NotNull LevelledMobs main) {
        this.main = main;
        this.data = new Yaml("customdrops", main.getDataFolder() + File.separator + "customdrops.yml");
    }

    private final Yaml data;

    @Override
    public void load(boolean isReload) {
        if(isReload) {
            data.forceReload();
        }
    }

    @Override
    public String getName() {
        return "customdrops.yml";
    }

    @Override
    public String getRelativePath() {
        return getName();
    }

    @Override
    public int getInstalledFileVersion() {
        return getData().getOrDefault("file-data-do-not-edit.version", -1);
    }

    @Override
    public int getSupportedFileVersion() {
        return 11;
    }

    @Override
    public void migrate() {
        switch(compareFileVersion()) {
            case CURRENT:
                return;
            case FUTURE:
                Utils.LOGGER.warning(
                        "Your '&b" + getName() + "&7' file is running a version &onewer&7 " +
                                "than what is supported by this version of LevelledMobs. " +
                                "Please update LevelledMobs, unless this is a development error.");
                Utils.LOGGER.warning("&8 -> &7Installed file version: &b" + getInstalledFileVersion());
                Utils.LOGGER.warning("&8 -> &7Supported file version: &b" + getSupportedFileVersion());
                Utils.LOGGER.warning("&8 -> &7Plugin version: &b" + main.getDescription().getVersion());
                return;
            case OUTDATED:
                for(int i = getInstalledFileVersion(); i < getSupportedFileVersion(); i++) {
                    Utils.LOGGER.info("Attempting to migrate file '&b" + getName() + "&7' to version '&b" + i + "&7'...");

                    switch(i) {
                        case 10:
                            //TODO migrate from LM3 to LM4 customdrops.
                            Utils.LOGGER.error("Migration logic not complete for LM 3's customdrops yet.");
                            break;
                        default:
                            if(i < 10) {
                                // Latest version of LM 3 used file version 10.
                                // if it's earlier than this then just don't bother migrating,
                                // users should have updated. don't want to bloat the migrator.
                                backup(main);
                                replace(main);
                                Utils.LOGGER.warning("Your customdrops file was too old to be migrated! " +
                                        "We have replaced your custom drops file with a default, brand new one. " +
                                        "Your previous customdrops file was backed up.");
                            } else {
                                Utils.LOGGER.warning("Migration logic was not programmed for the file version '&b" + i + "&7' of the file '&b" + getName() + "&7'! Please inform the LevelledMobs team.");
                            }
                    }
                }
                Utils.LOGGER.info("Migration complete for file '&b" + getName() + "&7'.");
                return;
            default:
                throw new IllegalStateException(compareFileVersion().toString());
        }
    }

    @NotNull
    @Override
    public Yaml getData() {
        return data;
    }
}
