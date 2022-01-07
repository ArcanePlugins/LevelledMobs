/*
 * This file is Copyright (c) 2020-2022 lokka30.
 * This file is/was present in the LevelledMobs resource.
 * Repository: <https://github.com/lokka30/LevelledMobs>
 * Use of this source code is governed by the GNU GPL v3.0
 * license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.nametag;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.levelling.LevelledMob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/*
TODO Javadocs
 */
public class NametagHandler {

    private final LevelledMobs main;
    public NametagHandler(final @NotNull LevelledMobs main) { this.main = main; }

    public String generateNametag(@NotNull LevelledMob levelledMob) {
        final String nameTranslated = main.getTranslationHandler().getTranslatedEntityName(levelledMob.getLivingEntity().getType());
        final String levelTranslated = main.getTranslationHandler().getTranslatedInteger(levelledMob.getLevel());
        final String nametagFormat = levelledMob.getNametagFormat();
        @NotNull String finalNametag = nametagFormat;

        for(NametagPlaceholder placeholder : NametagPlaceholder.values()) {
            if(nametagFormat.contains(placeholder.getId())) {
                finalNametag = finalNametag.replace(placeholder.getId(), placeholder.getValue(main, levelledMob));
            }
        }

        return finalNametag;
    }

    public void sendNametag(@NotNull LevelledMob levelledMob, @NotNull Player target, @NotNull String nametag) {
        main.getNMSHandler().getCurrentUtil().sendNametag(levelledMob.getLivingEntity(), nametag, target,false);
    }

    public void sendNametag(@NotNull LevelledMob levelledMob, @NotNull Player target) {
        sendNametag(levelledMob, target, generateNametag(levelledMob));
    }

}
