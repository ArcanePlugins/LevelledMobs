package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.util.Log
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.LinkedList
import java.util.Locale
import java.util.SortedMap
import java.util.TreeMap
import java.util.regex.Pattern
import io.github.arcaneplugins.levelledmobs.util.Utils
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

/**
 * Migrates older yml versions to the latest available
 *
 * @author stumper66
 * @since 2.4.0
 */
object FileMigrator {
    private fun getFieldDepth(
        line: String
    ): Int {
        var whiteSpace = 0

        for (element in line) {
            if (element != ' ') {
                break
            }
            whiteSpace++
        }
        return if (whiteSpace == 0) 0 else whiteSpace / 2
    }

    private class FieldInfo {
        var simpleValue: String? = null
        var valueList: MutableList<String?>? = null
        val depth: Int
        var hasValue: Boolean = false

        constructor(value: String?, depth: Int) {
            this.simpleValue = value
            this.depth = depth
        }

        constructor(value: String?, depth: Int, isListValue: Boolean) {
            if (isListValue) {
                addListValue(value)
            } else {
                this.simpleValue = value
            }
            this.depth = depth
        }

        val isList: Boolean
            get() = valueList != null

        fun addListValue(value: String?) {
            if (valueList == null) {
                valueList = LinkedList()
            }
            valueList!!.add(value)
        }

        override fun toString(): String {
            if (this.isList) {
                return if (this.valueList == null || valueList!!.isEmpty()) {
                    super.toString()
                } else {
                    this.valueList!!.joinToString(",")
                }
            }

            return if (this.simpleValue == null) {
                super.toString()
            } else {
                simpleValue!!
            }
        }
    }

    private class KeySectionInfo {
        var lineNumber: Int = 0
        val lines = mutableListOf<String>()
        var sectionNumber: Int = 0
        var sectionStartingLine: Int = 0
    }

    private fun getKeyFromList(
        list: MutableList<String>,
        currentKey: String?
    ): String? {
        if (list.isEmpty()) {
            return currentKey
        }

        var result = list.joinToString(".")
        if (currentKey != null) {
            result += ".$currentKey"
        }

        return result
    }

    private fun compileListFromArray(list: List<String>): String {
        val sb = StringBuilder()
        sb.append("[")
        for (item in list) {
            if (sb.length > 1) {
                sb.append(", ")
            }
            sb.append("'")
            sb.append(item)
            sb.append("'")
        }
        sb.append("]")

        return sb.toString()
    }

    fun copyCustomDrops(
        from: File, to: File,
        fileVersion: Int
    ) {
        val keysectionsOld: Map<String, KeySectionInfo>
        var keysectionsNew: Map<String, KeySectionInfo>

        try {
            val oldConfigLines = Files.readAllLines(
                from.toPath(),
                StandardCharsets.UTF_8
            )
            val newConfigLines = Files.readAllLines(
                to.toPath(),
                StandardCharsets.UTF_8
            )

            keysectionsOld = buildKeySections(oldConfigLines)
            keysectionsNew = buildKeySections(newConfigLines)

            for ((key, oldSection) in keysectionsOld) {
                if (key.lowercase(Locale.getDefault()).startsWith("file-version")) {
                    continue
                }

                if (keysectionsNew.containsKey(key)) {
                    // overwrite new section if different
                    val newSection = keysectionsNew[key]!!

                    if (!doSectionsContainSameLines(oldSection, newSection)) {
                        for (i in newSection.lines.indices) {
                            newConfigLines.removeAt(newSection.lineNumber + 1)
                        }

                        for (i in oldSection.lines.indices.reversed()) {
                            newConfigLines.add(newSection.lineNumber + 1, oldSection.lines[i])
                        }

                        keysectionsNew = buildKeySections(newConfigLines)
                    }
                } else {
                    // write the section into the new config, starting in corresponding new section
                    var insertAt = newConfigLines.size
                    if (fileVersion < 6) {
                        if (key.uppercase(Locale.getDefault()).startsWith("ALL_")) {
                            oldSection.sectionNumber = 2 // universal groups section
                        } else {
                            oldSection.sectionNumber = 3 // entity types section
                        }
                    }

                    if (oldSection.sectionNumber > 0) {
                        for (tempSection in keysectionsNew.values) {
                            if (tempSection.sectionNumber == oldSection.sectionNumber
                                && tempSection.sectionStartingLine > 0
                            ) {
                                insertAt = tempSection.sectionStartingLine
                            }
                        }
                    }

                    newConfigLines.add(insertAt, key)
                    // for (int i = oldSection.lines.size() - 1; i >= 0; i--) {
                    for (i in oldSection.lines.indices) {
                        insertAt++
                        newConfigLines.add(insertAt, oldSection.lines[i])
                    }
                    newConfigLines.add(insertAt + 1, "")

                    keysectionsNew = buildKeySections(newConfigLines)
                }
            }

            // build an index so we can modify the collection as we enumerate thru it
            Files.write(
                to.toPath(), newConfigLines, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            Log.inf(
                "&fFile Loader: &8(Migration) &7Migrated &b${to.name}&7 successfully."
            )
        } catch (e: IOException) {
            Log.sev(
                "&fFile Loader: &8(Migration) &7Failed to migrate &b" + to.name
                        + "&7! Stack trace:"
            )
            e.printStackTrace()
        }
    }

    private fun doSectionsContainSameLines(
        section1: KeySectionInfo,
        section2: KeySectionInfo
    ): Boolean {
        if (section1.lines.size != section2.lines.size) {
            return false
        }

        for (i in section1.lines.indices) {
            if (section1.lines[i] != section2.lines[i]) {
                return false
            }
        }

        return true
    }

    private fun buildKeySections(
        contents: MutableList<String>
    ): Map<String, KeySectionInfo> {
        val sections = mutableMapOf<String, KeySectionInfo>()
        var keySection: KeySectionInfo? = null
        var keyName: String? = null
        var sectionNumber = 0
        var sectionStartingLine = 0
        var foundNonComment = false

        for (i in contents.indices) {
            val origline = contents[i]

            val depth = getFieldDepth(origline)
            val line = origline.replace("\t", "").trim { it <= ' ' }

            if (line.startsWith("# ||  Section")) {
                val foundSectionNumber: Int = extractSectionNumber(line)
                if (foundSectionNumber > 0) {
                    sectionNumber = foundSectionNumber
                }
                sectionStartingLine = 0
            }

            if (sectionStartingLine == 0 && sectionNumber > 0 && line.startsWith("# ||||")) {
                sectionStartingLine = i + 2
                foundNonComment = false
            }

            if (line.startsWith("#") || line.isEmpty()) {
                continue
            }

            if (!foundNonComment) {
                if (sectionStartingLine > 0) {
                    sectionStartingLine = i
                }
                foundNonComment = true
            }

            if (depth == 0) {
                if (keySection != null) {
                    sections[keyName!!] = keySection
                }

                keySection = KeySectionInfo()
                keySection.lineNumber = i
                keySection.sectionNumber = sectionNumber
                keySection.sectionStartingLine = sectionStartingLine
                keyName = line
            } else keySection?.lines?.add(origline)
        }

        if (keySection != null) {
            sections[keyName!!] = keySection
        }

        return sections
    }

    private fun extractSectionNumber(input: String): Int {
        val p = Pattern.compile("# \\|\\|\\s{2}Section (\\d{2})")
        val m = p.matcher(input)
        if (m.find() && m.groupCount() == 1) {
            var temp = m.group(1)
            if (temp.length > 1 && temp[0] == '0') {
                temp = temp.substring(1)
            }

            if (Utils.isInteger(temp)) {
                return temp.toInt()
            }
        }

        return 0
    }

    fun migrateRules(to: File) {
        try {
            val newConfigLines = Files.readAllLines(
                to.toPath(),
                StandardCharsets.UTF_8
            )

            val enabledCompats = mutableListOf<String>()
            val rulesCfg = YamlConfiguration.loadConfiguration(to)
            val cs = rulesCfg.get("default-rule.conditions.level-plugins") as? ConfigurationSection
            if (cs != null){
                for (key in cs.getKeys(false)){
                    val foundValue = cs.getString(key)
                    if ("true".equals(foundValue, ignoreCase = true)){
                        enabledCompats.add(key.toString().lowercase().replace("_", "-"))
                    }
                }
            }
            else
                Log.war("cs was null")

            val newText = mutableListOf(
                "",
                "  - enabled: true",
                "    name: 'No Stat Change for External Plugins'",
                "    use-preset: vanilla_challenge, nametag_no_level",
                "    conditions:",
                "      external-plugins: '*'"
            )

            var foundLine = -1
            var madeUpdate = false
            var i = -1
            while (true) {
                i++
                if (i >= newConfigLines.size) break
                val line = newConfigLines[i]
                if (line.trim { it <= ' ' }.startsWith("#")) {
                    continue
                }

                var lineText = line.indexOf("No Stat Change for Specific Entities", ignoreCase = true)
                if (lineText <= 0){
                    lineText = line.indexOf("NoLevel All Passive + EntityTypes", ignoreCase = true)
                }

                if (lineText > 0){
                    foundLine = i
                }
                else if (!madeUpdate && foundLine > -1){
                    if (!line.contains("- enabled: true")) continue

                    i -= 1
                    for (newTextLine in newText){
                        var useNewTextLine = newTextLine
                        var hadExternalCompats = false
                        if (enabledCompats.isNotEmpty() && useNewTextLine.contains("external-plugins:")){
                            var isFirst = true
                            hadExternalCompats = true
                            val sb = StringBuilder((useNewTextLine.replace(" '*'",
                                "\n        excluded-list: [")))
                            for (compat in enabledCompats){
                                if (isFirst)
                                    isFirst = false
                                else
                                    sb.append(", ")

                                sb.append("'").append(compat).append("'")
                            }
                            sb.append("]")
                            useNewTextLine = sb.toString()
                        }

                        newConfigLines.add(i, useNewTextLine)
                        i++
                        if (hadExternalCompats) i++
                    }

                    madeUpdate = true
                }

                if (line.startsWith("file-version")) {
                    newConfigLines[i] = "file-version: 5"
                }
            }

            Files.write(
                to.toPath(), newConfigLines, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun copyYmlValues(from: File, to: File, oldVersion: Int) {
        val regexPattern = "^[^':]*:.*"
        val isSettings = to.name.equals("settings.yml", ignoreCase = true)
        val isCustomDrops = to.name.equals("customdrops.yml", ignoreCase = true)
        val isMessages = to.name.equals("messages.yml", ignoreCase = true)
        val showMessages = !isMessages
        val processedKeys: MutableList<String?> = LinkedList()

        // version 20 = 1.34 - last version before 2.0
        val version20KeysToKeep: List<String?> = listOf(
            "level-passive",
            "fine-tuning.min-level",
            "fine-tuning.max-level",
            "spawn-distance-levelling.active",
            "spawn-distance-levelling.variance.enabled",
            "spawn-distance-levelling.variance.max",
            "spawn-distance-levelling.variance.min",
            "spawn-distance-levelling.increase-level-distance",
            "spawn-distance-levelling.start-distance",
            "use-update-checker"
        )

        // version 2.1.0 - these fields should be reset to default
        val version24Resets: List<String?> = listOf(
            "fine-tuning.additions.movement-speed",
            "fine-tuning.additions.attack-damage",
            "world-level-override.min-level.example_world_123",
            "world-level-override.max-level.example_world_123",
            "world-level-override.max-level.example_world_456"
        )

        // version 2.2.0 - these fields should be reset to default
        val version26Resets: List<String?> = listOf(
            "world-level-override.min-level.example_world_123",
            "world-level-override.max-level.example_world_123",
            "world-level-override.max-level.example_world_456"
        )

        val messagesexemptV5: List<String?> = listOf(
            "command.levelledmobs.spawner.usage",
            "command.levelledmobs.spawner.spawner-give-message"
        )

        val messagesexemptV7: List<String?> = listOf(
            "command.levelledmobs.rules.reset"
        )

        val useCustomDrops = "use-custom-item-drops-for-mobs"

        try {
            val oldConfigLines = Files.readAllLines(
                from.toPath(),
                StandardCharsets.UTF_8
            )
            val newConfigLines = Files.readAllLines(
                to.toPath(),
                StandardCharsets.UTF_8
            )

            val oldConfigMap = getMapFromConfig(oldConfigLines)
            val newConfigMap = getMapFromConfig(
                newConfigLines
            )
            val currentKey = mutableListOf<String>()
            var keysMatched = 0
            var valuesUpdated = 0
            var valuesMatched = 0

            if (!isCustomDrops) {
                var currentLine = 0
                while (currentLine < newConfigLines.size) {
                    var line = newConfigLines[currentLine]
                    val depth = getFieldDepth(line)
                    if (line.trim { it <= ' ' }.startsWith("#") || line.trim { it <= ' ' }.isEmpty()) {
                        currentLine++
                        continue
                    }

                    if (line.matches(regexPattern.toRegex())) {
                        val firstColon = line.indexOf(':')
                        val hasValues = line.length > firstColon + 1
                        var key = line.substring(0, firstColon).replace("\t", "").trim { it <= ' ' }
                        val keyOnly = key
                        var oldKey = key
                        if (isSettings && oldVersion < 32 && key.equals(
                                "async-task-update-period", ignoreCase = true
                            )
                        ) {
                            oldKey = "nametag-auto-update-task-period"
                        }

                        if (depth == 0) {
                            currentKey.clear()
                        } else if (currentKey.size > depth) {
                            while (currentKey.size > depth) {
                                currentKey.removeAt(currentKey.size - 1)
                            }
                            key = getKeyFromList(currentKey, key)!!
                        } else {
                            key = getKeyFromList(currentKey, key)!!
                        }

                        if (!hasValues) {
                            currentKey.add(keyOnly)

                            if (isSettings && oldVersion <= 20 && !version20KeysToKeep.contains(
                                    key
                                )
                            ) {
                                currentLine++
                                continue
                            }
                            if (isMessages && oldVersion <= 5 && messagesexemptV5.contains(key)) {
                                currentLine++
                                continue
                            }
                            if (isMessages && oldVersion <= 7 && messagesexemptV7.contains(key)) {
                                currentLine++
                                continue
                            }

                            if (oldConfigMap.containsKey(oldKey) && newConfigMap.containsKey(key)) {
                                val fiOld = oldConfigMap[oldKey]
                                val fiNew = newConfigMap[key]
                                val padding: String = getPadding((depth + 1) * 2)
                                // arrays go here:
                                if (fiOld!!.isList) {
                                    // add any values present in old list that might not be present in new
                                    for (oldValue: String? in fiOld.valueList!!) {
                                        if (!fiNew!!.isList || !fiNew.valueList!!.contains(
                                                oldValue
                                            )
                                        ) {
                                            val newline =
                                                "$padding- $oldValue" // + "\r\n" + line;
                                            newConfigLines.add(currentLine + 1, newline)
                                            if (showMessages) {
                                                Log.inf(
                                                    "&fFile Loader: &8(Migration) &7Added array value: &b"
                                                            + oldValue
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // non-array values go here.  Loop thru and find any subkeys under here
                                    val numOfPeriods: Int = countPeriods(key)
                                    for (entry in oldConfigMap.entries) {
                                        val enumeratedKey = entry.key
                                        if (isSettings && (oldVersion > 20) && (oldVersion <= 24
                                                    ) && version24Resets.contains(enumeratedKey)
                                        ) {
                                            continue
                                        }
                                        if (isSettings && (oldVersion > 24) && (oldVersion <= 26
                                                    ) && version26Resets.contains(enumeratedKey)
                                        ) {
                                            continue
                                        }

                                        val numofperiodsEnumerated: Int = countPeriods(
                                            enumeratedKey!!
                                        )
                                        if ((enumeratedKey.startsWith((key))
                                                    && (numofperiodsEnumerated == numOfPeriods + 1
                                                    ) && !newConfigMap.containsKey(enumeratedKey))
                                        ) {
                                            val fi = entry.value
                                            if (!fi!!.isList && fi.simpleValue != null) {
                                                val newline =
                                                    (padding + getEndingKey(enumeratedKey) + ": "
                                                            + fi.simpleValue)
                                                newConfigLines.add(currentLine + 1, newline)
                                                if (showMessages) {
                                                    Log.inf(
                                                        ("&fFile Loader: &8(Migration) &7Adding key: &b"
                                                                + enumeratedKey + "&7, value: &r"
                                                                + fi.simpleValue + "&7.")
                                                    )
                                                }
                                                processedKeys.add(key)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (oldConfigMap.containsKey(oldKey)) {
                            keysMatched++
                            val value = line.substring(firstColon + 1).trim { it <= ' ' }
                            val fi = oldConfigMap[oldKey]
                            val migratedValue = fi!!.simpleValue

                            if (isSettings && (oldVersion <= 20) && !version20KeysToKeep.contains(
                                    key
                                )
                            ) {
                                currentLine++
                                continue
                            }
                            if (isSettings && (oldVersion > 20) && (oldVersion <= 24
                                        ) && version24Resets.contains(key)
                            ) {
                                currentLine++
                                continue
                            }
                            if (isSettings && (oldVersion > 24) && (oldVersion <= 26
                                        ) && version26Resets.contains(key)
                            ) {
                                currentLine++
                                continue
                            }
                            if (isMessages && (oldVersion <= 5) && messagesexemptV5.contains(key)) {
                                currentLine++
                                continue
                            }
                            if (isMessages && (oldVersion <= 7) && messagesexemptV7.contains(key)) {
                                currentLine++
                                continue
                            }
                            if (key.lowercase(Locale.getDefault()).startsWith("file-version")) {
                                currentLine++
                                continue
                            }
                            if ((isSettings && key.equals("creature-nametag", ignoreCase = true)
                                        && (oldVersion > 20) && (oldVersion < 26
                                        ) && (migratedValue == "'&8[&7Level %level%&8 | &f%displayname%&8 | &c%health%&8/&c%max_health% %heart_symbol%&8]'"))
                            ) {
                                // updating to the new default introduced in file ver 26 if they were using the previous default
                                currentLine++
                                continue
                            }
                            if ((isSettings && (oldVersion < 28) && key.equals(
                                    useCustomDrops, ignoreCase = true
                                ) &&
                                        oldConfigMap.containsKey(useCustomDrops) &&
                                        oldConfigMap[useCustomDrops]!!.simpleValue.equals(
                                            "true", ignoreCase = true
                                        ))
                            ) {
                                if (showMessages) {
                                    Log.inf(
                                        ("&fFile Loader: &8(Migration) &7Current key: &b" + key
                                                + "&7, resetting to: &rfalse&7.")
                                    )
                                }
                                newConfigMap[useCustomDrops]!!.simpleValue = "false"
                                valuesUpdated++
                                currentLine++
                                continue
                            }

                            val parentKey = getParentKey(key)
                            if (fi.hasValue && (parentKey != null) && !processedKeys.contains(
                                    parentKey
                                )
                            ) {
                                // here's where we add values from the old config not present in the new
                                for (entry in oldConfigMap.entries) {
                                    val oldValue = entry.key
                                    if (!oldValue!!.startsWith(parentKey)) {
                                        continue
                                    }
                                    if (newConfigMap.containsKey(oldValue)) {
                                        continue
                                    }
                                    if (!isEntitySameSubkey(parentKey, oldValue)) {
                                        continue
                                    }
                                    if (isSettings && (oldVersion > 20) && (oldVersion <= 24
                                                ) && version24Resets.contains(oldValue)
                                    ) {
                                        continue
                                    }
                                    if (isSettings && (oldVersion > 24) && (oldVersion <= 26
                                                ) && version26Resets.contains(oldValue)
                                    ) {
                                        continue
                                    }
                                    if (isMessages && (oldVersion <= 5) && messagesexemptV5.contains(
                                            key
                                        )
                                    ) {
                                        continue
                                    }
                                    if (isMessages && (oldVersion <= 7) && messagesexemptV7.contains(
                                            key
                                        )
                                    ) {
                                        continue
                                    }

                                    val fiOld = entry.value
                                    if (fiOld!!.isList) {
                                        continue
                                    }
                                    val padding: String = getPadding(depth * 2)
                                    val newline =
                                        padding + getEndingKey(oldValue) + ": " + fiOld.simpleValue
                                    newConfigLines.add(currentLine + 1, newline)
                                    if (showMessages) {
                                        Log.inf(
                                            ("&fFile Loader: &8(Migration) &7Adding key: &b"
                                                    + "$oldValue&7, value: &r" + fiOld.simpleValue
                                                    + "&7.")
                                        )
                                    }
                                }
                                processedKeys.add(parentKey)
                            }

                            if (value != migratedValue) {
                                if (migratedValue != null) {
                                    valuesUpdated++
                                    if (showMessages) {
                                        Log.inf(
                                            ("&fFile Loader: &8(Migration) &7Current key: &b$key"
                                                    + "&7, replacing: &r$value&7, with: &r"
                                                    + "$migratedValue&7.")
                                        )
                                    }
                                    line = line.replace(value, migratedValue)
                                    newConfigLines[currentLine] = line
                                }
                            } else {
                                valuesMatched++
                            }
                        }
                    } else if (line.trim { it <= ' ' }.startsWith("-")) {
                        val key = getKeyFromList(currentKey, null)
                        val value = line.trim { it <= ' ' }.substring(1).trim { it <= ' ' }

                        if (isMessages && (oldVersion <= 5) && messagesexemptV5.contains(key)) {
                            currentLine++
                            continue
                        }
                        if (isMessages && (oldVersion <= 7) && messagesexemptV7.contains(key)) {
                            currentLine++
                            continue
                        }

                        // we have an array value present in the new config but not the old, so it must've been removed
                        if ((oldConfigMap.containsKey(key) && oldConfigMap[key]!!.isList
                                    && !oldConfigMap[key]!!.valueList!!.contains(value))
                        ) {
                            newConfigLines.removeAt(currentLine)
                            currentLine--
                            if (showMessages) {
                                Log.inf(
                                    ("&fFile Loader: &8(Migration) &7Current key: &b" + key
                                            + "&7, removing value: &r$value&7.")
                                )
                            }
                        }
                    }
                    currentLine++
                }
            } // end if is not custom drops
            else {
                // migrate all values
                var startAt = 0

                for (i in newConfigLines.indices) {
                    val line = newConfigLines[i].trim { it <= ' ' }

                    if (line.lowercase(Locale.getDefault()).startsWith("file-version")) {
                        startAt = i + 1
                        break
                    }
                }

                while (newConfigLines.size > startAt + 1) {
                    newConfigLines.removeAt(newConfigLines.size - 1)
                }

                val firstNonCommentLine: Int = getFirstNonCommentLine(oldConfigLines)

                for (i in firstNonCommentLine until oldConfigLines.size) {
                    var temp = oldConfigLines[i].replace("\\s+$".toRegex(), "") // trimEnd()
                    if (temp.endsWith("nomultiplier:") || temp.endsWith("nospawner:")) {
                        temp += " true"
                        newConfigLines.add(temp)
                    } else {
                        newConfigLines.add(oldConfigLines[i])
                    }
                }
            }

            Files.write(
                to.toPath(), newConfigLines, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            Log.inf(
                "&fFile Loader: &8(Migration) &7Migrated &b${to.name}&7 successfully."
            )
            Log.inf(
                String.format(
                    "&fFile Loader: &8(Migration) &7Keys matched: &b%s&7, values matched: &b%s&7, values updated: &b%s&7.",
                    keysMatched, valuesMatched, valuesUpdated
                )
            )
        } catch (e: Exception) {
            Log.sev(
                ("&fFile Loader: &8(Migration) &7Failed to migrate &b${to.name}"
                        + "&7! Stack trace:")
            )
            e.printStackTrace()
        }
    }

    private fun countPeriods(text: String): Int {
        var count = 0

        for (element in text) {
            if (element == '.') {
                count++
            }
        }

        return count
    }

    private fun getPadding(space: Int): String {
        return " ".repeat(space)
    }

    private fun isEntitySameSubkey(
        key1: String,
        key2: String
    ): Boolean {
        val lastPeriod = key2.lastIndexOf('.')
        val checkKey = if (lastPeriod > 0) key2.substring(0, lastPeriod) else key2

        return (key1.equals(checkKey, ignoreCase = true))
    }

    private fun getEndingKey(input: String): String {
        val lastPeriod = input.lastIndexOf('.')
        if (lastPeriod < 0) {
            return input
        }

        return input.substring(lastPeriod + 1)
    }

    private fun getParentKey(input: String): String? {
        val lastPeriod = input.lastIndexOf('.')
        if (lastPeriod < 0) {
            return null
        }

        return input.substring(0, lastPeriod)
    }

    private fun getFirstNonCommentLine(input: List<String>): Int {
        for (lineNum in input.indices) {
            val line = input[lineNum].replace("\t", "").trim { it <= ' ' }
            if (line.startsWith("#") || line.isEmpty()) {
                continue
            }
            return lineNum
        }

        return -1
    }

    private fun getMapFromConfig(
        input: List<String>
    ): SortedMap<String, FieldInfo> {
        val configMap: SortedMap<String, FieldInfo> = TreeMap()
        val currentKey = mutableListOf<String>()
        val regexPattern = "^[^':]*:.*"

        for (line in input) {
            val depth = getFieldDepth(line)
            val useLine = line.replace("\t", "").trim { it <= ' ' }
            if (useLine.startsWith("#") || useLine.isEmpty()) {
                continue
            }

            //if (line.contains(":")) {
            if (useLine.matches(regexPattern.toRegex())) {
                val firstColon = useLine.indexOf(':')
                val hasValues = useLine.length > firstColon + 1
                var key: String? = useLine.substring(0, firstColon).replace("\t", "").trim { it <= ' ' }
                val origKey = key

                if (origKey!!.startsWith("-")) {
                    if (currentKey.size > depth) {
                        while (currentKey.size > depth) {
                            currentKey.removeAt(currentKey.size - 1)
                        }
                    }
                    val temp = origKey.substring(1).trim { it <= ' ' }
                    var tempKey: String
                    for (i in 0..99) {
                        tempKey = String.format("%s[%s]", temp, i)
                        val checkKey = getKeyFromList(currentKey, tempKey)
                        if (!configMap.containsKey(checkKey)) {
                            currentKey.add(tempKey)
                            configMap[checkKey] = null
                            break
                        }
                    }
                    continue
                }

                if (depth == 0) {
                    currentKey.clear()
                } else {
                    if (currentKey.size > depth) {
                        while (currentKey.size > depth) {
                            currentKey.removeAt(currentKey.size - 1)
                        }
                    }
                    key = getKeyFromList(currentKey, key)
                }

                if (!hasValues) {
                    currentKey.add(origKey)
                    if (!configMap.containsKey(key)) {
                        configMap[key] = FieldInfo(null, depth)
                    }
                } else {
                    val value = useLine.substring(firstColon + 1).trim { it <= ' ' }
                    val fi = FieldInfo(value, depth)
                    fi.hasValue = true
                    configMap[key] = fi
                }
            } else if (useLine.startsWith("-")) {
                val key = getKeyFromList(currentKey, null)
                val value = useLine.trim { it <= ' ' }.substring(1).trim { it <= ' ' }
                if (configMap.containsKey(key)) {
                    val fi = configMap[key]
                    fi!!.addListValue(value)
                } else {
                    configMap[key] = FieldInfo(value, depth, true)
                }
            }
        }

        return configMap
    }
}