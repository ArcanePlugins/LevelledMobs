package me.lokka30.levelledmobs.customdrops;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.misc.Utils;
import me.stumper66.lm_items.GetItemResult;
import me.stumper66.lm_items.ItemsAPI;
import me.stumper66.lm_items.LM_Items;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Interfaces with the plugin LM_Items so can custom items from 3rd party
 * plugins can be used directly in custom drops
 *
 * @author stumper66
 * @since 3.5.0
 */
public class LMItemsParser {
    public static boolean parseExternalItem(@NotNull String materialName, final CustomDropItem item){
        if (!ExternalCompatibilityManager.hasLMItemsInstalled()){
            Utils.logger.warning(String.format(
                    "customdrops.yml references external item '%s' but LM_Items is not installed",
                    materialName
            ));
            return false;
        }

        final int colon = materialName.indexOf(":");
        item.externalPluginName = materialName.substring(0, colon);
        item.externalItemId = materialName.substring(colon + 1);
        final LM_Items lm_items = LM_Items.plugin;

        if (!lm_items.doesSupportPlugin(item.externalPluginName)){
            Utils.logger.warning(String.format(
                    "customdrops.yml references item from plugin '%s' but LM_Items does not support that plugin",
                    item.externalPluginName
            ));
            return false;
        }

        item.isExternalItem = true;
        return true;
    }

    public static boolean getExternalItem(final @NotNull CustomDropItem item, final @NotNull LevelledMobs main){
        final ItemsAPI itemsAPI = LM_Items.plugin.getItemAPIForPlugin(item.externalPluginName);

        if (itemsAPI == null){
            Utils.logger.warning("Unable to get ItemsAPI from LM_Items for plugin " + item.externalPluginName);
            return false;
        }

        final GetItemResult result = itemsAPI.getItem(item.externalType, item.externalItemId);
        if (!result.pluginIsInstalled){
            Utils.logger.warning(String.format("custom item references plugin '%s' but that plugin is not installed", item.externalPluginName));
            return false;
        }

        final ItemStack itemStack = result.itemStack;
        if (itemStack == null){
            if (main.companion.debugsEnabled.contains(DebugType.CUSTOM_DROPS)) {
                if (item.externalType == null)
                    Utils.logger.warning(String.format("custom item '%s:%s' returned a null item", item.externalPluginName, item.externalItemId));
                else
                    Utils.logger.warning(String.format("custom item '%s:%s' (%s) returned a null item", item.externalPluginName, item.externalItemId, item.externalType));
            }

            return false;
        }

        item.setItemStack(itemStack);

        return true;
    }
}
