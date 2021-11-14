/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import de.tr7zw.nbtapi.NBTItem;
import me.lokka30.levelledmobs.customdrops.CustomDropItem;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.NBTApplyResult;
import me.lokka30.levelledmobs.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author stumper66
 * @since 3.1.0
 */
public class NBTManager {

    @NotNull
    public static NBTApplyResult applyNBT_Data_Item(@NotNull final CustomDropItem item, @NotNull final String nbtStuff) {
        final NBTApplyResult result = new NBTApplyResult();
        final NBTItem nbtent = new NBTItem(item.getItemStack());

        try {
            nbtent.mergeCompound(new NBTContainer(nbtStuff));
            result.itemStack = nbtent.getItem();
        } catch (Exception e) {
            result.exceptionMessage = e.getMessage();
        }

        return result;
    }

    public static @NotNull NBTApplyResult applyNBT_Data_Mob(@NotNull final LivingEntityWrapper lmEntity, @NotNull final String nbtStuff) {
        final NBTApplyResult result = new NBTApplyResult();

        try {
            final NBTEntity nbtent = new NBTEntity(lmEntity.getLivingEntity());
            final String jsonBefore = nbtent.toString();
            nbtent.mergeCompound(new NBTContainer(nbtStuff));
            final String jsonAfter = nbtent.toString();

            if (lmEntity.getMainInstance().helperSettings.getStringSet(lmEntity.getMainInstance().settingsCfg, "debug-misc").contains("NBT_APPLY_SUCCESS"))
                showChangedJson(jsonBefore, jsonAfter, result);

            if (jsonBefore.equals(jsonAfter))
                result.exceptionMessage = "No NBT data changed.  Make sure you have used proper NBT strings";
        } catch (Exception e) {
            result.exceptionMessage = e.getMessage();
        }

        return result;
    }

    private static void showChangedJson(final String jsonBefore, final String jsonAfter, final NBTApplyResult applyResult){
        final Map<String, String> objectsBefore = new TreeMap<>();
        final Map<String, String> objectsAfter = new TreeMap<>();
        final JsonObject jsonObjectBefore = JsonParser.parseString(jsonBefore).getAsJsonObject();
        final JsonObject jsonObjectAfter = JsonParser.parseString(jsonAfter).getAsJsonObject();

        try {
            for (final String key : jsonObjectBefore.keySet()) {
                objectsBefore.put(key, jsonObjectBefore.get(key).toString());
            }
            for (final String key : jsonObjectAfter.keySet()) {
                objectsAfter.put(key, jsonObjectAfter.get(key).toString());
            }
        }
        catch (Exception e){
            e.printStackTrace();
            return;
        }

        for (final String key : jsonObjectAfter.keySet()){
            final String value = jsonObjectAfter.get(key).toString();

            if (objectsBefore.containsKey(key) && objectsAfter.containsKey(key) && !objectsBefore.get(key).equals(value)) {
                if (applyResult.objectsUpdated == null) applyResult.objectsUpdated = new LinkedList<>();
                applyResult.objectsUpdated.add(key + ":" + value);
            }
            else if (!objectsBefore.containsKey(key) && objectsAfter.containsKey(key)) {
                if (applyResult.objectsAdded == null) applyResult.objectsAdded = new LinkedList<>();
                applyResult.objectsAdded.add(key + ":" + value);
            }
            else if (objectsBefore.containsKey(key) && !objectsAfter.containsKey(key)) {
                if (applyResult.objectsRemoved == null) applyResult.objectsRemoved = new LinkedList<>();
                applyResult.objectsRemoved.add(key + ":" + value);
            }
        }
    }
}
