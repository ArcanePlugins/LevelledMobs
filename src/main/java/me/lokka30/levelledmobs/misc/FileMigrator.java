/*
 * Copyright (c) 2020-2021  lokka30. Use of this source code is governed by the GNU AGPL v3.0 license that can be found in the LICENSE.md file.
 */

package me.lokka30.levelledmobs.misc;

import me.lokka30.levelledmobs.LevelledMobs;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migrates older yml versions to the latest available
 *
 * @author stumper66
 * @since 2.4.0
 */
public class FileMigrator {

    private static int getFieldDepth(@NotNull final String line) {
        int whiteSpace = 0;

        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != ' ') break;
            whiteSpace++;
        }
        return whiteSpace == 0 ? 0 : whiteSpace / 2;
    }

    private static class FieldInfo {
        String simpleValue;
        List<String> valueList;
        final int depth;
        boolean hasValue;

        FieldInfo(final String value, final int depth) {
            this.simpleValue = value;
            this.depth = depth;
        }

        FieldInfo(final String value, final int depth, final boolean isListValue) {
            if (isListValue) addListValue(value);
            else this.simpleValue = value;
            this.depth = depth;
        }

        boolean isList(){
            return valueList != null;
        }

        void addListValue(final String value){
            if (valueList == null) valueList = new LinkedList<>();
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

    private static class KeySectionInfo{
        KeySectionInfo(){
            this.lines = new LinkedList<>();
        }

        int lineNumber;
        @Nonnull
        final List<String> lines;
        int sectionNumber;
        int sectionStartingLine;
    }

    private static String getKeyFromList(final @NotNull List<String> list, final String currentKey){
        if (list.size() == 0) return currentKey;

        String result = String.join(".", list);
        if (currentKey != null) result += "." + currentKey;

        return result;
    }

    public static void migrateSettingsToRules(@NotNull final LevelledMobs main){
        final File fileSettings = new File(main.getDataFolder(), "settings.yml");
        final File fileRules = new File(main.getDataFolder(), "rules.yml");
        if (!fileSettings.exists() || !fileRules.exists()) return;

        final File backedupFile = new File(main.getDataFolder(), "rules.yml.old");
        FileUtil.copy(fileRules, backedupFile);

        final int worldListAllowedLine = 177 - 1; // minus 1 is due to 0 indexing of arrays
        final int worldListExcludedLine = worldListAllowedLine + 1;

        final YamlConfiguration settings = YamlConfiguration.loadConfiguration(fileSettings);
        final YamlConfiguration rules = YamlConfiguration.loadConfiguration(fileRules);
        try {
            final List<String> settingsLines = Files.readAllLines(fileSettings.toPath(), StandardCharsets.UTF_8);
            final List<String> rulesLines = Files.readAllLines(fileRules.toPath(), StandardCharsets.UTF_8);

            final String worldMode = settings.getString("allowed-worlds-list.mode");
            final List<String> worldList = settings.getStringList("allowed-worlds-list.list");

            if ("ALL".equalsIgnoreCase(worldMode)) {
                rulesLines.set(worldListAllowedLine, "        allowed-list: ['*']");
                rulesLines.set(worldListExcludedLine, "        excluded-list: ['']");
            } else if ("WHITELIST".equalsIgnoreCase(worldMode)) {
                final String newWorldList = compileListFromArray(worldList);
                rulesLines.set(worldListAllowedLine, "        allowed-list: " + newWorldList);
                rulesLines.set(worldListExcludedLine, "        excluded-list: ['']");
            } else {
                final String newWorldList = compileListFromArray(worldList);
                rulesLines.set(worldListAllowedLine, "        allowed-list: ['']");
                rulesLines.set(worldListExcludedLine, "        excluded-list: " + newWorldList);
            }

            Files.write(fileRules.toPath(), rulesLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            Utils.logger.info("&fFile Loader: &8(Migration) &7Migrated &bworld allowed list&7 successfully.");
            final List<String> msg = Arrays.asList("\n&c[WARNING] LevelledMobs3 Settings have Reset!",
                    "\n&c[WARNING] Your original LM configuration files have been saved!",
                    "\n&c[WARNING]&r Due to significant changes, most settings WILL NOT MIGRATE from LM2.X to LM3.X.",
                    "\n&c[WARNING]&r You must edit rules.yml to further customize LM!",
                    "\n&c[WARNING]&r FOR ASSISTANCE, VISIT OUR SUPPORT DISCORD",
                    "\n&c[WARNING]&r https://discord.io/arcaneplugins");
            final String msg2 = Utils.colorizeAllInList(msg).toString();
            Utils.logger.warning(msg2.substring(1, msg2.length() - 2));
            main.migratedFromPre30 = true;
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    @NotNull
    private static String compileListFromArray(final @NotNull List<String> list){
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (final String item : list){
            if (sb.length() > 1) sb.append(", ");
            sb.append("'");
            sb.append(item);
            sb.append("'");
        }
        sb.append("]");

        return sb.toString();
    }

    static void copyCustomDrops(@NotNull final File from, @NotNull final File to, final int fileVersion){
        final TreeMap<String, KeySectionInfo> keySections_Old;
        TreeMap<String, KeySectionInfo> keySections_New;

        try {
            final List<String> oldConfigLines = Files.readAllLines(from.toPath(), StandardCharsets.UTF_8);
            final List<String> newConfigLines = Files.readAllLines(to.toPath(), StandardCharsets.UTF_8);

            keySections_Old = buildKeySections(oldConfigLines);
            keySections_New = buildKeySections(newConfigLines);

            for (final Map.Entry<String, KeySectionInfo> keys : keySections_Old.entrySet()){
                final String key = keys.getKey();
                if (key.toLowerCase().startsWith("file-version")) continue;

                final KeySectionInfo oldSection = keys.getValue();
                if (keySections_New.containsKey(key)){
                    // overwrite new section if different
                    final KeySectionInfo newSection = keySections_New.get(key);

                    if (!doSectionsContainSameLines(oldSection, newSection)) {
                        for (int i = 0; i < newSection.lines.size(); i++)
                            newConfigLines.remove(newSection.lineNumber + 1);

                        for (int i = oldSection.lines.size() - 1; i >= 0; i--) {
                            newConfigLines.add(newSection.lineNumber + 1, oldSection.lines.get(i));
                        }

                        keySections_New = buildKeySections(newConfigLines);
                    }
                } else {
                    // write the section into the new config, starting in corresponding new section
                    int insertAt = newConfigLines.size();
                    if (fileVersion < 6) {
                        if (key.toUpperCase().startsWith("ALL_"))
                            oldSection.sectionNumber = 2; // universal groups section
                        else
                            oldSection.sectionNumber = 3; // entity types section
                    }

                    if (oldSection.sectionNumber > 0) {
                        for (final KeySectionInfo tempSection : keySections_New.values()){
                            if (tempSection.sectionNumber == oldSection.sectionNumber && tempSection.sectionStartingLine > 0){
                                insertAt = tempSection.sectionStartingLine;
                            }
                        }
                    }

                    newConfigLines.add(insertAt, key);
                    // for (int i = oldSection.lines.size() - 1; i >= 0; i--) {
                    for (int i = 0; i < oldSection.lines.size(); i++) {
                        insertAt++;
                        newConfigLines.add(insertAt, oldSection.lines.get(i));
                    }
                    newConfigLines.add(insertAt + 1, "");

                    keySections_New = buildKeySections(newConfigLines);
                }
            }

            // build an index so we can modify the collection as we enumerate thru it
            Files.write(to.toPath(), newConfigLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            Utils.logger.info("&fFile Loader: &8(Migration) &7Migrated &b" + to.getName() + "&7 successfully.");
        } catch (final IOException e) {
            Utils.logger.error("&fFile Loader: &8(Migration) &7Failed to migrate &b" + to.getName() + "&7! Stack trace:");
            e.printStackTrace();
        }
    }

    private static boolean doSectionsContainSameLines(@NotNull final KeySectionInfo section1, @NotNull final KeySectionInfo section2){
        if (section1.lines.size() != section2.lines.size()) return false;

        for (int i = 0; i < section1.lines.size(); i++){
            if (!section1.lines.get(i).equals(section2.lines.get(i))) return false;
        }

        return true;
    }

    @NotNull
    private static TreeMap<String, KeySectionInfo> buildKeySections(@NotNull final List<String> contents){

        final TreeMap<String, KeySectionInfo> sections = new TreeMap<>();
        KeySectionInfo keySection = null;
        String keyName = null;
        int sectionNumber = 0;
        int sectionStartingLine = 0;
        boolean foundNonComment = false;

        for (int i = 0; i < contents.size(); i++){
            final String origline = contents.get(i);

            final int depth = getFieldDepth(origline);
            final String line = origline.replace("\t", "").trim();

            if (line.startsWith("# ||  Section")){
                final int foundSectionNumber = extractSectionNumber(line);
                if (foundSectionNumber > 0) sectionNumber = foundSectionNumber;
                sectionStartingLine = 0;
            }

            if (sectionStartingLine == 0 && sectionNumber > 0 && line.startsWith("# ||||")) {
                sectionStartingLine = i + 2;
                foundNonComment = false;
            }

            if (line.startsWith("#") || line.isEmpty()) continue;

            if (!foundNonComment){
                if (sectionStartingLine > 0) sectionStartingLine = i;
                foundNonComment = true;
            }

            if (depth == 0) {
                if (keySection != null)
                    sections.put(keyName, keySection);

                keySection = new KeySectionInfo();
                keySection.lineNumber = i;
                keySection.sectionNumber = sectionNumber;
                keySection.sectionStartingLine = sectionStartingLine;
                keyName = line;
            } else if (keySection != null) {
                keySection.lines.add(origline);
            }
        }

        if (keySection != null)
            sections.put(keyName, keySection);

        return sections;
    }

    private static int extractSectionNumber(final String input){
        final Pattern p = Pattern.compile("# \\|\\|\\s{2}Section (\\d{2})");
        final Matcher m = p.matcher(input);
        if (m.find() && m.groupCount() == 1){
            String temp = m.group(1);
            if (temp.length() > 1 && temp.charAt(0) == '0')
                temp = temp.substring(1);

            if (Utils.isInteger(temp))
                return Integer.parseInt(temp);
        }

        return 0;
    }

    static void migrateRules(@NotNull final File to) {
        try {
            final List<String> newConfigLines = Files.readAllLines(to.toPath(), StandardCharsets.UTF_8);
            boolean hasVisibleTime = false;
            for (final String line : newConfigLines){
                if (line.toLowerCase().contains("nametag-visible-time")){
                    hasVisibleTime = true;
                    break;
                }
            }

            for (int i = 0; i < newConfigLines.size(); i++){
                final String line = newConfigLines.get(i);
                if (line.trim().startsWith("#")) continue;

                final int startOfText = line.toLowerCase().indexOf("creature-nametag-always-visible:");
                if (startOfText > 0){
                    String newline = line.substring(0, startOfText) + "nametag-visibility-method: ['TARGETED', 'ATTACKED', 'TRACKING']";
                    newConfigLines.set(i, newline);
                    if (!hasVisibleTime){
                        newline = line.substring(0, startOfText) + "nametag-visible-time: 1000";
                        newConfigLines.add(i, newline);
                        i++;
                    }

                }

                if (line.startsWith("file-version"))
                    newConfigLines.set(i, "file-version: 2");
            }

            Files.write(to.toPath(), newConfigLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (final IOException e){
            e.printStackTrace();
        }
    }

    static void copyYmlValues(final File from, @NotNull final File to, final int oldVersion) {

        final String regexPattern = "^[^':]*:.*";
        final boolean isSettings = to.getName().equalsIgnoreCase("settings.yml");
        final boolean isCustomDrops = to.getName().equalsIgnoreCase("customdrops.yml");
        final boolean isMessages = to.getName().equalsIgnoreCase("messages.yml");
        final boolean showMessages = !isMessages;
        final List<String> processedKeys = new LinkedList<>();

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
                "fine-tuning.additions.attack-damage",
                "world-level-override.min-level.example_world_123",
                "world-level-override.max-level.example_world_123",
                "world-level-override.max-level.example_world_456"
        );

        // version 2.2.0 - these fields should be reset to default
        final List<String> version26Resets = Arrays.asList(
                "world-level-override.min-level.example_world_123",
                "world-level-override.max-level.example_world_123",
                "world-level-override.max-level.example_world_456"
        );

        final List<String> messagesExempt_v5 = Arrays.asList(
                "command.levelledmobs.spawner.usage",
                "command.levelledmobs.spawner.spawner-give-message"
        );


        final String useCustomDrops = "use-custom-item-drops-for-mobs";

        try {
            final List<String> oldConfigLines = Files.readAllLines(from.toPath(), StandardCharsets.UTF_8);
            final List<String> newConfigLines = Files.readAllLines(to.toPath(), StandardCharsets.UTF_8);

            final SortedMap<String, FileMigrator.FieldInfo> oldConfigMap = getMapFromConfig(oldConfigLines);
            final SortedMap<String, FileMigrator.FieldInfo> newConfigMap = getMapFromConfig(newConfigLines);
            final List<String> currentKey = new LinkedList<>();
            int keysMatched = 0;
            int valuesUpdated = 0;
            int valuesMatched = 0;

            if (!isCustomDrops) {
                for (int currentLine = 0; currentLine < newConfigLines.size(); currentLine++) {
                    String line = newConfigLines.get(currentLine);
                    final int depth = getFieldDepth(line);
                    if (line.trim().startsWith("#") || line.trim().isEmpty()) continue;

                    if (line.matches(regexPattern)) {
                        final int firstColon = line.indexOf(':');
                        final boolean hasValues = line.length() > firstColon + 1;
                        String key = line.substring(0, firstColon).replace("\t", "").trim();
                        final String keyOnly = key;
                        String oldKey = key;
                        if (isSettings && oldVersion < 32 && key.equalsIgnoreCase("async-task-update-period"))
                            oldKey = "nametag-auto-update-task-period";

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
                            if (isMessages && oldVersion <= 5 && messagesExempt_v5.contains(key)) continue;

                            if (oldConfigMap.containsKey(oldKey) && newConfigMap.containsKey(key)) {
                                final FileMigrator.FieldInfo fiOld = oldConfigMap.get(oldKey);
                                final FileMigrator.FieldInfo fiNew = newConfigMap.get(key);
                                final String padding = getPadding((depth + 1) * 2);
                                // arrays go here:
                                if (fiOld.isList()) {
                                    // add any values present in old list that might not be present in new
                                    for (final String oldValue : fiOld.valueList) {
                                        if (!fiNew.isList() || !fiNew.valueList.contains(oldValue)) {
                                            final String newline = padding + "- " + oldValue; // + "\r\n" + line;
                                            newConfigLines.add(currentLine + 1, newline);
                                            if (showMessages)
                                                Utils.logger.info("&fFile Loader: &8(Migration) &7Added array value: &b" + oldValue);
                                        }
                                    }
                                } else {
                                    // non-array values go here.  Loop thru and find any subkeys under here
                                    final int numOfPeriods = countPeriods(key);
                                    for (final Map.Entry<String, FieldInfo> entry : oldConfigMap.entrySet()) {
                                        final String enumeratedKey = entry.getKey();
                                        if (isSettings && oldVersion > 20 && oldVersion <= 24 && version24Resets.contains(enumeratedKey))
                                            continue;
                                        if (isSettings && oldVersion > 24 && oldVersion <= 26 && version26Resets.contains(enumeratedKey))
                                            continue;

                                        final int numOfPeriods_Enumerated = countPeriods(enumeratedKey);
                                        if (enumeratedKey.startsWith(key) && numOfPeriods_Enumerated == numOfPeriods + 1 && !newConfigMap.containsKey(enumeratedKey)) {
                                            final FileMigrator.FieldInfo fi = entry.getValue();
                                            if (!fi.isList() && fi.simpleValue != null) {
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
                        } else if (oldConfigMap.containsKey(oldKey)) {
                            keysMatched++;
                            final String value = line.substring(firstColon + 1).trim();
                            final FileMigrator.FieldInfo fi = oldConfigMap.get(oldKey);
                            final String migratedValue = fi.simpleValue;

                            if (isSettings && oldVersion <= 20 && !version20KeysToKeep.contains(key)) continue;
                            if (isSettings && oldVersion > 20 && oldVersion <= 24 && version24Resets.contains(key))
                                continue;
                            if (isSettings && oldVersion > 24 && oldVersion <= 26 && version26Resets.contains(key))
                                continue;
                            if (isMessages && oldVersion <= 5 && messagesExempt_v5.contains(key)) continue;
                            if (key.toLowerCase().startsWith("file-version")) continue;
                            if (isSettings && key.equalsIgnoreCase("creature-nametag") && oldVersion > 20 && oldVersion < 26
                                    && migratedValue.equals("'&8[&7Level %level%&8 | &f%displayname%&8 | &c%health%&8/&c%max_health% %heart_symbol%&8]'")) {
                                // updating to the new default introduced in file ver 26 if they were using the previous default
                                continue;
                            }
                            if (isSettings && oldVersion < 28 && key.equalsIgnoreCase(useCustomDrops) &&
                                    oldConfigMap.containsKey(useCustomDrops) &&
                                    oldConfigMap.get(useCustomDrops).simpleValue.equalsIgnoreCase("true")) {

                                if (showMessages)
                                    Utils.logger.info("&fFile Loader: &8(Migration) &7Current key: &b" + key + "&7, resetting to: &rfalse&7.");
                                newConfigMap.get(useCustomDrops).simpleValue = "false";
                                valuesUpdated++;
                                continue;
                            }

                            final String parentKey = getParentKey(key);
                            if (fi.hasValue && parentKey != null && !processedKeys.contains(parentKey)){
                                // here's where we add values from the old config not present in the new
                                for (final Map.Entry<String, FieldInfo> entry : oldConfigMap.entrySet()){
                                    final String oldValue = entry.getKey();
                                    if (!oldValue.startsWith(parentKey)) continue;
                                    if (newConfigMap.containsKey(oldValue)) continue;
                                    if (!isEntitySameSubkey(parentKey, oldValue)) continue;
                                    if (isSettings && oldVersion > 20 && oldVersion <= 24 && version24Resets.contains(oldValue)) continue;
                                    if (isSettings && oldVersion > 24 && oldVersion <= 26 && version26Resets.contains(oldValue)) continue;
                                    if (isMessages && oldVersion <= 5 && messagesExempt_v5.contains(key)) continue;

                                    final FileMigrator.FieldInfo fiOld = entry.getValue();
                                    if (fiOld.isList()) continue;
                                    final String padding = getPadding(depth * 2);
                                    final String newline = padding + getEndingKey(oldValue) + ": " + fiOld.simpleValue;
                                    newConfigLines.add(currentLine + 1, newline);
                                    if (showMessages) Utils.logger.info("&fFile Loader: &8(Migration) &7Adding key: &b" + oldValue + "&7, value: &r" + fiOld.simpleValue + "&7.");
                                }
                                processedKeys.add(parentKey);
                            }

                            if (!value.equals(migratedValue)) {
                                if (migratedValue != null) {
                                    valuesUpdated++;
                                    if (showMessages)
                                        Utils.logger.info("&fFile Loader: &8(Migration) &7Current key: &b" + key + "&7, replacing: &r" + value + "&7, with: &r" + migratedValue + "&7.");
                                    line = line.replace(value, migratedValue);
                                    newConfigLines.set(currentLine, line);
                                }
                            } else
                                valuesMatched++;
                        }
                    } else if (line.trim().startsWith("-")) {
                        final String key = getKeyFromList(currentKey, null);
                        final String value = line.trim().substring(1).trim();

                        if (isMessages && oldVersion <= 5 && messagesExempt_v5.contains(key)) continue;

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
                    final String line = newConfigLines.get(i).trim();

                    if (line.toLowerCase().startsWith("file-version")){
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
        } catch (final Exception e) {
            Utils.logger.error("&fFile Loader: &8(Migration) &7Failed to migrate &b" + to.getName() + "&7! Stack trace:");
            e.printStackTrace();
        }
    }

    private static int countPeriods(@NotNull final String text){
        int count = 0;

        for (int i = 0; i < text.length(); i++){
            if (text.charAt(i) == '.') count++;
        }

        return count;
    }

    @NotNull
    private static String getPadding(final int space){
        return " ".repeat(space);
    }

    private static boolean isEntitySameSubkey(@NotNull final String key1, @NotNull final String key2){
        final int lastPeriod = key2.lastIndexOf('.');
        final String checkKey = lastPeriod > 0 ? key2.substring(0, lastPeriod) : key2;

        return (key1.equalsIgnoreCase(checkKey));
    }

    @NotNull
    private static String getEndingKey(@NotNull final String input){
        final int lastPeriod = input.lastIndexOf('.');
        if (lastPeriod < 0) return input;

        return input.substring(lastPeriod + 1);
    }

    @Nullable
    private static String getParentKey(@NotNull final String input){
        final int lastPeriod = input.lastIndexOf('.');
        if (lastPeriod < 0) return null;

        return input.substring(0, lastPeriod);
    }

    private static int getFirstNonCommentLine(@NotNull final List<String> input){
        for (int lineNum = 0; lineNum < input.size(); lineNum++) {
            final String line = input.get(lineNum).replace("\t", "").trim();
            if (line.startsWith("#") || line.isEmpty()) continue;
            return lineNum;
        }

        return -1;
    }

    @Nonnull
    private static SortedMap<String, FileMigrator.FieldInfo> getMapFromConfig(@NotNull final List<String> input) {
        final SortedMap<String, FileMigrator.FieldInfo> configMap = new TreeMap<>();
        final List<String> currentKey = new LinkedList<>();
        final String regexPattern = "^[^':]*:.*";

        int lineNum = -1;
        for (String line : input) {
            lineNum++;

            final int depth = getFieldDepth(line);
            line = line.replace("\t", "").trim();
            if (line.startsWith("#") || line.isEmpty()) continue;

            //if (line.contains(":")) {
            if (line.matches(regexPattern)) {
                final int firstColon = line.indexOf(':');
                final boolean hasValues = line.length() > firstColon + 1;
                String key = line.substring(0, firstColon).replace("\t", "").trim();
                final String origKey = key;

                if (origKey.startsWith("-")) {
                    if (currentKey.size() > depth)
                        while (currentKey.size() > depth) currentKey.remove(currentKey.size() - 1);
                    final String temp = origKey.substring(1).trim();
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
                    final FileMigrator.FieldInfo fi = configMap.get(key);
                    fi.addListValue(value);
                } else
                    configMap.put(key, new FileMigrator.FieldInfo(value, depth, true));
            }
        }

        return configMap;
    }
}
