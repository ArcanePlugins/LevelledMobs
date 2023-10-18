package io.github.arcaneplugins.levelledmobs.bukkit.config.translations

import de.themoep.minedown.adventure.MineDown
import io.github.arcaneplugins.levelledmobs.bukkit.util.Log.sev
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender

enum class Message/*
                                    This array contains the default messages, which are used if the translation file does not
                                    specify the translated message for whatever reason.
                                    Default values should be identical to those present in the default 'en_US' translation.
                                     */
/*
Path to the message's key in the translation file.
*/(
    val isListType: Boolean,
    val keyPath: MutableList<String>,
    val def: MutableList<String>
) {

    /* constants */
    COMMAND_COMMON_NO_ACCESS_MISSING_PERMISSION(
        true,
        mutableListOf("command", "common", "no-access-missing-permission"),
        mutableListOf(
        "%prefix-severe% You don't have access to that; it requires the permission " +
                "'&b%permission%&7'.")),

    COMMAND_COMMON_ACCESS_NOT_PLAYER(
        true,
        mutableListOf("command", "common", "no-access-not-player"),
        mutableListOf("%prefix-severe% You don't have access to that; it is only accessible to players.")
    ),

    COMMAND_LEVELLEDMOBS_INVALID_SUBCOMMAND(
        true,
        mutableListOf("command", "levelledmobs", "invalid-subcommand"),
        mutableListOf("%prefix-severe% Invalid subcommand '&b%subcommand%&7'.",
            "%prefix-info% Run '&b&n[/lm help](run_command=/lm help)&7' for a list of available commands.")
    ),

    COMMAND_LEVELLEDMOBS_SUBCOMMAND_ABOUT(
        true,
        mutableListOf("command", "levelledmobs", "subcommand", "about"),
        """
        
        &8┌ [LevelledMobs](color=aqua-blue format=bold) &8v%version%
        &8│
        &8│ &7by [ArcanePlugins](color=blue format=underlined open_url=https://github.com/ArcanePlugins) &8•&7 [lokka30](color=gray format=italic open_url=https://github.com/lokka30), [PenalBuffalo](color=gray format=italic open_url=https://github.com/stumper66) & [UltimaOath](color=gray format=italic open_url=https://github.com/Oathkeeper).
        &8│
        &8│ &8Copyright © 2020 - 2023 [LevelledMobs Contributors](color=dark_gray format=italic open_url=https://github.com/ArcanePlugins/LevelledMobs/wiki/Credits).
        &8│ &8LevelledMobs is [GNU AGPL v3 Free Software](color=dark_gray format=italic open_url=https://github.com/ArcanePlugins/LevelledMobs/blob/master/LICENSE.md).
        &8│
        &8│ &7Quick Links: [Resource](color=blue format=underlined open_url=https://spigotmc.org/resources/74304)&8 • [Wiki](color=blue format=underlined open_url=https://github.com/ArcanePlugins/LevelledMobs/Wiki)&8 • [Credits](color=blue format=underlined open_url=https://github.com/ArcanePlugins/LevelledMobs/wiki/Credits)&8 • [Source](color=blue format=underlined open_url=https://github.com/ArcanePlugins/LevelledMobs)
        &8│
        &8└ &7Run [/lm help](color=blue format=underlined run_command=/lm help) &7for help in using LevelledMobs.
        
        """.trimIndent().split("\n").toMutableList()
    ),

    COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_START(
         true,
        mutableListOf("command", "levelledmobs", "subcommand", "confirm", "start"),
        mutableListOf(
            """
        %prefix-info% Confirming action...""")
    ),

    COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_COMPLETE(
        true,
        mutableListOf("command", "levelledmobs", "subcommand", "confirm", "complete"),
        mutableListOf(
            """
        %prefix-info% Action confirmed.""")
    ),
    COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_NONE(
        true,
        mutableListOf("command", "levelledmobs", "subcommand", "confirm", "none"),
        mutableListOf(
            """
        %prefix-warning% You do not have any actions to confirm.""")
    ),

    COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_START(
        true,
        mutableListOf("command", "levelledmobs", "subcommand", "reload", "start"),
        mutableListOf("%prefix-info% Reloading configuration...")
    ),

    COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_COMPLETE_SUCCESS(
        true,
        mutableListOf("command", "levelledmobs", "subcommand", "reload", "complete-success"),
        mutableListOf("%prefix-info% Reload complete.")
    ),

    COMMAND_LEVELLEDMOBS_SUBCOMMAND_RELOAD_COMPLETE_FAILURE(
        true,
        mutableListOf("command", "levelledmobs", "subcommand", "reload", "complete-failure"),
        mutableListOf("%prefix-severe% Reload failed; check console for more information.")
    ),

    COMMAND_LEVELLEDMOBS_SUBCOMMAND_SUMMON_NOT_SUMMONABLE(
        true,
        mutableListOf("command", "levelledmobs", "subcommand", "summon", "not-summonable"),
        mutableListOf("%prefix-info% Summoning a levelled '&b%entity-name%&7'.")
    ),

    GENERIC_LIST_DELIMITER(
        false,
        mutableListOf("generic", "list-delimiter"),
        mutableListOf("&f&lLM:&7")
    ),

    GENERIC_PREFIX_INFO(
      false,
        mutableListOf("generic", "prefix", "info"),
        mutableListOf("&f&lLM:&7")
    ),

    GENERIC_PREFIX_SEVERE(
        false,
        mutableListOf("generic", "prefix", "severe"),
        mutableListOf("&c&lLM:&7")
    ),

    GENERIC_PREFIX_WARNING(
        false,
        mutableListOf("generic", "prefix", "warning"),
        mutableListOf("&e&lLM:&7")
    );

    var declared: MutableList<String>? = null
        get() {
            return if (field == null){
                this.def
            }
            else{
                field
            }
        }

    fun formatMd(messageStr: MutableList<String>, replacements: MutableList<String>): Component{
        var component = Component.empty()

        for (line in 0..<messageStr.size){
            var toParse = messageStr[line]

            if (toParse.isBlank()){
                component = component.append(Component.newline())
                continue
            }

            toParse = toParse
                .replace(
                    "%prefix-info%",
                    Message.GENERIC_PREFIX_INFO.def[0]
                )
                .replace(
                    "%prefix-warning%",
                    GENERIC_PREFIX_WARNING.def[0]
                )
                .replace(
                    "%prefix-severe%",
                    GENERIC_PREFIX_SEVERE.def[0]
                )

            if (replacements.size % 2 == 0) {
                var j = 0
                while (j < replacements.size) {
                    toParse = toParse.replace(replacements[j], replacements[j + 1])
                    j += 2
                }
            } else {
                sev(
                    "Skipping placeholder replacement in message '$messageStr' " +
                            "as an odd number of placeholder parameters were entered.",
                    true
                )
            }

            component = component.append(MineDown.parse(toParse))

            if (line > 0) {
                component = component.append(Component.newline())
            }
        }

        return component
    }

    fun sendTo(
        sender: CommandSender,
        replacements: MutableList<String>
    ){
        val declaredMaybeNull = declared ?: return
        sender.sendMessage(formatMd(declaredMaybeNull, replacements))
    }

    companion object{
        fun joinDelimited(args: Iterable<String?>?): String {
            return java.lang.String.join(GENERIC_LIST_DELIMITER.declared!![0], args)
        }
    }
}