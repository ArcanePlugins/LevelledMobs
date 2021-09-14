/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.rules.ApplicableRulesResult;
import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A wrapper for the LivingEntity class that provides various common function
 * and settings used for processing rules
 * Used only with the summon command
 *
 * @author stumper66
 * @since 3.0.0
 */
public class LivingEntityPlaceHolder extends LivingEntityWrapperBase implements LivingEntityInterface {

    public LivingEntityPlaceHolder(final EntityType entityType, final @NotNull Location location, final @NotNull World world, final @NotNull LevelledMobs main){
        super(main, world, location);

        this.entityType = entityType;
    }

    private final EntityType entityType;

    @NotNull
    public EntityType getEntityType() {
        return this.entityType;
    }

    public List<RuleInfo> getApplicableRules(){
        return main.rulesManager.getApplicableRules(this).allApplicableRules;
    }

    public ApplicableRulesResult getRules(){
        return main.rulesManager.getApplicableRules(this);
    }

    @NotNull
    public String getTypeName(){
        return this.entityType.name();
    }

    public void setSpawnedTimeOfDay(final int ticks){
        this.spawnedTimeOfDay = ticks;
    }

    public int getSpawnedTimeOfDay(){
        if (this.spawnedTimeOfDay != null)
            return this.spawnedTimeOfDay;

        final int result = (int) getWorld().getTime();
        setSpawnedTimeOfDay(result);

        return result;
    }
}
