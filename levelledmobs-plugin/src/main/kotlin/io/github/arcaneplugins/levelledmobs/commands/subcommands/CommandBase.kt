package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import java.util.regex.Pattern
import org.bukkit.entity.Player

abstract class CommandBase(val basePermission: String) {
    abstract val description: String

    internal fun createLiteralCommand(name: String): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal(name)
            .requires {
                cmdSender -> cmdSender.sender.hasPermission("$basePermission.$name")
            }
    }

    internal fun createStringArgument(name: String) : RequiredArgumentBuilder<CommandSourceStack, String> {
        return Commands.argument(name, StringArgumentType.word())
            .requires{
                cmdSender -> cmdSender.sender.hasPermission("$basePermission.$name")
            }
    }

    internal fun createNumberArgument(name: String) : RequiredArgumentBuilder<CommandSourceStack, Int> {
        return Commands.argument(name, IntegerArgumentType.integer())
            .requires{
                cmdSender -> cmdSender.sender.hasPermission("$basePermission.$name")
            }
    }

    internal fun createGreedyStringArgument(name: String) : RequiredArgumentBuilder<CommandSourceStack, String> {
        return Commands.argument(name, StringArgumentType.greedyString())
            .requires{
                    cmdSender -> cmdSender.sender.hasPermission("$basePermission.$name")
            }
    }

//    internal fun setCommandPermission(
//        ctx: CommandContext<CommandSourceStack>,
//        argument: LiteralArgumentBuilder<CommandSourceStack>
//    ){
//        val lastNodeName = ctx.nodes.last().node.name
//        argument.requires{
//            cmdSender -> cmdSender.sender.hasPermission("$basePermission.$lastNodeName")
//        }
//    }

    internal fun getPlayerArgument(
        ctx: CommandContext<CommandSourceStack>,
        name: String
    ): Player? {
        try{
            val targetResolver = ctx.getArgument(name, PlayerSelectorArgumentResolver::class.java)
            return targetResolver.resolve(ctx.source).first()
        }
        catch (_: IllegalArgumentException){ return null }
    }

    internal fun getStringArgumentAsBool(
        ctx: CommandContext<CommandSourceStack>,
        name: String
    ): Boolean{
        val result = getStringArgument(ctx, name)
        return name.equals(result, ignoreCase = true)
    }

    internal fun getStringArgument(
        ctx: CommandContext<CommandSourceStack>,
        name: String,
    ): String{
        val result = getStringArgument(ctx, name, null)
        return result ?: ""
    }

    internal fun getStringArgument(
        ctx: CommandContext<CommandSourceStack>,
        name: String,
        defaultValue: String?,
    ): String? {
        try{
            return ctx.getArgument(name, String::class.java)
        }
        catch (_: IllegalArgumentException){}

        return defaultValue
    }

    // taken from:
    // https://stackoverflow.com/questions/2817646/javascript-split-string-on-space-or-on-quotes-to-array
    fun splitStringWithQuotes(myString: String): MutableList<String>{
        val results = mutableListOf<String>()
        val pattern = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"")
        val match = pattern.matcher(myString)
        while (match.find()){
            var temp = match.group(0)
            if (temp.startsWith("\"") && temp.endsWith("\""))
                temp = temp.substring(1, temp.length - 1)
            results.add(temp)
        }

        return results
    }

    internal fun getOptionalResults(
        ctx: CommandContext<CommandSourceStack>,
        names: MutableList<String>
    ): MutableList<String> {
        val results = mutableListOf<String>()

        for (i in 0..<names.size){
            val temp = getStringArgument(ctx, names[i])
            if (temp.isNotEmpty()) results.add(temp)
        }

        return results
    }
}