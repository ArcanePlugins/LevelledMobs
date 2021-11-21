package me.lokka30.levelledmobs.commands.subcommands;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.commands.MessagesBase;
import me.lokka30.levelledmobs.misc.PaperUtils;
import me.lokka30.levelledmobs.misc.SpigotUtils;
import me.lokka30.levelledmobs.misc.Utils;
import me.lokka30.levelledmobs.rules.DoNotMerge;
import me.lokka30.microlib.messaging.MessageUtils;
import me.lokka30.microlib.other.VersionUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Provides common function between SpawnerSubCommand and SpawnerEggCommand
 *
 * @author stumper66
 * @since 3.3.0
 */
public class SpawnerBaseClass extends MessagesBase {
    SpawnerBaseClass(final LevelledMobs main){
        super(main);
    }

    boolean hadInvalidArg;
    int startingArgNum;

    @Nullable
    String getArgValue(final String key, final String @NotNull [] args, final boolean mustBeNumber){
        int keyFlag = -1;
        int nameStartFlag = - 1;
        int nameEndFlag = - 1;

        for (int i = startingArgNum; i < args.length; i++){
            final String arg = args[i];
            if (key.equalsIgnoreCase(arg))
                keyFlag = i;
            else if (keyFlag == i - 1 && arg.startsWith("\""))
                nameStartFlag = i;
            else if (nameStartFlag > -1 && !arg.startsWith("/") && arg.endsWith("\"")) {
                nameEndFlag = i;
                break;
            }
        }

        if (keyFlag < 0) return null;
        String keyValue;

        if (nameEndFlag > 0) {
            final StringBuilder sb = new StringBuilder();
            for (int i = nameStartFlag; i <= nameEndFlag; i++) {
                if (i > 0) sb.append(" ");
                sb.append(args[i].trim());
            }
            keyValue = sb.toString().trim();
            keyValue = keyValue.substring(1, keyValue.length() - 1);
        }
        else
            keyValue = parseFlagValue(key, keyFlag, args, mustBeNumber);

        return keyValue;
    }

    @Nullable
    private String parseFlagValue(final String keyName, final int argNumber, final String @NotNull [] args, final boolean mustBeNumber){
        if (argNumber + 1 >= args.length || args[argNumber + 1].startsWith("/")){
            showMessage("command.levelledmobs.spawner.no-value", "%keyname%", keyName);
            hadInvalidArg = true;
            return null;
        }

        if (mustBeNumber && !Utils.isInteger(args[argNumber + 1])){
            showMessage("command.levelledmobs.spawner.invalid-value", "%keyname%", keyName);
            hadInvalidArg = true;
            return null;
        }

        return args[argNumber + 1];
    }

    static void setMetaItems(final @Nullable ItemMeta meta, final @NotNull CustomSpawnerInfo info){
        if (meta == null) return;

        if (VersionUtils.isRunningPaper())
            PaperUtils.updateItemDisplayName(meta, info.customName == null ? "LM Spawn Egg" : info.customName);
        else
            SpigotUtils.updateItemDisplayName(meta, info.customName == null ? "LM Spawn Egg" : info.customName);

        List<String> lore = new LinkedList<>();

        try {
            int itemsCount = 0;
            final StringBuilder loreLine = new StringBuilder();
            for (final Field f : info.getClass().getDeclaredFields()) {
                if (!Modifier.isPublic(f.getModifiers())) continue;
                if (f.get(info) == null) continue;
                final String name = f.getName();
                if (f.isAnnotationPresent(DoNotMerge.class)) continue;

                if ("-1".equals(f.get(info).toString()) && (name.equals("minLevel") || name.equals("maxLevel")))
                    continue;

                if (itemsCount > 2){
                    lore.add(loreLine.toString());
                    loreLine.setLength(0);
                    itemsCount = 0;
                }

                if (loreLine.length() > 0) loreLine.append(", ");
                loreLine.append(String.format("&7%s: &b%s&7", name, f.get(info)));
                itemsCount++;
            }
            if (itemsCount > 0)
                lore.add(loreLine.toString());
        }
        catch (final Exception e){
            e.printStackTrace();
        }

        if (!info.noLore && info.lore == null && info.customLore == null) {
            lore = Utils.colorizeAllInList(lore);
            if (VersionUtils.isRunningPaper())
                PaperUtils.updateItemMetaLore(meta, lore);
            else
                SpigotUtils.updateItemMetaLore(meta, lore);

            final StringBuilder sbLore = new StringBuilder();
            for (final String loreLine : lore) {
                if (sbLore.length() > 0) sbLore.append("\n");
                sbLore.append(loreLine);
            }
            meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_Lore, PersistentDataType.STRING, sbLore.toString());
        }
        else if (!info.noLore || info.customLore != null){
            final String useLore = info.customLore == null ?
                    info.lore : MessageUtils.colorizeAll(info.customLore).replace("\\n", "\n");

            lore.clear();
            lore.addAll(List.of(useLore.split("\n")));
            if (VersionUtils.isRunningPaper())
                PaperUtils.updateItemMetaLore(meta, lore);
            else
                SpigotUtils.updateItemMetaLore(meta, lore);

            meta.getPersistentDataContainer().set(info.main.namespaced_keys.keySpawner_Lore, PersistentDataType.STRING, useLore);
        }
    }

    List<String> checkTabCompletion(final List<String> options, final String @NotNull [] args){
        final Set<String> commandsList = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        commandsList.addAll(options);

        boolean inQuotes = false;

        for (int i = 1; i < args.length; i++) {
            final String arg = args[i];

            if (arg.startsWith("\"") && !arg.endsWith("\""))
                inQuotes = true;
            else if (inQuotes && arg.endsWith("\""))
                inQuotes = false;

            commandsList.remove(arg);
        }

        final String lastArg = args[args.length - 1];

        if (inQuotes || lastArg.length() > 0 && lastArg.charAt(lastArg.length() - 1) == '\"')
            return Collections.emptyList();

        final List<String> result = new ArrayList<>(commandsList.size());
        result.addAll(commandsList);
        return result;
    }

    public static class CustomSpawnerInfo{
        public CustomSpawnerInfo(final LevelledMobs main, final String label){
            this.main = main;
            this.label = label;
            this.minLevel = -1;
            this.maxLevel = -1;
            this.spawnType = EntityType.UNKNOWN;
        }

        @DoNotMerge
        final public LevelledMobs main;
        @DoNotMerge
        final String label;
        @DoNotMerge
        public Player player;
        public int minLevel;
        public int maxLevel;
        @DoNotMerge
        boolean noLore;
        public Integer delay;
        public Integer maxNearbyEntities;
        public Integer minSpawnDelay;
        public Integer maxSpawnDelay;
        public Integer requiredPlayerRange;
        public Integer spawnCount;
        public Integer spawnRange;
        public String customDropId;
        @DoNotMerge
        public String customName;
        public EntityType spawnType;
        @DoNotMerge
        String customLore;
        public String lore;
    }
}
