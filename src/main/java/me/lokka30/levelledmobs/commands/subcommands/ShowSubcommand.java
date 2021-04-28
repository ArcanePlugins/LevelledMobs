package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.RuleInfo;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ShowSubcommand implements Subcommand {
    @Override
    public void parseSubcommand(LevelledMobs main, CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("levelledmobs.command.show")){
            main.configUtils.sendNoPermissionMsg(sender);
            return;
        }

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

    private void showAllValues(@NotNull final RuleInfo pi){
        // this is only used for dev work

        SortedMap<String, String> values = new TreeMap<>();

        Utils.logger.info("id: " + pi.getInternalId());
        try {
            for(final Field f : pi.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (f.get(pi) == null) continue;
                final Object value = f.get(pi);
                values.put(f.getName(), f.getName() + ", value: " + value);
                // values.put(f.getName(), f.getName() + ", value: " + (value == null ? "(null)" : value));
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        for (final String key : values.keySet()){
            Utils.logger.info(values.get(key));
        }
    }

    @Override
    public List<String> parseTabCompletions(LevelledMobs main, CommandSender sender, String[] args) {
        return null; //No tab completions.
    }
}
