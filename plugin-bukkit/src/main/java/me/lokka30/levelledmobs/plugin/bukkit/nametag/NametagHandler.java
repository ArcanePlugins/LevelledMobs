/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.plugin.bukkit.nametag;

import me.lokka30.levelledmobs.plugin.bukkit.LevelledMobs;
import me.lokka30.levelledmobs.plugin.bukkit.level.LevelledMob;
import me.lokka30.levelledmobs.plugin.bukkit.translation.TranslationHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NametagHandler {

    public String generateNametag(@NotNull LevelledMob levelledMob) {
        final TranslationHandler translationHandler = LevelledMobs.getInstance().translationHandler;
        final String nameTranslated = translationHandler.getTranslatedEntityName(
            levelledMob.livingEntity.getType());
        final String levelTranslated = translationHandler.getTranslatedLevel(
            levelledMob.getLevel());
        final String nametagFormat = levelledMob.getNametagFormat();
        String finalNametag = nametagFormat;
        for (NametagPlaceholder placeholder : NametagPlaceholder.values()) {
            if (nametagFormat.contains(placeholder.getId())) {
                finalNametag = finalNametag.replace(placeholder.getId(),
                    placeholder.getValue(levelledMob));
            }
        }

        return finalNametag;
    }

    public void sendNametag(@NotNull LevelledMob levelledMob, @NotNull Player target,
        @NotNull String nametag) {
        LevelledMobs.getInstance().nmsHandler.getNametagNMSHandler()
            .sendNametag(levelledMob.livingEntity, nametag, target, false);
    }

    public void generateAndSendNametag(@NotNull LevelledMob levelledMob, @NotNull Player target) {
        sendNametag(levelledMob, target, generateNametag(levelledMob));
    }

}
