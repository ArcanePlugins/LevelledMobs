package io.github.arcaneplugins.levelledmobs.misc

import io.github.arcaneplugins.levelledmobs.util.Log
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.SortedMap
import java.util.TreeMap
import java.util.regex.Pattern
import io.github.arcaneplugins.levelledmobs.util.Utils

/**
 * Migrates older yml versions to the latest available
 *
 * @author stumper66
 * @since 2.4.0
 */
object  FileMigrator {
    private fun getFieldDepth(
        line: String
    ): Int {
        var whiteSpace = 0

        for (element in line) {
            if (element != ' ') break

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
            if (isListValue)
                addListValue(value)
            else
                this.simpleValue = value

            this.depth = depth
        }

        val isList: Boolean
            get() = valueList != null

        fun addListValue(value: String?) {
            if (valueList == null)
                valueList = mutableListOf()

            valueList!!.add(value)
        }

        override fun toString(): String {
            if (this.isList) {
                return if (this.valueList == null || valueList!!.isEmpty())
                    super.toString()
                else
                    this.valueList!!.joinToString(",")
            }

            return if (this.simpleValue == null)
                super.toString()
            else
                simpleValue!!
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
        if (list.isEmpty()) return currentKey

        var result = list.joinToString(".")
        if (currentKey != null)
            result += ".$currentKey"

        return result
    }

    fun copyCustomDrops(
        from: File, to: File,
        fileVersion: Int
    ) {
        try {
            val content = StringReplacer(Files.readString(
                from.toPath(),
                StandardCharsets.UTF_8
            ))

            content.replace("overall_chance:", "overall-chance:")
            content.replace("overall_permission:", "overall-permission:")
            val foundFileVersion = "file-version:.*?\\d+".toRegex().find(content.text)
            if (foundFileVersion != null)
                content.replace(foundFileVersion.value, "file-version: 12")

            Files.writeString(
                to.toPath(), content.text, StandardCharsets.UTF_8,
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
        if (section1.lines.size != section2.lines.size)
            return false

        for (i in section1.lines.indices) {
            if (section1.lines[i] != section2.lines[i])
                return false
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
            val line = origline.replace("\t", "").trim()

            if (line.startsWith("# ||  Section")) {
                val foundSectionNumber: Int = extractSectionNumber(line)
                if (foundSectionNumber > 0)
                    sectionNumber = foundSectionNumber

                sectionStartingLine = 0
            }

            if (sectionStartingLine == 0 && sectionNumber > 0 && line.startsWith("# ||||")) {
                sectionStartingLine = i + 2
                foundNonComment = false
            }

            if (line.startsWith("#") || line.isEmpty())
                continue

            if (!foundNonComment) {
                if (sectionStartingLine > 0)
                    sectionStartingLine = i

                foundNonComment = true
            }

            if (depth == 0) {
                if (keySection != null)
                    sections[keyName!!] = keySection

                keySection = KeySectionInfo()
                keySection.lineNumber = i
                keySection.sectionNumber = sectionNumber
                keySection.sectionStartingLine = sectionStartingLine
                keyName = line
            }
            else
                keySection?.lines?.add(origline)
        }

        if (keySection != null)
            sections[keyName!!] = keySection

        return sections
    }

    private fun extractSectionNumber(input: String): Int {
        val p = Pattern.compile("# \\|\\|\\s{2}Section (\\d{2})")
        val m = p.matcher(input)
        if (m.find() && m.groupCount() == 1) {
            var temp = m.group(1)
            if (temp.length > 1 && temp[0] == '0')
                temp = temp.substring(1)

            if (Utils.isInteger(temp))
                return temp.toInt()
        }

        return 0
    }

    fun copyYmlValues(from: File, to: File, oldVersion: Int) {
        val regexPattern = "^[^':]*:.*"
        val isSettings = to.name.equals("settings.yml", ignoreCase = true)
        val isCustomDrops = to.name.equals("customdrops.yml", ignoreCase = true)
        val isMessages = to.name.equals("messages.yml", ignoreCase = true)
        val showMessages = !isMessages
        val processedKeys = mutableListOf<String>()
        val useCustomDrops = "use-custom-item-drops-for-mobs"
        val settingsToRemove = mutableListOf(
            "kill-skip-conditions.nametagged",
            "kill-skip-conditions.tamed",
            "kill-skip-conditions.leashed",
            "kill-skip-conditions.convertingZombieVillager"
        )

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
                    if (line.trim().startsWith("#") || line.trim().isEmpty()) {
                        currentLine++
                        continue
                    }

                    if (line.matches(regexPattern.toRegex())) {
                        val firstColon = line.indexOf(':')
                        val hasValues = line.length > firstColon + 1
                        var key = line.take(firstColon).replace("\t", "").trim()
                        val keyOnly = key
                        var oldKey = key
                        if (isSettings && oldVersion < 32 && key.equals(
                                "async-task-update-period", ignoreCase = true
                            )
                        ) {
                            oldKey = "nametag-auto-update-task-period"
                        }

                        if (depth == 0)
                            currentKey.clear()
                        else if (currentKey.size > depth) {
                            while (currentKey.size > depth) {
                                currentKey.removeAt(currentKey.size - 1)
                            }
                            key = getKeyFromList(currentKey, key)!!
                        }
                        else
                            key = getKeyFromList(currentKey, key)!!

                        if (!hasValues) {
                            currentKey.add(keyOnly)

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
                                        val numofperiodsEnumerated: Int = countPeriods(
                                            enumeratedKey!!
                                        )
                                        if ((enumeratedKey.startsWith((key))
                                                    && (numofperiodsEnumerated == numOfPeriods + 1
                                                    ) && !newConfigMap.containsKey(enumeratedKey))
                                        ) {
                                            val fi = entry.value

                                            if (isSettings && settingsToRemove.contains(enumeratedKey)){
                                                currentLine++
                                                continue
                                            }

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
                            val value = line.substring(firstColon + 1).trim()
                            val fi = oldConfigMap[oldKey]
                            val migratedValue = fi!!.simpleValue

                            if (key.lowercase().startsWith("file-version")) {
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
                                    if (!oldValue!!.startsWith(parentKey))
                                        continue

                                    if (newConfigMap.containsKey(oldValue))
                                        continue

                                    if (!isEntitySameSubkey(parentKey, oldValue))
                                        continue

                                    val fiOld = entry.value
                                    if (fiOld!!.isList) continue

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
                            }
                            else
                                valuesMatched++
                        }
                    } else if (line.trim().startsWith("-")) {
                        val key = getKeyFromList(currentKey, null)
                        val value = line.trim().substring(1).trim()

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
                    val line = newConfigLines[i].trim()

                    if (line.lowercase().startsWith("file-version")) {
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
                    }
                    else
                        newConfigLines.add(oldConfigLines[i])
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
                "&fFile Loader: &8(Migration) &7Keys matched: &b$keysMatched&7, values matched: &b$valuesMatched&7, values updated: &b$valuesUpdated&7."
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
            if (element == '.') count++
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
        val checkKey = if (lastPeriod > 0) key2.take(lastPeriod) else key2

        return (key1.equals(checkKey, ignoreCase = true))
    }

    private fun getEndingKey(input: String): String {
        val lastPeriod = input.lastIndexOf('.')
        if (lastPeriod < 0) return input

        return input.substring(lastPeriod + 1)
    }

    private fun getParentKey(input: String): String? {
        val lastPeriod = input.lastIndexOf('.')
        if (lastPeriod < 0) return null

        return input.take(lastPeriod)
    }

    private fun getFirstNonCommentLine(input: List<String>): Int {
        for (lineNum in input.indices) {
            val line = input[lineNum].replace("\t", "").trim()
            if (line.startsWith("#") || line.isEmpty())
                continue

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
            val useLine = line.replace("\t", "").trim()
            if (useLine.startsWith("#") || useLine.isEmpty())
                continue

            if (useLine.matches(regexPattern.toRegex())) {
                val firstColon = useLine.indexOf(':')
                val hasValues = useLine.length > firstColon + 1
                var key: String? = useLine.take(firstColon).replace("\t", "").trim()
                val origKey = key

                if (origKey!!.startsWith("-")) {
                    if (currentKey.size > depth) {
                        while (currentKey.size > depth) {
                            currentKey.removeAt(currentKey.size - 1)
                        }
                    }
                    val temp = origKey.substring(1).trim()
                    var tempKey: String
                    for (i in 0..99) {
                        tempKey = "$temp[$i]"
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
                    if (!configMap.containsKey(key))
                        configMap[key] = FieldInfo(null, depth)
                } else {
                    val value = useLine.substring(firstColon + 1).trim()
                    val fi = FieldInfo(value, depth)
                    fi.hasValue = true
                    configMap[key] = fi
                }
            } else if (useLine.startsWith("-")) {
                val key = getKeyFromList(currentKey, null)
                val value = useLine.trim().substring(1).trim()
                if (configMap.containsKey(key)) {
                    val fi = configMap[key]
                    fi!!.addListValue(value)
                }
                else
                    configMap[key] = FieldInfo(value, depth, true)
            }
        }

        return configMap
    }
}