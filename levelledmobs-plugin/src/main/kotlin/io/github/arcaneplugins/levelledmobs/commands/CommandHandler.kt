package io.github.arcaneplugins.levelledmobs.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arcaneplugins.levelledmobs.LevelledMobs
import io.github.arcaneplugins.levelledmobs.commands.subcommands.CommandFallback
import io.github.arcaneplugins.levelledmobs.commands.subcommands.DebugSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.InfoSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.KillSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.ReloadSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.RulesSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SpawnerEggCommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SpawnerSubcommand
import io.github.arcaneplugins.levelledmobs.commands.subcommands.SummonSubcommand
import io.github.arcaneplugins.levelledmobs.util.Log
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents

import org.bukkit.Bukkit
import org.bukkit.command.CommandMap

object CommandHandler {
    var hadErrorLoading = false
        private set

    fun load(){
        try {
            LevelledMobs.instance.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
                commands.registrar().register(
                    buildMainCommand(),
                    "Manage the LevelledMobs plugin",
                    mutableListOf("lm", "lvlmobs", "leveledmobs")
                )
            }
        }
        catch (_: NoSuchMethodError) {
            hadErrorLoading = true
            Log.war("The plugin may continue to work with limited commands. Note that this is expected on Spigot servers.")
            loadFallbackCommands()
        }
    }

    private fun buildMainCommand() : LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("levelledmobs")
            .requires{
                cmdSender -> cmdSender.sender.hasPermission("levelledmobs.command.levelledmobs")
            }
            .executes { ctx ->
                MessagesHelper.showMessage(ctx.source.sender, "command.levelledmobs.main-usage")
                return@executes Command.SINGLE_SUCCESS
            }
            .then(DebugSubcommand.buildCommand())
            .then(InfoSubcommand.buildCommand())
            .then(KillSubcommand.buildCommand())
            .then(ReloadSubcommand.buildCommand())
            .then(RulesSubcommand.buildCommand())
            .then(SpawnerEggCommand.buildCommand())
            .then(SpawnerSubcommand.buildCommand())
            .then(SummonSubcommand.buildCommand())
            .build()
    }

    private fun loadFallbackCommands() {
        val fallbackCommands = CommandFallback("levelledmobs")
        val aliases = mutableListOf("levelledmobs", "lm", "lvlmobs", "leveledmobs")
        val fieldCommandMap = Bukkit.getServer()::class.java.getDeclaredField("commandMap")
        fieldCommandMap.trySetAccessible()
        val commandMap = fieldCommandMap.get(Bukkit.getServer()) as CommandMap

        for (alias in aliases){
            commandMap.register(
                alias, LevelledMobs.instance.name, fallbackCommands
            )
        }
    }
}