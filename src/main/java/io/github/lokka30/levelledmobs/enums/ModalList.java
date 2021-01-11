package io.github.lokka30.levelledmobs.enums;

import io.github.lokka30.levelledmobs.utils.Utils;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * This enum will significantly decrease clutter from checking lists from the settings file with configurable modes (e.g. whitelist/blacklist)
 */
//TODO Should be used for CreatureSpawnListener.84 for example.
public enum ModalList {
    ALL,
    WHITELIST,
    BLACKLIST;

    public static ModalList fromString(String mode) {
        assert mode != null;
        switch (mode.toUpperCase()) {
            case "ALL":
                return ModalList.ALL;
            case "WHITELIST":
                return ModalList.WHITELIST;
            case "BLACKLIST":
                return ModalList.BLACKLIST;
            default:
                throw new IllegalStateException("Invalid ModalList '" + mode + "'!");
        }
    }

    public static boolean isEnabledInList(YamlConfiguration cfg, String path, String item) {
        if (cfg.contains(path + ".mode")) {
            @SuppressWarnings("ConstantConditions")
            ModalList mode = ModalList.fromString(cfg.getString(path + ".mode"));

            switch (mode) {
                case ALL:
                    return true;
                case WHITELIST:
                    return cfg.getStringList(path + ".list").contains(item);
                case BLACKLIST:
                    return !cfg.getStringList(path + ".list").contains(item);
                default:
                    throw new IllegalStateException("Invalid ModalList " + mode.toString() + "!");
            }
        } else {
            Utils.logger.error("Mode is unset in config file '" + cfg.getName() + "' at path '" + path + ".mode'! The plugin will malfunction until this is fixed!");
            return false;
        }
    }
}
