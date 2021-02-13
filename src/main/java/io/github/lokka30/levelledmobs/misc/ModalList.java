package io.github.lokka30.levelledmobs.misc;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * This enum will significantly decrease clutter from checking lists from the settings file with configurable modes (e.g. whitelist/blacklist)
 */
public class ModalList {

    public enum ListMode {
        ALL,
        WHITELIST,
        BLACKLIST
    }

    public static ListMode fromString(final String mode) {
        assert mode != null;
        switch (mode.toUpperCase()) {
            case "ALL":
                return ListMode.ALL;
            case "WHITELIST":
                return ListMode.WHITELIST;
            case "BLACKLIST":
                return ListMode.BLACKLIST;
            default:
                throw new IllegalStateException("Invalid ListMode '" + mode + "'!");
        }
    }

    public static boolean isEnabledInList(final YamlConfiguration cfg, final String path, final String item) {
        if (cfg.contains(path + ".mode")) {
            @SuppressWarnings("ConstantConditions")
            final ListMode listMode = ModalList.fromString(cfg.getString(path + ".mode"));

            switch (listMode) {
                case ALL:
                    return true;
                case WHITELIST:
                    return cfg.getStringList(path + ".list").contains(item);
                case BLACKLIST:
                    return !cfg.getStringList(path + ".list").contains(item);
                default:
                    throw new IllegalStateException("Invalid ListMode " + listMode.toString() + "!");
            }
        } else {
            Utils.logger.error("&c(Is your settings.yml file outdated?) &7ModalListMode is unset at path '&b" + path + ".mode&7'! The plugin will malfunction until you fix this!");
            return false;
        }
    }
}
