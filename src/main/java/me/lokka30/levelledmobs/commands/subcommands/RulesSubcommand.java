/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.managers.ExternalCompatibilityManager;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.PaperUtils;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.misc.SpigotUtils;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.RuleInfo;
import me.lokka30.microlib.messaging.MessageUtils;
import me.lokka30.microlib.other.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Shows the current rules as parsed from the various config files
 *
 * @author stumper66
 * @since 3.0.0
 */
public class RulesSubcommand extends MessagesBase implements Subcommand {

    public RulesSubcommand(final LevelledMobs main) {
        super(main);
    }

    @Override
    public void parseSubcommand(final LevelledMobs main, @NotNull final CommandSender sender, final String label, final String[] args) {
        commandSender = sender;
        messageLabel = label;

        if (!sender.hasPermission("levelledmobs.command.rules")){
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length == 1) {
            showMessage("command.levelledmobs.rules.incomplete-command");
            return;
        }

        boolean showOnConsole = false;
        boolean findNearbyEntities = false;

        for (int i = 2; i < args.length; i++){
            if ("/console".equalsIgnoreCase(args[i]))
                showOnConsole = true;
            else if ("/near".equalsIgnoreCase(args[i]))
                findNearbyEntities = true;
        }

        if ("show_all".equalsIgnoreCase(args[1])) {
            if (sender instanceof Player)
                showMessage("command.levelledmobs.rules.console-rules");

            final StringBuilder sb = new StringBuilder();

            for (final RuleInfo rpi : main.rulesParsingManager.rulePresets.values()) {
                sb.append("\n--------------------------------- Preset rule ----------------------------------\n");
                sb.append(rpi.formatRulesVisually(List.of("ruleIsEnabled")));
            }

            sb.append("\n--------------------------------- Default values -------------------------------\n");
            sb.append(main.rulesParsingManager.defaultRule.formatRulesVisually());

            for (final RuleInfo rpi : main.rulesParsingManager.customRules) {
                sb.append("\n--------------------------------- Custom rule ----------------------------------\n");
                sb.append(rpi.formatRulesVisually());
            }
            sb.append("\n--------------------------------------------------------------------------------------");

            if (showOnConsole)
                Utils.logger.info(sb.toString());
            else
                sender.sendMessage(sb.toString());
        } else if ("show_effective".equalsIgnoreCase(args[1])) {
            if (!(sender instanceof Player)) {
                showMessage("common.no-player");
                return;
            }

            getMobBeingLookedAt((Player) sender, showOnConsole, findNearbyEntities);
        } else if ("show_rule".equalsIgnoreCase(args[1]))
            showRule(sender, args);
        else if ("help_discord".equalsIgnoreCase(args[1])) {
            final String message = getMessage("command.levelledmobs.rules.discord-invite");
            showHyperlink(sender, message, "https://www.discord.io/arcaneplugins");
        }
        else if ("help_wiki".equalsIgnoreCase(args[1])) {
            final String message = getMessage("command.levelledmobs.rules.wiki-link");
            showHyperlink(sender, message, "https://github.com/lokka30/LevelledMobs/wiki");
        }
        else if ("reset".equalsIgnoreCase(args[1]))
            resetRules(sender, args);
        else if ("force_all".equalsIgnoreCase(args[1]))
            forceRelevel(sender);
        else
            showMessage("common.invalid-command");
    }

    private void forceRelevel(final CommandSender sender){
        int worldCount = 0;
        int entityCount = 0;

        main.reloadLM(sender);

        for (final World world : Bukkit.getWorlds()) {
            worldCount++;
            for (final Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity)) continue;

                synchronized (entity.getPersistentDataContainer()){
                    if (entity.getPersistentDataContainer().has(main.namespaced_keys.wasSummoned, PersistentDataType.INTEGER))
                        continue; // was summon using lm summon command.  don't relevel it
                }

                entityCount++;
                final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance((LivingEntity) entity, main);
                lmEntity.reEvaluateLevel = true;
                lmEntity.isRulesForceAll = true;
                lmEntity.wasPreviouslyLevelled = lmEntity.isLevelled();
                main._mobsQueueManager.addToQueue(new QueueItem(lmEntity, null));
                lmEntity.free();
            }
        }

        showMessage("command.levelledmobs.rules.rules-reprocessed",
                new String[]{ "%entitycount%", "%worldcount%" },
                new String[]{String.valueOf(entityCount), String.valueOf(worldCount)}
        );
    }

    private void resetRules(final CommandSender sender, @NotNull final String @NotNull [] args){
        if (args.length < 3 || args.length > 4){
            showMessage("command.levelledmobs.rules.reset");
            return;
        }

        ResetDifficulty difficulty = ResetDifficulty.UNSPECIFIED;
        switch (args[2].toLowerCase()){
            case "basic": difficulty = ResetDifficulty.BASIC;
                break;
            case "average": difficulty = ResetDifficulty.AVERAGE;
                break;
            case "advanced": difficulty = ResetDifficulty.ADVANCED;
                break;
            case "extreme": difficulty = ResetDifficulty.EXTREME;
                break;
        }

        if (difficulty == ResetDifficulty.UNSPECIFIED){
            showMessage("command.levelledmobs.rules.invalid-difficulty", "%difficulty%", args[2]);
            return;
        }

        if (args.length == 3){
            showMessage("command.levelledmobs.rules.reset-syntax", "%difficulty%", args[2]);
            return;
        }

        resetRules(sender, difficulty);
    }

    private void resetRules(final @NotNull CommandSender sender, final @NotNull ResetDifficulty difficulty){
        final String prefix = main.configUtils.getPrefix();
        showMessage("command.levelledmobs.rules.resetting", "%difficulty%", String.valueOf(difficulty));

        final String filename = "rules.yml";
        final String[] replaceWhat = new String[] {"    - average_challenge",  "    - weighted_random_average_difficulty",  "", ""};
        final String[] replaceWith = new String[] {"#    - average_challenge", "#    - weighted_random_average_difficulty", "", ""};

        switch (difficulty){
            case BASIC:
                replaceWhat[2] = "#    - basic_challenge";                  replaceWith[2] = "    - basic_challenge";
                replaceWhat[3] = "#    - weighted_random_basic_difficulty"; replaceWith[3] = "    - weighted_random_basic_difficulty";
                break;
            case ADVANCED:
                replaceWhat[2] = "#    - advanced_challenge";                  replaceWith[2] = "    - advanced_challenge";
                replaceWhat[3] = "#    - weighted_random_advanced_difficulty"; replaceWith[3] = "    - weighted_random_advanced_difficulty";
                break;
            case EXTREME:
                replaceWhat[2] = "#    - extreme_challenge"; replaceWith[2] = "    - extreme_challenge";
                break;
        }

        try (final InputStream stream = main.getResource(filename)) {
            if (stream == null){
                Utils.logger.error(prefix + " Input stream was null");
                return;
            }

            String rulesText = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (difficulty != ResetDifficulty.AVERAGE) {
                rulesText = rulesText.replace(replaceWhat[0], replaceWith[0])
                .replace(replaceWhat[1], replaceWith[1])
                .replace(replaceWhat[2], replaceWith[2]);

                if (difficulty != ResetDifficulty.EXTREME)
                    rulesText = rulesText.replace(replaceWhat[3], replaceWith[3]);
            }

            final File rulesFile = new File(main.getDataFolder(), filename);
            File rulesBackupFile = new File(main.getDataFolder(), "rules.yml.backup");

            for (int i = 0; i < 10; i++){
                if (!rulesBackupFile.exists()) break;
                rulesBackupFile = new File(main.getDataFolder(), "rules.yml.backup" + i);
            }

            Files.copy(rulesFile.toPath(), rulesBackupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(rulesFile.toPath(), rulesText, StandardCharsets.UTF_8);
        } catch (final IOException ex) {
            ex.printStackTrace();
            return;
        }

        showMessage("command.levelledmobs.rules.reset-complete");
        main.reloadLM(sender);
    }

    private enum ResetDifficulty{
        BASIC, AVERAGE, ADVANCED, EXTREME, UNSPECIFIED
    }

    private void showHyperlink(final CommandSender sender, final String message, final String url){
        if (!(sender instanceof Player)){
            sender.sendMessage(url);
            return;
        }

        if (VersionUtils.isRunningPaper())
            PaperUtils.sendHyperlink(sender, message, url);
        else
            SpigotUtils.sendHyperlink(sender, message, url);
    }

    private void showRule(final CommandSender sender, @NotNull final String @NotNull [] args){
        if (args.length < 3){
            showMessage("command.levelledmobs.rules.rule-name-missing");
            return;
        }

        boolean showOnConsole = sender instanceof ConsoleCommandSender;

        String foundRule = null;
        final Map<String, RuleInfo> allRuleNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (final RuleInfo ruleInfo : main.rulesParsingManager.getAllRules())
            allRuleNames.put(ruleInfo.getRuleName().replace(" ", "_"), ruleInfo);

        String badRuleName = null;

        for (int i = 2; i < args.length; i++){
            final String arg = args[i].toLowerCase();

            if (foundRule == null && arg.length() > 0 && !arg.startsWith("/")) {
                if (allRuleNames.containsKey(arg))
                    foundRule = args[i];
                else if (badRuleName == null)
                    badRuleName = args[i];
            }

            if ("/console".equalsIgnoreCase(arg))
                showOnConsole = true;
        }

        if (badRuleName != null){
            showMessage("command.levelledmobs.rules.rule-name-invalid", "%rulename%", badRuleName);
            return;
        }
        if (foundRule == null){
            showMessage("command.levelledmobs.rules.rule-name-missing");
            return;
        }

        final RuleInfo rule = allRuleNames.get(foundRule);

        final StringBuilder sb = new StringBuilder();
        sb.append(getMessage("command.levelledmobs.rules.showing-rules", "%rulename%", rule.getRuleName()));
        sb.append("\n");

        sb.append(rule.formatRulesVisually(List.of("id")));
        if (showOnConsole)
            Utils.logger.info(sb.toString());
        else
            sender.sendMessage(sb.toString());
    }

    private void getMobBeingLookedAt(@NotNull final Player player, final boolean showOnConsole, final boolean findNearbyEntities){
        LivingEntity livingEntity = null;
        final Location eye = player.getEyeLocation();
        final SortedMap<Double, LivingEntity> entities = new TreeMap<>();

        for(final Entity entity : player.getNearbyEntities(10, 10, 10)){
            if (!(entity instanceof LivingEntity)) continue;

            final LivingEntity le = (LivingEntity) entity;
            if (findNearbyEntities) {
                final double distance = le.getLocation().distanceSquared(player.getLocation());
                entities.put(distance, le);
            } else {
                final Vector toEntity = le.getEyeLocation().toVector().subtract(eye.toVector());
                final double dot = toEntity.normalize().dot(eye.getDirection());
                if (dot >= 0.975D) {
                    livingEntity = le;
                    break;
                }
            }
        }

        if (!findNearbyEntities && livingEntity == null)
            showMessage("command.levelledmobs.rules.no-entities-visible");
        else if (findNearbyEntities && entities.isEmpty())
            showMessage("command.levelledmobs.rules.no-entities-near");
        else {
            if (findNearbyEntities)
                livingEntity = entities.get(entities.firstKey());

            createParticleEffect(livingEntity.getLocation());
            final LivingEntityWrapper lmEntity = LivingEntityWrapper.getInstance(livingEntity, main);

            String entityName = lmEntity.getTypeName();
            if (ExternalCompatibilityManager.hasMythicMobsInstalled() && ExternalCompatibilityManager.isMythicMob(lmEntity))
                entityName = ExternalCompatibilityManager.getMythicMobInternalName(lmEntity);

            final String locationStr = String.format("%s, %s, %s",
                    lmEntity.getLivingEntity().getLocation().getBlockX(),
                    lmEntity.getLivingEntity().getLocation().getBlockY(),
                    lmEntity.getLivingEntity().getLocation().getBlockZ());
            final String mobLevel = lmEntity.isLevelled() ? lmEntity.getMobLevel() + "" : "0";
            final List<String> messages = getMessage("command.levelledmobs.rules.effective-rules",
                    new String[]{"%mobname%", "%entitytype%", "%location%", "%world%", "%level%"},
                    new String[]{ entityName, lmEntity.getNameIfBaby(), locationStr, lmEntity.getWorldName(), mobLevel }
            );


            final StringBuilder sb = new StringBuilder();
            sb.append(String.join("\n", messages).replace(main.configUtils.getPrefix() + " ", ""));

            player.sendMessage(sb.toString());
            if (!showOnConsole) sb.setLength(0);

            final BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    showEffectiveValues(player, lmEntity, showOnConsole, sb);
                }
            };

            lmEntity.inUseCount.getAndIncrement();
            runnable.runTaskLater(main, 25);
            lmEntity.free();
        }
    }

    private void createParticleEffect(@NotNull final Location location){
        final World world = location.getWorld();
        if (world == null) return;

        final BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 10; i++) {
                        world.spawnParticle(Particle.SPELL, location, 20, 0, 0, 0, 0.1);
                        Thread.sleep(50);
                    }
                } catch (final InterruptedException ignored) { }
            }
        };

        runnable.runTaskAsynchronously(main);
    }

    private void showEffectiveValues(final CommandSender sender, final @NotNull LivingEntityWrapper lmEntity, final boolean showOnConsole, final StringBuilder sb){
        final SortedMap<String, String> values = new TreeMap<>();
        final List<String> printedKeys = new LinkedList<>();
        final List<RuleInfo> effectiveRules = lmEntity.getApplicableRules();

        if (effectiveRules.isEmpty()){
            if (showOnConsole)
                Utils.logger.info(sb + "\n" + getMessage("command.levelledmobs.rules.no-effective-rules").replace(main.configUtils.getPrefix() + " ", ""));
            else
                showMessage("command.levelledmobs.rules.no-effective-rules");
            return;
        }

        if (sb.length() > 0) sb.append("\n");

        try {
            for (int i = effectiveRules.size() - 1; i >= 0; i--) {
                final RuleInfo pi = effectiveRules.get(i);

                for (final Field f : pi.getClass().getDeclaredFields()) {
                    if (Modifier.isPrivate(f.getModifiers())) continue;
                    f.setAccessible(true);
                    if (f.get(pi) == null) continue;
                    if (printedKeys.contains(f.getName())) continue;
                    if (f.getName().equals("ruleSourceNames")) continue;
                    final Object value = f.get(pi);
                    if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) continue;
                    if (value instanceof List && ((List<?>) value).isEmpty()) continue;
                    if (value instanceof Enum &&
                            ("NONE".equals(value.toString()) || "NOT_SPECIFIED".equals(value.toString()))) continue;
                    if (f.getName().equals("ruleIsEnabled")) continue;

                    String showValue = f.getName() + ", value: " + value;
                    showValue += ", &1source: " + (
                            pi.ruleSourceNames.containsKey(f.getName()) ? pi.ruleSourceNames.get(f.getName()) : pi.getRuleName()
                    );
                    values.put(f.getName(), showValue);

                    printedKeys.add(f.getName());
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        final String fineTuning = "fine-tuning: " + (lmEntity.getFineTuningAttributes() == null ? "(null)" : lmEntity.getFineTuningAttributes().toString());
        sb.append(fineTuning);
        sb.append("&r\n");

        for (final String s : values.values()){
            sb.append(s);
            sb.append("&r\n");
        }

        sb.setLength(sb.length() - 1);

        if (showOnConsole)
            Utils.logger.info(sb.toString());
        else
            sender.sendMessage(MessageUtils.colorizeAll(sb.toString()));
    }

    @Override
    public List<String> parseTabCompletions(final LevelledMobs main, final @NotNull CommandSender sender, @NotNull final String[] args) {
        if (!sender.hasPermission("levelledmobs.command.rules"))
            return Collections.emptyList();

        final List<String> suggestions = new LinkedList<>();

        if (args.length == 2)
            return Arrays.asList("force_all", "help_discord", "help_wiki", "reset", "show_all", "show_effective", "show_rule");
        else if (args.length >= 3) {
            if ("reset".equalsIgnoreCase(args[1]) && args.length == 3)
                suggestions.addAll(List.of("basic", "average", "enhanced", "extreme"));
            else if ("show_all".equalsIgnoreCase(args[1])) {
                boolean showOnConsole = false;
                for (int i = 2; i < args.length; i++) {
                    final String arg = args[i].toLowerCase();

                    if ("/console".equalsIgnoreCase(arg)) {
                        showOnConsole = true;
                        break;
                    }
                }
                if (!showOnConsole) suggestions.add("/console");
            } else if ("show_rule".equalsIgnoreCase(args[1]) || "show_effective".equalsIgnoreCase(args[1])) {
                final boolean isShowRule = "show_rule".equalsIgnoreCase(args[1]);
                final boolean isEffective = "show_effective".equalsIgnoreCase(args[1]);
                boolean showOnConsole = false;
                boolean findNearbyEntities = false;
                boolean foundValue = false;
                final Set<String> allRuleNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                for (final RuleInfo ruleInfo : main.rulesParsingManager.getAllRules())
                    allRuleNames.add(ruleInfo.getRuleName().replace(" ", "_"));

                for (int i = 2; i < args.length; i++) {
                    final String arg = args[i].toLowerCase();

                    if (arg.length() > 0 && !arg.startsWith("/") && allRuleNames.contains(arg))
                        foundValue = true;

                    if ("/console".equalsIgnoreCase(arg))
                        showOnConsole = true;
                    else if ("/near".equalsIgnoreCase(arg))
                        findNearbyEntities = true;
                }
                if (!showOnConsole) suggestions.add("/console");
                if (isEffective && !findNearbyEntities) suggestions.add("/near");
                if (isShowRule && !foundValue) suggestions.addAll(allRuleNames);
            }
        }

        if (suggestions.isEmpty()) return Collections.emptyList();
        return suggestions;
    }
}
