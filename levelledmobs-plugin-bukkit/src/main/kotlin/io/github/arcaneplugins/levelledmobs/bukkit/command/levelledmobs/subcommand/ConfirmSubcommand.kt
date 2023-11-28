package io.github.arcaneplugins.levelledmobs.bukkit.command.levelledmobs.subcommand

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor
import io.github.arcaneplugins.levelledmobs.bukkit.config.translations.Message
import org.bukkit.command.CommandSender

object ConfirmSubcommand {
    val CONFIRMATION_MAP = mutableMapOf<CommandSender, Runnable>()

    fun createInstance(): CommandAPICommand{
        return CommandAPICommand("confirm")
            .withPermission("levelledmobs.command.levelledmobs.confirm")
            .withShortDescription("Confirms a potentially dangerous action from another command.")
            .withFullDescription(
                "Allows users to confirm actions from potentially dangerous " +
                        "LM commands, such as some routines in `/lm routine`."
            )
            .executes(CommandExecutor { sender: CommandSender, args: Array<Any?>? ->
                if (CONFIRMATION_MAP.containsKey(sender)) {
                    Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_START.sendTo(
                        sender, null
                    )
                    CONFIRMATION_MAP[sender]!!.run()
                    CONFIRMATION_MAP.remove(sender)
                    Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_COMPLETE.sendTo(
                        sender, null
                    )
                } else {
                    Message.COMMAND_LEVELLEDMOBS_SUBCOMMAND_CONFIRM_NONE.sendTo(
                        sender, null
                    )
                }
            })
    }
}