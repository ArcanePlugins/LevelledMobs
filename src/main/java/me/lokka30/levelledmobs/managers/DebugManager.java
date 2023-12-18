package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.rules.RuleInfo;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public class DebugManager {
    public DebugManager(){
        instance = this;
        this.filterDebugTypes = new HashSet<>();
        this.filterEntityTypes = new HashSet<>();
        this.filterRuleNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.filterPlayerNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    private static DebugManager instance;
    private boolean _isEnabled;
    private boolean ignorePlayerDistance;
    public final Set<DebugType> filterDebugTypes;
    public final Set<EntityType> filterEntityTypes;
    public final Set<String> filterRuleNames;
    public final Set<String> filterPlayerNames;
    public @Nullable Player playerThatEnabledDebug;
    public EvaluationTypes evaluationType = EvaluationTypes.FAILURE;
    public OutputTypes outputType = OutputTypes.CONSOLE;
    public Integer maxPlayerDistance = 20;
    public Integer minYLevel;
    public Integer maxYLevel;
    public Long disableAfter;
    public String disableAfterStr;

    public void enableDebug(final @NotNull CommandSender sender){
        if (sender instanceof Player player) {
            this.playerThatEnabledDebug = player;
            this.ignorePlayerDistance = false;
        }
        else if (shouldIgnorePlayerFilter()){
            sender.sendMessage("Debug was not enabled from a player and the player " +
                    "list is empty, ignoring max player distance filter");
            this.ignorePlayerDistance = true;
        }
        this._isEnabled = true;
    }

    public void disableDebug(){
        this._isEnabled = false;
        this.playerThatEnabledDebug = null;
    }

    public boolean isEnabled(){
        return _isEnabled;
    }

    private boolean shouldIgnorePlayerFilter(){
        return maxPlayerDistance != null &&
                maxPlayerDistance > 0 &&
                filterPlayerNames.isEmpty() &&
                this.playerThatEnabledDebug == null;
    }

    public static void log(final @NotNull DebugType debugType,
                           final @NotNull RuleInfo ruleInfo,
                           final @NotNull LivingEntityWrapper lmEntity,
                           final @NotNull Supplier<String> msg) {
        instance.logInstance(debugType, ruleInfo, lmEntity, null, null, msg.get());
    }

    public static void log(final @NotNull DebugType debugType,
                           final @NotNull RuleInfo ruleInfo,
                           final @NotNull LivingEntityInterface lmInterface,
                           final boolean ruleResult,
                           final @NotNull Supplier<String> msg) {
        instance.logInstance(debugType, ruleInfo, lmInterface, null, ruleResult, msg.get());
    }

    public static void log(final @NotNull DebugType debugType,
                           final @NotNull LivingEntityWrapper lmEntity,
                           final @NotNull Supplier<String> msg) {
        instance.logInstance(debugType, null, lmEntity, null, null, msg.get());
    }

    public static void log(final @NotNull DebugType debugType,
                           final @NotNull LivingEntityWrapper lmEntity,
                           final boolean result,
                           final @NotNull Supplier<String> msg) {
        instance.logInstance(debugType, null, lmEntity, null, result, msg.get());
    }

    public static void log(final @NotNull DebugType debugType,
                           final @NotNull Entity entity,
                           final @NotNull Supplier<String> msg) {
        instance.logInstance(debugType, null, null, entity, null, msg.get());
    }

    /**
     * Sends a debug message to console if enabled in settings
     *
     * @param debugType Reference to whereabouts the debug log is called so that it can be traced
     *                  back easily
     * @param msg       Message to help de-bugging
     */
    public static void log(final @NotNull DebugType debugType, final @NotNull Supplier<String> msg) {
        instance.logInstance(debugType, null, null, null, null, msg.get());
    }

    private void logInstance(
            final @NotNull DebugType debugType,
            final @Nullable RuleInfo ruleInfo,
            final @Nullable LivingEntityInterface lmInterface,
            final @Nullable Entity entity,
            final Boolean ruleResult,
            @NotNull String msg
    ) {
        // now you have to pass all of the filters if they are configured
        if (!_isEnabled) return;

        if (!filterDebugTypes.isEmpty() && !filterDebugTypes.contains(debugType)) return;

        if (ruleInfo != null && !filterRuleNames.isEmpty() &&
                !filterRuleNames.contains(ruleInfo.getRuleName().replace(" ", "_")) ||
                ruleInfo == null && !filterRuleNames.isEmpty()) {
            return;
        }

        if (!filterEntityTypes.isEmpty()){
            EntityType et = null;
            if (entity != null) et = entity.getType();
            else if (lmInterface != null) et = lmInterface.getEntityType();
            if (!filterEntityTypes.contains(et)) return;
        }

        Entity useEntity = entity;
        if (lmInterface instanceof LivingEntityWrapper lmEntity) useEntity = lmEntity.getLivingEntity();

        if (maxPlayerDistance != null && maxPlayerDistance > 0 && useEntity != null && !ignorePlayerDistance){
            final List<Player> players = getPlayers();
            boolean foundMatch = false;
            if (players != null){
                for (final Player player : players){
                    if (player.getWorld() != useEntity.getWorld()) continue;
                    final double dist = player.getLocation().distance(useEntity.getLocation());
                    if (dist <= maxPlayerDistance){
                        foundMatch = true;
                        break;
                    }
                }
            }

            if (!foundMatch) return;
        }

        if (ruleResult != null && evaluationType != EvaluationTypes.BOTH){
            if (ruleResult && evaluationType == EvaluationTypes.FAILURE) return;
            if (!ruleResult && evaluationType == EvaluationTypes.PASS) return;
        }

        if (useEntity != null){
            if (minYLevel != null && useEntity.getLocation().getBlockY() < minYLevel) return;
            if (maxYLevel != null && useEntity.getLocation().getBlockY() > maxYLevel) return;
        }
        if (ruleResult != null){
            msg += ", result: " + ruleResult;
        }

        if (outputType == OutputTypes.BOTH || outputType == OutputTypes.CONSOLE){
            Utils.logger.info("&8[&bDebug: " + debugType + "&8]&7 " + msg);
        }
        if (outputType == OutputTypes.BOTH || outputType == OutputTypes.CHAT){
            if (playerThatEnabledDebug == null){
                Utils.logger.info("Unable to send debug message to chat, debug must be enabled from chat first");
            }
            else{
                playerThatEnabledDebug.sendMessage("&8[&bDebug: " + debugType + "&8]&7 " + msg);
            }
        }
    }

    private @Nullable List<Player> getPlayers(){
        if (filterPlayerNames.isEmpty()){
            return this.playerThatEnabledDebug != null ?
                    List.of(this.playerThatEnabledDebug) : null;
        }
        List<Player> players = new LinkedList<>();
        for (String playerName : filterPlayerNames){
            final Player player = Bukkit.getPlayer(playerName);
            if (player != null) players.add(player);
        }

        return players.isEmpty() ?
                null : players;
    }

    public String getDebugStatus(){
        final StringBuilder sb = new StringBuilder("Debug status: ");
        if (_isEnabled)
            sb.append("Enabled");
        else
            sb.append("Disabled");

        if (!hasFiltering()) return sb.toString();
        sb.append(", filter options:");

        if (!filterDebugTypes.isEmpty()){
            sb.append("\nDebug types: ");
            sb.append(filterDebugTypes);
        }

        if (!filterEntityTypes.isEmpty()){
            sb.append("\nEntity types: ");
            sb.append(filterEntityTypes);
        }

        if (!filterRuleNames.isEmpty()){
            sb.append("\nRule names: ");
            sb.append(filterRuleNames);
        }

        if (!filterPlayerNames.isEmpty()){
            sb.append("\nPlayer names: ");
            sb.append(filterPlayerNames);
        }

        if (evaluationType != EvaluationTypes.BOTH){
            sb.append("\nEvaluation type: ");
            sb.append(evaluationType);
        }

        if (maxPlayerDistance != null){
            sb.append("\nMax player distance: ");
            sb.append(maxPlayerDistance);
            if (shouldIgnorePlayerFilter())
                sb.append(" (ignoring)");
        }

        if (minYLevel != null){
            sb.append("\nMin y level: ");
            sb.append(minYLevel);
        }

        if (maxYLevel != null){
            if (minYLevel != null)
                sb.append(", ");
            else
                sb.append("\n");
            sb.append("Max y level: ");
            sb.append(maxYLevel);
        }

        if (outputType != OutputTypes.CONSOLE){
            sb.append("\nOutput to: ");
            sb.append(outputType);
        }

        if (disableAfter != null){
            sb.append("\nDisable after: ");
            sb.append(disableAfterStr);
        }

        return sb.toString();
    }

    private boolean hasFiltering(){
        return (
                !filterDebugTypes.isEmpty() ||
                !filterEntityTypes.isEmpty() ||
                !filterRuleNames.isEmpty() ||
                !filterPlayerNames.isEmpty() ||
                evaluationType != EvaluationTypes.BOTH ||
                outputType != OutputTypes.CONSOLE ||
                maxPlayerDistance != null ||
                minYLevel != null ||
                maxYLevel != null ||
                disableAfter != null
        );
    }

    public enum EvaluationTypes{
        FAILURE, PASS, BOTH
    }

    public enum OutputTypes{
        CONSOLE, CHAT, BOTH
    }

    public boolean isDebugTypeEnabled(final @NotNull DebugType debugType){
        if (!this._isEnabled) return false;

        return filterDebugTypes.isEmpty() || filterDebugTypes.contains(debugType);
    }
}
