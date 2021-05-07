package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class RulesSubcommand implements Subcommand {
    public RulesSubcommand(final LevelledMobs main){
        this.main = main;
    }

    private final LevelledMobs main;

    @Override
    public void parseSubcommand(LevelledMobs main, CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("levelledmobs.command.rules")){
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

        if (args.length == 1){
            return;
        }

        final boolean showOnConsole = args.length > 2 && "console".equalsIgnoreCase(args[2]);

        if ("show_all".equalsIgnoreCase(args[1])) {
            if (sender instanceof Player)
                sender.sendMessage("rules have been printed on the console");

            Utils.logger.info("--------------------------------- default values below -------------------------------");
            showAllValues(main.rulesParsingManager.defaultRule);
            for (final String key : main.rulesParsingManager.rulePresets.keySet()) {
                final RuleInfo rpi = main.rulesParsingManager.rulePresets.get(key);
                Utils.logger.info("--------------------------------- preset rule below ----------------------------------");
                showAllValues(rpi);
            }
            for (RuleInfo rpi : main.rulesParsingManager.customRules) {
                Utils.logger.info("--------------------------------- custom-rule below ----------------------------------");
                showAllValues(rpi);
            }
            Utils.logger.info("--------------------------------------------------------------------------------------");
        }
        else if ("show_effective".equalsIgnoreCase(args[1])) {
            if (!(sender instanceof Player)){
                sender.sendMessage("The command must be run by a player");
                return;
            }

            getMobBeingLookedAt((Player) sender, showOnConsole);
        }
    }

    private void getMobBeingLookedAt(Player player, final boolean showOnConsole){
        LivingEntity livingEntity = null;
        final Location eye = player.getEyeLocation();

        for(final Entity entity : player.getNearbyEntities(10, 10, 10)){
            if (!(entity instanceof LivingEntity)) continue;

            LivingEntity le = (LivingEntity) entity;
            Vector toEntity = le.getEyeLocation().toVector().subtract(eye.toVector());
            double dot = toEntity.normalize().dot(eye.getDirection());
            if (dot >= 0.975D){
                livingEntity = le;
                break;
            }
        }

        if (livingEntity == null)
            player.sendMessage("Must be looking at a nearby entity");
        else {
            createParticleEffect(livingEntity.getLocation());
            LivingEntityWrapper lmEntity = new LivingEntityWrapper(livingEntity, main);

            final String message = String.format("showing effective rules for: %s%s at location: %s, %s, %s",
                    lmEntity.isLevelled() ? "level " + lmEntity.getMobLevel() + " " : "",
                    lmEntity.getLivingEntity().getName(),
                    lmEntity.getLivingEntity().getLocation().getBlockX(),
                    lmEntity.getLivingEntity().getLocation().getBlockY(),
                    lmEntity.getLivingEntity().getLocation().getBlockZ()
            );

            player.sendMessage(message);
            if (showOnConsole) Utils.logger.info(message);

            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    showEffectiveValues(player, lmEntity, showOnConsole);
                }
            };

            runnable.runTaskLater(main, 25);
        }
    }

    private void createParticleEffect(final Location location){
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
                } catch (InterruptedException ignored) { }
            }
        };

        runnable.run();
    }


    private void showAllValues(@NotNull final RuleInfo pi){
        final SortedMap<String, String> values = new TreeMap<>();

        Utils.logger.info("id: " + pi.getRuleName());
        try {
            for(final Field f : pi.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (f.get(pi) == null) continue;
                if (f.getName().equals("ruleSourceNames")) continue;
                final Object value = f.get(pi);
                if (value.toString().equalsIgnoreCase("NOT_SPECIFIED")) continue;
                if (value.toString().equalsIgnoreCase("{}")) continue;
                if (value.toString().equalsIgnoreCase("[]")) continue;
                if (value.toString().equalsIgnoreCase("0")) continue;
                if (value.toString().equalsIgnoreCase("0.0")) continue;
                if (value.toString().equalsIgnoreCase("false")) continue;
                if (value.toString().equalsIgnoreCase("NONE")) continue;
                final String showValue = f.getName() + ", value: " + value;
                values.put(f.getName(), showValue);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        for (final String key : values.keySet()){
            Utils.logger.info(values.get(key));
        }
    }

    private void showEffectiveValues(CommandSender sender, @NotNull LivingEntityWrapper lmEntity, boolean showOnConsole){
        final SortedMap<String, String> values = new TreeMap<>();
        final List<String> printedKeys = new LinkedList<>();
        final List<RuleInfo> effectiveRules = lmEntity.getApplicableRules();

        if (effectiveRules.isEmpty()){
            if (showOnConsole)
                Utils.logger.info("No effective rules were found");
            else
                sender.sendMessage("No effective rules were found");
            return;
        }

        try {
            for (int i = effectiveRules.size() - 1; i >= 0; i--) {
                final RuleInfo pi = effectiveRules.get(i);

                for (final Field f : pi.getClass().getDeclaredFields()) {
                    if (!Modifier.isPublic(f.getModifiers())) continue;
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
        }
        catch (Exception e){
            e.printStackTrace();
        }

        final String fineTuning = "fine-tuning: " + (lmEntity.getFineTuningAttributes() == null ? "(null)" : lmEntity.getFineTuningAttributes().toString());
        if (showOnConsole)
            Utils.logger.info(fineTuning);
        else
            sender.sendMessage(fineTuning);

        for (final String key : values.keySet()){
            if (showOnConsole)
                Utils.logger.info(values.get(key));
            else
                sender.sendMessage(values.get(key));
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs main, CommandSender sender, String[] args) {
        if (args.length == 2)
            return Arrays.asList("show_all", "show_effective");
        else if (args.length == 3)
            return Collections.singletonList("console");

        return Collections.singletonList("");
    }
}
