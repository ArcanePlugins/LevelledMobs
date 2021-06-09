package me.lokka30.levelledmobs.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.microlib.MessageUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * TODO Describe...
 *
 * @author stumper66
 */
public class PAPIManager extends PlaceholderExpansion {

    public PAPIManager(final LevelledMobs main){
        this.main = main;
        this.lastKilledEntitiesByPlayer = new TreeMap<>();
    }

    private final LevelledMobs main;
    private final Map<UUID, LivingEntityWrapper> lastKilledEntitiesByPlayer;

    public void putEntityDeath(final @NotNull Player player, final LivingEntityWrapper lmEntity){
        this.lastKilledEntitiesByPlayer.put(player.getUniqueId(), lmEntity);
    }

    public void playedLoggedOut(final @NotNull Player player){
        this.lastKilledEntitiesByPlayer.remove(player.getUniqueId());
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return main.getDescription().getName();
    }

    @Override
    public @NotNull String getAuthor() {
        return main.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return main.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(final Player player, final @NotNull String identifier){
        if (player == null) return "";

        if ("level".equalsIgnoreCase(identifier))
            return getLevelFromPlayer(player);
        else if ("displayname".equalsIgnoreCase(identifier))
            return getDisplaynameFromPlayer(player);

        return null;
    }

    private String getLevelFromPlayer(final Player player){
        if (!this.lastKilledEntitiesByPlayer.containsKey(player.getUniqueId())) return "";

        final LivingEntityWrapper lmEntity = this.lastKilledEntitiesByPlayer.get(player.getUniqueId());
        if (!lmEntity.isLevelled()) return "0";
        return lmEntity.getMobLevel() + "";
    }

    private String getDisplaynameFromPlayer(final Player player){
        if (!this.lastKilledEntitiesByPlayer.containsKey(player.getUniqueId())) return "";

        final LivingEntityWrapper lmEntity = this.lastKilledEntitiesByPlayer.get(player.getUniqueId());

        if (lmEntity.getLivingEntity().getCustomName() != null)
            return lmEntity.getLivingEntity().getCustomName();

        final String overridenName = main.rulesManager.getRule_EntityOverriddenName(lmEntity);
        return overridenName == null ?
                Utils.capitalize(lmEntity.getTypeName().replaceAll("_", " ")) :
                MessageUtils.colorizeAll(overridenName);
    }
}
