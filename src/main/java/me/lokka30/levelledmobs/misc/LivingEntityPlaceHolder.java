package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.LivingEntityInterface;
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
        return main.rulesManager.getApplicableRules(this);
    }

    @NotNull
    public String getTypeName(){
        return this.entityType.name();
    }
}
