package me.lokka30.levelledmobs.customdrops;

import io.github.stumper66.lm_items.ExternalItemRequest;
import io.github.stumper66.lm_items.GetItemResult;
import io.github.stumper66.lm_items.ItemsAPI;
import io.github.stumper66.lm_items.LM_Items;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.managers.DebugManager;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.util.Utils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Hashtable;
import java.util.List;

/**
 * Interfaces with the plugin LM_Items so can custom items from 3rd party plugins can be used
 * directly in custom drops
 *
 * @author stumper66
 * @since 3.5.0
 */
public class LMItemsParser {

    public LMItemsParser(final @NotNull LevelledMobs main) {
        this.main = main;
    }

    private final LevelledMobs main;

    public boolean parseExternalItemAttributes(@NotNull String materialName,
        final CustomDropItem item) {
        if (!main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement()) {
            if (ExternalCompatibilityManager.hasLMItemsInstalled()){
                Utils.logger.warning(String.format(
                        "customdrops.yml references external item '%s' but LM_Items is an old version",
                        materialName
                ));
            }
            else {
                Utils.logger.warning(String.format(
                        "customdrops.yml references external item '%s' but LM_Items is not installed",
                        materialName
                ));
            }
            return false;
        }

        final int colon = materialName.indexOf(":");
        item.externalPluginName = materialName.substring(0, colon);
        item.externalItemId = materialName.substring(colon + 1);
        final LM_Items lm_items = LM_Items.plugin;

        if (!lm_items.doesSupportPlugin(item.externalPluginName)) {
            Utils.logger.warning(String.format(
                "customdrops.yml references item from plugin '%s' but LM_Items does not support that plugin",
                item.externalPluginName
            ));
            return false;
        }

        item.isExternalItem = true;
        return getExternalItem(item, null);
    }

    public boolean getExternalItem(final @NotNull CustomDropItem item, final @Nullable CustomDropProcessingInfo info) {
        final ItemsAPI itemsAPI = LM_Items.plugin.getItemAPIForPlugin(item.externalPluginName);

        if (itemsAPI == null) {
            Utils.logger.warning(
                "Unable to get ItemsAPI from LM_Items for plugin " + item.externalPluginName);
            return false;
        }

        final ExternalItemRequest itemRequest = new ExternalItemRequest(item.externalItemId);
        itemRequest.itemType = item.externalType;
        itemRequest.amount = item.externalAmount;

        if (main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement2()){
            itemRequest.getMultipleItems = "-".equals(itemRequest.itemId);
            itemRequest.minItems = item.minItems;
            itemRequest.maxItems = item.maxItems;
            itemRequest.allowedList = item.allowedList;
            itemRequest.excludedList = item.excludedList;
            itemRequest.isDebugEnabled = main.debugManager.isEnabled();
        }

        if (item.externalExtras != null){
            itemRequest.extras = new Hashtable<>(item.externalExtras.size());

            for (final String key : item.externalExtras.keySet()){
                Object value = item.externalExtras.get(key);
                if (value instanceof String stringValue && stringValue.contains("%")){
                    if (info != null) {
                        value = main.levelManager.replaceStringPlaceholders(stringValue, info.lmEntity, true, info.mobKiller, false);
                    }
                    else if (ExternalCompatibilityManager.hasPapiInstalled()) {
                        value = ExternalCompatibilityManager.getPapiPlaceholder(null, stringValue);
                    }
                }

                itemRequest.extras.put(key, value);
            }
        }

        final GetItemResult result = itemsAPI.getItem(itemRequest);

        if (!result.pluginIsInstalled) {
            Utils.logger.warning(
                String.format("custom item references plugin '%s' but that plugin is not installed",
                    item.externalPluginName));
            return false;
        }

        final ItemStack itemStack = result.itemStack;
        if (itemStack == null && (result.itemStacks == null || result.itemStacks.isEmpty())) {
            if (result.typeIsNotSupported) {
                if (item.externalType == null) {
                    Utils.logger.warning(
                        String.format("custom item '%s:%s' doesn't support type (null)",
                            item.externalPluginName, item.externalItemId));
                } else {
                    Utils.logger.warning(
                        String.format("custom item '%s:%s' doesn't support type %s",
                            item.externalPluginName, item.externalItemId, item.externalType));
                }

                return false;
            }

            final String msg = item.externalType == null ?
                    String.format("&4custom item '%s:%s' returned a null item&r",
                            item.externalPluginName, item.externalItemId) :
                    String.format("&4custom item '%s:%s' (%s) returned a null item&r",
                            item.externalPluginName, item.externalItemId, item.externalType);

            // on server startup show as warning message
            // after reload show as debug

            if (main.companion.hasFinishedLoading){
                DebugManager.log(DebugType.CUSTOM_DROPS, () -> msg);
            }
            else{
                Utils.logger.warning(msg);
            }

            main.customDropsHandler.customDropsParser.invalidExternalItems.add(msg);

            return false;
        }

        if (main.companion.externalCompatibilityManager.doesLMIMeetVersionRequirement2()){
            if (result.itemStacks != null && !result.itemStacks.isEmpty())
                item.setItemStacks((List<ItemStack>) result.itemStacks);
            else if (itemStack != null)
                item.setItemStack(itemStack);
        }
        else if (itemStack != null){
            item.setItemStack(itemStack);
        }

        return true;
    }
}
