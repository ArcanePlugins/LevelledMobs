package me.lokka30.levelledmobs.misc;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * @author stumper66
 */
public class FileMigrator {

    private static int getFieldDepth(String line) {
        int whiteSpace = 0;

        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ' ') break;
            whiteSpace++;
        }
        return whiteSpace == 0 ? 0 : whiteSpace / 2;
    }

    private static class FieldInfo {
        public String simpleValue;
        public List<String> valueList;
        public final int depth;
        public boolean hasValue;

        public FieldInfo(String value, int depth) {
            this.simpleValue = value;
            this.depth = depth;
        }

        public FieldInfo(String value, int depth, boolean isListValue) {
            if (isListValue) addListValue(value);
            else this.simpleValue = value;
            this.depth = depth;
        }

        public boolean isList(){
            return valueList != null;
        }

        public void addListValue(String value){
            if (valueList == null) valueList = new ArrayList<>();
            valueList.add(value);
        }

        public String toString() {
            if (this.isList()){
                if (this.valueList == null || this.valueList.isEmpty())
                    return super.toString();
                else
                    return String.join(",", this.valueList);
            }

            if (this.simpleValue == null)
                return super.toString();
            else
                return this.simpleValue;
        }
    }

    private static String getKeyFromList(List<String> list, String currentKey){
        if (list.size() == 0) return currentKey;

        String result = String.join(".", list);
        if (currentKey != null) result += "." + currentKey;

        return result;
    }

    protected static void copyYmlValues(File from, File to, int oldVersion) {

        final String regexPattern = "^[^':]*:.*";
        boolean isSettings = to.getName().equalsIgnoreCase("settings.yml");
        boolean isCustomDrops = to.getName().equalsIgnoreCase("customdrops.yml");
        boolean showMessages = !to.getName().equalsIgnoreCase("messages.yml");
        final List<String> processedKeys = new ArrayList<>();

        // version 20 = 1.34 - last version before 2.0
        final List<String> version20KeysToKeep = Arrays.asList(
                "level-passive",
                "fine-tuning.min-level",
                "fine-tuning.max-level",
                "spawn-distance-levelling.active",
                "spawn-distance-levelling.variance.enabled",
                "spawn-distance-levelling.variance.max",
                "spawn-distance-levelling.variance.min",
                "spawn-distance-levelling.increase-level-distance",
                "spawn-distance-levelling.start-distance",
                "use-update-checker");

        // version 2.1.0 - these fields should be reset to default
        final List<String> version24Resets = Arrays.asList(
                "fine-tuning.additions.movement-speed",
                "fine-tuning.additions.attack-damage"
        );

        final String useCustomDrops = "use-custom-item-drops-for-mobs";

        try {
            final List<String> oldConfigLines = Files.readAllLines(from.toPath(), StandardCharsets.UTF_8);
            final List<String> newConfigLines = Files.readAllLines(to.toPath(), StandardCharsets.UTF_8);

            final SortedMap<String, FileMigrator.FieldInfo> oldConfigMap = getMapFromConfig(oldConfigLines);
            final SortedMap<String, FileMigrator.FieldInfo> newConfigMap = getMapFromConfig(newConfigLines);
            final List<String> currentKey = new ArrayList<>();
            int keysMatched = 0;
            int valuesUpdated = 0;
            int valuesMatched = 0;

            if (!isCustomDrops) {
                for (int currentLine = 0; currentLine < newConfigLines.size(); currentLine++) {
                    String line = newConfigLines.get(currentLine);
                    final int depth = getFieldDepth(line);
                    if (line.trim().startsWith("#") || line.trim().isEmpty()) continue;

                    //if (line.contains(":")) {
                    if (line.matches(regexPattern)) {
                        //final String[] lineSplit = line.split(":", 2);
                        int firstColon = line.indexOf(":");
                        boolean hasValues = line.length() > firstColon + 1;
                        String key = line.substring(0, firstColon).replace("\t", "").trim();
                        final String keyOnly = key;

                        if (depth == 0)
                            currentKey.clear();
                        else if (currentKey.size() > depth) {
                            while (currentKey.size() > depth) currentKey.remove(currentKey.size() - 1);
                            key = getKeyFromList(currentKey, key);
                        } else
                            key = getKeyFromList(currentKey, key);

                        if (!hasValues) {
                            currentKey.add(keyOnly);

                            if (isSettings && oldVersion <= 20 && !version20KeysToKeep.contains(key)) continue;

                            if (oldConfigMap.containsKey(key) && newConfigMap.containsKey(key)) {
                                final FileMigrator.FieldInfo fiOld = oldConfigMap.get(key);
                                final FileMigrator.FieldInfo fiNew = newConfigMap.get(key);
                                final String padding = getPadding((depth + 1) * 2);
                                // arrays go here:
                                if (fiOld.isList()) {
                                    // add any values present in old list that might not be present in new
                                    for (String oldValue : fiOld.valueList) {
                                        if (!fiNew.isList() || !fiNew.valueList.contains(oldValue)) {
                                            final String newline = padding + "- " + oldValue; // + "\r\n" + line;
                                            newConfigLines.add(currentLine + 1, newline);
                                            if (showMessages)
                                                Utils.logger.info("&fFile Loader: &8(Migration) &7Added array value: &b" + oldValue);
                                        }
                                    }
                                }
                                else {
                                    // non-array values go here.  Loop thru and find any subkeys under here
                                    final int numOfPeriods = countPeriods(key);
                                    for (final String enumeratedKey : oldConfigMap.keySet()){
                                        final int numOfPeriods_Enumerated = countPeriods(enumeratedKey);
                                        if (enumeratedKey.startsWith(key) && numOfPeriods_Enumerated == numOfPeriods + 1 && !newConfigMap.containsKey(enumeratedKey)){
                                            final FileMigrator.FieldInfo fi = oldConfigMap.get(enumeratedKey);
                                            if (!fi.isList() && fi.simpleValue != null){
                                                final String newPadding = getPadding(depth * 2);
                                                final String newline = padding + getEndingKey(enumeratedKey) + ": " + fi.simpleValue;
                                                newConfigLines.add(currentLine + 1, newline);
                                                if (showMessages)
                                                    Utils.logger.info("&fFile Loader: &8(Migration) &7Adding key: &b" + enumeratedKey + "&7, value: &r" + fi.simpleValue + "&7.");
                                                processedKeys.add(key);
                                            }
                                        }
                                    }
                                }
                            }
                        } else //noinspection ConstantConditions
                            if (hasValues && oldConfigMap.containsKey(key)) {
                                keysMatched++;
                                final String value = line.substring(firstColon + 1).trim();
                                final FileMigrator.FieldInfo fi = oldConfigMap.get(key);
                                final String migratedValue = fi.simpleValue;

                                if (isSettings && oldVersion <= 20 && !version20KeysToKeep.contains(key)) continue;
                                if (isSettings && oldVersion < 24 && version24Resets.contains(key)) continue;
                                if (key.startsWith("file-version")) continue;
                                if (isSettings && key.equalsIgnoreCase("creature-nametag") && oldVersion > 20 && oldVersion < 26
                                        && migratedValue.equals("'&8[&7Level %level%&8 | &f%displayname%&8 | &c%health%&8/&c%max_health% %heart_symbol%&8]'")) {
                                // updating to the new default introduced in file ver 26 if they were using the previous default
                                continue;
                            }
                            if (isSettings && oldVersion < 28 && key.equalsIgnoreCase(useCustomDrops) &&
                                    oldConfigMap.containsKey(useCustomDrops) &&
                                    oldConfigMap.get(useCustomDrops).simpleValue.equalsIgnoreCase("true")){

                                if (showMessages) Utils.logger.info("&fFile Loader: &8(Migration) &7Current key: &b" + key + "&7, resetting to: &rfalse&7.");
                                newConfigMap.get(useCustomDrops).simpleValue = "false";
                                valuesUpdated++;
                                continue;
                            }

                            final String parentKey = getParentKey(key);
                            if (fi.hasValue && parentKey != null && !processedKeys.contains(parentKey)){
                                // here's where we add values from the old config not present in the new
                                for (String oldValue : oldConfigMap.keySet()){
                                    if (!oldValue.startsWith(parentKey)) continue;
                                    if (newConfigMap.containsKey(oldValue)) continue;
                                    if (!isEntitySameSubkey(parentKey, oldValue)) continue;

                                    FileMigrator.FieldInfo fiOld = oldConfigMap.get(oldValue);
                                    if (fiOld.isList()) continue;
                                    final String padding = getPadding(depth * 2);
                                    final String newline = padding + getEndingKey(oldValue) + ": " + fiOld.simpleValue;
                                    newConfigLines.add(currentLine + 1, newline);
                                    if (showMessages) Utils.logger.info("&fFile Loader: &8(Migration) &7Adding key: &b" + oldValue + "&7, value: &r" + fiOld.simpleValue + "&7.");
                                }
                                processedKeys.add(parentKey);
                            }

                            if (!value.equals(migratedValue)) {
                                valuesUpdated++;
                                if (showMessages) Utils.logger.info("&fFile Loader: &8(Migration) &7Current key: &b" + key + "&7, replacing: &r" + value + "&7, with: &r" + migratedValue + "&7.");
                                line = line.replace(value, migratedValue);
                                newConfigLines.set(currentLine, line);
                            } else
                                valuesMatched++;
                        }
                    } else if (line.trim().startsWith("-")) {
                        final String key = getKeyFromList(currentKey, null);
                        final String value = line.trim().substring(1).trim();

                        // we have an array value present in the new config but not the old, so it must've been removed
                        if (oldConfigMap.containsKey(key) && oldConfigMap.get(key).isList() && !oldConfigMap.get(key).valueList.contains(value)) {
                            newConfigLines.remove(currentLine);
                            currentLine--;
                            if (showMessages) Utils.logger.info("&fFile Loader: &8(Migration) &7Current key: &b" + key + "&7, removing value: &r" + value + "&7.");
                        }
                    }
                } // loop to next line
            } // end if is not custom drops
            else  {
                // migrate all values
                int startAt = 0;

                for (int i = 0; i < newConfigLines.size(); i++){
                    String line = newConfigLines.get(i).trim();

                    if (line.startsWith("file-version")){
                        startAt = i + 1;
                        break;
                    }
                }

                while (newConfigLines.size() > startAt + 1)
                    newConfigLines.remove(newConfigLines.size() - 1);

                final int firstNonCommentLine = getFirstNonCommentLine(oldConfigLines);

                for (int i = firstNonCommentLine; i < oldConfigLines.size(); i++){
                    String temp = oldConfigLines.get(i).replaceAll("\\s+$", ""); // trimEnd()
                    if (temp.endsWith("nomultiplier:") || temp.endsWith("nospawner:")) {
                        temp += " true";
                        newConfigLines.add(temp);
                    } else
                        newConfigLines.add(oldConfigLines.get(i));
                }
            }

            Files.write(to.toPath(), newConfigLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            Utils.logger.info("&fFile Loader: &8(Migration) &7Migrated &b" + to.getName() + "&7 successfully.");
            Utils.logger.info(String.format("&fFile Loader: &8(Migration) &7Keys matched: &b%s&7, values matched: &b%s&7, values updated: &b%s&7.", keysMatched, valuesMatched, valuesUpdated));
        } catch (Exception e) {
            Utils.logger.error("&fFile Loader: &8(Migration) &7Failed to migrate &b" + to.getName() + "&7! Stack trace:");
            e.printStackTrace();
        }
    }

    private static int countPeriods(final String text){
        int count = 0;

        for (int i = 0; i < text.length(); i++){
            if (text.charAt(i) == '.') count++;
        }

        return count;
    }

    private static String getPadding(final int space){
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < space; i++)
            sb.append(" ");

        return sb.toString();
    }

    private static boolean isEntitySameSubkey(final String key1, final String key2){
        final int lastPeriod = key2.lastIndexOf(".");
        final String checkKey = lastPeriod > 0 ? key2.substring(0, lastPeriod) : key2;

        return (key1.equalsIgnoreCase(checkKey));
    }

    private static String getEndingKey(String input){
        final int lastPeriod = input.lastIndexOf(".");
        if (lastPeriod < 0) return input;

        return input.substring(lastPeriod + 1);
    }

    private static String getParentKey(String input){
        final int lastPeriod = input.lastIndexOf(".");
        if (lastPeriod < 0) return null;

        return input.substring(0, lastPeriod);
    }

    private static int getFirstNonCommentLine(List<String> input){
        for (int lineNum = 0; lineNum < input.size(); lineNum++) {
            final String line = input.get(lineNum).replace("\t", "").trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            return lineNum;
        }

        return -1;
    }

    @Nonnull
    private static SortedMap<String, FileMigrator.FieldInfo> getMapFromConfig(List<String> input) {
        //final Map<String, FieldInfo> configMap = new HashMap<>();
        final SortedMap<String, FileMigrator.FieldInfo> configMap = new TreeMap<>();
        final List<String> currentKey = new ArrayList<>();
        final String regexPattern = "^[^':]*:.*";

        int lineNum = -1;
        for (String line : input) {
            lineNum++;

            final int depth = getFieldDepth(line);
            line = line.replace("\t", "").trim();
            if (line.startsWith("#") || line.isEmpty()) continue;

            //if (line.contains(":")) {
            if (line.matches(regexPattern)) {
                int firstColon = line.indexOf(":");
                boolean hasValues = line.length() > firstColon + 1;
                String key = line.substring(0, firstColon).replace("\t", "").trim();
                final String origKey = key;

                if (origKey.startsWith("-")) {
                    if (currentKey.size() > depth)
                        while (currentKey.size() > depth) currentKey.remove(currentKey.size() - 1);
                    String temp = origKey.substring(1).trim();
                    String tempKey;
                    for (int i = 0; i < 100; i++) {
                        tempKey = String.format("%s[%s]", temp, i);
                        final String checkKey = getKeyFromList(currentKey, tempKey);
                        if (!configMap.containsKey(checkKey)) {
                            currentKey.add(tempKey);
                            configMap.put(checkKey, null);
                            break;
                        }
                    }
                    continue;
                }

                if (depth == 0)
                    currentKey.clear();
                else {
                    if (currentKey.size() > depth)
                        while (currentKey.size() > depth) currentKey.remove(currentKey.size() - 1);
                    key = getKeyFromList(currentKey, key);
                }

                if (!hasValues) {
                    currentKey.add(origKey);
                    if (!configMap.containsKey(key)) configMap.put(key, new FileMigrator.FieldInfo(null, depth));
                } else {
                    final String value = line.substring(firstColon + 1).trim();
                    final FileMigrator.FieldInfo fi = new FileMigrator.FieldInfo(value, depth);
                    fi.hasValue = true;
                    configMap.put(key, fi);
                }
            } else if (line.startsWith("-")) {
                final String key = getKeyFromList(currentKey, null);
                final String value = line.trim().substring(1).trim();
                if (configMap.containsKey(key)) {
                    FileMigrator.FieldInfo fi = configMap.get(key);
                    fi.addListValue(value);
                } else
                    configMap.put(key, new FileMigrator.FieldInfo(value, depth, true));
            }
        }

        return configMap;
    }
}
