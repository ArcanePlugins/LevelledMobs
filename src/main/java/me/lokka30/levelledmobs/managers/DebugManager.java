package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LivingEntityInterface;
import me.lokka30.levelledmobs.misc.DebugType;
import me.lokka30.levelledmobs.rules.RuleInfo;
import me.lokka30.levelledmobs.util.MessageUtils;
import me.lokka30.levelledmobs.util.Utils;
import me.lokka30.levelledmobs.wrappers.LivingEntityWrapper;
import me.lokka30.levelledmobs.wrappers.SchedulerResult;
import me.lokka30.levelledmobs.wrappers.SchedulerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Provides the logic for the debug system
 *
 * @author stumper66
 * @since 3.14.0
 */
public class DebugManager {
    public DebugManager(){
        instance = this;
        this.filterDebugTypes = new HashSet<>();
        this.filterEntityTypes = new HashSet<>();
        this.filterRuleNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.filterPlayerNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.maxPlayerDistance = defaultPlayerDistance;
        buildExcludedEntityTypes();
    }

    private final static int defaultPlayerDistance = 16;
    private static DebugManager instance;
    private boolean _isEnabled;
    //private boolean ignorePlayerDistance;
    private boolean isTimerEnabled;
    private boolean bypassAllFilters;
    private Instant timerEndTime;
    private SchedulerResult timerTask;
    public final Set<DebugType> filterDebugTypes;
    public final Set<EntityType> filterEntityTypes;
    public final Set<String> filterRuleNames;
    public final Set<String> filterPlayerNames;
    public List<String> excludedEntityTypes;
    public @Nullable Player playerThatEnabledDebug;
    public EvaluationTypes evaluationType = EvaluationTypes.FAILURE;
    public OutputTypes outputType = OutputTypes.TO_CONSOLE;
    public Integer maxPlayerDistance;
    public Integer minYLevel;
    public Integer maxYLevel;
    public Long disableAfter;
    public String disableAfterStr;

    public void enableDebug(final @NotNull CommandSender sender, final boolean usetimer, final boolean bypassFilters){
        if (sender instanceof Player player)
            this.playerThatEnabledDebug = player;
        this.bypassAllFilters = bypassFilters;
        this._isEnabled = true;
        checkTimerSettings(usetimer);
    }

    public void disableDebug(){
        this._isEnabled = false;
        this.isTimerEnabled = false;
        disableTimer();
    }

    private void disableTimer(){
        isTimerEnabled = false;

        if (this.timerTask == null) {
            return;
        }

        this.timerTask.cancelTask();
        this.timerTask = null;
    }

    private void checkTimerSettings(final boolean useTimer){
        if (!_isEnabled) return;

        final boolean canUseTimer = this.disableAfter != null && this.disableAfter > 0L;
        if (!useTimer || !canUseTimer){
            disableTimer();
            return;
        }

        this.timerEndTime = Instant.now().plusMillis(this.disableAfter);

        if (!this.isTimerEnabled){
            this.isTimerEnabled = true;
            final SchedulerWrapper wrapper = new SchedulerWrapper(this::timerLoop);
            this.timerTask = wrapper.runTaskTimerAsynchronously(20, 20);
        }
    }

    public boolean isEnabled(){
        return _isEnabled;
    }

    public boolean getIsTimerEnabled(){
        return this.isTimerEnabled;
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
                           final boolean result,
                           final @NotNull Supplier<String> msg) {
        instance.logInstance(debugType, null, null, entity, result, msg.get());
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
        if (!_isEnabled) return;

        // now you have to pass all of the filters if they are configured
        if (!bypassAllFilters) {
            if (!filterDebugTypes.isEmpty() && !filterDebugTypes.contains(debugType)) return;

            if (ruleInfo != null && !filterRuleNames.isEmpty() &&
                    !filterRuleNames.contains(ruleInfo.getRuleName().replace(" ", "_")) ||
                    ruleInfo == null && !filterRuleNames.isEmpty()) {
                return;
            }

            if (!filterEntityTypes.isEmpty()) {
                EntityType et = null;
                if (entity != null) et = entity.getType();
                else if (lmInterface != null) et = lmInterface.getEntityType();
                if (!filterEntityTypes.contains(et)) return;
            }

            Entity useEntity = entity;
            if (lmInterface instanceof LivingEntityWrapper lmEntity) useEntity = lmEntity.getLivingEntity();

            if (maxPlayerDistance != null && maxPlayerDistance > 0 && useEntity != null) {
                final List<Player> players = getPlayers();
                boolean foundMatch = false;
                if (players != null) {
                    for (final Player player : players) {
                        if (player.getWorld() != useEntity.getWorld()) continue;
                        final double dist = player.getLocation().distance(useEntity.getLocation());
                        if (dist <= maxPlayerDistance) {
                            foundMatch = true;
                            break;
                        }
                    }
                }

                if (!foundMatch) return;
            }

            if (ruleResult != null && evaluationType != EvaluationTypes.BOTH) {
                if (ruleResult && evaluationType == EvaluationTypes.FAILURE) return;
                if (!ruleResult && evaluationType == EvaluationTypes.SUCCESS) return;
            }

            if (useEntity != null) {
                if (minYLevel != null && useEntity.getLocation().getBlockY() < minYLevel) return;
                if (maxYLevel != null && useEntity.getLocation().getBlockY() > maxYLevel) return;
            }
        } // end bypass all

        if (ruleResult != null){
            msg += ", result: " + ruleResult;
        }

        if (outputType == OutputTypes.TO_BOTH || outputType == OutputTypes.TO_CONSOLE){
            Utils.logger.info("&8[&bDebug: " + debugType + "&8]&7 " + msg);
        }
        if (outputType == OutputTypes.TO_BOTH || outputType == OutputTypes.TO_CHAT){
            if (playerThatEnabledDebug == null){
                Utils.logger.info("No player to send chat messages to");
            }
            else{
                playerThatEnabledDebug.sendMessage(MessageUtils.colorizeHexCodes(
                        "&8[&bDebug: " + debugType + "&8]&7 " + msg));
            }
        }
    }

    private @Nullable List<Player> getPlayers(){
        if (filterPlayerNames.isEmpty()){
            return new LinkedList<>(Bukkit.getOnlinePlayers());
        }

        final List<Player> players = new LinkedList<>();
        for (String playerName : filterPlayerNames){
            final Player player = Bukkit.getPlayer(playerName);
            if (player != null) players.add(player);
        }

        return players.isEmpty() ?
                null : players;
    }

    public String getDebugStatus(){
        final StringBuilder sb = new StringBuilder("\nDebug Status: ");
        if (_isEnabled) {
            sb.append("ENABLED");
            if (isTimerEnabled) {
                sb.append("-(Time Left: ");
                sb.append(getTimeRemaining()).append(")");
            }
        }
        else
            sb.append("DISABLED");

        if (!bypassAllFilters && !hasFiltering()) return sb.toString();
        sb.append("\n--------------------------\n")
        .append("Current Filter Options:");

        if (bypassAllFilters){
            sb.append("\n- All filters bypassed");
            return sb.toString();
        }

        if (!filterDebugTypes.isEmpty()){
            sb.append("\n- Debug types: ");
            sb.append(filterDebugTypes);
        }

        if (!filterEntityTypes.isEmpty()){
            sb.append("\n- Entity types: ");
            sb.append(filterEntityTypes);
        }

        if (!filterRuleNames.isEmpty()){
            sb.append("\n- Rule names: ");
            sb.append(filterRuleNames);
        }

        if (!filterPlayerNames.isEmpty()){
            sb.append("\n- Player names: ");
            sb.append(filterPlayerNames);
        }

        if (evaluationType != EvaluationTypes.BOTH){
            sb.append("\n- Listen for: ");
            sb.append(evaluationType.name().toLowerCase());
        }

        if (maxPlayerDistance != null){
            sb.append("\n- Max player distance: ");
            sb.append(maxPlayerDistance);
        }

        if (minYLevel != null){
            sb.append("\n- Min y level: ");
            sb.append(minYLevel);
        }

        if (maxYLevel != null){
            if (minYLevel != null)
                sb.append(", Max y level: ");
            else
                sb.append("\n- Max y level: ");
            sb.append(maxYLevel);
        }

        if (outputType != OutputTypes.TO_CONSOLE){
            sb.append("\n- Output to: ");
            sb.append(outputType.name().toLowerCase());
        }

        return sb.toString();
    }

    private boolean hasFiltering(){
        return (
                !filterDebugTypes.isEmpty() ||
                !filterEntityTypes.isEmpty() ||
                !filterRuleNames.isEmpty() ||
                !filterPlayerNames.isEmpty() ||
                evaluationType != EvaluationTypes.FAILURE ||
                outputType != OutputTypes.TO_CONSOLE ||
                maxPlayerDistance == null || maxPlayerDistance != 0 ||
                minYLevel != null ||
                maxYLevel != null
                //disableAfter != null
        );
    }

    public void resetFilters(){
        filterDebugTypes.clear();
        filterEntityTypes.clear();
        filterRuleNames.clear();
        filterPlayerNames.clear();
        evaluationType = EvaluationTypes.FAILURE;
        outputType = OutputTypes.TO_CONSOLE;
        maxPlayerDistance = defaultPlayerDistance;
        minYLevel = null;
        maxYLevel = null;
        disableAfter = null;
        disableAfterStr = null;
    }

    public enum EvaluationTypes{
        FAILURE, SUCCESS, BOTH
    }

    public enum OutputTypes{
        TO_CONSOLE, TO_CHAT, TO_BOTH
    }

    public boolean isDebugTypeEnabled(final @NotNull DebugType debugType){
        if (!this._isEnabled) return false;

        return filterDebugTypes.isEmpty() || filterDebugTypes.contains(debugType);
    }

    private void timerLoop(){
        if (Instant.now().isAfter(this.timerEndTime)){
            disableDebug();

            final String msg = "Debug timer has elapsed, debugging is now disabled";
            if (outputType == OutputTypes.TO_CONSOLE || outputType == OutputTypes.TO_BOTH){
                Utils.logger.info(msg);
            }
            if ((outputType == OutputTypes.TO_CHAT || outputType == OutputTypes.TO_BOTH)
                    && playerThatEnabledDebug != null){
                playerThatEnabledDebug.sendMessage(msg);
            }
        }
    }

    public void timerWasChanged(){
        checkTimerSettings(isTimerEnabled);
    }

    private @Nullable String getTimeRemaining(){
        if (!_isEnabled || disableAfter == null ||
                disableAfter <= 0 || timerEndTime == null) return null;

        final Duration duration = Duration.between(Instant.now(), timerEndTime);
        final int secondsLeft = (int)duration.getSeconds();
        if (secondsLeft < 60){
            return secondsLeft == 1 ?
                    "1 second" : secondsLeft + " seconds";
        }
        else if (secondsLeft < 3600){
            final int minutes = (int)Math.floor((double) secondsLeft / 60.0);
            final int newSeconds = secondsLeft % 60;
            final StringBuilder sb = new StringBuilder();
            sb.append(minutes)
                    .append(minutes == 1 ? " minute, " : " minutes, ")
                    .append(newSeconds)
                    .append(newSeconds == 1 ? " second" : " seconds");
            return sb.toString();
        }

        return String.valueOf(secondsLeft);

    }

    private void buildExcludedEntityTypes(){
        this.excludedEntityTypes = Arrays.asList(
                "AREA_EFFECT_CLOUD",
                "ARMOR_STAND",
                "ARROW",
                "BLOCK_DISPLAY",
                "CHEST_BOAT",
                "DRAGON_FIREBALL",
                "DROPPED_ITEM",
                "EGG",
                "ENDER_CRYSTAL",
                "ENDER_PEARL",
                "ENDER_SIGNAL",
                "EVOKER_FANGS",
                "EXPERIENCE_ORB",
                "FALLING_BLOCK",
                "FIREWORK",
                "FISHING_HOOK",
                "GIANT",
                "INTERACTION",
                "ITEM_DISPLAY",
                "ITEM_FRAME",
                "LEASH_HITCH",
                "LIGHTNING",
                "LLAMA_SPIT",
                "MARKER",
                "MINECART",
                "MINECART_CHEST",
                "MINECART_COMMAND",
                "MINECART_FURNACE",
                "MINECART_HOPPER",
                "MINECART_MOB_SPAWNER",
                "MINECART_TNT",
                "PAINTING",
                "PLAYER",
                "PRIMED_TNT",
                "SHULKER_BULLET",
                "SMALL_FIREBALL",
                "SPECTRAL_ARROW",
                "SPLASH_POTION",
                "TEXT_DISPLAY",
                "THROWN_EXP_BOTTLE",
                "TRIDENT",
                "UNKNOWN",
                "BOAT",
                "FIREBALL",
                "GLOW_ITEM_FRAME",
                "TROPICAL_FISH",
                "WIND_CHARGE",
                "WITHER_SKULL"
        );
    }
}
