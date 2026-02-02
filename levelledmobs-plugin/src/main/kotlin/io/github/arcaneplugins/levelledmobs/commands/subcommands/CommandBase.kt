package io.github.arcaneplugins.levelledmobs.commands.subcommands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.arcaneplugins.levelledmobs.commands.MessagesBase
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern
import org.bukkit.entity.Player

abstract class CommandBase(val basePermission: String) : MessagesBase() {
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

    internal fun createPlayerArgument(name: String) : RequiredArgumentBuilder<CommandSourceStack, PlayerSelectorArgumentResolver> {
        return Commands.argument(name, ArgumentTypes.player())
            .requires{
                cmdSender -> cmdSender.sender.hasPermission("$basePermission.$name")
            }
    }

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

    internal fun getIntegerArgument(
        ctx: CommandContext<CommandSourceStack>,
        name: String
    ): Int?{
        try{
            return ctx.getArgument(name, Int::class.java)
        }
        catch (_: IllegalArgumentException){}

        return null
    }

    // taken from:
    // https://stackoverflow.com/questions/2817646/javascript-split-string-on-space-or-on-quotes-to-array
    fun splitStringWithQuotes(
        myString: String,
        preserveQuotes: Boolean,
    ): MutableList<String>{
        val results = mutableListOf<String>()
        val pattern = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"")
        val match = pattern.matcher(myString)
        while (match.find()){
            var temp = match.group(0)
            if (!preserveQuotes && temp.startsWith("\"") && temp.endsWith("\""))
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

    fun createTargetWithConsoleOption(
        name: String,
        target: (ctx: CommandContext<CommandSourceStack>) -> Unit
    ): LiteralCommandNode<CommandSourceStack>{
        return createLiteralCommand(name)
            .then(createStringArgument("option1")
                .suggests { ctx, builder -> buildTargetSuggestions(ctx, builder) }
                .executes { ctx -> target(ctx)
                    return@executes Command.SINGLE_SUCCESS }
                .then(createStringArgument("option2")
                    .suggests { ctx, builder -> buildTargetSuggestions(ctx, builder) }
                    .executes { ctx -> target(ctx)
                        return@executes Command.SINGLE_SUCCESS }))
            .executes { ctx -> target(ctx)
                return@executes Command.SINGLE_SUCCESS
            }
            .build()
    }

    private fun buildTargetSuggestions(
        ctx: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ) : CompletableFuture<Suggestions>{
        var hasConsole = false
        var hasLookingAt = false
        val words = getOptionalResults(ctx, mutableListOf("option1", "option2"))

        for (i in 3..<words.size){
            val word = words[i]
            if (word.startsWith("console", ignoreCase = true))
                hasConsole = true
            else if (word.startsWith("looking-at", ignoreCase = true))
                hasLookingAt = true
        }

        if (!hasConsole) builder.suggest("console")
        if (!hasLookingAt) builder.suggest("looking-at")

        return builder.buildFuture()
    }
}